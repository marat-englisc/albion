package com.marat.albion

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.library.*
import com.marat.albion.data.repo.StudyRepository
import com.marat.albion.data.settings.SettingsRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IterationTwoDataTest {
    private lateinit var db: AlbionDatabase
    private lateinit var settingsScope: CoroutineScope
    private lateinit var preferences: File
    private lateinit var study: StudyRepository
    private lateinit var words: BookWordsRepository
    private val draft = BookWordDraft("letter", "письмо", "A letter arrived.", BookWordSource("book", "Letters", "One"))

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        preferences = File(context.cacheDir, "ux2-${UUID.randomUUID()}.preferences_pb")
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = settingsScope) { preferences })
        study = StudyRepository(db, settings, Json)
        words = BookWordsRepository(db, Json)
    }

    @After fun tearDown() = runBlocking {
        settingsScope.coroutineContext[Job]?.cancelAndJoin()
        db.close(); preferences.delete(); Unit
    }

    @Test fun undoRemovesOnlyTheNewAdditionAndDuplicateCannotBeUndone() = runBlocking {
        val saved = words.add(draft)
        val other = words.add(draft.copy(term = "book", meaning = "книга"))
        assertEquals(draft.source, BookWordsRepository.source(saved.card))
        assertEquals(draft.example, saved.card.example1)
        assertFalse(words.undoAdd(words.add(draft)))
        assertNotNull(db.cards().get(saved.card.id))
        assertTrue(words.undoAdd(saved))
        assertNull(db.cards().get(saved.card.id))
        assertNotNull(db.cards().get(other.card.id))
        assertFalse(words.undoAdd(saved))
    }

    @Test fun undoCannotDeleteReviewHistoryEvenAfterResettingTheSchedule() = runBlocking {
        val saved = words.add(draft)
        study.answer(saved.card, Rating.Easy, 100)
        study.resetCard(saved.card.id)
        assertFalse(words.undoAdd(saved))
        assertEquals(1, db.reviews().countAllSince(0))
        assertNotNull(db.cards().get(saved.card.id))
    }

    @Test fun undoPreservesCardsUsedInPractice() = runBlocking {
        val saved = words.add(draft)
        study.recordGame(saved.card.id, "TYPE_ANSWER", false)
        assertFalse(words.undoAdd(saved))
        assertNotNull(db.cards().get(saved.card.id))
        assertEquals(1, db.cards().gameStat(saved.card.id, "TYPE_ANSWER")!!.wrong)
    }

    @Test fun changedCardAndRecreatedCardRejectAStaleUndoReceipt() = runBlocking {
        val old = words.add(draft)
        db.cards().upsert(listOf(old.card.copy(example1 = "An edited example.")))
        assertFalse(words.undoAdd(old))
        words.remove(old.card.id)
        val fresh = words.add(draft)
        assertFalse(words.undoAdd(old))
        assertNotNull(db.cards().get(fresh.card.id))
        assertTrue(words.undoAdd(fresh))
    }

    @Test fun recommendationsCountCurrentDistinctTermsAndExcludeUnplayableAudio() = runBlocking {
        fun card(id: String, term: String, audio: String? = null) = CardEntity(id, Deck.WORDS.id, 0, term, "", "noun", "A1", "", "", "", "", audio, null)
        db.cards().upsert(listOf(card("a", "Letter", "letter.mp3"), card("b", "letter"), card("c", "book")))
        study.recordGame("a", "TYPE_ANSWER", false)
        study.recordGame("b", "TYPE_ANSWER", false)
        study.recordGame("a", "SHADOWING", false)
        study.recordGame("c", "PRONUNCIATION", false)
        study.recordGame("a", "REMOVED_GAME", false)
        val needs = study.observePracticeNeeds().first()
        assertEquals(setOf("TYPE_ANSWER", "SHADOWING"), needs.map { it.game }.toSet())
        assertTrue(needs.all { it.count == 1 })
        repeat(2) { study.recordGame("a", "TYPE_ANSWER", true); study.recordGame("b", "TYPE_ANSWER", true) }
        assertEquals(listOf("SHADOWING"), study.observePracticeNeeds().first().map { it.game })
    }
}
