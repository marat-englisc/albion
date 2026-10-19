package com.marat.albion.data.grammar

/**
 * Разбор заметок Obsidian в блоки для нативного отображения в Compose:
 * заголовки, абзацы, списки (вложенные, чек-листы), таблицы, callout-блоки
 * ([!tip], сворачиваемые [!success]-), цитаты, код и схемы Mermaid.
 * Инлайн-разметка (**жирный**, `код`, [[ссылки]]) остаётся строкой и
 * раскрашивается при отрисовке.
 */
data class MdDocument(
    val frontmatter: Map<String, String>,
    val title: String?,
    val blocks: List<MdBlock>,
)

sealed interface MdBlock
data class MdHeading(val level: Int, val text: String) : MdBlock
data class MdParagraph(val text: String) : MdBlock
data class MdList(val ordered: Boolean, val start: Int, val items: List<MdListItem>) : MdBlock
data class MdListItem(val checked: Boolean?, val blocks: List<MdBlock>)
data class MdTable(val header: List<String>, val rows: List<List<String>>) : MdBlock
data class MdCode(val lang: String, val code: String) : MdBlock
data class MdDiagram(val graph: FlowGraph) : MdBlock
data class MdCallout(
    val kind: String,
    val title: String,
    val foldable: Boolean,
    val initiallyExpanded: Boolean,
    val blocks: List<MdBlock>,
) : MdBlock
data class MdQuote(val blocks: List<MdBlock>) : MdBlock
data object MdRule : MdBlock

object MarkdownParser {

