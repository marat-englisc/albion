package com.marat.albion.data.library

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipInputStream

internal data class EpubLimits(
    val entryBytes: Int = 8 * 1024 * 1024,
    val unpackedBytes: Long = 64L * 1024 * 1024,
    val retainedBytes: Long = 16L * 1024 * 1024,
    val entries: Int = 4096,
)

/** EPUB читается как XML; двоичные ресурсы не удерживаются в памяти. */
internal object EpubReader {
    private val whitespace = Regex("[\\s\u00A0]+")
    private val binaryExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "avif", "svg", "ttf", "otf", "woff", "woff2", "mp3", "mp4", "m4a", "ogg", "css")
    private val blocks = setOf("p", "div", "section", "article", "li", "blockquote", "pre", "dt", "dd", "figure", "figcaption", "table", "tr", "hr")
    private val ignored = setOf("script", "style", "nav", "svg")
    private val headingTag = Regex("h[1-6]")
    private val entities = mapOf(
        "nbsp" to " ", "mdash" to "—", "ndash" to "–", "hellip" to "…", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "laquo" to "«", "raquo" to "»", "copy" to "©", "reg" to "®",
        "trade" to "™", "pound" to "£", "euro" to "€", "shy" to "", "eacute" to "é", "Eacute" to "É",
    )

    fun read(bytes: ByteArray, fileName: String, limits: EpubLimits = EpubLimits()): BookContent {
        if (bytes.size > BookInput.MAX_FILE_BYTES) throw BookImportException("Файл слишком большой: поддерживаются книги до 30 МБ")
        try {
            val files = unpack(bytes, limits)
            val container = files["META-INF/container.xml"] ?: throw BookImportException("В архиве нет структуры EPUB (META-INF/container.xml)")
            var packagePath: String? = null
            xml(container) { p ->
                if (p.eventType == XmlPullParser.START_TAG && p.localName() == "rootfile" && packagePath == null) {
                    packagePath = resolve("", p.attr("full-path"))
                }
            }
            val opfPath = packagePath ?: throw BookImportException("В EPUB не найден файл описания книги")
            val opf = files[opfPath] ?: throw BookImportException("В EPUB отсутствует описание книги: $opfPath")
            val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
            data class Item(val href: String, val navigation: Boolean, val mediaType: String)
            val manifest = linkedMapOf<String, Item>()
            val spine = mutableListOf<String>()
            var title = ""
            val authors = mutableListOf<String>()
            var metadata = false
            var inManifest = false
            var inSpine = false
            var field: String? = null
            var metadataChars = 0
            val value = StringBuilder()
            xml(opf) { p ->
                val tag = p.localName()
                when (p.eventType) {
                    XmlPullParser.START_TAG -> when {
                        tag == "metadata" -> metadata = true
                        tag == "manifest" -> inManifest = true
                        tag == "spine" -> inSpine = true
                        metadata && tag in setOf("title", "creator") -> { field = tag; value.clear() }
                        inManifest && tag == "item" -> {
                            if (manifest.size >= limits.entries) throw BookImportException("В EPUB слишком много разделов")
                            val id = p.attr("id")
                            if (id.isNotBlank() && manifest.put(id, Item(p.attr("href"), "nav" in p.attr("properties").split(whitespace), p.attr("media-type"))) != null) {
                                throw BookImportException("В EPUB повторяется идентификатор раздела: $id")
                            }
                        }
                        inSpine && tag == "itemref" && p.attr("linear") != "no" -> {
                            if (spine.size >= limits.entries) throw BookImportException("В EPUB слишком много разделов")
                            spine += p.attr("idref")
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> if (field != null) {
                        val part = p.textValue()
                        metadataChars += part.length
                        if (value.length + part.length > 8192 || metadataChars > 65_536) throw BookImportException("В EPUB слишком длинные метаданные книги")
                        value.append(part)
                    }
                    XmlPullParser.END_TAG -> when {
                        tag == field -> {
                            val text = plain(value.toString())
                            if (field == "title" && title.isEmpty()) title = text
                            if (field == "creator" && text.isNotEmpty()) authors += text
                            field = null
                        }
                        tag == "metadata" -> metadata = false
                        tag == "manifest" -> inManifest = false
                        tag == "spine" -> inSpine = false
                    }
                }
            }
            val chapters = mutableListOf<BookChapter>()
            var chars = 0L
            for (id in spine.distinct()) {
                val item = manifest[id] ?: throw BookImportException("В EPUB отсутствует раздел из оглавления: $id")
                if (item.navigation || item.mediaType.startsWith("image/")) continue
                val path = resolve(base, item.href)
                val document = files[path] ?: throw BookImportException("В EPUB отсутствует глава: $path")
                val sections = chapter(document)
                if (BookParser.isFrontMatter(sections.firstOrNull()?.title, sections.flatMap { it.paragraphs }, path)) continue
                for (section in sections) {
                    chars += section.paragraphs.sumOf { it.length.toLong() }
                    if (chars > BookInput.MAX_TEXT_CHARS) throw BookImportException("В книге слишком много текста: предел — 5 млн символов")
                    chapters += section.copy(title = section.title.ifBlank { "Глава ${chapters.size + 1}" })
                }
            }
            if (chapters.isEmpty()) throw BookImportException("В EPUB не найден текст для чтения. Книги из одних изображений не поддерживаются")
            return BookContent(title.ifBlank { BookInput.title(fileName) }, authors.distinct().joinToString(", "), chapters = chapters)
        } catch (e: BookImportException) {
            throw e
        } catch (e: Exception) {
            throw BookImportException("Не удалось прочитать EPUB: архив или разметка книги повреждены", e)
        }
    }

    private fun unpack(bytes: ByteArray, limits: EpubLimits): Map<String, ByteArray> {
        val files = HashMap<String, ByteArray>()
        val names = HashSet<String>()
        var unpacked = 0L
        var retained = 0L
        var entries = 0
        val buffer = ByteArray(8192)
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > limits.entries) throw BookImportException("В EPUB слишком много файлов")
                val name = normalize(entry.name)
                if (!names.add(name)) throw BookImportException("В EPUB повторяется путь файла: $name")
                val keep = !entry.isDirectory && name.substringAfterLast('.', "").lowercase(Locale.ROOT) !in binaryExtensions
                val out = if (keep) ByteArrayOutputStream() else null
                var size = 0
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    unpacked += n
                    size += n
                    if (unpacked > limits.unpackedBytes) throw BookImportException("EPUB слишком велик после распаковки (больше 64 МБ)")
                    if (keep) {
                        retained += n
                        if (size > limits.entryBytes || retained > limits.retainedBytes) {
                            throw BookImportException("Текстовые разделы EPUB слишком велики")
                        }
                        out!!.write(buffer, 0, n)
                    }
                }
                if (out != null) files[name] = out.toByteArray()
            }
        }
        return files
    }

    private fun chapter(bytes: ByteArray): List<BookChapter> {
        val sections = mutableListOf<BookChapter>()
        val paragraphs = mutableListOf<String>()
        val text = StringBuilder()
        var title = ""
        var heading: String? = null
        var body = false
        var skip = 0
        var chars = 0L
        fun flush() {
            plain(text.toString()).takeIf { it.isNotEmpty() }?.let { paragraphs += it }
            text.clear()
        }
        fun finish() {
            flush()
            if (paragraphs.isNotEmpty()) sections += BookChapter(title, paragraphs.toList())
            paragraphs.clear()
        }
        xml(bytes) { p ->
            val tag = p.localName()
            when (p.eventType) {
                XmlPullParser.START_TAG -> {
                    if (tag == "body") body = true
                    if (skip == 0 && body && (tag in ignored || p.hasAttr("hidden") || p.attr("aria-hidden") == "true")) skip = p.depth
                    if (body && skip == 0) when {
                        tag.matches(headingTag) -> { flush(); heading = tag }
                        tag in blocks -> flush()
                        tag in setOf("br", "td", "th") -> text.append(' ')
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> if (body && skip == 0) {
                    val part = p.textValue()
                    chars += part.length
                    if (chars > BookInput.MAX_TEXT_CHARS) throw BookImportException("В книге слишком много текста: предел — 5 млн символов")
                    text.append(part)
                }
                XmlPullParser.END_TAG -> {
                    if (body && skip == 0) when {
                        tag == heading -> {
                            val h = plain(text.toString())
                            text.clear()
                            if (h.isNotEmpty()) {
                                if (paragraphs.isEmpty() && title.isEmpty()) title = h
                                else if (tag in setOf("h1", "h2") && paragraphs.isNotEmpty()) { finish(); title = h }
                                else paragraphs += h
                            }
                            heading = null
                        }
                        tag in blocks -> flush()
                        tag == "body" -> { finish(); body = false }
                    }
                    if (p.depth == skip) skip = 0
                }
            }
        }
        return sections
    }

    private fun xml(bytes: ByteArray, visit: (XmlPullParser) -> Unit) {
        val p = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
        // KXml2 не поддерживает включение DTD; Android-парсер отключаем явно.
        try {
            p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        } catch (e: org.xmlpull.v1.XmlPullParserException) {
            if (p.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) throw e
        }
        p.setInput(bytes.inputStream(), null) // BOM и XML declaration определяют кодировку.
        entities.forEach { (name, text) -> p.defineEntityReplacementText(name, text) }
        while (p.nextToken() != XmlPullParser.END_DOCUMENT) {
            if (p.depth > 128) throw BookImportException("В EPUB слишком сложная вложенность разметки")
            if (p.eventType == XmlPullParser.DOCDECL && '[' in p.text.orEmpty()) {
                throw BookImportException("EPUB содержит неподдерживаемые объявления XML-сущностей")
            }
            visit(p)
        }
    }

    private fun XmlPullParser.localName() = name.orEmpty().substringAfter(':').lowercase(Locale.ROOT)
    private fun XmlPullParser.attr(name: String): String = (0 until attributeCount).firstOrNull {
        getAttributeName(it).substringAfter(':') == name
    }?.let { getAttributeValue(it) }.orEmpty()
    private fun XmlPullParser.hasAttr(name: String) = (0 until attributeCount).any { getAttributeName(it).substringAfter(':') == name }
    private fun XmlPullParser.textValue(): String {
        // Старые XML-парсеры обрезают числовые ссылки вне BMP до одного Char.
        if (eventType == XmlPullParser.ENTITY_REF && name.orEmpty().startsWith('#')) {
            val entity = name.orEmpty()
            val code = if (entity.startsWith("#x")) entity.drop(2).toIntOrNull(16) else entity.drop(1).toIntOrNull()
            if (code == null || !Character.isValidCodePoint(code) || code in 0xD800..0xDFFF || (code < 32 && code !in listOf(9, 10, 13))) {
                throw BookImportException("В EPUB найдена некорректная ссылка на символ")
            }
            return String(Character.toChars(code))
        }
        return text ?: if (eventType == XmlPullParser.ENTITY_REF) "&$name;" else ""
    }
    private fun plain(text: String) = text.replace(whitespace, " ").trim()

    private fun resolve(base: String, href: String): String {
        val path = href.substringBefore('#').substringBefore('?')
        if (path.isBlank() || path.startsWith('/') || Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(path)) {
            throw BookImportException("В EPUB указан недопустимый путь к разделу")
        }
        // URLDecoder предназначен для форм: настоящий '+' в имени файла нужно сохранить.
        return normalize(base + URLDecoder.decode(path.replace("+", "%2B"), "UTF-8"))
    }

    private fun normalize(path: String): String {
        if (path.startsWith('/') || '\\' in path || '\u0000' in path) throw BookImportException("В EPUB указан недопустимый путь файла")
        val parts = ArrayDeque<String>()
        path.split('/').forEach {
            when (it) {
                "", "." -> Unit
                ".." -> if (parts.isEmpty()) throw BookImportException("Путь в EPUB выходит за пределы архива") else parts.removeLast()
                else -> parts.addLast(it)
            }
        }
        return parts.joinToString("/")
    }
}
