package com.marat.albion.data.library

import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@Serializable
data class BookContent(
    val title: String,
    val author: String = "",
    val level: String = "",
    val chapters: List<BookChapter>,
) {
    val totalChars: Long get() = chapters.sumOf { ch -> ch.paragraphs.sumOf { it.length.toLong() } }
}

@Serializable
data class BookChapter(val title: String, val paragraphs: List<String>)

/** Место в книге: глава → абзац → символ. */
data class BookPosition(val chapter: Int, val paragraph: Int, val offset: Int) : Comparable<BookPosition> {
    override fun compareTo(other: BookPosition) =
        compareValuesBy(this, other, { it.chapter }, { it.paragraph }, { it.offset })
}

/**
 * Разбор книг. Поддерживаются:
 * - TXT/MD: «# Название», строки «Author:» и «Level:», главы «## Глава»
 *   (или строки вида «CHAPTER 1» в обычном TXT); абзацы разделены пустой строкой;
 * - EPUB: порядок глав по spine из OPF, заголовки h1–h3 и абзацы p (или div);
 *   обложка, титул, копирайт и оглавление пропускаются.
 */
object BookParser {

    fun parse(fileName: String, bytes: ByteArray): BookContent {
        val isZip = bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        return if (isZip || fileName.endsWith(".epub", true)) parseEpub(bytes, fileName)
        else parseText(String(bytes, Charsets.UTF_8).removePrefix("\uFEFF"), fileName)
    }

    // ------------------------------------------------------------------ TXT / MD

    private val CHAPTER_LINE = Regex("^(CHAPTER|Chapter|ГЛАВА|Глава|PART|Part)\\s+[\\wIVXLC]+.*$")

    fun parseText(text: String, fileName: String = ""): BookContent {
        var title = ""
        var author = ""
        var level = ""
        val chapters = mutableListOf<BookChapter>()
        var chapterTitle: String? = null
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()

        fun flushParagraph() {
            val p = current.toString().replace(Regex("\\s+"), " ").trim()
            if (p.isNotEmpty()) paragraphs += p
            current.setLength(0)
        }
        fun flushChapter() {
            flushParagraph()
            // Короткая строка перед первой главой в TXT без разметки — это название книги.
            if (chapters.isEmpty() && chapterTitle == null && title.isEmpty() && paragraphs.size == 1 && paragraphs[0].length < 80) {
                title = paragraphs[0]
                paragraphs.clear()
                return
            }
            if (paragraphs.isNotEmpty() || chapterTitle != null) {
                chapters += BookChapter(chapterTitle ?: if (chapters.isEmpty()) "Начало" else "Часть ${chapters.size + 1}", paragraphs.toList())
            }
            paragraphs.clear()
        }

        for (raw in text.replace("\r\n", "\n").lines()) {
            val line = raw.trim()
            when {
                line.startsWith("# ") && title.isEmpty() -> title = line.removePrefix("# ").trim()
                title.isNotEmpty() && chapters.isEmpty() && paragraphs.isEmpty() && current.isEmpty() &&
                    line.startsWith("Author:", true) -> author = line.substringAfter(':').trim()
                title.isNotEmpty() && chapters.isEmpty() && paragraphs.isEmpty() && current.isEmpty() &&
                    line.startsWith("Level:", true) -> level = line.substringAfter(':').trim()
                line.startsWith("## ") || (CHAPTER_LINE.matches(line) && line.length < 80) -> {
                    flushChapter()
                    chapterTitle = line.removePrefix("## ").trim()
                }
                line.isEmpty() -> flushParagraph()
                else -> current.append(' ').append(line)
            }
        }
        flushChapter()
        if (title.isEmpty()) title = fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Без названия" }
        return BookContent(title, author, level, chapters.filter { it.paragraphs.isNotEmpty() })
    }

    // ------------------------------------------------------------------ PDF

    private val PAGE_NUMBER = Regex("^[\\s\\-–—]*(\\d{1,4}|[ivxlcdm]{1,7})[\\s\\-–—]*$", RegexOption.IGNORE_CASE)

