package com.marat.albion.data.audio

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Мелодика и громкость фразы по кадрам (шаг 10 мс), уже без тишины по краям.
 * [pitch] — высота в полутонах относительно среднего тона говорящего (NaN — глухой кадр),
 * поэтому мужской и женский голос сравниваются честно.
 */
class Prosody(
    val energy: FloatArray,
    val pitch: FloatArray,
    val medianHz: Float,
) {
    val frames get() = energy.size
    val seconds get() = frames * ProsodyAnalyzer.HOP_SEC
    val voiced get() = pitch.count { !it.isNaN() }
}

/** Итог сравнения записи с оригиналом. Проценты 0…100; null — не удалось оценить. */
data class ShadowReport(
    val overall: Int,
    val intonation: Int?,
    val rhythm: Int,
    val tempo: Int,
    /** Во сколько раз вы говорили дольше оригинала (1.3 — медленнее, 0.8 — быстрее). */
    val tempoRatio: Float,
    val tips: List<String>,
    /** Для графика — всё на шкале времени оригинала. */
    val originalPitch: FloatArray,
    val minePitch: FloatArray,
    val originalEnergy: FloatArray,
    val mineEnergy: FloatArray,
)

object ProsodyAnalyzer {
    const val RATE = 16_000
    private const val HOP = 160 // 10 мс
    const val HOP_SEC = HOP / RATE.toFloat()
    private const val INTEGRATION = 320 // окно сравнения YIN, 20 мс
    private const val MIN_HZ = 70f
    private const val MAX_HZ = 450f
    private val TAU_MIN = (RATE / MAX_HZ).toInt()
    private val TAU_MAX = (RATE / MIN_HZ).toInt()
    private val FRAME = INTEGRATION + TAU_MAX + 2

    /** Разбор моно-сигнала 16 кГц (−1…1). null — слишком коротко или одна тишина. */
    fun analyze(x: FloatArray): Prosody? {
        val n = (x.size - FRAME) / HOP + 1
        if (n < 8) return null
        val db = FloatArray(n) { i ->
            var s = 0.0
            val start = i * HOP
            for (j in start until start + INTEGRATION) s += x[j] * x[j]
            (20 * log10(sqrt(s / INTEGRATION) + 1e-9)).toFloat()
        }
        val peak = db.max()
        val floor = db.sorted()[n / 10]
        if (peak - floor < 10f || peak < -55f) return null
        val threshold = max(peak - 35f, floor + 8f)
        val first = db.indexOfFirst { it > threshold }
        val last = db.indexOfLast { it > threshold }
        if (first < 0 || last - first < 5) return null
        val from = max(0, first - 2)
        val to = min(n - 1, last + 2)
        val len = to - from + 1

        val energy = FloatArray(len) { k -> ((db[from + k] - threshold) / (peak - threshold)).coerceIn(0f, 1f) }
        val hz = FloatArray(len) { k ->
            if (db[from + k] > threshold) yin(x, (from + k) * HOP) else Float.NaN
        }
        val smooth = medianFilter(hz, 5)
        val voicedHz = smooth.filter { !it.isNaN() }.sorted()
        if (voicedHz.size < 3) return Prosody(energy, FloatArray(len) { Float.NaN }, 0f)
        val median = voicedHz[voicedHz.size / 2]
        val pitch = FloatArray(len) { k ->
            val h = smooth[k]
            if (h.isNaN()) Float.NaN else (12 * log2(h / median)).let { s -> if (abs(s) > 12f) Float.NaN else s }
        }
        return Prosody(energy, pitch, median)
    }

