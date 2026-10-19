package com.marat.albion

import com.marat.albion.data.library.BookParser
import com.marat.albion.data.library.BookPosition
import com.marat.albion.ui.library.sentenceAt
import com.marat.albion.ui.library.wordAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BookParserTest {

    @Test
    fun parsesSampleBook() {
        val f = File("src/main/assets/books/the-keeper-of-gull-island.md")
        val book = BookParser.parse(f.name, f.readBytes())
        assertEquals("The Keeper of Gull Island", book.title)
        assertEquals("Albion Readers", book.author)
        assertEquals("B1", book.level)
        assertEquals(6, book.chapters.size)
        assertTrue(book.chapters.all { it.title.startsWith("Chapter") && it.paragraphs.size >= 5 })
        val words = book.chapters.sumOf { ch -> ch.paragraphs.sumOf { it.split(' ').size } }
        println("Книга: ${book.chapters.size} глав, $words слов")
        assertTrue(words > 2500)
    }

    @Test
    fun parsesPlainTextWithChapterLines() {
        val text = "My Story\n\nCHAPTER 1\n\nFirst line\ncontinues here.\n\nSecond paragraph.\n\nCHAPTER 2\n\nThe end."
        val book = BookParser.parseText(text, "my_story.txt")
        assertEquals("my story", book.title.lowercase())
        assertEquals(2, book.chapters.size)
        assertEquals("First line continues here.", book.chapters[0].paragraphs[0])
    }

    @Test
    fun parsesEpub() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
            put(
                "OEBPS/content.opf",
                """<package><metadata><dc:title>Test &amp; Book</dc:title><dc:creator>Jane Doe</dc:creator></metadata>
                <manifest><item id="c1" href="text/one.xhtml"/><item id="c2" href="text/two.xhtml"/></manifest>
                <spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""",
            )
            put("OEBPS/text/one.xhtml", "<html><body><h1>Chapter One</h1><p>It was a <em>dark</em> night&nbsp;&mdash; cold.</p><p>Second.</p></body></html>")
            put("OEBPS/text/two.xhtml", "<html><body><h2>Chapter Two</h2><p>The end.</p></body></html>")
        }
        val book = BookParser.parse("test.epub", out.toByteArray())
        assertEquals("Test & Book", book.title)
        assertEquals("Jane Doe", book.author)
        assertEquals(listOf("Chapter One", "Chapter Two"), book.chapters.map { it.title })
        assertEquals("It was a dark night — cold.", book.chapters[0].paragraphs[0])
    }

    @Test
    fun epubSkipsFrontMatterAndReadsDivs() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry() }
            put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
            put(
                "content.opf",
                """<package><metadata><dc:title>Novel</dc:title></metadata><manifest>
                <item id="cover" href="cover.xhtml"/><item id="nav" href="nav.xhtml" properties="nav"/>
                <item id="copy" href="copyright.xhtml"/><item id="ch1" href="ch1.xhtml"/><item id="notes" href="notes.xhtml"/>
                </manifest><spine><itemref idref="cover"/><itemref idref="nav"/><itemref idref="copy"/>
                <itemref idref="ch1"/><itemref idref="notes" linear="no"/></spine></package>""",
            )
            put("cover.xhtml", "<html><body><img src=\"c.jpg\"/></body></html>")
            put("nav.xhtml", "<html><body><nav><ol><li>Chapter 1</li></ol></nav></body></html>")
            put("copyright.xhtml", "<html><body><p>Copyright © 2020 Someone. All rights reserved.</p><p>ISBN 978-0-00-000000-0</p></body></html>")
            put("ch1.xhtml", "<html><body><h1>One</h1><div class=\"para\">First paragraph in a div.</div><div><div>Nested second.</div></div></body></html>")
            put("notes.xhtml", "<html><body><p>Footnotes that are not part of the reading order.</p></body></html>")
        }
        val book = BookParser.parse("novel.epub", out.toByteArray())
        assertEquals(listOf("One"), book.chapters.map { it.title })
        assertEquals(listOf("First paragraph in a div.", "Nested second."), book.chapters[0].paragraphs)
    }

    @Test
    fun wordAndSentenceBoundaries() {
        val text = "\"Dear Miss Hart,\" it began. She didn't know him! Then she smiled."
        val word = wordAt(text, text.indexOf("didn't") + 3)!!
        assertEquals("didn't", text.substring(word.first, word.last + 1))
        val quoted = wordAt(text, 1)!!
        assertEquals("Dear", text.substring(quoted.first, quoted.last + 1))
        val sentence = sentenceAt(text, text.indexOf("know"))
        assertEquals("She didn't know him!", text.substring(sentence.first, sentence.last + 1))
        val last = sentenceAt(text, text.indexOf("smiled"))
        assertEquals("Then she smiled.", text.substring(last.first, last.last + 1))
    }

    @Test
    fun positionsAreOrdered() {
        assertTrue(BookPosition(1, 0, 0) > BookPosition(0, 99, 999))
        assertTrue(BookPosition(2, 3, 10) > BookPosition(2, 3, 9))
    }
}
