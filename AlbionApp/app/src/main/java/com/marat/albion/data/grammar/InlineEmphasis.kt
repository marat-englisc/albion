package com.marat.albion.data.grammar

data class EmphasisMark(val length: Int, val strong: Boolean, val opening: Boolean)

/**
 * Находит парные '*' / '_' с учётом границ слов и вложенности.
 * Непарные маркеры и пропуски остаются текстом. Код и ссылки не участвуют
 * в поиске пар. Правила границ: https://spec.commonmark.org/0.31.2/#emphasis-and-strong-emphasis
 */
object InlineEmphasis {
    private class Run(val start: Int, val length: Int, val marker: Char, val canOpen: Boolean, val canClose: Boolean) {
        var opened = 0
        var closed = 0
        var active = true
        val remaining get() = length - opened - closed
    }

    fun marks(text: String): Map<Int, EmphasisMark> {
        val runs = ArrayList<Run>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '\\' && text.getOrNull(i + 1)?.let { it in ESCAPABLE } == true) { i += 2; continue }
            if (ch == '`') {
                val end = text.indexOf('`', i + 1)
                if (end >= 0) { i = end + 1; continue }
            }
            if (text.startsWith("[[", i)) {
                val end = text.indexOf("]]", i + 2)
                if (end >= 0) { i = end + 2; continue }
            }
            if (ch == '[') {
                val close = text.indexOf("](", i + 1)
                val end = if (close >= 0) text.indexOf(')', close + 2) else -1
                if (end >= 0 && !text.substring(i + 1, close).contains('\n')) { i = end + 1; continue }
            }
            if (ch != '*' && ch != '_') { i++; continue }
            var end = i + 1
            while (end < text.length && text[end] == ch) end++
            val before = if (i == 0) null else text.codePointBefore(i)
            val after = if (end == text.length) null else text.codePointAt(end)
            // Учебные пропуски сохраняются и рядом со скобками / пунктуацией.
            if (ch == '_' && end - i >= 3 && (before == null || !Character.isLetterOrDigit(before)) &&
                (after == null || !Character.isLetterOrDigit(after))) { i = end; continue }
            val left = !whitespace(after) && (!punctuation(after) || whitespace(before) || punctuation(before))
            val right = !whitespace(before) && (!punctuation(before) || whitespace(after) || punctuation(after))
            runs += Run(i, end - i, ch,
                canOpen = left && (ch == '*' || !right || punctuation(before)),
                canClose = right && (ch == '*' || !left || punctuation(after)))
            i = end
        }

        val marks = HashMap<Int, EmphasisMark>()
        for (ci in runs.indices) {
            val closer = runs[ci]
            if (!closer.active || !closer.canClose) continue
            while (closer.remaining > 0) {
                val oi = (ci - 1 downTo 0).firstOrNull { index ->
                    val opener = runs[index]
                    opener.active && opener.canOpen && opener.marker == closer.marker && opener.remaining > 0 &&
                        !((opener.canClose || closer.canOpen) && (opener.remaining + closer.remaining) % 3 == 0 &&
                            (opener.remaining % 3 != 0 || closer.remaining % 3 != 0))
                } ?: break
                val opener = runs[oi]
                val length = if (opener.remaining >= 2 && closer.remaining >= 2) 2 else 1
                marks[opener.start + opener.length - opener.opened - length] = EmphasisMark(length, length == 2, true)
                marks[closer.start + closer.closed] = EmphasisMark(length, length == 2, false)
                opener.opened += length
                closer.closed += length
                // Непарный маркер внутри уже закрытого выделения не может открыться снаружи.
                for (between in oi + 1 until ci) runs[between].active = false
            }
        }
        return marks
    }

    private fun whitespace(cp: Int?) = cp == null || Character.isWhitespace(cp) || Character.isSpaceChar(cp)
    private fun punctuation(cp: Int?) = cp != null && Character.getType(cp) in PUNCTUATION
    private val PUNCTUATION = setOf(
        Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(), Character.MATH_SYMBOL.toInt(), Character.CURRENCY_SYMBOL.toInt(),
        Character.MODIFIER_SYMBOL.toInt(), Character.OTHER_SYMBOL.toInt(),
    )
    private val ESCAPABLE = "\\`*_[]()#|~=<>!-.".toSet()
}
