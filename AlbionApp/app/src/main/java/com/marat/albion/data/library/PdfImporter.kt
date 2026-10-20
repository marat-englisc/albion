package com.marat.albion.data.library

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.Writer
import java.io.IOException

/**
 * Текст из PDF через PdfBox-Android: страницы, название, автор и оглавление.
 * Сканы без текстового слоя не поддерживаются — для них нужно распознавание текста.
 */
object PdfImporter {
    private const val PAGE_BREAK = "\u000C"
    @Volatile private var initialized = false

    fun isPdf(fileName: String, bytes: ByteArray) =
        fileName.endsWith(".pdf", ignoreCase = true) ||
            (bytes.size >= 5 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte() && bytes[4] == '-'.code.toByte())

    fun parse(context: Context, fileName: String, bytes: ByteArray): BookContent {
        if (bytes.size > BookInput.MAX_FILE_BYTES) throw BookImportException("Файл слишком большой: поддерживаются книги до 30 МБ")
        synchronized(this) {
            if (!initialized) { PDFBoxResourceLoader.init(context.applicationContext); initialized = true }
        }
        try {
            val memory = MemoryUsageSetting.setupMixed(16L * 1024 * 1024).setTempDir(context.cacheDir)
            PDDocument.load(bytes.inputStream(), memory).use { doc ->
                if (!doc.currentAccessPermission.canExtractContent()) throw BookImportException("Этот PDF запрещает извлечение текста")
                if (doc.numberOfPages > BookInput.MAX_PDF_PAGES) throw BookImportException("PDF слишком большой: поддерживается до 2000 страниц")
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    pageEnd = PAGE_BREAK
                }
                val writer = LimitedTextWriter()
                stripper.writeText(doc, writer)
                val pages = writer.toString().split(PAGE_BREAK).take(doc.numberOfPages)
                if (pages.none { page -> page.any(Char::isLetterOrDigit) }) throw BookImportException("В PDF нет текста — похоже, это отсканированные страницы")
                val info = doc.documentInformation
                val title = info?.title?.trim().orEmpty().take(1024).ifEmpty { BookInput.title(fileName) }
                return BookParser.parsePdfText(pages, title, info?.author?.trim().orEmpty().take(1024), outline(doc))
            }
        } catch (e: BookImportException) {
            throw e
        } catch (e: InvalidPasswordException) {
            throw BookImportException("PDF защищён паролем. Для импорта выберите копию без пароля", e)
        } catch (e: IOException) {
            throw BookImportException("Не удалось прочитать PDF: файл повреждён или имеет неподдерживаемую структуру", e)
        }
    }

    private class LimitedTextWriter : Writer() {
        private val text = StringBuilder()
        override fun write(chars: CharArray, offset: Int, length: Int) {
            if (text.length.toLong() + length > BookInput.MAX_TEXT_CHARS) {
                throw BookImportException("В PDF слишком много текста: предел — 5 млн символов")
            }
            text.append(chars, offset, length)
        }
        override fun flush() = Unit
        override fun close() = Unit
        override fun toString() = text.toString()
    }

    /** Верхний уровень оглавления PDF: название главы → страница (с нуля). */
    private fun outline(doc: PDDocument): List<Pair<String, Int>> = runCatching {
        val root = doc.documentCatalog.documentOutline ?: return emptyList()
        root.children().mapNotNull { item: PDOutlineItem ->
            val page = runCatching { item.findDestinationPage(doc) }.getOrNull() ?: return@mapNotNull null
            val index = doc.pages.indexOf(page)
            if (index < 0) null else (item.title.orEmpty() to index)
        }
    }.getOrDefault(emptyList())
}
