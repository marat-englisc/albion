package com.marat.albion.data.content

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Минимальный читатель .xlsx без Apache POI: распаковывает архив и разбирает
 * sharedStrings.xml и первый лист. Возвращает строки как списки ячеек
 * (пустые ячейки — ""), с учётом пропусков по адресу ячейки (A1, C1 …).
 */
object XlsxReader {

    fun read(input: InputStream): List<List<String>> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name == "xl/sharedStrings.xml" || name == "xl/workbook.xml" ||
                    name == "xl/_rels/workbook.xml.rels" || name.startsWith("xl/worksheets/sheet")
                ) {
                    entries[name] = zip.readBytes()
                }
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings) ?: emptyList()
        val sheetPath = firstSheetPath(entries) ?: error("В книге нет листов")
        val sheet = entries[sheetPath] ?: error("Не найден лист $sheetPath")
        return parseSheet(sheet, shared)
    }

    private fun parser(bytes: ByteArray): XmlPullParser =
        XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser().apply {
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }

    private fun firstSheetPath(entries: Map<String, ByteArray>): String? {
        val workbook = entries["xl/workbook.xml"]
        val rels = entries["xl/_rels/workbook.xml.rels"]
        if (workbook != null && rels != null) {
            var firstRid: String? = null
            val p = parser(workbook)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                    firstRid = p.getAttributeValue(null, "r:id"); break
                }
            }
            if (firstRid != null) {
                val r = parser(rels)
                while (r.next() != XmlPullParser.END_DOCUMENT) {
                    if (r.eventType == XmlPullParser.START_TAG && r.name == "Relationship" &&
                        r.getAttributeValue(null, "Id") == firstRid
                    ) {
                        val target = r.getAttributeValue(null, "Target").removePrefix("/")
                        val path = if (target.startsWith("xl/")) target else "xl/$target"
                        if (entries.containsKey(path)) return path
                    }
                }
            }
        }
        return entries.keys.filter { it.startsWith("xl/worksheets/sheet") }.minOrNull()
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val out = ArrayList<String>()
        val p = parser(bytes)
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var inRph = false // фонетические подсказки (ruby) пропускаем
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inSi = true; sb.setLength(0) }
                    "t" -> inT = true
                    "rPh" -> inRph = true
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "si" -> { inSi = false; out += sb.toString() }
                    "t" -> inT = false
                    "rPh" -> inRph = false
                }
                XmlPullParser.TEXT -> if (inSi && inT && !inRph) sb.append(p.text)
            }
        }
        return out
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val p = parser(bytes)
        var row: MutableList<String>? = null
        var cellCol = 0
        var cellType: String? = null
        var nextCol = 0
        var inValue = false
        val value = StringBuilder()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> {
                        row = ArrayList(); nextCol = 0
                        val r = p.getAttributeValue(null, "r")?.toIntOrNull()
                        // Сохраняем пустые строки, чтобы номер строки совпадал с Excel.
                        if (r != null) while (rows.size < r - 1) rows += emptyList<String>()
                    }
                    "c" -> {
                        cellType = p.getAttributeValue(null, "t")
                        cellCol = p.getAttributeValue(null, "r")?.let(::columnIndex) ?: nextCol
                        value.setLength(0)
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val raw = value.toString()
                        val text = when (cellType) {
                            "s" -> raw.trim().toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            else -> raw
                        }
                        row?.let { r ->
                            while (r.size < cellCol) r += ""
                            r += text
                        }
                        nextCol = cellCol + 1
                    }
                    "row" -> { row?.let { rows += it }; row = null }
                }
                XmlPullParser.TEXT -> if (inValue) value.append(p.text)
            }
        }
        return rows
    }

    /** "AB12" → 27 (нумерация с нуля). */
    internal fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch !in 'A'..'Z') break
            n = n * 26 + (ch - 'A' + 1)
        }
        return n - 1
    }
}
