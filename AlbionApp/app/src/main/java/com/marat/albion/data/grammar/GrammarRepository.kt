package com.marat.albion.data.grammar

import android.content.res.AssetManager
import com.marat.albion.data.content.ContentImporter
import com.marat.albion.data.db.AlbionDatabase
import com.marat.albion.data.db.GrammarProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class Topic(
    /** Имя файла без .md — так на тему ссылаются [[вики-ссылки]]. */
    val id: String,
    val number: Int?,
    val title: String,
    val level: String?,
    val kind: TopicKind,
)

enum class TopicKind { Lesson, Reference, Contents, Sources }

data class Chapter(val title: String, val topics: List<Topic>)

data class GrammarBook(
    val topics: List<Topic>,
    val chapters: List<Chapter>,
    val references: List<Topic>,
    /** Вступление из оглавления: как устроены темы, условные обозначения. */
    val intro: MdDocument?,
) {
    private val byId = topics.associateBy { it.id }
    fun topic(id: String) = byId[id]
    val lessons get() = topics.filter { it.kind == TopicKind.Lesson }
    fun next(id: String) = lessons.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { lessons.getOrNull(it + 1) }
    fun previous(id: String) = lessons.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { lessons.getOrNull(it - 1) }

    companion object {
        val EMPTY = GrammarBook(emptyList(), emptyList(), emptyList(), null)
    }
}

/**
 * Учебник грамматики из assets/content/grammar (*.md в формате Obsidian).
 * Оглавление строится по «000 - Оглавление.md», уровни — из frontmatter.
 */
class GrammarRepository(
    private val assets: AssetManager,
    private val db: AlbionDatabase,
) {
    private val mutex = Mutex()
    private var book: GrammarBook? = null
    private val documents = ConcurrentHashMap<String, MdDocument>()
    private val dao get() = db.grammar()

    suspend fun book(): GrammarBook = mutex.withLock {
        book ?: withContext(Dispatchers.IO) { load() }.also { book = it }
    }

    suspend fun document(id: String): MdDocument? = documents[id] ?: withContext(Dispatchers.Default) {
        val text = runCatching { assets.open("${ContentImporter.GRAMMAR}/$id.md").bufferedReader().use { it.readText() } }.getOrNull()
            ?: return@withContext null
        MarkdownParser.parse(text).also { documents[id] = it }
    }

    fun observeProgress() = dao.observeAll()
    fun observeProgress(id: String) = dao.observe(id)

    suspend fun markOpened(id: String) {
        val p = dao.get(id)
        dao.upsert(p?.copy(openedAt = System.currentTimeMillis()) ?: GrammarProgressEntity(id, null, false, System.currentTimeMillis()))
    }

    suspend fun setRead(id: String, read: Boolean) {
        val p = dao.get(id) ?: GrammarProgressEntity(id, null, false, null)
        dao.upsert(p.copy(readAt = if (read) System.currentTimeMillis() else null))
    }

    suspend fun toggleBookmark(id: String) {
        val p = dao.get(id) ?: GrammarProgressEntity(id, null, false, null)
        dao.upsert(p.copy(bookmarked = !p.bookmarked))
    }

    suspend fun resetProgress() = dao.clear()

    private suspend fun load(): GrammarBook {
        val files = runCatching { assets.list(ContentImporter.GRAMMAR)?.toList() }.getOrNull().orEmpty()
            .filter { it.endsWith(".md") }
        if (files.isEmpty()) return GrammarBook.EMPTY

        val topics = files.map { file ->
            val id = file.removeSuffix(".md")
            val head = runCatching {
                assets.open("${ContentImporter.GRAMMAR}/$file").bufferedReader().useLines { it.take(40).toList() }
            }.getOrDefault(emptyList())
            val level = head.firstOrNull { it.startsWith("level:") }?.substringAfter(":")?.trim()
            val h1 = head.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
            val number = NUMBERED.find(id)?.groupValues?.get(1)?.toIntOrNull()
            val kind = when {
                id.startsWith("Справочник") -> TopicKind.Reference
                number == 0 && id.contains("Оглавление") -> TopicKind.Contents
                number == 0 -> TopicKind.Sources
                else -> TopicKind.Lesson
            }
            val cleanTitle = h1 ?: id.replace(NUMBERED, "")
            Topic(id, number, cleanTitle.removePrefix("Справочник — "), level, kind)
        }.sortedWith(compareBy<Topic>({ it.number ?: Int.MAX_VALUE }, { it.title }))

        val byId = topics.associateBy { it.id }
        val contents = topics.firstOrNull { it.kind == TopicKind.Contents }
        val contentsDoc = contents?.let { document(it.id) }

        val chapters = ArrayList<Chapter>()
        var intro: MdDocument? = null
        if (contentsDoc != null) {
            var current: String? = null
            val introBlocks = ArrayList<MdBlock>()
            for (block in contentsDoc.blocks) {
                when {
                    block is MdHeading && block.level == 2 -> current = block.text
                    block is MdList && current != null -> {
                        val linked = block.items.mapNotNull { item ->
                            val text = (item.blocks.firstOrNull() as? MdParagraph)?.text ?: return@mapNotNull null
                            WIKILINK.find(text)?.groupValues?.get(1)?.let { byId[it.substringBefore('#')] }
                        }
                        if (linked.isNotEmpty()) chapters += Chapter(current, linked)
                    }
                    current == null || current.startsWith("Условные") || current.startsWith("Если грамматические") ||
                        current.startsWith("Как заниматься") -> {
                        if (current != null && introBlocks.none { it is MdHeading && it.text == current }) {
                            introBlocks += MdHeading(2, current)
                        }
                        introBlocks += block
                    }
                }
            }
            intro = MdDocument(emptyMap(), contentsDoc.title, introBlocks)
        }
        val lessonChapters = chapters.filter { ch -> ch.topics.any { it.kind == TopicKind.Lesson } }
        val covered = lessonChapters.flatMap { it.topics }.map { it.id }.toSet()
        val orphans = topics.filter { it.kind == TopicKind.Lesson && it.id !in covered }
        val finalChapters = if (lessonChapters.isEmpty()) {
            listOf(Chapter("Темы", topics.filter { it.kind == TopicKind.Lesson }))
        } else if (orphans.isNotEmpty()) lessonChapters + Chapter("Другие темы", orphans) else lessonChapters

        val references = topics.filter { it.kind == TopicKind.Reference } + topics.filter { it.kind == TopicKind.Sources }
        return GrammarBook(topics, finalChapters, references, intro)
    }

    companion object {
        private val NUMBERED = Regex("^(\\d{1,4})\\s*-\\s*")
        val WIKILINK = Regex("\\[\\[([^\\]|]+)(?:\\|([^\\]]+))?]]")
    }
}
