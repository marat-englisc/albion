package com.marat.albion

import com.marat.albion.data.fsrs.Fsrs
import com.marat.albion.data.fsrs.FsrsOptimizer
import com.marat.albion.data.fsrs.Memory
import com.marat.albion.data.fsrs.OptimizeOutcome
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.fsrs.ReviewEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FsrsOptimizerTest {

    /**
     * Синтетический ученик: его «настоящая» память описывается параметрами [truth],
     * а повторения он проходит по расписанию стандартного FSRS. Ответ «вспомнил»
     * выпадает с вероятностью, которую даёт истинная модель.
     */
    private fun simulate(truth: DoubleArray, cards: Int, seed: Int = 7): List<ReviewEvent> {
        val rnd = Random(seed)
        val scheduler = Fsrs(enableFuzz = false)
        val real = Fsrs(w = truth, enableFuzz = false)
        val events = mutableListOf<ReviewEvent>()
        repeat(cards) { c ->
            var m = Memory()
            var realS = 0.0
            var realD = 0.0
            var now = 1_700_000_000_000L + c * 60_000L
            repeat(8) { k ->
                val rating = if (k == 0) Rating.Good else {
                    val elapsed = (now - m.lastReview).toDouble() / Fsrs.DAY
                    val p = if (elapsed < 1) 0.95 else real.recallProbability(elapsed, realS)
                    if (rnd.nextDouble() < p) Rating.Good else Rating.Again
                }
                val elapsed = if (k == 0) 0.0 else (now - m.lastReview).toDouble() / Fsrs.DAY
                val (s, d) = real.memoryStep(realS, realD, isNew = k == 0, elapsedDays = elapsed, rating = rating)
                realS = s; realD = d
                events += ReviewEvent("card$c", rating.value, now)
                m = scheduler.review(m, rating, now)
                now = m.due
            }
        }
        return events
    }

    @Test
    fun notEnoughData() {
        val outcome = FsrsOptimizer(simulate(Fsrs.DEFAULT_W, cards = 3)).optimize(iterations = 5)
        assertTrue(outcome is OptimizeOutcome.NotEnoughData)
    }

    @Test
    fun learnsBetterParametersForStrongerMemory() {
        // У этого ученика память заметно крепче стандартной: стабильность растёт быстрее.
        val truth = Fsrs.DEFAULT_W.copyOf().apply { this[8] = 2.6; this[2] = 6.0 }
        val events = simulate(truth, cards = 120)
        val optimizer = FsrsOptimizer(events)
        val outcome = optimizer.optimize(iterations = 60)
        assertTrue(outcome is OptimizeOutcome.Done)
        outcome as OptimizeOutcome.Done
        println("loss ${outcome.lossBefore} → ${outcome.lossAfter}, items ${outcome.reviews}, w8=${outcome.params[8]}")
        assertTrue("ошибка прогноза уменьшилась", outcome.lossAfter < outcome.lossBefore)
        assertTrue("параметры в границах", outcome.params.withIndex().all { (i, v) -> v in Fsrs.BOUNDS[i] })
    }

    @Test
    fun paramsRoundTrip() {
        val encoded = Fsrs.encodeParams(Fsrs.DEFAULT_W)
        val decoded = Fsrs.decodeParams(encoded)!!
        assertEquals(Fsrs.DEFAULT_W.size, decoded.size)
        Fsrs.DEFAULT_W.forEachIndexed { i, v -> assertEquals(v, decoded[i], 1e-4) }
        assertEquals(null, Fsrs.decodeParams("1,2,3"))
    }
}
