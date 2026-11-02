package com.marat.albion.data.speech

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Слово из распознавания: [start]–[end] в кадрах по 10 мс от начала записи, [confidence] 0…1. */
data class HeardWord(val text: String, val start: Int, val end: Int, val confidence: Float)

enum class WordVerdict {
    /** Узнано уверенно. */
    CLEAR,
    /** Узнано, но Whisper сомневался — звуки размыты. */
    UNCLEAR,
    /** Услышано другое слово. */
    MISHEARD,
    /** Слово не услышано. */
    MISSING,
}

/** Слово фразы карточки и то, как его услышал Whisper. */
data class WordCheck(val expected: String, val verdict: WordVerdict, val heard: HeardWord?)

/** Разбор разборчивости: каждое слово фразы и всё, что услышал Whisper. */
data class SpeechCheck(val words: List<WordCheck>, val heard: List<HeardWord>) {
    val recognized get() = words.count { it.verdict == WordVerdict.CLEAR || it.verdict == WordVerdict.UNCLEAR }
    val allClear get() = words.all { it.verdict == WordVerdict.CLEAR }
}

/** Слова из токенов Whisper и их сверка с фразой карточки. Чистые функции — без модели. */
object SpokenWords {
    /** Ниже этой уверенности слово считается произнесённым нечётко. */
    const val UNCLEAR_BELOW = 0.5f
    private val WORD = Regex("[\\p{L}\\p{N}'’-]+")

    /** Слова фразы в исходном написании. */
    fun split(text: String): List<String> = WORD.findAll(text).map { it.value }.filter { key(it).isNotEmpty() }.toList()

    /** Ключ сравнения: регистр, апострофы, дефисы и британское написание не важны (Whisper пишет по-американски). */
    fun key(word: String): String {
        var k = word.lowercase().filter { it.isLetterOrDigit() }
        for ((from, to) in SPELLING) k = k.replace(from, to)
        if (k.length > 4 && k.endsWith("tre")) k = k.dropLast(3) + "ter"
        if (k.length > 5 && k.endsWith("ogue")) k = k.dropLast(4) + "og"
        return k
    }
    private val SPELLING = listOf("our" to "or", "ise" to "ize", "isa" to "iza", "yse" to "yze", "ence" to "ense", "ll" to "l", "ae" to "e")

    /**
     * Токены из albion_stt.cpp («t0 TAB t1 TAB p TAB текст») → слова. Новое слово начинается с пробела;
     * пунктуация приклеивается к слову, но не влияет на уверенность; шумовые пометки вроде «[BLANK_AUDIO]» отбрасываются.
     */
    fun parse(raw: String): List<HeardWord> {
        class Token(val t0: Int, val t1: Int, val p: Float, val text: String)
        val groups = mutableListOf<MutableList<Token>>()
        raw.lineSequence().forEach { line ->
            val parts = line.split('\t', limit = 4)
            if (parts.size < 4) return@forEach
            val token = Token(parts[0].toIntOrNull() ?: return@forEach, parts[1].toIntOrNull() ?: return@forEach,
                parts[2].toFloatOrNull() ?: return@forEach, parts[3])
            if (token.text.startsWith(" ") || groups.isEmpty()) groups += mutableListOf(token) else groups.last() += token
        }
        val words = mutableListOf<HeardWord>()
        var noise = false
        for (group in groups) {
            val text = group.joinToString("") { it.text }.trim()
            val bare = text.trimEnd('.', ',', '!', '?', ';', ':')
            val opens = bare.startsWith("[") || bare.startsWith("(") || bare.startsWith("*")
            if (opens) noise = true
            if (noise) {
                if (bare.endsWith("]") || bare.endsWith(")") || (bare.endsWith("*") && (bare.length > 1 || !opens))) noise = false
                continue
            }
            val spoken = group.filter { t -> t.text.any { it.isLetterOrDigit() } }
            val clean = text.trim { !it.isLetterOrDigit() }
            if (spoken.isEmpty() || key(clean).isEmpty()) continue
            // Среднее геометрическое: одна размытая часть слова заметно снижает итог, но не обнуляет его.
            val confidence = exp(spoken.map { ln(it.p.coerceIn(1e-4f, 1f).toDouble()) }.average()).toFloat()
            words += HeardWord(clean, max(0, spoken.first().t0), max(0, spoken.last().t1), confidence)
        }
        return words
    }

    /**
     * Сверка услышанного с фразой [expected] по словам (выравнивание с минимумом правок).
     * [alternatives] — для каждого слова фразы другие допустимые написания: так омофоны вроде read/red
     * засчитываются, если диктора Whisper услышал так же.
     */
    fun check(expected: List<String>, heard: List<HeardWord>, alternatives: List<Set<String>> = emptyList()): SpeechCheck {
        val keys = expected.map(::key)
        val heardKeys = heard.map { key(it.text) }
        fun same(i: Int, j: Int) = keys[i] == heardKeys[j] || heardKeys[j] in alternatives.getOrElse(i) { emptySet() }
        val n = keys.size
        val m = heard.size
        val cost = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) cost[i][0] = i
        for (j in 0..m) cost[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            cost[i][j] = min(min(cost[i - 1][j] + 1, cost[i][j - 1] + 1), cost[i - 1][j - 1] + if (same(i - 1, j - 1)) 0 else 1)
        }
        val result = arrayOfNulls<WordCheck>(n)
        var i = n
        var j = m
        while (i > 0) {
            when {
                j > 0 && cost[i][j] == cost[i - 1][j - 1] + if (same(i - 1, j - 1)) 0 else 1 -> {
                    val word = heard[j - 1]
                    val verdict = when {
                        !same(i - 1, j - 1) -> WordVerdict.MISHEARD
                        word.confidence < UNCLEAR_BELOW -> WordVerdict.UNCLEAR
                        else -> WordVerdict.CLEAR
                    }
                    result[i - 1] = WordCheck(expected[i - 1], verdict, word)
                    i--; j--
                }
                j > 0 && cost[i][j] == cost[i][j - 1] + 1 -> j--
                else -> { result[i - 1] = WordCheck(expected[i - 1], WordVerdict.MISSING, null); i-- }
            }
        }
        return SpeechCheck(result.map { requireNotNull(it) }, heard)
    }

    /** Слово, на которое приходится кадр [frame]; между словами — ближайшее. */
    fun wordAt(words: List<HeardWord>, frame: Int): HeardWord? =
        words.firstOrNull { frame in it.start..it.end }
            ?: words.minByOrNull { min(kotlin.math.abs(frame - it.start), kotlin.math.abs(frame - it.end)) }
}
