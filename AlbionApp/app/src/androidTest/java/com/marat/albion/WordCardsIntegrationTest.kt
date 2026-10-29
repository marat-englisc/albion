package com.marat.albion

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.*
import com.marat.albion.data.fsrs.*
import com.marat.albion.data.library.*
import com.marat.albion.data.repo.StudyRepository
import com.marat.albion.data.settings.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WordCardsIntegrationTest {
    private lateinit var db: AlbionDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var preferences: File
    private lateinit var study: StudyRepository
    private val now = System.currentTimeMillis()
    private fun card(id: String, position: Int) = CardEntity(id, Deck.WORDS.id, position,
        if (position == 1) " ABOUT " else "about", "/əˈbaʊt/", "preposition", "A1",
        "definition $id", "перевод $id", "example $id", "", "about.mp3", null)
    private val first = card("about#0", 0)
    private val second = card("about#1", 1)
    private val third = card("about#2", 2)

    @Before fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        preferences = File(context.cacheDir, "word-cards-${UUID.randomUUID()}.preferences_pb")
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { preferences })
        settings.setNewPerDay(20)
        settings.setReviewsPerDay(200)
        study = StudyRepository(db, settings, Json)
        db.cards().upsert(listOf(first, second, third))
    }

    @After fun tearDown() = runBlocking {
        scope.coroutineContext[Job]?.cancelAndJoin()
        db.close()
        preferences.delete()
        Unit
    }

    @Test fun dictionarySearchFindsRussianAndEnglishInEveryMeaning() = runBlocking {
        assertEquals(3, study.browse(Deck.WORDS, "перевод about#2", 0).first().single().senses.size)
        assertEquals(3, study.browse(Deck.WORDS, "definition about#1", 0).first().single().senses.size)
        assertTrue(study.browse(Deck.WORDS, "нет такого слова", 0).first().isEmpty())
    }

    @Test fun listDetailsQueueAndCountersAgreeOnOneWordWithThreeMeanings() = runBlocking {
        val row = study.browse(Deck.WORDS, "about", 0).first().single()
        assertEquals(3, row.senses.size)
        assertEquals(listOf(first, second, third), row.senses)
        assertEquals(row, study.observeCard(second.id).first())
        val summary = study.observeSummaries().first().first { it.deck == Deck.WORDS }
        assertEquals(1, summary.total)
        assertEquals(1, summary.newAvailable)
        assertEquals(row, study.buildQueue(Deck.WORDS, now).news.single())
        assertEquals(3, study.gamePool(Deck.WORDS, "TYPE_ANSWER", 20).single().senses.size)
        assertEquals(3, db.cards().all().size)
    }

    @Test fun oldProgressContinuesOnAllMeaningsAndAnswerUndoResetPreserveHistory() = runBlocking {
        val older = ReviewStateEntity(first.id, first.deck, CardState.LEARNING, 0, 2.0, 5.0,
            now - Fsrs.DAY, now - 5 * Fsrs.DAY, 2, 0)
        val latest = older.copy(cardId = second.id, state = CardState.REVIEW,
            lastReview = now - 3 * Fsrs.DAY, stability = 10.0, reps = 6)
        db.reviews().upsertState(older)
        db.reviews().upsertState(latest)
        val historical = ReviewLogEntity(cardId = second.id, deck = second.deck, rating = 3,
            reviewedAt = latest.lastReview, stateBefore = CardState.REVIEW, durationMs = 500,
            prevStability = null, prevDifficulty = null, prevDue = null, prevLastReview = null,
            prevStep = null, prevReps = null, prevLapses = null)
        val historicalId = db.reviews().insertLog(historical)
        assertTrue(study.browse(Deck.WORDS, "", 1).first().isEmpty())
        assertEquals(1, study.browse(Deck.WORDS, "", 3).first().size)
        assertEquals(1, study.buildQueue(Deck.WORDS, now).reviews.size)
        assertTrue(study.buildQueue(Deck.WORDS, now).learning.isEmpty())
        val before = study.memory(third.id)
        val (log, next) = study.answer(third, Rating.Easy, 100, now)
        assertEquals(7, next.reps)
        assertEquals(latest, db.reviews().state(second.id))
        assertEquals(next, study.memory(second.id))
        assertEquals(first.id, db.reviews().log(log)!!.cardId)
        assertTrue(study.buildQueue(Deck.WORDS, now + Fsrs.DAY).reviews.isEmpty())
        assertEquals(3, study.undo(log)!!.senses.size)
        assertEquals(before, study.memory(third.id))
        assertEquals(1, db.reviews().countAllSince(0))
        assertEquals(historical.copy(id = historicalId), db.reviews().log(historicalId))
        study.answer(first, Rating.Good, 100, now)
        study.resetCard(second.id)
        assertEquals(CardState.NEW, study.memory(first.id).state)
        assertEquals(1, study.buildQueue(Deck.WORDS, now).news.size)
        assertEquals(2, db.reviews().countAllSince(0))
    }

    @Test fun practiceResolvesAnOldMeaningMistakeOnceWithoutDuplicatingTotals() = runBlocking {
        val game = "TYPE_ANSWER"
        db.cards().upsertGameStat(GameCardStatEntity(second.id, game, 4, 3, 0, now - 1000))
        assertEquals(1, study.observeGameMistakes(Deck.WORDS, game).first())
        assertEquals(3, study.gamePool(Deck.WORDS, game, 20, onlyMistakes = true).single().senses.size)
        study.recordGame(third.id, game, true, now = now)
        assertEquals(1, study.observeGameMistakes(Deck.WORDS, game).first())
        study.recordGame(first.id, game, true, now = now + 1)
        assertEquals(0, study.observeGameMistakes(Deck.WORDS, game).first())
        assertTrue(study.gamePool(Deck.WORDS, game, 20, onlyMistakes = true).isEmpty())
        assertTrue(study.observePracticeNeeds().first().isEmpty())
        val total = db.gameActivity().observeTotals().first().single()
        assertEquals(6, total.correct)
        assertEquals(3, total.wrong)
        assertEquals(4, db.cards().gameStat(second.id, game)!!.correct)
    }

    @Test fun personalMeaningsShareACardAndUndoCannotDeleteAMeaningAfterGroupStudy() = runBlocking {
        val words = BookWordsRepository(db, Json)
        val draft = BookWordDraft("letter", "письмо", "A letter.", BookWordSource("book", "Letters", "One"))
        val saved = words.add(draft)
        study.answer(saved.card, Rating.Easy, 100, now)
        val other = words.add(draft.copy(meaning = "буква", example = "The letter A."))
        assertEquals(2, words.observeTerm("letter").first().single().senses.size)
        assertTrue(words.undoAdd(other))
        val again = words.add(draft.copy(meaning = "буква"))
        study.answer(again.card, Rating.Good, 100, now + 1)
        assertFalse(words.undoAdd(again))
        assertTrue(words.remove(again.card.id))
        assertTrue(words.observeTerm("letter").first().isEmpty())
        assertNull(db.reviews().state(saved.card.id))
        assertEquals(0, db.reviews().countAllSince(0))
    }

    @Test fun irregularMeaningsKeepEveryFormExampleAndNote() = runBlocking {
        fun data(example: String, note: String) = com.marat.albion.data.content.IrregularData(
            listOf(com.marat.albion.data.content.VerbForm("Infinitive",
                listOf(com.marat.albion.data.content.FormVariant("read", "/riːd/", listOf("read.mp3"))),
                listOf(example))), note)
        val one = first.copy(id = "read#0", deck = Deck.IRREGULAR.id, term = "read",
            extra = Json.encodeToString(com.marat.albion.data.content.IrregularData.serializer(), data("I read books.", "First note")))
        val two = one.copy(id = "read#1", position = 1,
            extra = Json.encodeToString(com.marat.albion.data.content.IrregularData.serializer(), data("Read the sign.", "Second note")))
        db.cards().upsert(listOf(one, two))
        val merged = study.irregular(study.observeCard(two.id).first()!!)!!
        assertEquals(listOf("I read books.", "Read the sign."), merged.forms.single().examples)
        assertEquals(1, merged.forms.single().variants.size)
        assertEquals("First note\nSecond note", merged.note)
    }
}