    private val HEADING = Regex("^(#{1,6})\\s+(.*?)\\s*#*$")
    private val LIST_ITEM = Regex("^(\\s*)([-*+]|\\d{1,3}[.)])\\s+(.*)$")
    private val TABLE_SEP = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")
    private val RULE = Regex("^(-{3,}|\\*{3,}|_{3,})$")
    private val CALLOUT = Regex("^\\[!([A-Za-z-]+)]([+-]?)\\s*(.*)$")
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)

    fun parse(source: String): MdDocument {
        var lines = source.replace("\r\n", "\n").replace(COMMENT, "").split("\n")
        val front = LinkedHashMap<String, String>()
        if (lines.firstOrNull()?.trim() == "---") {
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (end >= 0) {
                var lastKey: String? = null
                for (l in lines.subList(1, end + 1)) {
                    val kv = l.split(":", limit = 2)
                    if (kv.size == 2 && !l.startsWith(" ") && !l.startsWith("-")) {
                        lastKey = kv[0].trim()
                        front[lastKey] = kv[1].trim()
                    } else if (lastKey != null && l.trim().startsWith("- ")) {
                        val v = l.trim().removePrefix("- ").trim()
                        front[lastKey] = listOf(front[lastKey].orEmpty(), v).filter { it.isNotEmpty() }.joinToString(", ")
                    }
                }
                lines = lines.drop(end + 2)
            }
        }
        val blocks = parseBlocks(lines).toMutableList()
        val titleIndex = blocks.indexOfFirst { it is MdHeading && it.level == 1 }
        val title = if (titleIndex >= 0) (blocks.removeAt(titleIndex) as MdHeading).text else null
        return MdDocument(front, title, blocks)
    }

    fun parseBlocks(lines: List<String>): List<MdBlock> {
        val out = ArrayList<MdBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val t = line.trim()
            when {
                t.isEmpty() -> i++

                t.startsWith("```") || t.startsWith("~~~") -> {
                    val fence = t.take(3)
                    val lang = t.drop(3).trim().lowercase()
                    val code = ArrayList<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith(fence)) code += lines[i++]
                    i++ // закрывающий fence
                    val text = code.joinToString("\n")
                    out += if (lang == "mermaid") {
                        MermaidParser.parse(text)?.let(::MdDiagram) ?: MdCode(lang, text)
                    } else MdCode(lang, text)
                }

                HEADING.matches(t) -> {
                    val m = HEADING.find(t)!!
                    out += MdHeading(m.groupValues[1].length, m.groupValues[2])
                    i++
                }

                t.startsWith(">") -> {
                    val inner = ArrayList<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        inner += lines[i].trimStart().removePrefix(">").let { if (it.startsWith(" ")) it.drop(1) else it }
                        i++
                    }
                    val head = CALLOUT.find(inner.first().trim())
                    out += if (head != null) {
                        val kind = head.groupValues[1].lowercase()
                        val fold = head.groupValues[2]
                        MdCallout(
                            kind = kind,
                            title = head.groupValues[3].ifBlank { defaultCalloutTitle(kind) },
                            foldable = fold.isNotEmpty(),
                            initiallyExpanded = fold != "-",
                            blocks = parseBlocks(inner.drop(1)),
                        )
                    } else MdQuote(parseBlocks(inner))
                }

                isTableStart(lines, i) -> {
                    val header = splitRow(lines[i])
                    i += 2
                    val rows = ArrayList<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val cells = splitRow(lines[i])
                        rows += List(header.size) { cells.getOrElse(it) { "" } }
                        i++
                    }
                    out += MdTable(header, rows)
                }

                RULE.matches(t) -> { out += MdRule; i++ }

                LIST_ITEM.matches(line) && indentOf(line) < 4 -> {
                    val (list, next) = parseList(lines, i)
                    out += list
                    i = next
                }

                else -> {
                    val sb = StringBuilder()
                    while (i < lines.size) {
                        val l = lines[i]
                        val lt = l.trim()
                        if (lt.isEmpty() || (sb.isNotEmpty() && startsBlock(lines, i))) break
                        if (sb.isNotEmpty()) {
                            val hardBreak = sb.endsWith("  ") || sb.endsWith("\\")
                            val trimmed = sb.trimEnd().removeSuffix("\\").toString()
                            sb.setLength(0); sb.append(trimmed)
                            sb.append(if (hardBreak) "\n" else " ")
                        }
                        sb.append(l.trimStart())
                        i++
                    }
                    out += MdParagraph(sb.toString().trim())
                }
            }
        }
        return out
    }

    private fun startsBlock(lines: List<String>, i: Int): Boolean {
        val l = lines[i]
        val t = l.trim()
        return t.startsWith("```") || t.startsWith(">") || HEADING.matches(t) || RULE.matches(t) ||
            (LIST_ITEM.matches(l) && indentOf(l) < 4) || isTableStart(lines, i)
    }

    private fun parseList(lines: List<String>, start: Int): Pair<MdList, Int> {
        val first = LIST_ITEM.find(lines[start])!!
        val baseIndent = indentOf(lines[start])
        val ordered = first.groupValues[2].first().isDigit()
        val startNumber = if (ordered) first.groupValues[2].dropLast(1).toIntOrNull() ?: 1 else 1
        val items = ArrayList<MdListItem>()
        var i = start
        while (i < lines.size) {
            val m = LIST_ITEM.find(lines[i]) ?: break
            if (indentOf(lines[i]) != baseIndent) break
            if (m.groupValues[2].first().isDigit() != ordered) break
            val contentIndent = m.groups[3]!!.range.first
            val body = arrayListOf(m.groupValues[3])
            i++
            while (i < lines.size) {
                val l = lines[i]
                if (l.isBlank()) {
                    var j = i + 1
                    while (j < lines.size && lines[j].isBlank()) j++
                    if (j < lines.size && indentOf(lines[j]) > baseIndent) {
                        body += ""; i++; continue
                    }
                    break
                }
                val ind = indentOf(l)
                if (ind <= baseIndent) break
                body += l.drop(minOf(ind, contentIndent).let { n -> leadingChars(l, n) })
                i++
            }
            var checked: Boolean? = null
            val head = body[0]
            if (head.startsWith("[ ] ") || head == "[ ]") { checked = false; body[0] = head.drop(3).trimStart() }
            else if (head.startsWith("[x] ", true) || head.equals("[x]", true)) { checked = true; body[0] = head.drop(3).trimStart() }
            items += MdListItem(checked, parseBlocks(body))
        }
        return MdList(ordered, startNumber, items) to i
    }

    private fun isTableStart(lines: List<String>, i: Int): Boolean =
        lines[i].trim().startsWith("|") && i + 1 < lines.size && TABLE_SEP.matches(lines[i + 1].trim())

    /** Делит строку таблицы по '|', кроме экранированных '\|' (так Obsidian пишет [[ссылка\|текст]]). */
    private fun splitRow(line: String): List<String> {
        var s = line.trim()
        if (s.startsWith("|")) s = s.drop(1)
        if (s.endsWith("|") && !s.endsWith("\\|")) s = s.dropLast(1)
        val cells = ArrayList<String>()
        val sb = StringBuilder()
        var inCode = false
        var k = 0
        while (k < s.length) {
            val c = s[k]
            when {
                c == '\\' && k + 1 < s.length && s[k + 1] == '|' -> { sb.append('|'); k++ }
                c == '`' -> { inCode = !inCode; sb.append(c) }
                c == '|' && !inCode -> { cells += sb.toString().trim(); sb.setLength(0) }
                else -> sb.append(c)
            }
            k++
        }
        cells += sb.toString().trim()
        return cells
    }

    private fun indentOf(line: String): Int {
        var n = 0
        for (c in line) {
            when (c) {
                ' ' -> n++
                '\t' -> n += 4
                else -> return n
            }
        }
        return n
    }

    /** Сколько символов строки занимают первые [width] позиций отступа (табы = 4). */
    private fun leadingChars(line: String, width: Int): Int {
        var w = 0
        var k = 0
        while (k < line.length && w < width) {
            w += if (line[k] == '\t') 4 else if (line[k] == ' ') 1 else break
            k++
        }
        return k
    }

    private fun defaultCalloutTitle(kind: String) = when (kind) {
        "abstract", "summary", "tldr" -> "Коротко"
        "tip", "hint" -> "Совет"
        "warning", "caution", "attention" -> "Внимание"
        "success", "check", "done" -> "Ответы"
        "example" -> "Пример"
        "question", "faq", "help" -> "Вопрос"
        "danger", "error" -> "Ошибка"
        "quote", "cite" -> "Цитата"
        else -> "Заметка"
    }
}
