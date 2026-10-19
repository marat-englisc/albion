package com.marat.albion.data.library

/**
 * Находит в тексте книги слова и выражения из ваших карточек — с учётом форм
 * (studied → study, went → go) и подстановок в идиомах (drive someone up the wall →
 * «drove me up the wall»). Служебные слова не подсвечиваются, даже если они есть в карточках.
 */
class WordMarker(terms: Collection<String>, private val forms: Map<String, String> = emptyMap()) {

    private val single = HashSet<String>()
    /** Первое слово выражения → варианты выражения (слова по порядку). */
    private val phrases = HashMap<String, MutableList<List<String>>>()

    init {
        for (raw in terms) {
            val term = raw.lowercase().replace('’', '\'').replace(Regex("\\(.*?\\)"), " ").replace(Regex("[^a-z' -]"), " ").trim()
            val words = term.split(Regex("\\s+")).filter { it.isNotBlank() }
            when {
                words.isEmpty() -> {}
                words.size == 1 -> if (words[0] !in STOP && words[0].length > 1) single += words[0]
                words.first() !in WILDCARDS -> phrases.getOrPut(words.first()) { mutableListOf() } += words
            }
        }
        // Длинные выражения проверяем первыми.
        phrases.values.forEach { list -> list.sortByDescending { it.size } }
    }

    val isEmpty get() = single.isEmpty() && phrases.isEmpty()

    /** Диапазоны символов [first, last] с подсвечиваемыми словами и выражениями. */
    fun ranges(text: String): List<IntRange> {
        if (isEmpty) return emptyList()
        val tokens = WORD.findAll(text).toList()
        val result = mutableListOf<IntRange>()
        var i = 0
        while (i < tokens.size) {
            val lemmas = lemmas(tokens[i].value)
            val phraseEnd = lemmas.firstNotNullOfOrNull { lemma ->
                phrases[lemma]?.firstNotNullOfOrNull { phrase -> matchPhrase(tokens, i, phrase) }
            }
            when {
                phraseEnd != null -> {
                    result += tokens[i].range.first..tokens[phraseEnd].range.last
                    i = phraseEnd + 1
                }
                lemmas.any { it in single } -> { result += tokens[i].range; i++ }
                else -> i++
            }
        }
        return result
    }

    /** Индекс последнего слова выражения или null. Подстановки (someone, one's) — любое одно слово. */
    private fun matchPhrase(tokens: List<MatchResult>, start: Int, phrase: List<String>): Int? {
        if (start + phrase.size > tokens.size) return null
        for (k in 1 until phrase.size) {
            val want = phrase[k]
            val got = tokens[start + k].value.lowercase().replace('’', '\'')
            val ok = when (want) {
                in WILDCARDS -> true
                "one's" -> got in POSSESSIVE || got.endsWith("'s")
                else -> got == want || (k == phrase.lastIndex && want in lemmas(got))
            }
            if (!ok) return null
        }
        return start + phrase.size - 1
    }

    private fun lemmas(word: String): List<String> {
        // Само слово — всегда кандидат: иначе «a blessing in disguise» не нашлось бы из-за короткого «a».
        val base = listOf(word.lowercase().replace('’', '\'')) + wordCandidates(word)
        return (base + base.mapNotNull { forms[it] }).distinct()
    }

    companion object {
        private val WORD = Regex("[A-Za-z]+(?:['’-][A-Za-z]+)*")
        private val WILDCARDS = setOf("someone", "somebody", "something", "sb", "sth", "smb", "smth")
        private val POSSESSIVE = setOf("my", "your", "his", "her", "its", "our", "their")

        /** Слишком частые слова — их подсветка мешала бы читать. */
        private val STOP = setOf(
            "a", "an", "the", "and", "or", "but", "if", "of", "to", "in", "on", "at", "by", "for", "with", "from", "up", "out",
            "about", "into", "over", "after", "before", "as", "so", "than", "then", "that", "this", "these", "those", "there",
            "here", "is", "are", "was", "were", "be", "been", "being", "am", "do", "does", "did", "have", "has", "had", "will",
            "would", "can", "could", "may", "might", "must", "shall", "should", "not", "no", "yes", "i", "you", "he", "she",
            "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their", "what", "who",
            "which", "when", "where", "why", "how", "all", "some", "any", "one", "two", "very", "just", "also", "only", "too",
            "go", "get", "make", "say", "see", "come", "take", "know", "think", "look", "want", "give", "use", "find", "tell",
        )
    }
}
