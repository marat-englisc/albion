package com.marat.albion

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.library.BookWordSource
import com.marat.albion.ui.library.TranslationSheet
import com.marat.albion.ui.nav.AlbionNavigation
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Регрессии: старт без прокрутки, устойчивые подписанные вкладки, перевод раньше действий. */
@RunWith(AndroidJUnit4::class)
class UsabilityUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val container get() = (instrumentation.targetContext.applicationContext as AlbionApp).container
    private fun navigator() = com.marat.albion.ui.nav.Navigator(
        com.marat.albion.ui.nav.primaryRoutes.associateWith { androidx.navigation3.runtime.NavBackStack<androidx.navigation3.runtime.NavKey>(it) },
        androidx.compose.runtime.mutableIntStateOf(0))

    @Test fun librarySearchAndStableGrammarSearchWorkAcrossTextSizes(): Unit = runBlocking {
        container.importer.importIfNeeded()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (width in listOf(320, 360, 411)) for (scale in listOf(1f, 1.5f, 2f)) {
                val name = "$width-$scale"
                val nav = navigator()
                show(scenario, "library-$name", width, scale, false) { com.marat.albion.ui.library.LibraryScreen(nav) }
                val add = clickableBounds(awaitText("Добавить"))
                assertTrue(add.height() >= 48 * instrumentation.targetContext.resources.displayMetrics.density)
                val field = awaitNode { it.isEditable }
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "no-book-matches-this-phrase")
                }))
                scrollToText("Книги не найдены")
                activate(scrollToText("Показать все книги"))
                awaitText("Статус: Все ▾")
                capture("library-$name")
                show(scenario, "grammar-$name", width, scale, false) { com.marat.albion.ui.grammar.GrammarScreen(nav) }
                val search = awaitNode { it.isEditable && find(it) { child -> child.contentDescription?.toString() == "Найти правило или пример…" } != null }
                val top = bounds(search).top
                assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "articles")
                }))
                awaitNode { it.text?.toString()?.startsWith("Найдено:", ignoreCase = true) == true }
                assertEquals("Search moved after filtering $name", top, bounds(awaitNode { it.isEditable }).top)
                capture("grammar-search-$name")
            }
        }
    }

    @Test fun groupedSettingsHaveAccessiblePreciseControls(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "settings-large", 320, 2f, false) { com.marat.albion.ui.settings.SettingsScreen({}) }
            activate(scrollToText("Чтение и учебник"))
            val increase = scrollToDescription("Увеличить: Текст книги")
            assertTrue(clickableBounds(increase).height() >= 48 * instrumentation.targetContext.resources.displayMetrics.density)
            capture("settings-reading-320-2")
        }
    }

    @Test fun readerHasAccessibleSelectionSearchReturnAndAppearance(): Unit = runBlocking {
        container.importer.importIfNeeded()
        val source = java.io.File(instrumentation.targetContext.cacheDir, "usability-reader.md").apply {
            writeText("# Usability test book\n\n## Chapter One\n\nThe lighthouse watches the quiet sea. A keeper opens the door.\n\n## Chapter Two\n\nTomorrow the keeper will return to the lighthouse.")
        }
        val book = container.library.import(android.net.Uri.fromFile(source))
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                show(scenario, "reader-controls", 360, 1f, false) { com.marat.albion.ui.library.ReaderScreen(book.id, navigator()) }
                val paragraph = awaitNode { it.text?.toString()?.startsWith("The lighthouse watches") == true }
                assertTrue("Paragraph must have an accessible translation action", paragraph.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
                assertTrue(paragraph.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                activate(awaitText("Уточнить выделение"))
                awaitText("Предложение")
                capture("reader-accessible-selection")
                activate(awaitNode { it.contentDescription?.toString() == "Закрыть перевод" })
                activate(awaitNode { it.contentDescription?.toString() == "Меню чтения" })
                activate(awaitText("Поиск и переход к странице"))
                val search = awaitNode { it.isEditable && find(it) { child -> child.contentDescription?.toString() == "Поиск по тексту книги" } != null }
                assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Tomorrow")
                }))
                awaitText("Совпадений: 1")
                activate(awaitNode { it.text?.toString()?.startsWith("Tomorrow the keeper") == true })
                awaitNode { it.text?.toString()?.startsWith("Tomorrow the keeper") == true }
                activate(awaitNode { it.contentDescription?.toString() == "Меню чтения" })
                activate(awaitText("Вернуться к месту чтения"))
                awaitNode { it.text?.toString()?.startsWith("The lighthouse watches") == true }
                activate(awaitNode { it.contentDescription?.toString() == "Меню чтения" })
                activate(awaitText("Оформление страницы"))
                awaitNode { it.contentDescription?.toString() == "Увеличить: Размер текста" }
                capture("reader-appearance")
            }
        } finally { container.library.delete(book.id); source.delete() }
    }

    @Test fun reminderTimeCanBeTypedWithoutUsingTheClock(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "reminder-time-input", 360, instrumentation.targetContext.resources.configuration.fontScale, false) {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().then(
                    androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))) {
                    com.marat.albion.ui.settings.GoalReminderSection(com.marat.albion.data.settings.Settings(reminderEnabled = true))
                }
            }
            activate(scrollToText("Время"))
            awaitNode { it.isEditable }
            val inputs = mutableListOf<AccessibilityNodeInfo>()
            fun collect(node: AccessibilityNodeInfo?) {
                if (node == null) return
                if (node.isVisibleToUser && node.isEditable) inputs += node
                for (i in 0 until node.childCount) collect(node.getChild(i))
            }
            collect(instrumentation.uiAutomation.rootInActiveWindow)
            assertEquals("Both hours and minutes must be visible", 2, inputs.size)
            for ((index, value) in listOf("21", "45").withIndex()) assertTrue(inputs[index].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }))
            awaitNode { it.isEditable && it.text?.toString() == "45" }
            instrumentation.waitForIdleSync(); SystemClock.sleep(350)
            val hourLabelHeight = bounds(awaitText("Часы")).height()
            val minuteLabelHeight = bounds(awaitText("Минуты")).height()
            assertTrue("Hour and minute labels must stay on one line", maxOf(hourLabelHeight, minuteLabelHeight) <= minOf(hourLabelHeight, minuteLabelHeight) * 1.25f)
            capture("reminder-digital-time")
            activate(awaitText("Отмена"))
        }
    }

    @Test fun loadingIntroKeepsItsProgressSeparateAtTwoHundredPercent() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (height in listOf(320, 640)) {
                show(scenario, "intro-$height", 320, 2f, false) {
                    Box(Modifier.height(height.dp)) {
                        com.marat.albion.ui.intro.IntroScreen(com.marat.albion.data.content.ImportStatus.Checking, full = false, onFinished = {})
                    }
                }
                val progress = awaitText("Проверяю материалы")
                assertTrue(bounds(awaitText("Albion")).bottom <= bounds(progress).top)
                capture("intro-320-$height-2")
            }
        }
    }

    private fun scrollToDescription(description: String): AccessibilityNodeInfo {
        repeat(15) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            find(instrumentation.uiAutomation.rootInActiveWindow) { it.contentDescription?.toString() == description }?.let { return it }
            instrumentation.uiAutomation.rootInActiveWindow?.let { root -> find(root) { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }
            SystemClock.sleep(300)
        }
        error("Missing control: $description")
    }

    private fun show(scenario: ActivityScenario<MainActivity>, name: String, width: Int,
                     scale: Float, dark: Boolean, content: @Composable () -> Unit) {
        scenario.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalAppContainer provides container,
                    LocalDensity provides Density(density, scale)) {
                    key(name) {
                        AlbionTheme {
                            Box(Modifier.fillMaxSize().background(Ink.Bg)) {
                                Box(Modifier.width(width.dp).height(640.dp)) { content() }
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(750)
        if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
    }

    private fun awaitText(text: String) = awaitNode { it.text?.toString() == text }

    private fun awaitTab(label: String): AccessibilityNodeInfo {
        awaitText(label)
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.isVisibleToUser && node.text?.toString() == label) matches += node
            for (i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(instrumentation.uiAutomation.rootInActiveWindow)
        return matches.maxBy { bounds(it).top }
    }

    private fun awaitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < until) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Не найден элемент. Экран: ${visibleText(instrumentation.uiAutomation.rootInActiveWindow)}")
    }

    private fun scrollToText(text: String): AccessibilityNodeInfo {
        repeat(15) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            find(root) { it.text?.toString() == text }?.let { return it }
            // В AlertDialog свайп должен начинаться в прокручиваемом тексте, выше кнопок.
            val viewport = bounds(find(root) { it.isScrollable } ?: root ?: error("Нет окна"))
            val downTime = SystemClock.uptimeMillis()
            for (step in 0..15) {
                val y = viewport.top + viewport.height() * (0.82f - 0.4f * step / 15)
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    if (step == 0) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_MOVE,
                    viewport.exactCenterX(), y, 0)
                instrumentation.sendPointerSync(event)
                event.recycle()
                SystemClock.sleep(16)
            }
            SystemClock.sleep(150)
            val up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
                viewport.exactCenterX(), viewport.top + viewport.height() * 0.42f, 0)
            instrumentation.sendPointerSync(up)
            up.recycle()
            instrumentation.waitForIdleSync()
            SystemClock.sleep(450)
        }
        error("Не найдено после прокрутки: $text")
    }

    private fun activate(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable) target = target.parent ?: error("Нет действия нажатия")
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(650)
    }

    private fun bounds(node: AccessibilityNodeInfo) = Rect().also(node::getBoundsInScreen)
    private fun clickableBounds(node: AccessibilityNodeInfo): Rect {
        var target = node
        // Android не помечает уже выбранную вкладку как clickable, но сохраняет её selected-узел.
        while (!target.isClickable && !target.isSelected) target = target.parent ?: error("Нет области нажатия")
        return bounds(target)
    }

    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && predicate(node)) return node
        for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
        return null
    }

    private fun visibleText(node: AccessibilityNodeInfo?): String = buildString {
        if (node == null) return@buildString
        if (node.isVisibleToUser && (node.text != null || node.contentDescription != null)) append(node.text).append(" [").append(node.contentDescription).append("] | ")
        for (i in 0 until node.childCount) append(visibleText(node.getChild(i)))
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureCompact") != "true") return
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "usability-ui").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Нет снимка экрана")
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
