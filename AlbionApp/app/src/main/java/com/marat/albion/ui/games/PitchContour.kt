package com.marat.albion.ui.games

import com.marat.albion.data.audio.ProsodyAnalyzer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class PitchPoint(val frame: Int, val semitones: Float)

internal data class PitchContour(val segments: List<List<PitchPoint>>) {
    val available get() = segments.isNotEmpty()
}

/** Подготовка только для рисунка: исходные измерения и оценки попытки не меняются. */
internal fun preparePitchContour(values: FloatArray, energy: FloatArray? = null): PitchContour {
    val cleaned = FloatArray(values.size) { Float.NaN }
    var start = 0
    while (start < values.size) {
        if (!values[start].isFinite()) { start++; continue }
        var end = start + 1
        while (end < values.size && values[end].isFinite()) end++
        // Одиночные определения тона дают случайные точки, а не мелодику.
        if (end - start >= 3) {
            for (i in start until end) {
                val window = (maxOf(start, i - 2)..minOf(end - 1, i + 2)).map { values[it] }.sorted()
                cleaned[i] = window[window.size / 2]
            }
        }
        start = end
    }
    val observed = cleaned.count { it.isFinite() }
    if (observed < maxOf(8, ceil(values.size * 0.15).toInt())) return PitchContour(emptyList())

    // Короткие потери трекинга, в том числе на согласных. Длинные паузы и скачки
    // больше половины октавы остаются разрывами. По краям ничего не достраиваем.
    val maxGap = (0.12f / ProsodyAnalyzer.HOP_SEC).roundToInt()
    var previous = -1
    for (i in cleaned.indices) {
        if (!cleaned[i].isFinite()) continue
        val gap = i - previous - 1
        val hasSound = energy == null || (previous + 1 until i).all { (energy.getOrNull(it) ?: 0f) > 0.03f }
        if (previous >= 0 && gap in 1..maxGap && hasSound && abs(cleaned[i] - cleaned[previous]) <= 6f) {
            for (j in previous + 1 until i) {
                val fraction = (j - previous).toFloat() / (i - previous)
                cleaned[j] = cleaned[previous] + (cleaned[i] - cleaned[previous]) * fraction
            }
        }
        previous = i
    }

    val segments = mutableListOf<List<PitchPoint>>()
    start = 0
    while (start < cleaned.size) {
        if (!cleaned[start].isFinite()) { start++; continue }
        var end = start + 1
        while (end < cleaned.size && cleaned[end].isFinite()) end++
        val from = start
        val to = end
        segments += (from until to).map { i ->
            // Мягкое сглаживание в пределах сегмента, без выхода за его значения.
            var sum = 0f
            var weights = 0
            for (j in maxOf(from, i - 2)..minOf(to - 1, i + 2)) {
                val weight = 3 - abs(j - i)
                sum += cleaned[j] * weight
                weights += weight
            }
            PitchPoint(i, sum / weights)
        }
        start = end
    }
    // Несколько разрозненных штрихов не выдают полноценную мелодику.
    return PitchContour(segments.takeIf { runs -> runs.any { it.size >= 8 } } ?: emptyList())
}
