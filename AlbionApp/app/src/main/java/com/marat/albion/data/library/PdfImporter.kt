package com.marat.albion.data.library

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Текст из PDF через PdfBox-Android: страницы, название, автор и оглавление.
 * Сканы без текстового слоя не поддерживаются — для них нужно распознавание текста.
 */
object PdfImporter {
    private const val PAGE_BREAK = "\u000C"
    @Volatile private var initialized = false

    fun isPdf(fileName: String, bytes: ByteArray) =
        fileName.endsWith(".pdf", ignoreCase = true) ||
            (bytes.size > 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte())

    fun parse(context: Context, fileName: String, bytes: ByteArray): BookContent {
        if (!initialized) { PDFBoxResourceLoader.init(context.applicationContext); initialized = true }
        PDDocument.load(bytes).use { doc ->
            if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
                pageEnd = PAGE_BREAK
            }
            val pages = stripper.getText(doc).split(PAGE_BREAK).let { if (it.size > doc.numberOfPages) it.take(doc.numberOfPages) else it }
            if (pages.sumOf { it.count(Char::isLetter) } < 50) error("В PDF нет текста — похоже, это отсканированные страницы")
            val info = doc.documentInformation
            val title = info?.title?.trim().orEmpty().ifEmpty { fileName.substringBeforeLast('.').replace('_', ' ') }
            return BookParser.parsePdfText(pages, title, info?.author?.trim().orEmpty(), outline(doc))
        }
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