    /**
     * Книга из текста страниц PDF. В PDF нет абзацев — только строки, поэтому:
     * - убираем номера страниц и колонтитулы (одинаковые строки вверху/внизу многих страниц);
     * - склеиваем переносы «exam-» + «ple»;
     * - новый абзац — после пустой строки или короткой строки, кончающейся точкой;
     * - главы — по оглавлению PDF ([outline]: название → номер страницы с 0),
     *   иначе по строкам «Chapter 1», иначе по 10 страниц.
     */
    fun parsePdfText(pages: List<String>, title: String, author: String = "", outline: List<Pair<String, Int>> = emptyList()): BookContent {
        val lines = pages.map { page -> page.replace("\r", "").lines().map { it.trim() } }
        val repeated = repeatedEdgeLines(lines)
        val cleaned = lines.map { page ->
            page.filterIndexed { i, line ->
                val edge = i < 2 || i >= page.size - 2
                !(PAGE_NUMBER.matches(line) && line.isNotEmpty()) && !(edge && normalizeEdge(line) in repeated)
            }
                // Граница страницы — не конец абзаца: пустые строки по краям убираем.
                .dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
        }
        val typical = cleaned.flatten().filter { it.length > 20 }.map { it.length }.sorted().let { if (it.isEmpty()) 60 else it[it.size / 2] }

        val usableOutline = outline.filter { it.first.isNotBlank() && it.second in pages.indices }.sortedBy { it.second }.distinctBy { it.second }
        val chapters = mutableListOf<BookChapter>()
        if (usableOutline.size >= 2) {
            val starts = usableOutline.map { it.second }
            // Страницы до первой главы оглавления (титул, предисловие) — отдельной главой, если там есть текст.
            if (starts.first() > 0) addChapter(chapters, "Начало", cleaned.subList(0, starts.first()), typical)
            usableOutline.forEachIndexed { k, (name, from) ->
                val to = starts.getOrNull(k + 1) ?: cleaned.size
                addChapter(chapters, name.trim(), cleaned.subList(from, to), typical)
            }
        } else {
            val byLines = splitByChapterLines(cleaned, typical)
            if (byLines.size >= 2) chapters += byLines
            else cleaned.chunked(10).forEachIndexed { k, chunk ->
                val from = k * 10 + 1
                addChapter(chapters, "Страницы $from–${from + chunk.size - 1}", chunk, typical)
            }
        }
        if (chapters.isEmpty()) error("В PDF нет текста — похоже, это отсканированные страницы")
        return BookContent(title = title.ifBlank { "Без названия" }, author = author, chapters = chapters)
    }

    private fun normalizeEdge(line: String) = line.lowercase().replace(Regex("\\d+"), "#").trim()

