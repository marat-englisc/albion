package com.marat.albion

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.BookEntity
import com.marat.albion.data.library.*
import com.marat.albion.data.settings.SettingsRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class LibraryImportTest {
    private lateinit var context: Context
    private lateinit var root: File
    private lateinit var db: AlbionDatabase
    private lateinit var library: LibraryRepository
    private val bookDir get() = File(root, "books")
    private val text = "# A book\nAuthor: Test Author\n\n## Chapter One\n\nA readable paragraph."

    @Before fun setUp() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(base.cacheDir, "book-import-${System.nanoTime()}").apply { mkdirs() }
        context = object : ContextWrapper(base) {
            override fun getFilesDir() = root
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
        }
        db = Room.inMemoryDatabaseBuilder(context, AlbionDatabase::class.java).build()
        library = repository()
        PDFBoxResourceLoader.init(context)
    }

    @After fun tearDown() {
        db.close()
        root.deleteRecursively()
    }

    private fun repository() = LibraryRepository(context, db, Json, Dictionary(context.assets, db, SettingsRepository(context), Json))
    private fun source(name: String, bytes: ByteArray) = File(root, name).apply { writeBytes(bytes) }
    private suspend fun import(file: File) = library.import(Uri.fromFile(file))
    private suspend fun failure(file: File): BookImportException {
        try { import(file) } catch (e: BookImportException) { return e }
        throw AssertionError("Ожидался отказ импорта")
    }

    @Test fun undoAnnotationDeletionRestoresTextNoteAndBookPosition() = runBlocking {
        val book = import(source("undo.md", text.toByteArray()))
        val position = BookPosition(0, 0, 2)
        library.savePosition(book.id, position, .25f)
        library.addBookmark(book.id, position, "readable")
        library.addHighlight(book.id, 0, 0, 2, 10, "readable", "A very important note")
        val bookmark = library.observeBookmarks(book.id).first().single()
        val highlight = library.observeHighlights(book.id).first().single()
        library.deleteBookmark(bookmark.id)
        library.deleteHighlight(highlight.id)
        assertTrue(library.observeBookmarks(book.id).first().isEmpty())
        assertTrue(library.observeHighlights(book.id).first().isEmpty())
        library.restoreBookmark(bookmark)
        library.restoreHighlight(highlight)
        assertEquals(bookmark.copy(id = 0), library.observeBookmarks(book.id).first().single().copy(id = 0))
        assertEquals(highlight.copy(id = 0), library.observeHighlights(book.id).first().single().copy(id = 0))
        assertEquals(position, library.position(book.id))
    }

    @Test fun repeatedImportPreservesBookPositionBookmarksAndNotes() = runBlocking {
        val book = import(source("first.md", text.toByteArray()))
        val position = BookPosition(0, 0, 5)
        library.savePosition(book.id, position, 0.5f)
        library.addBookmark(book.id, position, "A readable paragraph.")
        library.addHighlight(book.id, 0, 0, 0, 1, "A", "My note")
        val again = import(source("renamed.md", text.toByteArray()))
        assertEquals(book, again)
        assertEquals(1, library.observeLibrary().first().size)
        assertEquals(position, library.position(book.id))
        assertEquals(1, library.observeBookmarks(book.id).first().size)
        assertEquals("My note", library.observeHighlights(book.id).first().single().note)
        assertEquals("A readable paragraph.", repository().content(book.id)!!.chapters.single().paragraphs.single())
    }

    @Test fun concurrentImportsDoNotCreateDuplicatesOrCorruptFiles() = runBlocking {
        val file = source("same.md", text.toByteArray())
        val books = (1..2).map { async(Dispatchers.IO) { import(file) } }.awaitAll()
        assertEquals(books[0], books[1])
        assertEquals(1, library.observeLibrary().first().size)
        assertNotNull(repository().content(books[0].id))
    }

    @Test fun emptyAndDamagedFilesLeaveNoBooksOrStoredContent() = runBlocking {
        failure(source("empty.txt", byteArrayOf()))
        failure(source("broken.epub", "Not a zip".toByteArray()))
        assertTrue(library.observeLibrary().first().isEmpty())
        assertTrue(bookDir.list().orEmpty().isEmpty())
    }

    @Test fun databaseFailureRemovesUnpublishedFileAndAllowsRetry() = runBlocking {
        val file = source("book.md", text.toByteArray())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_book BEFORE INSERT ON books BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        failure(file)
        assertTrue(library.observeLibrary().first().isEmpty())
        assertTrue(bookDir.list().orEmpty().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_book")
        val book = import(file)
        assertNotNull(repository().content(book.id))
    }

    @Test fun continuesToReadBooksStoredByPreviousAppVersion() = runBlocking {
        val id = "user:legacy-id"
        val book = BookParser.parse("legacy.md", text.toByteArray())
        File(bookDir, "user_legacy-id.json").writeText(Json.encodeToString(BookContent.serializer(), book))
        db.library().upsertBook(BookEntity(id, book.title, book.author, "", "user", 100, 1, book.totalChars, 100))
        library.savePosition(id, BookPosition(0, 0, 3), 0.3f)
        assertEquals(book, library.content(id))
        assertEquals(BookPosition(0, 0, 3), library.position(id))
        assertEquals(id, import(source("legacy.md", text.toByteArray())).id)
        assertEquals(1, library.observeLibrary().first().size)
    }

    @Test fun reimportRepairsDamagedStoredFileWithoutResettingPosition() = runBlocking {
        val file = source("book.md", text.toByteArray())
        val book = import(file)
        val position = BookPosition(0, 0, 3)
        library.savePosition(book.id, position, 0.3f)
        File(bookDir, BookInput.fingerprint(book.id.toByteArray()) + ".json").writeText("broken json")
        library = repository() // Имитируем запуск без кэша книги.
        assertEquals(book.id, import(file).id)
        assertEquals(position, library.position(book.id))
        assertNotNull(repository().content(book.id))
    }

    @Test fun repairsMissingBundledBookEvenWhenSourceVersionHasNotChanged() = runBlocking {
        library.syncBundled()
        val bundled = library.observeLibrary().first().first { it.book.source == "asset" }.book
        val stored = File(bookDir, BookInput.fingerprint(bundled.id.toByteArray()) + ".json")
        assertTrue(stored.delete())
        library.syncBundled()
        assertTrue(stored.exists())
        assertNotNull(repository().content(bundled.id))
    }

    @Test fun epubWorksWithAndroidXmlParserAndPreservesUnicode() = runBlocking {
        val file = File(root, "book.epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            val files = mapOf(
                "META-INF/container.xml" to "<container><rootfiles><rootfile full-path = 'book.opf'/></rootfiles></container>",
                "book.opf" to "<package><metadata><dc:title>Unicode book</dc:title></metadata><manifest><item id='c' href='A+B.xhtml'/></manifest><spine><itemref idref='c'/></spine></package>",
                "A+B.xhtml" to "<html><body><p>Hello&nbsp;&mdash; &#x1F642;.</p></body></html>",
            )
            files.forEach { (name, value) -> zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray()); zip.closeEntry() }
        }
        val book = import(file)
        assertEquals("Unicode book", book.title)
        assertEquals(listOf("Hello — 🙂."), library.content(book.id)!!.chapters.single().paragraphs)
    }

    private fun pdf(name: String, text: String?, password: Boolean = false): File {
        val file = File(root, name)
        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            if (text != null) PDPageContentStream(doc, page).use {
                it.beginText(); it.setFont(PDType1Font.HELVETICA, 12f); it.newLineAtOffset(50f, 700f); it.showText(text); it.endText()
            }
            if (password) doc.protect(StandardProtectionPolicy("owner", "secret", AccessPermission()))
            doc.save(file)
        }
        return file
    }

    @Test fun importsShortTextPdfInsteadOfMistakingItForAScan() = runBlocking {
        val book = import(pdf("tiny.pdf", "Hello!"))
        assertEquals(listOf("Hello!"), library.content(book.id)!!.chapters.single().paragraphs)
    }

    @Test fun givesClearErrorsForPasswordProtectedScannedAndCorruptPdf() = runBlocking {
        assertTrue(failure(pdf("locked.pdf", "Secret text", password = true)).message!!.contains("паролем"))
        assertTrue(failure(pdf("scan.pdf", null)).message!!.contains("отсканированные"))
        assertTrue(failure(source("corrupt.pdf", "%PDF-1.7\nbroken".toByteArray())).message!!.contains("PDF"))
        assertTrue(library.observeLibrary().first().isEmpty())
    }
}
