package com.marat.albion.data.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.marat.albion.data.content.ContentImporter
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.BookEntity
import com.marat.albion.data.db.BookmarkEntity
import com.marat.albion.data.db.HighlightEntity
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.db.ReadingPositionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections
import java.io.IOException

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
    private val cache = Collections.synchronizedMap(object : LinkedHashMap<String, BookContent>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BookContent>?) = size > 2
    })
    private val writes = Mutex()

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
                try {
                    currentCoroutineContext().ensureActive()
                    val bytes = context.assets.open("$folder/$name").use { BookInput.read(it) }
                    val version = "${BookInput.VERSION}:${BookInput.fingerprint(bytes)}"
                    val existing = dao.book(id)
                    if (existing != null && storedFile(id).exists() && versionFile(id).takeIf { it.exists() }?.readText() == version) continue
                    val content = if (PdfImporter.isPdf(name, bytes)) PdfImporter.parse(context, name, bytes) else BookParser.parse(name, bytes)
                    save(id, "asset", content, bytes.size.toLong(), existing?.addedAt)
                    writeAtomic(versionFile(id), version.toByteArray())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("Albion", "Не удалось обновить встроенную книгу: $name", e)
                }
            }
        }
    }

    /** Добавляет книгу из файла, выбранного пользователем. */
    suspend fun import(uri: Uri): BookEntity = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "book.txt" }
            var size: Long? = null
            val cursor = try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            } catch (_: IllegalArgumentException) { null } catch (_: UnsupportedOperationException) { null }
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !c.isNull(it) }?.let { name = c.getString(it) }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { size = c.getLong(it) }
                }
            }
            if (size != null && size > BookInput.MAX_FILE_BYTES) throw BookImportException("Файл слишком большой: поддерживаются книги до 30 МБ")
            val bytes = resolver.openInputStream(uri)?.use { BookInput.read(it) } ?: throw BookImportException("Не удалось открыть файл книги")
            if (bytes.isEmpty()) throw BookImportException("Файл книги пуст")
            currentCoroutineContext().ensureActive()
            // Содержание определяет идентификатор: повторный импорт сохраняет место и заметки.
            val id = "user:${BookInput.fingerprint(bytes)}"
            dao.book(id)?.let { if (storedFile(id).exists() && content(id) != null) return@withContext it }
            val content = if (PdfImporter.isPdf(name, bytes)) PdfImporter.parse(context, name, bytes) else BookParser.parse(name, bytes)
            save(id, "user", content, bytes.size.toLong(), null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BookImportException) {
            throw e
        } catch (e: SecurityException) {
            throw BookImportException("Нет доступа к файлу. Выберите книгу заново через «Добавить книгу»", e)
        } catch (e: IOException) {
            throw BookImportException("Не удалось прочитать или сохранить книгу. Проверьте доступность файла и свободное место", e)
        } catch (e: Exception) {
            throw BookImportException("Не удалось импортировать книгу. Проверьте файл и свободное место на устройстве", e)
        }
    }

    private suspend fun save(id: String, source: String, content: BookContent, size: Long, addedAt: Long?): BookEntity = writes.withLock {
        currentCoroutineContext().ensureActive()
        val existing = dao.book(id)
        if (source == "user") {
            if (existing != null && storedFile(id).exists() && readStored(id) != null) return@withLock existing
            // У старых установок случайные UUID; узнаём такие книги по уже сохранённому тексту.
            val matches = dao.observeBooks().first().filter {
                it.source == "user" && it.title == content.title && it.author == content.author &&
                    it.chars == content.totalChars && it.chapters == content.chapters.size && storedFile(it.id).exists()
            }
            for (book in matches) if (readStored(book.id) == content) return@withLock book
        }
        val entity = BookEntity(
            id = id,
            title = content.title,
            author = content.author,
            level = content.level,
            source = source,
            sourceSize = size,
            chapters = content.chapters.size,
            chars = content.totalChars,
            addedAt = addedAt ?: existing?.addedAt ?: System.currentTimeMillis(),
        )
        val encoded = json.encodeToString(BookContent.serializer(), content).toByteArray()
        if (encoded.size > BookInput.MAX_FILE_BYTES) throw BookImportException("Обработанный текст книги слишком большой для сохранения")
        // Отмена импорта не должна оставить строку без файла или оборванный JSON.
        withContext(NonCancellable) {
            writeAtomic(file(id), encoded)
            try {
                dao.upsertBook(entity)
            } catch (e: Exception) {
                if (existing == null) AtomicFile(file(id)).delete()
                throw e
            }
            cache[id] = content
            entity
        }
    }

    suspend fun content(id: String): BookContent? = cache[id] ?: withContext(Dispatchers.IO) { writes.withLock {
        cache[id] ?: readStored(id)
    } }

    private fun readStored(id: String): BookContent? {
        val f = storedFile(id)
        return try {
            val bytes = AtomicFile(f).openRead().use { BookInput.read(it) }
            json.decodeFromString(BookContent.serializer(), bytes.toString(Charsets.UTF_8)).also { cache[id] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { null }
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

    suspend fun restoreBookmark(bookmark: BookmarkEntity) = dao.insertBookmark(bookmark.copy(id = 0))

    suspend fun deleteBookmark(bookmarkId: Long) = dao.deleteBookmark(bookmarkId)

    fun observeHighlights(id: String) = dao.observeHighlights(id)

    suspend fun addHighlight(id: String, chapter: Int, paragraph: Int, start: Int, end: Int, text: String, note: String = ""): Long =
        dao.insertHighlight(HighlightEntity(bookId = id, chapter = chapter, paragraph = paragraph, start = start, end = end, text = text, note = note.trim(), createdAt = System.currentTimeMillis()))

    suspend fun updateHighlightNote(highlightId: Long, note: String) = dao.updateHighlightNote(highlightId, note.trim())

    suspend fun restoreHighlight(highlight: HighlightEntity) = dao.insertHighlight(highlight.copy(id = 0))

    suspend fun deleteHighlight(highlightId: Long) = dao.deleteHighlight(highlightId)

    /** Слова из начатых карточек, которые ещё не выучены, — для подсветки в тексте. */
    fun observeStudiedTerms() = combine(db.cards().observeAll(), db.reviews().observeStates()) { rows, states ->
        val byId = states.associateBy { it.cardId }
        com.marat.albion.data.repo.WordCards.group(rows).filter { card ->
            val state = com.marat.albion.data.repo.WordCards.state(card, byId)
            state != null && !(state.state == 2 && state.stability >= 21)
        }.map { it.term }.distinct()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { writes.withLock {
        dao.deleteBook(id)
        cache.remove(id)
        AtomicFile(file(id)).delete()
        AtomicFile(legacyFile(id)).delete()
        versionFile(id).delete()
        Unit
    } }

    /**
     * Справка по вашим карточкам: те же формы слова, что и в словаре
     * (cats → cat, studied → study, went → go, children's → children).
     */
    suspend fun lookup(word: String): List<CardEntity> {
        val candidates = dictionary.candidates(word)
        return if (candidates.isEmpty()) emptyList() else
            com.marat.albion.data.repo.WordCards.group(db.lookup().findByTerms(candidates)).take(3)
    }

    private fun file(id: String) = File(dir, BookInput.fingerprint(id.toByteArray()) + ".json")
    private fun legacyFile(id: String) = File(dir, id.replace(Regex("[^A-Za-z0-9_.-]"), "_") + ".json")
    private fun storedFile(id: String): File = file(id).let { if (it.exists() || File(it.path + ".bak").exists()) it else legacyFile(id) }
    private fun versionFile(id: String) = File(dir, BookInput.fingerprint(id.toByteArray()) + ".version")

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val atomic = AtomicFile(target)
        val out = atomic.startWrite()
        try {
            out.write(bytes)
            atomic.finishWrite(out)
            if (!target.isFile || File(target.path + ".new").exists()) throw IOException("Не удалось завершить запись книги")
        } catch (e: Exception) {
            atomic.failWrite(out)
            throw e
        }
    }

    companion object {
        /** Книги, которые всегда входят в приложение. */
        const val SAMPLE_DIR = "books"
    }
}
