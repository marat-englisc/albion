package com.marat.albion

import android.app.UiAutomation
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.db.BookEntity
import com.marat.albion.data.grammar.GrammarNextStep
import com.marat.albion.data.library.LibraryBook
import com.marat.albion.ui.browse.SearchField
import com.marat.albion.ui.components.*
import com.marat.albion.ui.conversation.CorrectionCard
import com.marat.albion.ui.conversation.ConversationsScreen
import com.marat.albion.ui.deck.DeckStudyMetrics
import com.marat.albion.ui.games.GamesScreen
import com.marat.albion.ui.grammar.GrammarNextCard
import com.marat.albion.ui.library.ContinueReading
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.primaryRoutes
import com.marat.albion.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToInt

/** Actual rendered colors, full-sized lesson text, and working controls in both themes. */
@RunWith(AndroidJUnit4::class)
class ContrastAndHeroUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container
    private var activity: MainActivity? = null
    private val density get() = activity!!.resources.displayMetrics.density

    @Before fun prepareWindow() {
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0)
    }

    @Test fun semanticColorPairsHaveContrastAcrossSurfaces() {
        lateinit var scheme: ColorScheme
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "palette") { scheme = MaterialTheme.colorScheme }
            val report = StringBuilder()
            val surfaces = mapOf("background" to scheme.background, "low" to scheme.surfaceContainerLow,
                "container" to scheme.surfaceContainer, "high" to scheme.surfaceContainerHigh,
                "highest" to scheme.surfaceContainerHighest, "bright" to scheme.surfaceBright,
                "selected" to scheme.primaryContainer, "secondary" to scheme.secondaryContainer)
            for ((name, surface) in surfaces) {
                val ratio = contrast(scheme.controlBorder, surface)
                assertTrue("Functional border on $name: $ratio", ratio >= 3.0)
                report.append("controlBorder / $name: $ratio\n")
                val focus = contrast(scheme.primary, surface)
                assertTrue("Focus on $name: $focus", focus >= 3.0)
            }
            assertTrue("Decorative and functional borders are not separated", scheme.controlBorder != scheme.outlineVariant)
            val correction = contrast(scheme.onTertiaryContainer, scheme.tertiaryContainer)
            assertTrue("Correction text: $correction", correction >= 4.5)
            report.append("correction: $correction\n")
            // Conservative combination: the lightest stop, maximum glow, and crossing grid lines.
            var brightest = Hero.Glow.compositeOver(Hero.GradientStops.first())
            repeat(2) { brightest = Color.White.copy(alpha = .035f).compositeOver(brightest) }
            val hero = contrast(Hero.Content, brightest)
            assertTrue("Hero at its brightest: $hero", hero >= 4.5)
            report.append("hero / conservative brightest background: $hero\n")
            val track = Hero.Deep.copy(alpha = .18f).compositeOver(brightest)
            val segment = contrast(Hero.Content, track)
            assertTrue("Unselected hero segment: $segment", segment >= 4.5)
            report.append("hero segment: $segment\n")
            artifact("color-pairs.txt", report.toString())
        }
    }

    @Test fun renderedHeroMaintainsContrastThroughGlowAndGrid() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "hero-background") {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(gutter)) {
                    BoundVolume(Modifier.fillMaxWidth().semantics { contentDescription = "Акцентная поверхность" }) {
                        Box(Modifier.fillMaxWidth().height(180.dp))
                    }
                }
            }
            val rect = bounds(await { it.contentDescription?.toString() == "Акцентная поверхность" })
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No screenshot")
            val inset = (24 * density).roundToInt()
            var minimum = Double.POSITIVE_INFINITY
            var worst = 0
            // Scan every pixel of the content area; text and rounded corners are intentionally absent.
            for (y in rect.top + inset until rect.bottom - inset) for (x in rect.left + inset until rect.right - inset) {
                val pixel = bitmap.getPixel(x, y)
                val ratio = contrast(Color.White, Color(pixel))
                if (ratio < minimum) { minimum = ratio; worst = pixel }
            }
            bitmap.recycle()
            assertTrue("Rendered hero contrast: $minimum", minimum >= 4.5)
            artifact("rendered-hero.txt", "Minimum content-area contrast: $minimum\nBrightest pixel: ${Integer.toHexString(worst)}\n")
            capture("hero-background")
        }
    }

    @Test fun nextLessonKeepsWholeWordsAndBothActions(): Unit = runBlocking {
        val topic = c.grammar.book().learningOrder.first()
        val step = GrammarNextStep.Step(topic, GrammarNextStep.Kind.Check, "")
        var checked = false
        var read = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "next-lesson") {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState()).padding(horizontal = gutter)) {
                    GrammarNextCard(step, { checked = true }, { read = true })
                }
            }
            val title = text(topic.title)
            assertWholeText(title)
            assertEquals(22.sp, title.layout.layoutInput.style.fontSize)
            for (label in listOf("Следующий шаг · Закрепить", "Урок ${topic.number} · ${topic.level}", topic.title, "Читать урок")) {
                assertEquals("Hero caption must be opaque white: $label", Color.White, text(label).layout.layoutInput.style.color)
            }
            capture("next-lesson")
            click(scrollTo { it.text?.toString() == "Проверить знания" })
            click(scrollTo { it.text?.toString() == "Читать урок" })
            assertTrue(checked)
            assertTrue(read)
        }
    }

    @Test fun captionsStayOpaqueInLibraryDeckPracticeAndConversation(): Unit = runBlocking {
        val book = LibraryBook(BookEntity("contrast-book", "The Keeper of Gull Island", "Alex Morgan", "B1", "asset", 0, 1, 100, 0), .37f, 1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "library-caption") {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState()).padding(gutter)) { ContinueReading(book, Modifier, {}) }
            }
            for (label in listOf("Продолжить", "Alex Morgan")) assertEquals(Color.White, text(label).layout.layoutInput.style.color)
            capture("library-caption")
            show(scenario, "deck-caption") {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState()).padding(gutter)) { BoundVolume { DeckStudyMetrics(1234, 5678) } }
            }
            for (label in listOf("к повторению", "новых сегодня")) assertEquals(Color.White, text(label).layout.layoutInput.style.color)
            capture("deck-caption")
            show(scenario, "practice-caption") { GamesScreen(navigator()) }
            await { it.text?.toString() in listOf("Первое занятие", "Продолжить") }
            val caption = runCatching { text("Первое занятие") }.getOrElse { text("Продолжить") }
            assertEquals(Color.White, caption.layout.layoutInput.style.color)
            capture("practice-caption")
            show(scenario, "conversation-caption") { ConversationsScreen(navigator()) }
            assertEquals(Color.White, text("На устройстве · без интернета").layout.layoutInput.style.color)
            assertEquals(Color.White, text("Пишите или говорите — модель ответит, поправит ошибки и подскажет. Бесплатно, без лимитов и интернета.").layout.layoutInput.style.color)
            capture("conversation-caption")
        }
    }

    @Test fun correctionUsesItsOwnReadableSurface() {
        lateinit var scheme: ColorScheme
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "correction") {
                scheme = MaterialTheme.colorScheme
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(gutter)) {
                    CorrectionCard("I went to work.\nДля прошлого события нужна форма went.",
                        Modifier.fillMaxWidth().semantics { contentDescription = "Исправление фразы" })
                }
            }
            for (label in listOf("Лучше сказать", "I went to work.", "Для прошлого события нужна форма went.")) {
                val sample = text(label)
                assertWholeText(sample)
                assertEquals(scheme.onTertiaryContainer, sample.layout.layoutInput.style.color)
            }
            val rect = bounds(await { it.contentDescription?.toString() == "Исправление фразы" })
            val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
            val background = Color(bitmap.getPixel(rect.left + (8 * density).roundToInt(), rect.centerY()))
            bitmap.recycle()
            assertEquals(scheme.tertiaryContainer.toArgb(), background.toArgb())
            val ratio = contrast(scheme.onTertiaryContainer, background)
            assertTrue(ratio >= 4.5)
            artifact("correction.txt", "Rendered correction text / surface: $ratio\n")
            capture("correction")
        }
    }

    @Test fun controlsKeepVisibleIdleAndFocusedBoundaries() {
        var query by mutableStateOf("")
        var level by mutableIntStateOf(0)
        var chosen by mutableStateOf(false)
        var clicked = false
        lateinit var scheme: ColorScheme
        var clearFocus: () -> Unit = {}
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, "controls") {
                scheme = MaterialTheme.colorScheme
                val focus = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                clearFocus = { focus.clearFocus(); keyboard?.hide() }
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState()).padding(gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.semantics { contentDescription = "Контур поиска" }) { SearchField(query, { query = it }, "Найти слово") }
                    Box(Modifier.semantics { contentDescription = "Контур выбора" }) { OptionPicker("Уровень", listOf("A1", "B1"), level, { level = it }) }
                    GhostButton("Второе действие", { clicked = true })
                    ChoiceRow("Вариант", chosen, { chosen = true })
                }
            }
            assertBorder("Контур поиска", scheme.controlBorder, 1f)
            assertBorder("Контур выбора", scheme.controlBorder, 1f)
            capture("controls-idle")
            tap(await { it.isEditable })
            assertBorder("Контур поиска", scheme.primary, 2f)
            val editor = await { it.isEditable }
            assertTrue(editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "hello")
            }))
            instrumentation.waitForIdleSync()
            assertEquals("hello", query)
            capture("controls-focused")
            click(await { it.contentDescription?.toString() == "Очистить" })
            assertEquals("", query)
            instrumentation.runOnMainSync { clearFocus() }
            assertBorder("Контур поиска", scheme.controlBorder, 1f)
            click(scrollTo { it.contentDescription?.toString() == "Уровень" })
            click(await { it.text?.toString() == "B1" })
            assertEquals(1, level)
            click(scrollTo { it.text?.toString() == "Второе действие" })
            click(scrollTo { it.text?.toString() == "Вариант" })
            assertTrue(clicked)
            assertTrue(chosen)
        }
    }

    private fun navigator() = Navigator(primaryRoutes.associateWith { NavBackStack<NavKey>(it) }, mutableIntStateOf(0))
    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.compositeOver(background).luminance().toDouble()
        val b = background.luminance().toDouble()
        return (maxOf(a, b) + .05) / (minOf(a, b) + .05)
    }
    private fun artifact(name: String, value: String) {
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "contrast-and-hero").apply { mkdirs() }
        File(dir, name).writeText(value)
    }
    private fun assertBorder(label: String, expected: Color, stroke: Float) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300)
        val rect = bounds(await { it.contentDescription?.toString() == label })
        val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
        val pixel = bitmap.getPixel(rect.left + (stroke * density / 2).roundToInt().coerceAtLeast(1), rect.centerY())
        val outer = Color(bitmap.getPixel(rect.left - (4 * density).roundToInt(), rect.centerY()))
        bitmap.recycle()
        assertEquals("Wrong rendered border: $label", expected.toArgb(), pixel)
        assertTrue("Boundary does not contrast with adjacent background", contrast(Color(pixel), outer) >= 3.0)
    }
    private fun scrollTo(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        repeat(20) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            val all = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            all.firstOrNull { it.isVisibleToUser && predicate(it) }?.let { return it }
            val scroll = all.firstOrNull { it.isScrollable && it.actionList.any { a -> a.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD } }
                ?: error("No scroll container")
            assertTrue(scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            SystemClock.sleep(300)
        }
        error("Control not found after scrolling")
    }
    private fun show(scenario: ActivityScenario<MainActivity>, name: String, content: @Composable () -> Unit) {
        scenario.onActivity { current ->
            activity = current
            val dark = InstrumentationRegistry.getArguments().getString("testDark") == "true"
            val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            current.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            current.setContent {
                CompositionLocalProvider(LocalAppContainer provides c, LocalReducedMotion provides true) {
                    key(name) { AlbionTheme(darkTheme = dark) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                    } }
                }
            }
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(700)
    }
    private data class Sample(val text: String, val layout: TextLayoutResult, val bounds: ComposeRect, val heading: Boolean)
    private fun text(value: String): Sample {
        repeat(60) {
            val samples = mutableListOf<Sample>()
            fun visitNode(node: SemanticsNode) {
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.forEach { samples.add(Sample(it.layoutInput.text.text, it, node.boundsInRoot, node.config.contains(SemanticsProperties.Heading))) }
                node.children.forEach(::visitNode)
            }
            fun visitView(view: View) {
                if (view is ViewRootForTest) visitNode(view.semanticsOwner.unmergedRootSemanticsNode)
                if (view is ViewGroup) for (i in 0 until view.childCount) visitView(view.getChildAt(i))
            }
            instrumentation.runOnMainSync { visitView(activity!!.window.decorView) }
            samples.firstOrNull { it.text == value }?.let { return it }
            SystemClock.sleep(100)
        }
        error("Text layout not found: $value")
    }
    private fun assertWholeText(sample: Sample) {
        val result = sample.layout
        for (line in 0 until result.lineCount) {
            assertTrue("Line clipped: ${sample.text}; size=${result.size}; left=${result.getLineLeft(line)}; right=${result.getLineRight(line)}",
                result.getLineLeft(line) >= -1f && result.getLineRight(line) <= result.size.width + 1f)
            assertTrue(result.getLineBottom(line) <= result.size.height + 1f)
            assertFalse(result.isLineEllipsized(line))
            if (line > 0) {
                val offset = result.getLineStart(line)
                assertFalse("Word split: ${sample.text}", offset in 1 until sample.text.length && sample.text[offset - 1].isLetterOrDigit() && sample.text[offset].isLetterOrDigit())
            }
        }
        assertEquals(sample.text.length, result.getLineEnd(result.lineCount - 1, true))
    }
    private fun bounds(node: AccessibilityNodeInfo) = android.graphics.Rect().also(node::getBoundsInScreen)
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (node == null) emptyList()
        else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    private fun await(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        repeat(100) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
            nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull { it.isVisibleToUser && predicate(it) }?.let { return it }
            SystemClock.sleep(100)
        }
        capture("missing-element")
        error("UI element not found")
    }
    private fun click(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable) target = target.parent ?: error("Missing click target")
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(400)
    }
    private fun tap(node: AccessibilityNodeInfo) {
        val rect = bounds(node)
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, rect.exactCenterX(), rect.exactCenterY(), 0)
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        SystemClock.sleep(500)
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureContrast") != "true") return
        SystemClock.sleep(600)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "contrast-and-hero").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
