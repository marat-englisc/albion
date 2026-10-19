package com.marat.albion.data.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.marat.albion.data.content.ContentImporter
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.BookEntity
import com.marat.albion.data.db.BookmarkEntity
import com.marat.albion.data.db.HighlightEntity
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.ReadingPositionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LibraryBook(val book: BookEntity, val progress: Float, val lastRead: Long?)

/**
 * Библиотека: встроенные книги (assets/books и материалы из папки library),
 * книги, добавленные пользователем (TXT/MD/EPUB/PDF), место чтения и закладки.
 * Текст книги хранится JSON-файлом во внутренней памяти приложения.
 */
class LibraryRepository(
    private val context: Context,
    private val db: AlbionDatabase,
    private val json: Json,
    private val dictionary: Dictionary,
) {
    private val dao get() = db.library()
    private val dir = File(context.filesDir, "books").apply { mkdirs() }
    private val cache = ConcurrentHashMap<String, BookContent>()

    fun observeLibrary(): Flow<List<LibraryBook>> =
        combine(dao.observeBooks(), dao.observePositions()) { books, positions ->
            val byId = positions.associateBy { it.bookId }
            books.map { b -> LibraryBook(b, byId[b.id]?.progress ?: 0f, byId[b.id]?.updatedAt) }
                .sortedWith(compareByDescending<LibraryBook> { it.lastRead ?: 0L }.thenBy { it.book.addedAt })
        }

    /** Переносит встроенные книги в библиотеку (повторно — только если файл изменился). */
    suspend fun syncBundled() = withContext(Dispatchers.IO) {
        for (folder in listOf(SAMPLE_DIR, ContentImporter.ROOT + "/library")) {
            val names = runCatching { context.assets.list(folder)?.toList() }.getOrNull().orEmpty()
                .filter { it.endsWith(".txt", true) || it.endsWith(".md", true) || it.endsWith(".epub", true) || it.endsWith(".pdf", true) }
            for (name in names) {
                val id = "asset:$folder/$name"
                val bytes = runCatching { context.assets.open("$folder/$name").use { it.readBytes() } }.getOrNull() ?: continue
                val existing = dao.book(id)
                if (existing != null && existing.sourceSize == bytes.size.toLong()) continue
                runCatching {
                    val content = if (PdfImporter.isPdf(name, bytes)) PdfImporter.parse(context, name, bytes) else BookParser.parse(name, bytes)
                    save(id, "asset", content, bytes.size.toLong(), existing?.addedAt)
                }
            }
        }
    }

    /** Добавляет книгу из файла, выбранного пользователем. */
    suspend fun import(uri: Uri): BookEntity = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "book.txt"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось открыть файл")
        if (bytes.size > MAX_SIZE) error("Файл слишком большой (больше 30 МБ)")
        val content = if (PdfImporter.isPdf(name, bytes)) PdfImporter.parse(context, name, bytes) else BookParser.parse(name, bytes)
        save("user:${UUID.randomUUID()}", "user", content, bytes.size.toLong(), null)
    }

    private suspend fun save(id: String, source: String, content: BookContent, size: Long, addedAt: Long?): BookEntity {
        file(id).writeText(json.encodeToString(BookContent.serializer(), content))
        cache[id] = content
        val entity = BookEntity(
            id = id,
            title = content.title,
            author = content.author,
            level = content.level,
            source = source,
            sourceSize = size,
            chapters = content.chapters.size,
            chars = content.totalChars,
            addedAt = addedAt ?: System.currentTimeMillis(),
        )
        dao.upsertBook(entity)
        return entity
    }

    suspend fun content(id: String): BookContent? = cache[id] ?: withContext(Dispatchers.IO) {
        val f = file(id)
        if (!f.exists()) return@withContext null
        runCatching { json.decodeFromString(BookContent.serializer(), f.readText()) }.getOrNull()?.also { cache[id] = it }
    }

    suspend fun book(id: String) = dao.book(id)

    suspend fun position(id: String): BookPosition? =
        dao.position(id)?.let { BookPosition(it.chapter, it.paragraph, it.offset) }

    suspend fun savePosition(id: String, pos: BookPosition, progress: Float) = dao.upsertPosition(
        ReadingPositionEntity(id, pos.chapter, pos.paragraph, pos.offset, progress.coerceIn(0f, 1f), System.currentTimeMillis())
    )

    fun observeBookmarks(id: String) = dao.observeBookmarks(id)

    suspend fun addBookmark(id: String, pos: BookPosition, snippet: String) = dao.insertBookmark(
        BookmarkEntity(bookId = id, chapter = pos.chapter, paragraph = pos.paragraph, offset = pos.offset, snippet = snippet.take(160), createdAt = System.currentTimeMillis())
    )

    suspend fun deleteBookmark(bookmarkId: Long) = dao.deleteBookmark(bookmarkId)

    fun observeHighlights(id: String) = dao.observeHighlights(id)

    suspend fun addHighlight(id: String, chapter: Int, paragraph: Int, start: Int, end: Int, text: String, note: String = ""): Long =
        dao.insertHighlight(HighlightEntity(bookId = id, chapter = chapter, paragraph = paragraph, start = start, end = end, text = text, note = note.trim(), createdAt = System.currentTimeMillis()))

    suspend fun updateHighlightNote(highlightId: Long, note: String) = dao.updateHighlightNote(highlightId, note.trim())

    suspend fun deleteHighlight(highlightId: Long) = dao.deleteHighlight(highlightId)

    /** Слова из начатых карточек, которые ещё не выучены, — для подсветки в тексте. */
    fun observeStudiedTerms() = db.lookup().observeStudiedTerms()

    suspend fun delete(id: String) {
        dao.deleteBook(id)
        cache.remove(id)
        withContext(Dispatchers.IO) { file(id).delete() }
    }

    /**
     * Справка по вашим карточкам: те же формы слова, что и в словаре
     * (cats → cat, studied → study, went → go, children's → children).
     */
    suspend fun lookup(word: String): List<CardEntity> {
        val candidates = dictionary.candidates(word)
        return if (candidates.isEmpty()) emptyList() else db.lookup().findByTerms(candidates)
    }

    private fun file(id: String) = File(dir, id.replace(Regex("[^A-Za-z0-9_.-]"), "_") + ".json")

    companion object {
        /** Книги, которые всегда входят в приложение. */
        const val SAMPLE_DIR = "books"
        private const val MAX_SIZE = 30L * 1024 * 1024
    }
}
