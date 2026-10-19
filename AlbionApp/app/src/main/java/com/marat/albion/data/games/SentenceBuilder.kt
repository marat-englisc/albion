package com.marat.albion.data.games

/**
 * Предложение для игры «Собери предложение»: плашки (слова с прилипшими запятыми, а идиома
 * или коллокация — одной плашкой) и завершающий знак, который стоит на месте.
 */
data class SentencePuzzle(
    /** Предложение целиком — показывается после ответа и озвучивается. */
    val original: String,
    /** Плашки в правильном порядке, как их показывать. */
    val tokens: List<String>,
    /** «.», «?», «!» или пусто. */
    val ending: String,
) {
    /** Порядок верен, если плашки совпадают (регистр не важен: одинаковые слова взаимозаменяемы). */
    fun isCorrect(order: List<String>): Boolean =
        order.size == tokens.size && order.zip(tokens).all { (a, b) -> a.equals(b, ignoreCase = true) }

    /** Позиции, где стоит не та плашка. */
    fun wrongPositions(order: List<String>): Set<Int> =
        order.indices.filter { i -> !order[i].equals(tokens.getOrNull(i) ?: "", ignoreCase = true) }.toSet()

    companion object {
        private const val MIN_TILES = 4
        private const val MAX_TILES = 13
        private val ENDING = Regex("[.!?…]+$")
        /** Границы, по которым длинный пример делится на части: предложения, «:» и «;». */
        private val PARTS = Regex("(?<=[.!?])\\s+|\\s*[:;]\\s*")

        /**
         * Слова, с которых начинается предложение и которые не имена собственные, —
         * их показываем со строчной буквы, чтобы заглавная не подсказывала первое слово.
         */
        private val LOWERCASE_START = setOf(
            "a", "an", "the", "he", "she", "it", "we", "they", "you", "my", "your", "his", "her", "its", "our", "their",
            "this", "that", "these", "those", "there", "here", "if", "when", "after", "before", "as", "because", "although",
            "can", "could", "do", "does", "did", "is", "are", "was", "were", "have", "has", "had", "will", "would", "should",
            "what", "where", "why", "how", "who", "which", "please", "let's", "don't", "doesn't", "didn't", "it's", "there's",
            "she's", "he's", "we're", "they're", "you're", "every", "some", "most", "many", "all", "no", "not", "never",
            "she'll", "he'll", "we'll", "they'll", "you'll", "once", "while", "since", "until", "so", "but", "and", "or",
            "one", "two", "three", "last", "next", "yesterday", "today", "tomorrow", "in", "on", "at", "for", "with", "by",
            "from", "to", "of", "about", "during", "without", "even", "just", "only", "also", "luckily", "unfortunately",
            "losing", "missing", "working", "living", "saving", "getting", "being", "having", "making", "taking",
        )

        /**
         * [chunk] — выражение, которое нужно оставить одной плашкой (идиома, коллокация).
         * null — пример не подходит: слишком короткий, длинный или с кавычками/скобками.
         */
        fun from(sentence: String, chunk: String? = null): SentencePuzzle? {
            val clean = sentence.replace(Regex("\\s+"), " ").trim()
            if (clean.isEmpty() || clean.any { it in "\"“”()[]/" }) return null
            val parts = clean.split(PARTS).map { it.trim() }.filter { it.isNotEmpty() && it != clean }
            // Сначала пример целиком, затем его части. Если выражение в примере есть,
            // годится только часть с ним — иначе тренировка теряет смысл.
            val hasChunk = chunk != null && clean.contains(chunk, ignoreCase = true)
            val candidates = (listOf(clean) + parts).filter { !hasChunk || it.contains(chunk!!, ignoreCase = true) }
            return candidates.firstNotNullOfOrNull { build(it, chunk) }
        }

        private fun build(text: String, chunk: String?): SentencePuzzle? {
            val ending = ENDING.find(text)?.value?.take(1) ?: "."
            val body = text.replace(ENDING, "").trim()
            // Внутри не должно быть конца другого предложения: «...to spare. It was...».
            if (Regex("[.!?]\\s").containsMatchIn(body)) return null
            var words = body.split(' ').filter { it.isNotBlank() }
            if (chunk != null) words = merge(words, chunk)
            if (words.size !in MIN_TILES..MAX_TILES || words.any { ' ' !in it && it.length > 22 }) return null
            val first = words.first()
            val tiles = if (' ' !in first && first.lowercase() in LOWERCASE_START) listOf(first.lowercase()) + words.drop(1) else words
            val original = body.replaceFirstChar { it.uppercase() } + ending
            return SentencePuzzle(original, tiles, ending)
        }

        /** Склеивает слова выражения в одну плашку (если оно встречается в предложении как есть). */
        private fun merge(words: List<String>, chunk: String): List<String> {
            val parts = chunk.trim().split(' ').filter { it.isNotBlank() }
            if (parts.size < 2) return words
            fun bare(w: String) = w.trimEnd(',', ';', ':').lowercase()
            for (i in 0..words.size - parts.size) {
                if (parts.indices.all { k -> bare(words[i + k]) == parts[k].lowercase() }) {
                    val merged = words.subList(i, i + parts.size).joinToString(" ")
                    return words.subList(0, i) + merged + words.subList(i + parts.size, words.size)
                }
            }
            return words
        }

        /** Первый подходящий пример карточки. */
        fun fromExamples(examples: List<String>, chunk: String? = null): SentencePuzzle? =
            examples.firstNotNullOfOrNull { from(it, chunk) }
    }
}
