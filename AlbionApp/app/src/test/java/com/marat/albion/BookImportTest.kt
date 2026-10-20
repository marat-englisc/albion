package com.marat.albion

import com.marat.albion.data.library.BookImportException
import com.marat.albion.data.library.BookInput
import com.marat.albion.data.library.BookParser
import com.marat.albion.data.library.EpubLimits
import com.marat.albion.data.library.EpubReader
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BookImportTest {
    private fun zip(files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (name, bytes) ->
            z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry()
        } }
        return out.toByteArray()
    }

    private fun epub(
        body: String = "<html><body><h1>One</h1><p>A short readable paragraph.</p></body></html>",
        href: String = "chapter.xhtml", path: String = "OEBPS/chapter.xhtml",
        container: String = "<container><rootfiles><rootfile full-path = 'OEBPS/content.opf'/></rootfiles></container>",
        metadata: String = "<dc:title>Book</dc:title><dc:creator>Author</dc:creator>",
        bodyBytes: ByteArray = body.toByteArray(),
        extra: Map<String, ByteArray> = emptyMap(),
    ): ByteArray = zip(linkedMapOf(
        "META-INF/container.xml" to container.toByteArray(),
        "OEBPS/content.opf" to """<opf:package xmlns:opf='http://www.idpf.org/2007/opf'>
            <opf:metadata>$metadata</opf:metadata><opf:manifest>
            <opf:item id = 'c' href = '$href' media-type = 'application/xhtml+xml'/>
            </opf:manifest><opf:spine><opf:itemref idref = 'c'/></opf:spine></opf:package>""".toByteArray(),
        path to bodyBytes,
    ) + extra)

    private fun paragraphs(bytes: ByteArray) = BookParser.parseEpub(bytes).chapters.flatMap { it.paragraphs }

    @Test fun acceptsSingleQuotesWhitespaceAndNamespacePrefixes() {
        val book = BookParser.parseEpub(epub(
            body = "<x:html xmlns:x='http://www.w3.org/1999/xhtml'><x:body><x:h1>Chapter</x:h1><x:p>One &amp; two.</x:p></x:body></x:html>",
            metadata = "<d:title xmlns:d='http://purl.org/dc/elements/1.1/'>Test &amp; Book</d:title><d:creator>Jane</d:creator><d:creator>John</d:creator>",
        ))
        assertEquals("Test & Book", book.title)
        assertEquals("Jane, John", book.author)
        assertEquals(listOf("One & two."), book.chapters.single().paragraphs)
    }

    @Test fun resolvesRelativeAndEncodedPathsWithoutChangingPlusSigns() {
        assertEquals(listOf("A short readable paragraph."), paragraphs(epub(
            href = "Text/../Text/A+B%20C.xhtml#part", path = "OEBPS/Text/A+B C.xhtml",
        )))
    }

    @Test fun readsXmlDeclaredEncoding() {
        val chapter = "<?xml version='1.0' encoding='ISO-8859-1'?><html><body><p>A café.</p></body></html>"
        assertEquals(listOf("A café."), paragraphs(epub(bodyBytes = chapter.toByteArray(Charsets.ISO_8859_1))))
    }

    @Test fun preservesMixedDivAndParagraphTextWithoutDuplication() {
        val body = "<html><head><title>Ignore</title></head><body><h1>One</h1><div>Before<p>Nested <em>paragraph</em>.</p>After<div>Last.</div></div></body></html>"
        assertEquals(listOf("Before", "Nested paragraph.", "After", "Last."), paragraphs(epub(body)))
    }

    @Test fun preservesShortChaptersWithoutHeadingsAndSkipsNonReadingText() {
        val body = "<html><body><script>bad()</script><style>p { color:red }</style><nav>Contents</nav><p hidden=''>Hidden</p><p>Hi.</p></body></html>"
        assertEquals(listOf("Hi."), paragraphs(epub(body)))
    }

    @Test fun retainsInlineWordsEntitiesAndLineBreaks() {
        val body = "<html><body><p>Don<em>'t</em>&nbsp;stop.<br/>Next &ldquo;word&rdquo; &#x1F642;.</p></body></html>"
        assertEquals(listOf("Don't stop. Next “word” 🙂."), paragraphs(epub(body)))
    }

    @Test fun splitsMultipleChaptersInsideOneXhtmlFile() {
        val book = BookParser.parseEpub(epub("<html><body><h1>First</h1><p>One.</p><h1>Second</h1><p>Two.</p></body></html>"))
        assertEquals(listOf("First", "Second"), book.chapters.map { it.title })
        assertEquals(listOf(listOf("One."), listOf("Two.")), book.chapters.map { it.paragraphs })
    }

    @Test fun missingChapterIsAnErrorInsteadOfSilentPartialImport() {
        assertTrue(assertThrows(BookImportException::class.java) {
            BookParser.parseEpub(epub(path = "wrong.xhtml"))
        }.message!!.contains("отсутствует глава"))
    }

    @Test fun rejectsCorruptArchivesAndMalformedXml() {
        assertThrows(BookImportException::class.java) { BookParser.parseEpub("not a zip".toByteArray()) }
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(epub("<html><body><p>Unclosed</body></html>")) }
        val bytes = epub()
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(bytes.copyOf(bytes.size / 2)) }
    }

    @Test fun rejectsTraversalAndDuplicateNormalizedPaths() {
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(epub(href = "../../outside.xhtml")) }
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(epub(extra = mapOf("OEBPS/./chapter.xhtml" to "duplicate".toByteArray()))) }
    }

    @Test fun rejectsDeclaredEntitiesAndExcessiveXmlDepth() {
        val container = "<!DOCTYPE container [<!ENTITY x SYSTEM 'file:///never-read'>]><container><rootfiles><rootfile full-path='OEBPS/content.opf'/></rootfiles></container>"
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(epub(container = container)) }
        assertThrows(BookImportException::class.java) {
            BookParser.parseEpub(epub("<html><body>" + "<div>".repeat(140) + "Text" + "</div>".repeat(140) + "</body></html>"))
        }
    }

    @Test fun acceptsLegacyDoctypeWithoutFetchingExternalDtd() {
        val body = "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'https://example.invalid/never-fetch.dtd'><html><body><p>Still readable.</p></body></html>"
        assertEquals(listOf("Still readable."), paragraphs(epub(body)))
    }

    @Test fun limitsExpandedEntriesIncludingIgnoredImages() {
        assertThrows(BookImportException::class.java) { EpubReader.read(epub(), "test.epub", EpubLimits(entryBytes = 16)) }
        assertThrows(BookImportException::class.java) { EpubReader.read(epub(), "test.epub", EpubLimits(retainedBytes = 32)) }
        assertThrows(BookImportException::class.java) { EpubReader.read(epub(), "test.epub", EpubLimits(entries = 2)) }
        val bomb = epub(extra = mapOf("huge.png" to ByteArray(100_000)))
        assertThrows(BookImportException::class.java) { EpubReader.read(bomb, "test.epub", EpubLimits(unpackedBytes = 2048)) }
    }

    @Test fun boundedReadStopsAfterOneExtraByteForUnknownLengthStreams() {
        var consumed = 0
        val stream = object : InputStream() {
            override fun read(): Int { consumed++; return 65 }
            override fun read(bytes: ByteArray, off: Int, len: Int): Int { consumed += len; bytes.fill(65, off, off + len); return len }
        }
        assertThrows(BookImportException::class.java) { BookInput.read(stream, limit = 64) }
        assertEquals(65, consumed)
        assertArrayEquals(ByteArray(64), BookInput.read(ByteArray(64).inputStream(), limit = 64))
    }

    @Test fun preservesSingleShortParagraphAndNormalizesLineEndings() {
        assertEquals(listOf("Hello."), BookParser.parse("short.txt", "Hello.".toByteArray()).chapters.single().paragraphs)
        assertEquals(listOf("First line continues.", "Second."), BookParser.parseText("First line\rcontinues.\r\rSecond.").chapters.single().paragraphs)
    }

    @Test fun supportsUtf8AndUtf16WithBom() {
        val text = "# A book\n\nHello, мир."
        for (bytes in listOf(
            text.toByteArray(), "\uFEFF$text".toByteArray(),
            byteArrayOf(0xff.toByte(), 0xfe.toByte()) + text.toByteArray(Charsets.UTF_16LE),
            byteArrayOf(0xfe.toByte(), 0xff.toByte()) + text.toByteArray(Charsets.UTF_16BE),
        )) assertEquals(listOf("Hello, мир."), BookParser.parse("book.txt", bytes).chapters.single().paragraphs)
    }

    @Test fun rejectsEmptyBinaryUnsupportedAndOversizedText() {
        assertThrows(BookImportException::class.java) { BookParser.parse("empty.txt", byteArrayOf()) }
        assertThrows(BookImportException::class.java) { BookParser.parseText(" \n\t\n") }
        assertThrows(BookImportException::class.java) { BookParser.parse("bad.txt", byteArrayOf(0, 1, 2, 3)) }
        assertThrows(BookImportException::class.java) { BookParser.parse("bad.txt", byteArrayOf(0xff.toByte())) }
        assertThrows(BookImportException::class.java) { BookParser.parse("book.doc", "Not a supported document".toByteArray()) }
        assertThrows(BookImportException::class.java) { BookParser.parseText("a".repeat(BookInput.MAX_TEXT_CHARS + 1)) }
    }

    @Test fun fingerprintsDetectChangesEvenWhenSourceSizeIsUnchanged() {
        assertNotEquals(BookInput.fingerprint("First".toByteArray()), BookInput.fingerprint("Other".toByteArray()))
        assertEquals(BookInput.fingerprint("First".toByteArray()), BookInput.fingerprint("First".toByteArray()))
    }

    @Test fun doesNotMistakeRealChaptersForCopyrightOrTitlePages() {
        val body = "<html><body><h1>Contents of a letter</h1><p>She checked the ISBN on the back of the book.</p></body></html>"
        assertEquals(listOf("She checked the ISBN on the back of the book."), paragraphs(epub(
            body = body, href = "title-of-letter.xhtml", path = "OEBPS/title-of-letter.xhtml",
        )))
    }

    @Test fun rejectsInvalidUnicodeCharacterReferences() {
        assertThrows(BookImportException::class.java) { paragraphs(epub("<html><body><p>&#x110000;</p></body></html>")) }
        assertThrows(BookImportException::class.java) { paragraphs(epub("<html><body><p>&#xD800;</p></body></html>")) }
    }

    @Test fun limitsMetadataAndDoesNotMistakeTextForAZipSignature() {
        assertThrows(BookImportException::class.java) { BookParser.parseEpub(epub(metadata = "<dc:title>" + "x".repeat(8193) + "</dc:title>")) }
        assertEquals(listOf("PK is a character in the story."), BookParser.parse("pk.txt", "PK is a character in the story.".toByteArray()).chapters.single().paragraphs)
    }
}
