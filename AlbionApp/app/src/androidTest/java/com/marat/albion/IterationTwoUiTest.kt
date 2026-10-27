package com.marat.albion

import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.BookWordsRepository
import com.marat.albion.data.settings.PracticeOptions
import com.marat.albion.ui.components.MeasureNarrow
import com.marat.albion.ui.games.Game
import com.marat.albion.ui.games.GameViewModel
import com.marat.albion.ui.games.Phase
import com.marat.albion.ui.library.ReaderScreen
import com.marat.albion.ui.nav.*
import com.marat.albion.ui.theme.AlbionTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IterationTwoUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container

    @Test fun quickSaveReturnsToTheReaderAndUndoRemovesOnlyThatAddition(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val id = "ux2-${UUID.randomUUID()}"
        val text = "Quillfyx rests."
        val prototype = CardEntity(id, Deck.WORDS.id, 999999, "quillfyx", "/kwɪl/", "noun", "B1", "a quill", "перо", text, "", null, null)
        val source = File(instrumentation.targetContext.cacheDir, "$id.md").apply {
            writeText("# A reading exercise\n\n## A quiet morning\n\n$text")
        }
        val oldIds = c.bookWords.observeTerm(prototype.term).first().map { it.id }.toSet()
        c.database.cards().upsert(listOf(prototype))
        val book = c.library.import(Uri.fromFile(source))
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val nav = Navigator(primaryRoutes.associateWith { NavBackStack<NavKey>(it) }, mutableIntStateOf(2))
                scenario.onActivity { activity -> activity.setContent {
                    CompositionLocalProvider(LocalAppContainer provides c) {
                        AlbionTheme { MeasureNarrow { ReaderScreen(book.id, nav) } }
                    }
                } }
                val paragraph = awaitNode { it.text?.toString() == text }
                val bounds = Rect().also(paragraph::getBoundsInScreen)
                tap(bounds.left + 40f, bounds.exactCenterY())
                awaitNode { it.text?.toString() == "перо" }
                clickText("Сохранить слово")
                awaitNode { it.text?.toString() == "Отменить" }
                val saved = withTimeout(5_000) { c.bookWords.observeTerm(prototype.term).first { cards -> cards.any { it.id !in oldIds } } }
                    .first { it.id !in oldIds }
                assertEquals("перо", saved.meaningRu)
                assertEquals(text, saved.example1)
                assertEquals(book.id, BookWordsRepository.source(saved)!!.bookId)
                assertNull(find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Слово из книги" })
                clickText("Отменить")
                withTimeout(5_000) { c.bookWords.observeTerm(prototype.term).first { cards -> cards.none { it.id == saved.id } } }
                awaitNode { it.text?.toString() == text }
                assertNotNull(c.database.cards().get(prototype.id))
            }
        } finally {
            c.bookWords.observeTerm(prototype.term).first().filter { it.id !in oldIds && BookWordsRepository.source(it)?.bookId == book.id }
                .forEach { c.bookWords.remove(it.id) }
            c.database.cards().delete(listOf(id)); c.library.delete(book.id); source.delete()
        }
    }

    @Test fun targetedPracticeOverridesRememberedParametersAndKeepsOnlyCurrentMistakes(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val id = "ux2-${UUID.randomUUID()}"
        val fixture = CardEntity(id, Deck.IDIOMS.id, 999999, "a quiet test $id", "", "idiom", "B1", "a meaning", "значение", "", "", null, null)
        val old = c.settings.practiceOptions(Game.TYPE_ANSWER.name).first()
        c.database.cards().upsert(listOf(fixture))
        c.study.recordGame(id, Game.TYPE_ANSWER.name, false)
        c.settings.savePracticeOptions(Game.TYPE_ANSWER.name, PracticeOptions(Deck.WORDS.id, 30), markAsLast = false)
        val models = androidx.lifecycle.ViewModelStore()
        try {
            val vm = withContext(Dispatchers.Main) {
                GameViewModel(c, Game.TYPE_ANSWER, Deck.IDIOMS, true, 10).also { models.put(id, it) }
            }
            withTimeout(5_000) { while (withContext(Dispatchers.Main) { vm.phase != Phase.Setup }) delay(20) }
            withContext(Dispatchers.Main) {
                assertEquals(Deck.IDIOMS, vm.deck); assertEquals(10, vm.count); assertTrue(vm.onlyMistakes)
                vm.start()
            }
            withTimeout(5_000) { while (withContext(Dispatchers.Main) { vm.phase == Phase.Loading }) delay(20) }
            withContext(Dispatchers.Main) {
                assertEquals(Phase.Playing, vm.phase)
                assertTrue(vm.items.any { it.id == id })
            }
            for (item in vm.items) assertTrue(c.database.cards().gameStat(item.id, Game.TYPE_ANSWER.name)!!.isMistake)
        } finally {
            withContext(Dispatchers.Main) { models.clear() }
            c.settings.savePracticeOptions(Game.TYPE_ANSWER.name, old, markAsLast = false)
            c.database.openHelper.writableDatabase.execSQL("DELETE FROM game_card_stats WHERE card_id = ?", arrayOf(id))
            c.database.openHelper.writableDatabase.execSQL("DELETE FROM game_answer_log WHERE item_id = ?", arrayOf(id))
            c.database.cards().delete(listOf(id))
        }
    }

    private fun clickText(text: String) {
        val node = awaitNode { it.text?.toString() == text }
        val bounds = Rect().also(node::getBoundsInScreen)
        tap(bounds.exactCenterX(), bounds.exactCenterY())
    }
    private fun tap(x: Float, y: Float) {
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            instrumentation.sendPointerSync(event); event.recycle()
        }
        instrumentation.waitForIdleSync(); SystemClock.sleep(350)
    }
    private fun awaitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 20_000
        while (SystemClock.uptimeMillis() < until) {
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected UI element was not found")
    }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isVisibleToUser && predicate(node)) return node
        for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
        return null
    }
}