    /** Строки, повторяющиеся в первых/последних двух строках у 40% страниц и больше. */
    private fun repeatedEdgeLines(pages: List<List<String>>): Set<String> {
        if (pages.size < 2) return emptySet()
        val counts = HashMap<String, Int>()
        pages.forEach { page ->
            val nonEmpty = page.filter { it.isNotEmpty() }
            // Колонтитул — короткая строка на странице, где есть и основной текст.
            if (nonEmpty.size < 2) return@forEach
            (nonEmpty.take(2) + nonEmpty.takeLast(2)).filter { it.length <= 70 }.map(::normalizeEdge).filter { it.length > 1 }.toSet()
                .forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        // В коротком документе колонтитул должен быть на каждой странице, в длинном — на 40%.
        val need = if (pages.size < 5) pages.size.toDouble() else pages.size * 0.4
        return counts.filterValues { it >= need }.keys
    }

    private fun addChapter(out: MutableList<BookChapter>, title: String, pages: List<List<String>>, typical: Int) {
        val paragraphs = joinLines(pages.flatten(), typical)
        if (paragraphs.isNotEmpty()) out += BookChapter(title, paragraphs)
    }

    private fun splitByChapterLines(pages: List<List<String>>, typical: Int): List<BookChapter> {
        val result = mutableListOf<BookChapter>()
        var title: String? = null
        val buffer = mutableListOf<String>()
        fun flush() {
            val paragraphs = joinLines(buffer, typical)
            if (paragraphs.isNotEmpty()) result += BookChapter(title ?: "Начало", paragraphs)
            buffer.clear()
        }
        for (line in pages.flatten()) {
            if (line.length < 60 && CHAPTER_LINE.matches(line)) { flush(); title = line } else buffer += line
        }
        flush()
        return if (result.count { it.title != "Начало" } >= 2) result else emptyList()
    }

    /** Строки PDF → абзацы. */
    private fun joinLines(lines: List<String>, typical: Int): List<String> {
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        var prev = ""
        fun flush() {
            val p = current.toString().replace(Regex("\\s+"), " ").trim()
            if (p.isNotEmpty()) paragraphs += p
            current.setLength(0)
        }
        for (line in lines) {
            if (line.isEmpty()) { flush(); prev = ""; continue }
            when {
                current.isEmpty() -> current.append(line)
                // Перенос слова: «exam-» + «ple».
                prev.endsWith("-") && prev.length > 1 && prev[prev.length - 2].isLetter() && line.first().isLowerCase() -> {
                    current.setLength(current.length - 1); current.append(line)
                }
                // Короткая строка с точкой в конце — конец абзаца.
                prev.last() in ".!?:\"”’»" && prev.length < typical * 0.8 -> { flush(); current.append(line) }
                else -> current.append(' ').append(line)
            }
            prev = line
        }
        flush()
        return paragraphs
    }

    // ------------------------------------------------------------------ EPUB

    private val TAG = Regex("<[^>]+>")
    private val DIV = Regex("<div\\b[^>]*>((?:(?!<div\\b).)*?)</div>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val BLOCK = Regex("<(h[1-3]|p|li|blockquote)\\b[^>]*>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    fun parseEpub(bytes: ByteArray, fileName: String = ""): BookContent {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (!e.isDirectory) files[e.name] = zip.readBytes()
            }
        }
        fun text(path: String) = files[path]?.toString(Charsets.UTF_8)

        val container = text("META-INF/container.xml") ?: error("Не найден META-INF/container.xml")
        val opfPath = Regex("full-path=\"([^\"]+)\"").find(container)?.groupValues?.get(1) ?: error("Не найден OPF")
        val opf = text(opfPath) ?: error("Не найден $opfPath")
        val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

        val title = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL).find(opf)?.groupValues?.get(1)?.let(::clean)
        val author = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL).find(opf)?.groupValues?.get(1)?.let(::clean)

        val items = Regex("<item\\b[^>]*>").findAll(opf).map { it.value }.toList()
        val manifest = items.associate { attr(it, "id") to attr(it, "href") }
        // Навигационный документ EPUB 3 (оглавление) — не текст книги.
        val navIds = items.filter { attr(it, "properties").split(' ').contains("nav") }.map { attr(it, "id") }.toSet()
        val spine = Regex("<itemref\\b[^>]*>").findAll(opf)
            .filter { attr(it.value, "linear") != "no" }
            .map { attr(it.value, "idref") }
            .filter { it !in navIds }
            .toList()

        val chapters = mutableListOf<BookChapter>()
        for (idref in spine) {
            val href = manifest[idref] ?: continue
            val path = normalize(base + java.net.URLDecoder.decode(href.substringBefore('#'), "UTF-8"))
            val html = text(path) ?: continue
            val body = html.substringAfter("<body", html)
            var chapterTitle: String? = null
            val paragraphs = mutableListOf<String>()
            var blocks = BLOCK.findAll(body).map { Triple(it.range.first, it.groupValues[1], it.groupValues[2]) }.toList()
            // Некоторые EPUB размечают абзацы через <div> — берём самые вложенные блоки.
            if (blocks.none { it.second.equals("p", true) }) {
                blocks = (blocks + DIV.findAll(body).map { Triple(it.range.first, "p", it.groupValues[1]) }).sortedBy { it.first }
            }
            for ((_, tag, raw) in blocks) {
                val content = clean(raw)
                if (content.isEmpty()) continue
                if (tag.startsWith("h", true) && chapterTitle == null && paragraphs.isEmpty()) chapterTitle = content
                else paragraphs += content
            }
            if (paragraphs.isEmpty() || isFrontMatter(chapterTitle, paragraphs, path)) continue
            chapters += BookChapter(chapterTitle ?: "Глава ${chapters.size + 1}", paragraphs)
        }
        if (chapters.isEmpty()) error("В EPUB не найден текст")
        return BookContent(
            title = title?.ifEmpty { null } ?: fileName.substringBeforeLast('.').ifEmpty { "Без названия" },
            author = author.orEmpty(),
            chapters = chapters,
        )
    }

    /**
     * Служебные страницы: обложка, титул, копирайт, оглавление. Их узнаём по короткому
     * тексту с характерными словами или по имени файла — в библиотеку они не попадают.
     */
    internal fun isFrontMatter(title: String?, paragraphs: List<String>, path: String = ""): Boolean {
        val chars = paragraphs.sumOf { it.length }
        if (chars < 40 && title == null) return true
        if (chars > 2500) return false
        val name = path.substringAfterLast('/').lowercase()
        if (FRONT_FILE.containsMatchIn(name)) return true
        val text = (listOfNotNull(title) + paragraphs).joinToString(" ").lowercase()
        return FRONT_TEXT.containsMatchIn(text)
    }

    private val FRONT_FILE = Regex("^(cover|titlepage|title|copyright|toc|nav|contents|halftitle|imprint)[^a-z]")
    private val FRONT_TEXT = Regex(
        "all rights reserved|copyright ©|©\\s*\\d{4}|\\bisbn\\b|table of contents|^contents\\b|" +
            "все права защищены|^оглавление|^содержание|project gutenberg license"
    )

    private fun attr(tag: String, name: String) =
        Regex("\\b$name=\"([^\"]*)\"").find(tag)?.groupValues?.get(1).orEmpty()

    private fun normalize(path: String): String {
        val out = ArrayDeque<String>()
        path.split('/').forEach { part ->
            when (part) {
                "", "." -> {}
                ".." -> out.removeLastOrNull()
                else -> out.addLast(part)
            }
        }
        return out.joinToString("/")
    }

    private fun clean(html: String): String = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        .replace(TAG, "")
        .let(::decodeEntities)
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun decodeEntities(s: String): String = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(s) { m ->
        val e = m.groupValues[1]
        when {
            e.startsWith("#x", true) -> e.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
            e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
            else -> when (e) {
                "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> " "
                "mdash" -> "—"; "ndash" -> "–"; "hellip" -> "…"; "lsquo" -> "‘"; "rsquo" -> "’"
                "ldquo" -> "“"; "rdquo" -> "”"
                else -> m.value
            }
        }
    }
}
