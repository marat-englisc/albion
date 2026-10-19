package com.marat.albion

import com.marat.albion.data.library.BookParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTextTest {

    private fun page(n: Int, vararg body: String) = (listOf("THE SILENT HARBOUR", "") + body.toList() + listOf("", "$n")).joinToString("\n")

    @Test
    fun joinsLinesRemovesHeadersAndHyphens() {
        val pages = listOf(
            page(1, "Chapter 1", "", "The fog came in early that morning and covered the old har-", "bour like a grey blanket over the water.", "Nobody saw the boat.", "Tom walked down to the pier and waited for his father to come back."),
            page(2, "He waited for a long time, but the sea stayed silent and empty.", "", "Chapter 2", "", "The next day began with rain and a strong wind from the west side."),
            page(3, "Mary opened the shop at nine o'clock, as she always did on Mondays."),
            page(4, "Chapter 3", "", "In the evening the boat finally appeared on the horizon near the rocks."),
        )
        val book = BookParser.parsePdfText(pages, "The Silent Harbour")
        assertEquals(listOf("Chapter 1", "Chapter 2", "Chapter 3"), book.chapters.map { it.title })
        val first = book.chapters[0].paragraphs
        assertEquals("The fog came in early that morning and covered the old harbour like a grey blanket over the water.", first[0])
        assertEquals("Nobody saw the boat.", first[1])
        // Абзац продолжается на следующей странице, колонтитул и номер страницы убраны.
        assertTrue(first.toString(), first[2].startsWith("Tom walked") && first[2].endsWith("silent and empty."))
        assertTrue(book.chapters.toString(), book.chapters.flatMap { it.paragraphs }.none { "SILENT HARBOUR" in it || it == "2" })
    }

    @Test
    fun usesOutlineForChapters() {
        val pages = (1..6).map { n -> "Text of page $n goes here and it is long enough to be a typical line of prose." }
        val book = BookParser.parsePdfText(pages, "Book", outline = listOf("Part One" to 1, "Part Two" to 4))
        assertEquals(listOf("Начало", "Part One", "Part Two"), book.chapters.map { it.title })
        assertEquals(3, book.chapters[1].paragraphs.joinToString(" ").split("Text of page").size - 1)
    }

    @Test
    fun withoutStructureSplitsEveryTenPages() {
        val pages = (1..23).map { "Plain page $it with some words that make up a normal line of text in a book." }
        val book = BookParser.parsePdfText(pages, "Book")
        assertEquals(listOf("Страницы 1–10", "Страницы 11–20", "Страницы 21–23"), book.chapters.map { it.title })
    }

    @Test
    fun shortDocumentDropsHeaderPresentOnEveryPage() {
        val pages = listOf(
            listOf("NOTES", "", "First page has a long enough line of text to look like a book.", "And it goes on.", "1"),
            listOf("NOTES", "", "Second page also has a long enough line of text like a real book.", "The end.", "2"),
        ).map { it.joinToString("\n") }
        val text = BookParser.parsePdfText(pages, "Notes").chapters.flatMap { it.paragraphs }.joinToString(" ")
        assertTrue(text, "NOTES" !in text)
    }
}
