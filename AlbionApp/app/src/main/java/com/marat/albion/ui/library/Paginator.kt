package com.marat.albion.ui.library

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import com.marat.albion.data.library.BookContent
import com.marat.albion.data.library.BookPosition

/** Кусок абзаца на странице. paragraph = -1 — заголовок главы. */
data class PageBlock(val chapter: Int, val paragraph: Int, val start: Int, val end: Int) {
    val isHeading get() = paragraph < 0
}

data class Page(val blocks: List<PageBlock>) {
    val start: BookPosition
        get() = blocks.first().let { BookPosition(it.chapter, maxOf(it.paragraph, 0), if (it.isHeading) 0 else it.start) }
}

/**
 * Разбивка книги на страницы под конкретный размер экрана и шрифт.
 * Каждая глава начинается с новой страницы; длинный абзац переносится
 * на следующую страницу по границе строки.
 */
object Paginator {

    fun paginate(
        book: BookContent,
        measurer: TextMeasurer,
        width: Int,
        height: Int,
        body: TextStyle,
        heading: TextStyle,
        paragraphGap: Int,
        headingGap: Int,
        edition: Boolean = false,
        dropCaps: Boolean = false,
        headingDecoration: Int = 0,
    ): List<Page> {
        val pages = ArrayList<Page>()
        val constraints = Constraints(maxWidth = width)

        book.chapters.forEachIndexed { ci, chapter ->
            var blocks = ArrayList<PageBlock>()
            var used = 0

            fun newPage() {
                if (blocks.isNotEmpty()) pages += Page(blocks)
                blocks = ArrayList()
                used = 0
            }

            val titleHeight = measurer.measure(if (edition) readingChapterTitle(chapter.title) else chapter.title, heading,
                constraints = constraints).size.height + headingGap + headingDecoration
            blocks += PageBlock(ci, -1, 0, chapter.title.length)
            used = titleHeight

            chapter.paragraphs.forEachIndexed { pi, paragraph ->
                var start = 0
                while (start < paragraph.length) {
                    val gap = if (blocks.isEmpty() || blocks.last().isHeading) 0 else paragraphGap
                    val remaining = height - used - gap
                    val text = paragraph.substring(start)
                    val layout = measureEditionParagraph(measurer, AnnotatedString(text), body, width, dropCaps && pi == 0 && start == 0)
                    if (layout.height <= remaining) {
                        blocks += PageBlock(ci, pi, start, paragraph.length)
                        used += gap + layout.height
                        break
                    }
                    var fitting = layout.offsetThatFits(remaining)
                    if (fitting <= 0) {
                        if (blocks.isEmpty()) {
                            // Very large fonts still make progress, without repeatedly creating an empty page.
                            fitting = measurer.measure(text, body, constraints = constraints).getLineEnd(0, visibleEnd = false)
                        } else { newPage(); continue }
                    }
                    val split = (start + fitting).coerceAtMost(paragraph.length)
                    if (split <= start) { newPage(); continue }
                    blocks += PageBlock(ci, pi, start, split)
                    newPage()
                    start = split
                    while (start < paragraph.length && paragraph[start] == ' ') start++
                }
            }
            newPage()
        }
        return pages
    }

    /** Страница, на которой находится позиция. */
    fun pageOf(pages: List<Page>, pos: BookPosition): Int {
        var result = 0
        pages.forEachIndexed { i, page -> if (page.start <= pos) result = i else return result }
        return result
    }

    /** Доля прочитанного по символам — не зависит от размера шрифта и экрана. */
    fun progressOf(book: com.marat.albion.data.library.BookContent, pos: BookPosition, isLastPage: Boolean): Float {
        if (isLastPage) return 1f
        val total = book.totalChars
        if (total <= 0L) return 0f
        var before = 0L
        book.chapters.forEachIndexed { ci, ch ->
            if (ci < pos.chapter) before += ch.paragraphs.sumOf { it.length.toLong() }
            else if (ci == pos.chapter) {
                ch.paragraphs.forEachIndexed { pi, p ->
                    if (pi < pos.paragraph) before += p.length else if (pi == pos.paragraph) before += minOf(pos.offset, p.length)
                }
            }
        }
        return (before.toFloat() / total).coerceIn(0f, 1f)
    }
}
