package com.marat.albion.data.fsrs

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Один ответ из журнала. */
data class ReviewEvent(val cardId: String, val rating: Int, val time: Long)

sealed interface OptimizeOutcome {
    /** Недостаточно повторений с интервалом от суток. */
    data class NotEnoughData(val have: Int, val need: Int) : OptimizeOutcome

    data class Done(
        val params: DoubleArray,
        /** Средняя логарифмическая ошибка прогноза (меньше — лучше). */
        val lossBefore: Double,
        val lossAfter: Double,
        val reviews: Int,
        val cards: Int,
    ) : OptimizeOutcome {
        val improved get() = lossAfter < lossBefore * 0.999
        val improvementPercent get() = ((1 - lossAfter / lossBefore) * 100).coerceAtLeast(0.0)
    }
}

/**
 * Подстройка параметров FSRS-6 под конкретного ученика.
 *
 * По журналу ответов восстанавливается история каждой карточки, и модель
 * памяти прогоняется заново с пробными параметрами. Для каждого повторения
 * через сутки и больше модель предсказывает вероятность вспомнить; ошибка —
 * бинарная кросс-энтропия с фактическим ответом («Снова» = забыл).
 * Параметры подбираются Adam с численным градиентом. Начальные стабильности
 * (w0–w3) оптимизируются в логарифмической шкале, у остальных — границы как
 * в эталонном оптимизаторе. Мягкая привязка к стандартным значениям не даёт
 * «переобучиться» на небольшой истории.
 */
class FsrsOptimizer(events: List<ReviewEvent>, private val initial: DoubleArray = Fsrs.DEFAULT_W) {

    /** История карточки: (дней с прошлого ответа, оценка). */
    private val sequences: List<List<Pair<Double, Rating>>> = events
        .groupBy { it.cardId }
        .values
        .map { list ->
            val sorted = list.sortedBy { it.time }
            sorted.mapIndexed { i, e ->
                val elapsed = if (i == 0) 0.0 else (e.time - sorted[i - 1].time).toDouble() / Fsrs.DAY
                elapsed to Rating.of(e.rating.coerceIn(1, 4))
            }
        }
        .filter { it.size >= 2 }

    /** Сколько повторений участвует в оценке (интервал ≥ 1 дня). */
    val trainingItems: Int = sequences.sumOf { seq -> seq.drop(1).count { it.first >= 1.0 } }

    val cards: Int get() = sequences.size

    fun loss(w: DoubleArray): Double {
        val fsrs = Fsrs(w = w, enableFuzz = false)
        var total = 0.0
        var n = 0
        for (seq in sequences) {
            var s = 0.0
            var d = 0.0
            seq.forEachIndexed { i, (elapsed, rating) ->
                if (i == 0) {
                    val (s0, d0) = fsrs.memoryStep(0.0, 0.0, isNew = true, elapsedDays = 0.0, rating = rating)
                    s = s0; d = d0
                    return@forEachIndexed
                }
                if (elapsed >= 1.0) {
                    val p = fsrs.recallProbability(elapsed, s).coerceIn(1e-4, 1 - 1e-4)
                    val y = if (rating == Rating.Again) 0.0 else 1.0
                    total += -(y * ln(p) + (1 - y) * ln(1 - p))
                    n++
                }
                val (s1, d1) = fsrs.memoryStep(s, d, isNew = false, elapsedDays = elapsed, rating = rating)
                s = s1; d = d1
            }
        }
        return if (n == 0) 0.0 else total / n
    }

    fun optimize(
        iterations: Int = 220,
        minItems: Int = MIN_ITEMS,
        onProgress: (Float) -> Unit = {},
    ): OptimizeOutcome {
        if (trainingItems < minItems) return OptimizeOutcome.NotEnoughData(trainingItems, minItems)

        val dims = initial.size
        val x0 = DoubleArray(dims) { toX(it, initial[it]) }
        val x = x0.copyOf()
        val scale = DoubleArray(dims) { i -> if (i < 4) 1.0 else (Fsrs.BOUNDS[i].endInclusive - Fsrs.BOUNDS[i].start) }
        // Чем меньше данных, тем сильнее держимся стандартных значений.
        val lambda = 4.0 / trainingItems

        fun objective(v: DoubleArray): Double {
            var reg = 0.0
            for (i in 0 until dims) { val t = (v[i] - x0[i]) / scale[i]; reg += t * t }
            return loss(toW(v)) + lambda * reg
        }

        val m = DoubleArray(dims)
        val vv = DoubleArray(dims)
        val lr = 0.03
        val b1 = 0.9
        val b2 = 0.999
        var best = x.copyOf()
        var bestObj = objective(x)
        val grad = DoubleArray(dims)

        for (t in 1..iterations) {
            for (i in 0 until dims) {
                val h = 1e-4 * maxOf(1.0, abs(x[i]))
                val keep = x[i]
                x[i] = keep + h; val up = objective(x)
                x[i] = keep - h; val down = objective(x)
                x[i] = keep
                grad[i] = (up - down) / (2 * h)
            }
            for (i in 0 until dims) {
                m[i] = b1 * m[i] + (1 - b1) * grad[i]
                vv[i] = b2 * vv[i] + (1 - b2) * grad[i] * grad[i]
                val mh = m[i] / (1 - Math.pow(b1, t.toDouble()))
                val vh = vv[i] / (1 - Math.pow(b2, t.toDouble()))
                x[i] = clampX(i, x[i] - lr * mh / (sqrt(vh) + 1e-8))
            }
            val obj = objective(x)
            if (obj < bestObj) { bestObj = obj; best = x.copyOf() }
            onProgress(t.toFloat() / iterations)
        }

        val params = toW(best)
        return OptimizeOutcome.Done(
            params = params,
            lossBefore = loss(initial),
            lossAfter = loss(params),
            reviews = trainingItems,
            cards = cards,
        )
    }

    private fun toX(i: Int, w: Double) = if (i < 4) ln(w) else w
    private fun toW(x: DoubleArray) = DoubleArray(x.size) { i -> clampW(i, if (i < 4) exp(x[i]) else x[i]) }
    private fun clampW(i: Int, w: Double) = w.coerceIn(Fsrs.BOUNDS[i].start, Fsrs.BOUNDS[i].endInclusive)
    private fun clampX(i: Int, x: Double) = if (i < 4) {
        x.coerceIn(ln(Fsrs.BOUNDS[i].start), ln(Fsrs.BOUNDS[i].endInclusive))
    } else clampW(i, x)

    companion object {
        /** Минимум повторений с интервалом от суток, чтобы подстройка имела смысл. */
        const val MIN_ITEMS = 150
    }
}
