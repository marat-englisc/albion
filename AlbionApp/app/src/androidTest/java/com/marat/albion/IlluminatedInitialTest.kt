package com.marat.albion

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marat.albion.data.library.BookChapter
import com.marat.albion.data.library.BookContent
import com.marat.albion.ui.library.Paginator
import com.marat.albion.ui.library.measureEditionParagraph
import com.marat.albion.ui.library.readingChapterTitle
import com.marat.albion.ui.library.wordAt
import com.marat.albion.ui.theme.AlbionTheme
import com.marat.albion.ui.theme.ReadingStyle
import com.marat.albion.ui.theme.Typography
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real bundled fonts: ornament, pagination and dictionary offsets share the same measurement. */
@RunWith(AndroidJUnit4::class)
class IlluminatedInitialTest {
    @Test fun openingLettersRetainAllTextAndRemainDictionaryTargets() = withMetrics { measurer, density ->
        val samples = listOf(
            "The keeper watched the harbour. ".repeat(12) to "The",
            "“When the bell rang, the keeper opened the door.” ".repeat(9) to "When",
            "E\u0301mile looked out across the quiet sea. ".repeat(10) to "E\u0301mile",
            "Winding paths led through the ancient gardens. ".repeat(10) to "Winding",
            "I" to "I", "The sea." to "The",
        )
        for (width in listOf(240, 308, 520)) for ((sample, firstWord) in samples) {
            val pixels = with(density) { width.dp.roundToPx() }
            val layout = measureEditionParagraph(measurer, AnnotatedString(sample), ReadingStyle.copy(color = Color.Black), pixels, true)
            val frame = layout.initialFrame ?: run {
                val normal = measurer.measure(sample, ReadingStyle, constraints = androidx.compose.ui.unit.Constraints(maxWidth = pixels))
                val capital = measurer.measure(sample.take(1), ReadingStyle.copy(fontFamily = com.marat.albion.ui.theme.AlbionFonts.Display,
                    fontSize = ReadingStyle.fontSize * 3.2f, lineHeight = ReadingStyle.fontSize * 3.6f,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)), softWrap = false, maxLines = 1)
                error("Missing initial: width=$width dp/$pixels px, line=${normal.getLineBottom(0) - normal.getLineTop(0)}, capital=${capital.size}, sample=$sample")
            }
            assertEquals(sample, layout.parts.joinToString("") { it.layout.layoutInput.text.text })
            assertEquals(sample.indexOfFirst(Char::isLetter), layout.offsetAt(frame.center))
            assertEquals(wordAt(sample, sample.indexOfFirst(Char::isLetter)), wordAt(sample, layout.offsetAt(frame.center)))
            val selectedWord = wordAt(sample, layout.offsetAt(frame.center)) ?: error("Initial is not a word target")
            assertEquals(firstWord, sample.substring(selectedWord))
            assertEquals("Initial must stay whole at a page break", 0, layout.offsetThatFits(frame.bottom.toInt() - 1))
            assertEquals(sample.length, layout.offsetThatFits(layout.height))
            for ((index, part) in layout.parts.withIndex()) {
                val rect = Rect(part.origin, Size(part.layout.size.width.toFloat(), part.layout.size.height.toFloat()))
                assertTrue("Part exceeds paragraph width", rect.left >= 0 && rect.right <= pixels + 1f)
                assertTrue("Part exceeds paragraph height", rect.top >= 0 && rect.bottom <= layout.height + 1f)
                if (index == 1) assertTrue("Opening text overlaps the initial", rect.left > frame.right)
                if (index == 2) assertTrue("Full-width text overlaps the initial", rect.top >= frame.bottom)
            }
        }
    }

    @Test fun pageBreaksKeepEveryWordAndStayInsideTheReadingArea() = withMetrics { measurer, density ->
        val paragraph = "The keeper followed the winding path towards the old lighthouse, watching the pale sea beyond the harbour. ".repeat(12)
        val book = BookContent("An English morning", chapters = listOf(
            BookChapter("Chapter 1. The Harbour", listOf(paragraph, "A second paragraph. ".repeat(15))),
            BookChapter("Chapter 2. The Bell", listOf("“When the bell rang, the keeper opened the door.” ".repeat(18))),
        ))
        for (width in listOf(240, 308, 520)) for (scale in listOf(.8f, 1f, 1.2f)) {
            val body = ReadingStyle.copy(fontSize = ReadingStyle.fontSize * scale, lineHeight = ReadingStyle.lineHeight * scale)
            val heading = Typography.headlineSmall
            val w = with(density) { width.dp.roundToPx() }
            val h = with(density) { 340.dp.roundToPx() }
            val gap = with(density) { 12.dp.roundToPx() }
            val headingGap = with(density) { 24.dp.roundToPx() }
            val decoration = with(density) { 42.dp.roundToPx() }
            val pages = Paginator.paginate(book, measurer, w, h, body, heading, gap, headingGap,
                edition = true, dropCaps = true, headingDecoration = decoration)
            book.chapters.forEachIndexed { chapterIndex, chapter ->
                chapter.paragraphs.forEachIndexed { paragraphIndex, original ->
                    val recovered = pages.flatMap { it.blocks }.filter {
                        it.chapter == chapterIndex && it.paragraph == paragraphIndex
                    }.joinToString("") { original.substring(it.start, it.end) }
                    assertEquals("Text lost or repeated at $width dp / $scale", original.filterNot(Char::isWhitespace), recovered.filterNot(Char::isWhitespace))
                }
            }
            for (page in pages) {
                var used = 0
                page.blocks.forEachIndexed { index, block ->
                    val chapter = book.chapters[block.chapter]
                    if (block.isHeading) {
                        used += measurer.measure(readingChapterTitle(chapter.title), heading,
                            constraints = androidx.compose.ui.unit.Constraints(maxWidth = w)).size.height + headingGap + decoration
                    } else {
                        if (index > 0 && !page.blocks[index - 1].isHeading) used += gap
                        used += measureEditionParagraph(measurer, AnnotatedString(chapter.paragraphs[block.paragraph].substring(block.start, block.end)),
                            body, w, block.paragraph == 0 && block.start == 0).height
                    }
                }
                assertTrue("Page overflows at $width dp / $scale: $used > $h", used <= h + 1)
            }
        }
    }

    @Test fun narrowAndNonLetterOpeningsKeepTheOrdinaryLayout() = withMetrics { measurer, density ->
        for ((sample, width, enabled) in listOf(
            Triple("The keeper watched the sea.", 120, true),
            Triple("The keeper watched the sea.", 308, false),
            Triple("1842 was a quiet year.", 308, true),
            Triple("", 308, true),
        )) {
            val layout = measureEditionParagraph(measurer, AnnotatedString(sample), ReadingStyle,
                with(density) { width.dp.roundToPx() }, enabled)
            assertNull(layout.initialFrame)
            assertEquals(sample, layout.parts.single().layout.layoutInput.text.text)
        }
    }

    private fun withMetrics(check: (TextMeasurer, Density) -> Unit) {
        lateinit var measurer: TextMeasurer
        lateinit var metrics: Density
        val ready = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    AlbionTheme {
                        val textMeasurer = rememberTextMeasurer(cacheSize = 64)
                        val fixed = LocalDensity.current
                        SideEffect { measurer = textMeasurer; metrics = fixed; ready.countDown() }
                    }
                }
            } }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            scenario.onActivity { check(measurer, metrics) }
        }
    }
}
