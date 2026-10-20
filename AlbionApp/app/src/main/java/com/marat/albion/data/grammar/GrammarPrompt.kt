package com.marat.albion.data.grammar

/** Текст задания и отдельные пропуски. Любая полоса из трёх и более '_' — один пропуск. */
class GrammarPrompt(text: String) {
    val parts: List<String> = GAP.split(text)
    val gapCount: Int get() = parts.size - 1

    /** В одном пропуске ответ целиком; в нескольких — значения по порядку через '/'. */
    fun answerParts(answer: String): List<String>? {
        if (gapCount == 0) return null
        val values = if (gapCount == 1) listOf(answer.trim()) else answer.split('/').map { it.trim() }
        return values.takeIf { it.size == gapCount && it.all(String::isNotEmpty) }
    }

    /** Сокращение присоединяется к подлежащему: "She ___" + "'s gone" → "She's gone". */
    fun textPart(index: Int, followingAnswer: String?): String =
        if (followingAnswer != null && CONTRACTION.containsMatchIn(followingAnswer)) parts[index].trimEnd() else parts[index]

    /** Не подставляет неполный ответ и не повторяет его в остальных пропусках. */
    fun complete(answer: String): String? {
        val values = answerParts(answer) ?: return null
        return buildString {
            parts.forEachIndexed { index, part ->
                append(textPart(index, values.getOrNull(index)))
                if (index < values.size) append(values[index])
            }
        }
    }

    companion object {
        private val GAP = Regex("_{3,}")
        private val CONTRACTION = Regex("^[’‘'`](?:s|d|ll|re|ve|m)(?:\\s|$)", RegexOption.IGNORE_CASE)
    }
}
