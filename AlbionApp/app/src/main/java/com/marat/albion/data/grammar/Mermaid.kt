package com.marat.albion.data.grammar

enum class NodeShape { Box, Round, Decision, Circle }

data class FlowNode(val id: String, val label: String, val shape: NodeShape)
data class FlowEdge(val from: String, val to: String, val label: String?)

/** Граф из блока ```mermaid flowchart```. Порядок узлов — порядок появления в исходнике. */
data class FlowGraph(val direction: String, val nodes: List<FlowNode>, val edges: List<FlowEdge>) {
    private val byId = nodes.associateBy { it.id }
    fun node(id: String) = byId.getValue(id)
    fun outgoing(id: String) = edges.filter { it.from == id }
    fun incoming(id: String) = edges.filter { it.to == id }
}

/**
 * Разбирает подмножество синтаксиса flowchart, которое встречается в учебнике:
 * узлы A["текст"], A("…"), A{"…"}, A(("…")), рёбра -->, ---, -.->, ==>,
 * подписи рёбер |текст| и -- текст -->, цепочки A --> B --> C и A & B --> C.
 */
object MermaidParser {

    private val HEADER = Regex("^(flowchart|graph)\\s*(TB|TD|BT|LR|RL)?", RegexOption.IGNORE_CASE)
    private val SKIP = Regex("^(style|classDef|class|linkStyle|click|subgraph|end|direction|%%)\\b.*")
    private val OPENERS = listOf("([" to "])", "[[" to "]]", "[(" to ")]", "((" to "))", "{{" to "}}", "[" to "]", "(" to ")", "{" to "}", ">" to "]")

    fun parse(source: String): FlowGraph? {
        val lines = source.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        val header = HEADER.find(lines.first()) ?: return null
        val direction = header.groupValues[2].uppercase().ifEmpty { "TD" }
        val nodes = LinkedHashMap<String, FlowNode>()
        val edges = ArrayList<FlowEdge>()

        for (raw in lines.drop(1)) {
            if (SKIP.matches(raw)) continue
            for (statement in raw.split(';')) parseStatement(statement.trim(), nodes, edges)
        }
        if (nodes.isEmpty()) return null
        return FlowGraph(direction, nodes.values.toList(), edges)
    }

    private fun parseStatement(s: String, nodes: MutableMap<String, FlowNode>, edges: MutableList<FlowEdge>) {
        if (s.isEmpty()) return
        val scanner = Scanner(s)
        var previous: List<String> = scanner.nodeGroup(nodes) ?: return
        while (true) {
            scanner.skipSpaces()
            if (scanner.atEnd()) break
            val label = scanner.edge() ?: break
            scanner.skipSpaces()
            val next = scanner.nodeGroup(nodes) ?: break
            for (a in previous) for (b in next) edges += FlowEdge(a, b, label.ifBlank { null })
            previous = next
        }
    }

    private class Scanner(val s: String) {
        var pos = 0
        fun atEnd() = pos >= s.length
        fun skipSpaces() { while (pos < s.length && s[pos].isWhitespace()) pos++ }

        /** A или A & B. */
        fun nodeGroup(nodes: MutableMap<String, FlowNode>): List<String>? {
            val ids = ArrayList<String>()
            while (true) {
                skipSpaces()
                ids += node(nodes) ?: return ids.ifEmpty { null }
                skipSpaces()
                if (pos < s.length && s[pos] == '&') pos++ else break
            }
            return ids
        }

        fun node(nodes: MutableMap<String, FlowNode>): String? {
            val start = pos
            while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_')) pos++
            if (pos == start) return null
            val id = s.substring(start, pos)
            val opener = OPENERS.firstOrNull { s.startsWith(it.first, pos) }
            if (opener != null) {
                pos += opener.first.length
                val label: String
                if (pos < s.length && s[pos] == '"') {
                    val end = s.indexOf('"', pos + 1).let { if (it < 0) s.length else it }
                    label = s.substring(pos + 1, end)
                    pos = end + 1
                    val close = s.indexOf(opener.second, pos)
                    pos = if (close < 0) s.length else close + opener.second.length
                } else {
                    val close = s.indexOf(opener.second, pos).let { if (it < 0) s.length else it }
                    label = s.substring(pos, close)
                    pos = (close + opener.second.length).coerceAtMost(s.length)
                }
                val shape = when (opener.first) {
                    "{", "{{" -> NodeShape.Decision
                    "(", "([" -> NodeShape.Round
                    "((" -> NodeShape.Circle
                    else -> NodeShape.Box
                }
                nodes[id] = FlowNode(id, cleanLabel(label), shape)
            } else if (id !in nodes) {
                nodes[id] = FlowNode(id, id, NodeShape.Box)
            }
            // Классы вида A:::accent
            if (s.startsWith(":::", pos)) {
                pos += 3
                while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_' || s[pos] == '-')) pos++
            }
            return id
        }

        /** Возвращает подпись ребра ("" — без подписи) или null, если ребра нет. */
        fun edge(): String? {
            val start = pos
            // "-- текст -->" / "== текст ==>"
            if ((s.startsWith("-- ", pos) || s.startsWith("== ", pos))) {
                val arrowAt = Regex("(-->|---|==>)").find(s, pos + 3)
                if (arrowAt != null) {
                    val text = s.substring(pos + 3, arrowAt.range.first).trim()
                    pos = arrowAt.range.last + 1
                    return cleanLabel(text)
                }
            }
            while (pos < s.length && s[pos] in "-.=<>ox") {
                // 'o' и 'x' — концы стрелок только сразу после тире
                if ((s[pos] == 'o' || s[pos] == 'x') && (pos == start || s[pos - 1] !in "-=>")) break
                if ((s[pos] == 'o' || s[pos] == 'x') && pos + 1 < s.length && s[pos + 1].isLetterOrDigit()) break
                pos++
            }
            if (pos - start < 2) { pos = start; return null }
            skipSpaces()
            if (pos < s.length && s[pos] == '|') {
                val end = s.indexOf('|', pos + 1).let { if (it < 0) s.length else it }
                val text = s.substring(pos + 1, end)
                pos = (end + 1).coerceAtMost(s.length)
                return cleanLabel(text)
            }
            return ""
        }
    }

    private fun cleanLabel(raw: String): String = raw.trim()
        .removeSurrounding("\"")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace("#quot;", "\"")
        .replace("#35;", "#")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .trim()
}