    /** Основной тон кадра по алгоритму YIN; NaN — кадр без отчётливого тона. */
    fun yin(x: FloatArray, start: Int): Float {
        if (start + FRAME > x.size) return Float.NaN
        val d = FloatArray(TAU_MAX + 2)
        for (tau in 1..TAU_MAX + 1) {
            var s = 0f
            for (j in start until start + INTEGRATION) {
                val diff = x[j] - x[j + tau]
                s += diff * diff
            }
            d[tau] = s
        }
        // Кумулятивная нормированная разность.
        val cmnd = FloatArray(TAU_MAX + 2)
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1..TAU_MAX + 1) {
            running += d[tau]
            cmnd[tau] = if (running == 0f) 1f else d[tau] * tau / running
        }
        fun period(threshold: Float): Int? {
            var tau = TAU_MIN
            while (tau <= TAU_MAX) {
                if (cmnd[tau] < threshold) {
                    while (tau + 1 <= TAU_MAX && cmnd[tau + 1] < cmnd[tau]) tau++
                    return tau
                }
                tau++
            }
            return null
        }
        // Сначала ищем отчётливый период. На придыхании и мягких окончаниях
        // допускаем более слабый: строгий порог терял слышимый тон целыми слогами.
        // Шумовые кадры без выраженного минимума по-прежнему не получают высоту.
        val tau = period(0.15f) ?: period(0.25f) ?: return Float.NaN
        // Параболическое уточнение минимума.
        val a = cmnd[tau - 1]; val b = cmnd[tau]; val c = cmnd[tau + 1]
        val denom = a - 2 * b + c
        val shift = if (denom != 0f) 0.5f * (a - c) / denom else 0f
        return RATE / (tau + shift.coerceIn(-1f, 1f))
    }

    private fun medianFilter(v: FloatArray, size: Int): FloatArray {
        val half = size / 2
        return FloatArray(v.size) { i ->
            if (v[i].isNaN()) return@FloatArray Float.NaN
            val window = (max(0, i - half)..min(v.lastIndex, i + half)).map { v[it] }.filter { !it.isNaN() }.sorted()
            window[window.size / 2]
        }
    }

    // ------------------------------------------------------------------ сравнение

    /** Сравнивает запись [mine] с оригиналом [original]. */
    fun compare(original: Prosody, mine: Prosody): ShadowReport {
        val path = dtw(original.energy, mine.energy)
        val n = original.frames
        // Для каждого кадра оригинала — среднее по сопоставленным кадрам записи.
        val sumE = FloatArray(n); val sumP = FloatArray(n); val cntE = IntArray(n); val cntP = IntArray(n)
        val firstJ = IntArray(n) { -1 }
        for ((i, j) in path) {
            sumE[i] += mine.energy[j]; cntE[i]++
            if (firstJ[i] < 0) firstJ[i] = j
            val p = mine.pitch[j]
            if (!p.isNaN()) { sumP[i] += p; cntP[i]++ }
        }
        val mineEnergy = FloatArray(n) { if (cntE[it] == 0) 0f else sumE[it] / cntE[it] }
        val minePitch = FloatArray(n) { if (cntP[it] == 0) Float.NaN else sumP[it] / cntP[it] }

        // Интонация: форма мелодики там, где звучат оба голоса.
        val pairs = (0 until n).filter { !original.pitch[it].isNaN() && !minePitch[it].isNaN() }
        val intonation: Int? = if (pairs.size < 8) null else {
            val a = pairs.map { original.pitch[it] }
            val b = pairs.map { minePitch[it] }
            val ma = a.average().toFloat(); val mb = b.average().toFloat()
            val mad = pairs.indices.map { abs((a[it] - ma) - (b[it] - mb)) }.average().toFloat()
            val r = pearson(a, b)
            (100 * (0.6f * max(0f, r) + 0.4f * (1f - mad / 4f).coerceIn(0f, 1f))).toInt().coerceIn(0, 100)
        }

        // Ритм: насколько неравномерно пришлось «растягивать» запись под оригинал.
        val m = mine.frames
        val deviation = (0 until n).filter { firstJ[it] >= 0 }
            .map { abs(firstJ[it].toFloat() / max(1, m - 1) - it.toFloat() / max(1, n - 1)) }
            .average().toFloat()
        val rhythm = (100 * (1f - deviation / 0.15f)).toInt().coerceIn(0, 100)

        // Темп: длительность фразы без тишины по краям.
        val ratio = mine.seconds / original.seconds
        // Разница до 10% не штрафуется; вдвое медленнее (или быстрее) — ноль.
        val tempo = (100 * (1f - max(0f, abs(ln(ratio)) - ln(1.1f)) / ln(2f))).toInt().coerceIn(0, 100)

        val overall = if (intonation != null) (0.5f * intonation + 0.25f * rhythm + 0.25f * tempo).toInt()
        else (0.5f * rhythm + 0.5f * tempo).toInt()

        return ShadowReport(
            overall = overall.coerceIn(0, 100),
            intonation = intonation,
            rhythm = rhythm,
            tempo = tempo,
            tempoRatio = ratio,
            tips = tips(original, minePitch, mineEnergy, ratio, mine),
            originalPitch = original.pitch,
            minePitch = minePitch,
            originalEnergy = original.energy,
            mineEnergy = mineEnergy,
        )
    }

    private fun tips(original: Prosody, minePitch: FloatArray, mineEnergy: FloatArray, ratio: Float, mine: Prosody): List<String> {
        val tips = mutableListOf<String>()
        when {
            ratio > 1.3f -> tips += "Вы говорите медленнее диктора (×${fmt(ratio)}). Произносите слитнее, без пауз между словами."
            ratio < 0.75f -> tips += "Вы говорите быстрее диктора (×${fmt(ratio)}). Не торопитесь — дайте ударным слогам прозвучать."
        }
        val origRange = range(original.pitch)
        val mineRange = range(mine.pitch)
        if (origRange != null && mineRange != null) {
            if (origRange > 3f && mineRange < origRange * 0.6f) {
                tips += "Интонация ровнее, чем у диктора: его голос ходит на ${origRange.toInt()} полутонов, ваш — на ${mineRange.toInt()}. Добавьте мелодики, особенно на ударном слоге."
            } else if (mineRange > 4f && mineRange > origRange * 1.7f) {
                tips += "Голос скачет сильнее, чем у диктора. Британская речь сдержаннее — сгладьте интонацию."
            }
        }
        val origEnd = endSlope(original.pitch)
        val mineEnd = endSlope(minePitch)
        if (origEnd != null && mineEnd != null) {
            if (origEnd < -1.5f && mineEnd > 1f) tips += "В конце диктор понижает тон (утверждение), а вы повышаете — звучит как вопрос."
            if (origEnd > 1.5f && mineEnd < -1f) tips += "В конце диктор повышает тон, а у вас голос падает."
        }
        if (original.frames > 35) {
            val o = original.energy.indices.maxBy { original.energy[it] }
            val y = mineEnergy.indices.maxBy { mineEnergy[it] }
            if (abs(o - y) > original.frames * 0.25f) {
                tips += "Самый громкий слог у вас не там, где у диктора. Проверьте ударение — знак ˈ в транскрипции стоит перед ударным слогом."
            }
        }
        return tips
    }

    /** Диапазон мелодики (10–90 перцентиль) в полутонах. */
    private fun range(p: FloatArray): Float? {
        val v = p.filter { !it.isNaN() }.sorted()
        if (v.size < 8) return null
        return v[v.size * 9 / 10] - v[v.size / 10]
    }

    /** Изменение тона на последней трети звучащей части (полутоны по прямой регрессии). */
    private fun endSlope(p: FloatArray): Float? {
        val idx = p.indices.filter { !p[it].isNaN() }
        if (idx.size < 9) return null
        val tail = idx.drop(idx.size * 2 / 3)
        if (tail.size < 4) return null
        val xs = tail.map { it.toFloat() }; val ys = tail.map { p[it] }
        val mx = xs.average().toFloat(); val my = ys.average().toFloat()
        var num = 0f; var den = 0f
        for (k in xs.indices) { num += (xs[k] - mx) * (ys[k] - my); den += (xs[k] - mx) * (xs[k] - mx) }
        if (den == 0f) return null
        return num / den * (xs.last() - xs.first())
    }

    private fun pearson(a: List<Float>, b: List<Float>): Float {
        val ma = a.average(); val mb = b.average()
        var num = 0.0; var da = 0.0; var db = 0.0
        for (k in a.indices) {
            val x = a[k] - ma; val y = b[k] - mb
            num += x * y; da += x * x; db += y * y
        }
        return if (da == 0.0 || db == 0.0) 0f else (num / sqrt(da * db)).toFloat()
    }

    /** Выравнивание по громкости (DTW); путь — пары (кадр оригинала, кадр записи). */
    fun dtw(a: FloatArray, b: FloatArray): List<Pair<Int, Int>> {
        val n = a.size; val m = b.size
        val inf = Float.MAX_VALUE / 4
        val cost = Array(n) { FloatArray(m) { inf } }
        fun d(i: Int, j: Int): Float {
            val da = if (i > 0) a[i] - a[i - 1] else 0f
            val db = if (j > 0) b[j] - b[j - 1] else 0f
            return abs(a[i] - b[j]) + 0.5f * abs(da - db)
        }
        for (i in 0 until n) for (j in 0 until m) {
            val best = when {
                i == 0 && j == 0 -> 0f
                i == 0 -> cost[0][j - 1]
                j == 0 -> cost[i - 1][0]
                else -> min(cost[i - 1][j - 1], min(cost[i - 1][j], cost[i][j - 1]))
            }
            cost[i][j] = best + d(i, j)
        }
        val path = ArrayList<Pair<Int, Int>>(n + m)
        var i = n - 1; var j = m - 1
        path += i to j
        while (i > 0 || j > 0) {
            when {
                i == 0 -> j--
                j == 0 -> i--
                else -> {
                    val diag = cost[i - 1][j - 1]; val up = cost[i - 1][j]; val left = cost[i][j - 1]
                    when {
                        diag <= up && diag <= left -> { i--; j-- }
                        up < left -> i--
                        else -> j--
                    }
                }
            }
            path += i to j
        }
        return path.asReversed()
    }

    private fun fmt(v: Float) = "%.1f".format(java.util.Locale.US, v)

    /** Линейная передискретизация и сведение в моно для [analyze]. */
    fun toMono16k(pcm: ShortArray, channels: Int, rate: Int): FloatArray {
        val frames = pcm.size / max(1, channels)
        val mono = FloatArray(frames) { f ->
            var s = 0f
            for (ch in 0 until channels) s += pcm[f * channels + ch]
            s / channels / 32768f
        }
        if (rate == RATE) return mono
        val outLen = (frames.toLong() * RATE / rate).toInt()
        val step = rate.toDouble() / RATE
        return FloatArray(outLen) { k ->
            val pos = k * step
            val i = pos.toInt()
            val frac = (pos - i).toFloat()
            val x0 = mono.getOrElse(i) { 0f }; val x1 = mono.getOrElse(i + 1) { x0 }
            x0 + (x1 - x0) * frac
        }
    }
}
