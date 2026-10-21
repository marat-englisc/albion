package com.marat.albion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.ContentImporter
import com.marat.albion.data.content.Deck
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.BookEntity
import com.marat.albion.data.fsrs.CardState
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.library.BookWordDraft
import com.marat.albion.data.library.BookWordSource
import com.marat.albion.data.library.BookWordsRepository
import com.marat.albion.data.repo.StudyRepository
import com.marat.albion.data.settings.NewCardOrder
import com.marat.albion.data.settings.Settings
import com.marat.albion.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookWordsTest {
    private lateinit var db: AlbionDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var original: Settings
    private lateinit var words: BookWordsRepository
    private lateinit var study: StudyRepository
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = System.currentTimeMillis()
    private val source = BookWordSource("test-book", "A book", "Chapter 2")
    private val draft = BookWordDraft("letter", "письмо.", "The letter was on the table.", source)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        settings = SettingsRepository(context)
        original = settings.current()
        settings.setNewPerDay(20)
        settings.setReviewsPerDay(200)
        settings.setNewOrder(NewCardOrder.FILE)
        words = BookWordsRepository(db, Json)
        study = StudyRepository(db, settings, Json)
    }

    @After fun tearDown() = runBlocking {
        settings.setNewPerDay(original.newPerDay)
        settings.setReviewsPerDay(original.reviewsPerDay)
        settings.setNewOrder(original.newOrder)
        db.close()
    }

    @Test fun savedWordUsesTheNormalFsrsQueueAndSchedule() = runBlocking {
        val result = words.add(draft)
        assertTrue(result.added)
        assertNull(db.reviews().state(result.card.id))
        val summary = study.observeSummaries().first().first { it.deck == Deck.BOOK_WORDS }
        assertEquals(1, summary.total)
        assertEquals(1, summary.newAvailable)
        assertEquals(result.card, study.buildQueue(Deck.BOOK_WORDS, now).news.single())
        val (logId, _) = study.answer(result.card, Rating.Easy, 500, now)
        val state = db.reviews().state(result.card.id)!!
        assertEquals(CardState.REVIEW, state.state)
        assertTrue(state.due > now)
        assertEquals(1, state.reps)
        assertEquals(result.card.id, db.reviews().log(logId)!!.cardId)
        assertTrue(study.buildQueue(Deck.BOOK_WORDS, now).news.isEmpty())
        assertEquals(result.card.id, study.buildQueue(Deck.BOOK_WORDS, state.due + 1).reviews.single().id)
    }

    @Test fun duplicatesPreserveContentAndReviewHistoryButAnotherMeaningIsAllowed() = runBlocking {
        val originalCard = words.add(draft).card
        study.answer(originalCard, Rating.Good, 500, now)
        val before = db.reviews().state(originalCard.id)
        val repeated = words.add(draft.copy(term = "  LETTER  ", meaning = "Письмо", example = "Another example.",
            source = source.copy(title = "Another book")))
        assertFalse(repeated.added)
        assertEquals(originalCard, repeated.card)
        assertEquals(before, db.reviews().state(originalCard.id))
        assertEquals(1, db.reviews().countAllSince(0))
        val second = words.add(draft.copy(meaning = "буква"))
        assertTrue(second.added)
        assertNotEquals(originalCard.id, second.card.id)
        assertEquals(2, words.observeTerm("Letter").first().size)
    }

    @Test fun simultaneousSavesAndTypographicApostrophesDoNotCreateDuplicates() = runBlocking {
        val copies = (0 until 8).map { i ->
            async(Dispatchers.Default) {
                words.add(draft.copy(term = if (i % 2 == 0) "someone’s   letter" else "someone's letter"))
            }
        }.awaitAll()
        assertEquals(1, copies.count { it.added })
        assertEquals(1, copies.map { it.card.id }.toSet().size)
        assertEquals(1, db.cards().idsOf(Deck.BOOK_WORDS.id).size)
    }

    @Test fun contentRefreshAndBookDeletionKeepPersonalCardsAndTheirSource() = runBlocking {
        db.library().upsertBook(BookEntity(source.bookId, source.title, "", "B1", "user", 100, 1, 100, now))
        val card = words.add(draft).card
        study.answer(card, Rating.Good, 500, now)
        val before = db.reviews().state(card.id)
        val importer = ContentImporter(context.assets, db, settings, Json)
        importer.importIfNeeded(force = true)
        assertEquals(ImportStatus.Ready, importer.status.value)
        assertTrue(db.cards().idsOf(Deck.WORDS.id).isNotEmpty())
        db.library().deleteBook(source.bookId)
        assertEquals(card, db.cards().get(card.id))
        assertEquals(before, db.reviews().state(card.id))
        assertEquals(source, BookWordsRepository.source(db.cards().get(card.id)!!))
        assertEquals(draft.example, db.cards().get(card.id)!!.example1)
        assertNull(study.irregular(card))
    }

    @Test fun removingAPersonalCardClearsItsScheduleAndAllowsAddingItAgain() = runBlocking {
        val card = words.add(draft).card
        val other = words.add(draft.copy(term = "table", meaning = "стол")).card
        study.answer(card, Rating.Good, 500, now)
        assertTrue(words.remove(card.id))
        assertNull(db.cards().get(card.id))
        assertNull(db.reviews().state(card.id))
        assertEquals(0, db.reviews().countAllSince(0))
        assertEquals(other, db.cards().get(other.id))
        assertFalse(words.remove(card.id))
        val bundled = card.copy(id = "bundled:test", deck = Deck.WORDS.id)
        db.cards().upsert(listOf(bundled))
        assertFalse(words.remove(bundled.id))
        assertEquals(bundled, db.cards().get(bundled.id))
        assertTrue(words.add(draft).added)
        assertNull(db.reviews().state(card.id))
    }
}
