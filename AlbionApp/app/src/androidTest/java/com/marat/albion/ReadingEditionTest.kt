package com.marat.albion

import android.graphics.Rect
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.BookChapter
import com.marat.albion.data.library.BookContent
import com.marat.albion.ui.components.MeasureNarrow
import com.marat.albion.ui.library.*
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.primaryRoutes
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.Brand
import com.marat.albion.ui.theme.Reading
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Text content and dictionary coordinates must survive the new hanging initial and page headers. */
@RunWith(AndroidJUnit4::class)
class ReadingEditionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = (instrumentation.targetContext.applicationContext as AlbionApp).container

    @Test fun initialAndPaginationKeepEveryCharacterAndWordCoordinate() {
        lateinit var measurer: TextMeasurer
        val ready = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                AlbionTheme {
                    val m = rememberTextMeasurer()
                    SideEffect { measurer = m; ready.countDown() }
                }
            } }
            assertTrue("Text measurer did not become ready", ready.await(10, TimeUnit.SECONDS))
            assertEquals("The Light", readingChapterTitle("Chapter 6. The Light"))
            val paragraph = "Lighthouse keepers watched the sea and waited for the morning. ".repeat(18).trim()
            val book = BookContent("A reading test", chapters = listOf(BookChapter("Chapter 6. The Light", listOf(paragraph))))
            for (fontSize in listOf(18, 27)) for (width in listOf(600, 900)) {
                val body = TextStyle(fontFamily = Reading, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = fontSize.sp, lineHeight = (fontSize * 1.6f).sp)
                val heading = TextStyle(fontFamily = Brand, fontSize = 32.sp, lineHeight = 39.sp)
                val layout = measureEditionParagraph(measurer, AnnotatedString(paragraph), body, width, dropCap = true)
                assertTrue("Expected a real hanging initial", layout.parts.size >= 3)
                val point = layout.initialFrame!!.center
                val offset = layout.offsetAt(point)
                val word = wordAt(paragraph, offset)!!
                assertEquals("Lighthouse", paragraph.substring(word.first, word.last + 1))
                val last = layout.parts.last()
                val restPoint = last.layout.getBoundingBox(0).center + last.origin
                assertTrue(layout.offsetAt(restPoint) >= last.start)
                val pages = Paginator.paginate(book, measurer, width, 1200, body, heading, 24, 50,
                    edition = true, dropCaps = true, headingDecoration = 100)
                assertTrue(pages.size > 1)
                val coverage = IntArray(paragraph.length)
                pages.flatMap { it.blocks }.filter { !it.isHeading }.forEach { block ->
                    for (i in block.start until block.end) coverage[i]++
                }
                paragraph.forEachIndexed { i, char ->
                    if (!char.isWhitespace()) assertEquals("Lost or repeated character at $i", 1, coverage[i])
                }
            }
        }
    }

    @Test fun openingTextReturnsBelowTheInitialWithoutAnEmptyThirdLine() {
        lateinit var measurer: TextMeasurer
        val ready = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                AlbionTheme { val m = rememberTextMeasurer(); SideEffect { measurer = m; ready.countDown() } }
            } }
            assertTrue("Text measurer did not become ready", ready.await(10, TimeUnit.SECONDS))
            val text = "The next day Daniel came back to the island with a box of groceries and a toolbox."
            for (size in listOf(18, 22, 27)) {
                val body = TextStyle(fontFamily = Reading, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = size.sp, lineHeight = (size * 1.6f).sp)
                for (letter in listOf('T', 'L', 'A', 'W', 'J', 'É', 'Д')) {
                    val sample = AnnotatedString(letter + text.drop(1))
                    val layout = measureEditionParagraph(measurer, sample, body, 600, dropCap = true)
                    val full = measurer.measure(sample, body, constraints = androidx.compose.ui.unit.Constraints(maxWidth = 600))
                    val frame = requireNotNull(layout.initialFrame)
                    assertTrue("Ornament must fit above the third line", frame.bottom <= full.getLineTop(2))
                    val rest = layout.parts.last()
                    assertEquals("The next line must return to the left margin", 0f, rest.origin.x, 0f)
                    assertEquals("No blank line below the initial", full.getLineBaseline(2) - full.firstBaseline, rest.origin.y, 1f)
                    assertEquals("Font padding must not extend the paragraph", rest.origin.y + rest.layout.size.height, layout.height.toFloat(), 1f)
                    assertEquals("The whole opening still fits on its page", sample.length, layout.offsetThatFits(layout.height))
                    assertEquals(0, layout.offsetAt(frame.center))
                }
            }
        }
    }

    @Test fun tappingTheInitialOpensTheWholeWordTranslation(): Unit = runBlocking {
        c.importer.importIfNeeded()
        val id = "edition-${UUID.randomUUID()}"
        val term = "Lanterfyx"
        val paragraph = "$term stands beside the window, looking towards the sea. The room is quiet and the evening light falls across the table. A letter waits beside a small cup of tea."
        val prototype = CardEntity(id, Deck.WORDS.id, 999999, term.lowercase(), "", "noun", "B1", "a lamp", "учебный фонарь", paragraph, "", null, null)
        val source = File(instrumentation.targetContext.cacheDir, "$id.md").apply {
            writeText("# The reading room\n\n## Chapter 1. The Letter\n\n$paragraph")
        }
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
                val node = awaitNode { it.text?.toString() == paragraph }
                val bounds = Rect().also(node::getBoundsInScreen)
                val density = instrumentation.targetContext.resources.displayMetrics.density
                tap(bounds.left + 8 * density, bounds.top + 22 * density)
                awaitNode { it.text?.toString() == "учебный фонарь" }
            }
        } finally {
            c.database.cards().delete(listOf(id)); c.library.delete(book.id); source.delete()
        }
    }

    @Test fun stormOpeningHasNormalParagraphSpacing(): Unit = runBlocking {
        val opening = "The next day Daniel came back to the island with a box of groceries and a toolbox."
        val following = "\"Uncle Tom thought you might need some help,\" he said. \"There is a storm coming.\""
        val source = File(instrumentation.targetContext.cacheDir, "initial-${UUID.randomUUID()}.md").apply {
            val earlier = (1..4).joinToString("\n\n") { "## Chapter $it. The Island\n\nThe lighthouse stood above the sea." }
            writeText("# The Keeper of Gull Island\n\n$earlier\n\n## Chapter 5. The Storm\n\n$opening\n\n$following\n\nThe wind was getting stronger. Dark clouds were gathering above the sea.\n\nDaniel looked up at the lighthouse. It had stood on the island for more than a hundred years.")
        }
        val original = c.settings.current()
        val book = c.library.import(Uri.fromFile(source))
        try {
            c.settings.setReaderTextScale(1f)
            c.library.savePosition(book.id, com.marat.albion.data.library.BookPosition(4, 0, 0), 0.5f)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val nav = Navigator(primaryRoutes.associateWith { NavBackStack<NavKey>(it) }, mutableIntStateOf(2))
                scenario.onActivity { activity -> activity.setContent {
                    CompositionLocalProvider(LocalAppContainer provides c) {
                        AlbionTheme { MeasureNarrow { ReaderScreen(book.id, nav) } }
                    }
                } }
                val first = Rect().also(awaitNode { it.text?.toString() == opening }::getBoundsInScreen)
                val second = Rect().also(awaitNode { it.text?.toString() == following }::getBoundsInScreen)
                val density = instrumentation.targetContext.resources.displayMetrics.density
                assertEquals("Normal paragraph gap below the decorated initial", 12f * density, (second.top - first.bottom).toFloat(), density)
                if (InstrumentationRegistry.getArguments().getString("captureEditorial") == "true") {
                    instrumentation.waitForIdleSync()
                    SystemClock.sleep(450)
                    val image = instrumentation.uiAutomation.takeScreenshot()!!
                    val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "initials").apply { mkdirs() }
                    File(directory, "storm-opening.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    image.recycle()
                }
            }
        } finally {
            c.settings.setReaderTextScale(original.readerTextScale)
            c.library.delete(book.id); source.delete()
        }
    }

    private fun tap(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(time, time + 80, MotionEvent.ACTION_UP, x, y, 0)
        instrumentation.sendPointerSync(down); instrumentation.sendPointerSync(up)
        down.recycle(); up.recycle()
    }
    private fun awaitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < until) {
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        error("Expected reader content or translation did not appear")
    }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && predicate(node)) return node
        for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
        return null
    }
}
