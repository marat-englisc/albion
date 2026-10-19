package com.marat.albion.data.games

import com.marat.albion.data.content.IrregularData
import com.marat.albion.data.content.VerbForm

/**
 * Задание тренажёра времён: пример из таблицы неправильных глаголов, где нужная
 * форма заменена пропуском. Подсказка — инфинитив, ответ — форма из примера.
 */
data class TenseTask(
    val before: String,
    val after: String,
    /** Форма, стоящая в примере. */
    val answer: String,
    /** Что засчитывать: варианты той же формы (learnt / learned), кроме was / were. */
    val accepted: List<String>,
    val infinitive: String,
    /** 0 — инфинитив, 1 — Past Simple, 2 — Past Participle. */
    val formIndex: Int,
    /** Варианты для режима выбора: три формы и «неправильная правильная» (goed). */
    val options: List<String>,
    /** Почему здесь именно эта форма — по словам перед пропуском. */
    val rule: String,
) {
    val sentence get() = before + answer + after
    val formLabel get() = FORM_LABELS[formIndex]

    companion object {
        val FORM_LABELS = listOf("Инфинитив · V1", "Past Simple · V2", "Past Participle · V3")
    }
}

object TenseDrill {

    private val HAVE = setOf("have", "has", "'ve", "haven't", "hasn't", "having")
    private val HAD = setOf("had", "'d", "hadn't")
    private val BE = setOf("be", "been", "being", "is", "are", "am", "was", "were", "isn't", "aren't", "wasn't", "weren't", "get", "got")
    private val MODAL = setOf(
        "can", "could", "may", "might", "must", "should", "would", "will", "shall", "'ll", "to",
        "can't", "cannot", "couldn't", "mustn't", "shouldn't", "wouldn't", "won't", "do", "does", "did",
        "don't", "doesn't", "didn't", "let", "let's", "help", "make", "please",
    )
    /** Слова, которые могут стоять между вспомогательным глаголом и формой: «has never seen». */
    private val SKIP = setOf(
        "not", "never", "ever", "just", "already", "always", "often", "recently", "finally", "also", "really",
        "still", "only", "probably", "certainly", "usually", "sometimes", "even", "once", "all", "both",
        "i", "you", "he", "she", "it", "we", "they", "the", "a", "an", "this", "that", "your", "my", "our", "their", "his", "her",
    )

    /** Все задания, которые можно составить по глаголу. */
    fun tasks(data: IrregularData): List<TenseTask> {
        if (data.forms.size < 3) return emptyList()
        val texts = data.forms.map { f -> f.variants.map { it.text.trim() }.filter(String::isNotEmpty) }
        val infinitive = texts[0].firstOrNull() ?: return emptyList()
        // cut / cut / cut — тренировать нечего.
        if (texts.map { it.toSet() }.toSet().size == 1) return emptyList()
        val result = mutableListOf<TenseTask>()
        data.forms.forEachIndexed { formIndex, form ->
            for (example in form.examples) {
                task(example, form, formIndex, texts, infinitive)?.let { result += it }
            }
        }
        return result
    }

    private fun task(example: String, form: VerbForm, formIndex: Int, texts: List<List<String>>, infinitive: String): TenseTask? {
        val sentence = example.trim()
        val match = texts[formIndex].firstNotNullOfOrNull { v ->
            Regex("(?i)(?<![\\w'])" + Regex.escape(v) + "(?![\\w'])").find(sentence)
        } ?: return null
        val answer = match.value
        val isBe = infinitive.equals("be", ignoreCase = true)
        val accepted = if (isBe) listOf(answer) else (listOf(answer) + texts[formIndex]).distinctBy { it.lowercase() }
        val before = sentence.substring(0, match.range.first)
        val after = sentence.substring(match.range.last + 1)
        return TenseTask(
            before = before,
            after = after,
            answer = answer,
            accepted = accepted,
            infinitive = infinitive,
            formIndex = formIndex,
            options = options(texts, answer, formIndex, isBe),
            rule = rule(before, formIndex),
        )
    }

    private fun options(texts: List<List<String>>, answer: String, formIndex: Int, isBe: Boolean): List<String> {
        val list = mutableListOf<String>()
        texts.forEachIndexed { i, variants ->
            // У нужной формы — ровно тот вариант, что в примере; у «be» — обе формы was / were.
            when {
                i == formIndex -> list += answer
                isBe -> list += variants
                else -> list += variants.first()
            }
            if (isBe && i == formIndex) list += variants
        }
        if (!isBe) list += regularized(texts[0].first())
        // abide → abided / abode: если формы совпали, добавляем и вторые варианты.
        if (list.distinctBy { it.lowercase() }.size < 4) list += texts.flatten()
        return list.distinctBy { it.lowercase() }
    }

    /** Ошибка «по правилу»: go → goed, write → writed, cry → cried. */
    fun regularized(infinitive: String): String {
        val w = infinitive.lowercase()
        return when {
            w.endsWith("e") -> w + "d"
            w.endsWith("y") && w.length > 1 && w[w.length - 2] !in "aeiou" -> w.dropLast(1) + "ied"
            else -> w + "ed"
        }
    }

    /** Объяснение по ближайшему значимому слову перед пропуском. */
    fun rule(before: String, formIndex: Int): String {
        val words = before.lowercase().replace("’", "'").split(Regex("[^a-z']+")).filter { it.isNotBlank() }.reversed()
        val prev = words.firstOrNull { it !in SKIP }
        val sentenceStart = before.isBlank()
        return when (formIndex) {
            2 -> when (prev) {
                in HAVE -> "Present Perfect: have / has + V3 — результат к настоящему моменту."
                in HAD -> "Past Perfect: had + V3 — действие произошло раньше другого момента в прошлом."
                in BE -> "Пассив: be + V3 — действие совершают над подлежащим."
                else -> "Past Participle (V3) — третья форма, причастие прошедшего времени."
            }
            1 -> "Past Simple (V2) — законченное действие в прошлом; вспомогательный глагол не нужен."
            else -> when {
                sentenceStart -> "Повелительное наклонение — начальная форма (V1)."
                prev == "to" -> "После to — инфинитив (V1)."
                prev in setOf("do", "does", "did", "don't", "doesn't", "didn't") -> "После do / does / did — всегда начальная форма (V1), даже в прошедшем времени."
                prev in MODAL -> "После модального глагола (can, must, will…) — начальная форма (V1) без to."
                else -> "Настоящее время — начальная форма (V1); с he / she / it добавляется -s."
            }
        }
    }
}
