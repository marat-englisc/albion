package com.marat.albion

import com.marat.albion.data.fsrs.CardState
import com.marat.albion.data.fsrs.Fsrs
import com.marat.albion.data.fsrs.Memory
import com.marat.albion.data.fsrs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsTest {
    private val fsrs = Fsrs(enableFuzz = false)
    private val t0 = 1_700_000_000_000L

    @Test
    fun newCardGoesThroughLearningSteps() {
        val first = fsrs.review(Memory(), Rating.Good, t0)
        assertEquals(CardState.LEARNING, first.state)
        assertEquals(1, first.step)
        assertEquals(10 * Fsrs.MINUTE, first.due - t0)

        val second = fsrs.review(first, Rating.Good, first.due)
        assertEquals(CardState.REVIEW, second.state)
        assertTrue("интервал не меньше суток", second.due - first.due >= Fsrs.DAY)
    }

    @Test
    fun againRestartsLearning() {
        val m = fsrs.review(Memory(), Rating.Again, t0)
        assertEquals(CardState.LEARNING, m.state)
        assertEquals(0, m.step)
        assertEquals(Fsrs.MINUTE, m.due - t0)
    }

    @Test
    fun easyOnNewCardGraduatesImmediately() {
        val m = fsrs.review(Memory(), Rating.Easy, t0)
        assertEquals(CardState.REVIEW, m.state)
        assertTrue(m.due - t0 >= Fsrs.DAY)
    }

    @Test
    fun reviewIntervalsAreOrderedAndLapseCounts() {
        var m = fsrs.review(Memory(), Rating.Good, t0)
        m = fsrs.review(m, Rating.Good, m.due)
        val now = m.due
        val p = fsrs.preview(m, now)
        val hard = p.getValue(Rating.Hard).due
        val good = p.getValue(Rating.Good).due
        val easy = p.getValue(Rating.Easy).due
        assertTrue(hard <= good && good < easy)

        val lapse = p.getValue(Rating.Again)
        assertEquals(CardState.RELEARNING, lapse.state)
        assertEquals(1, lapse.lapses)
        assertTrue(lapse.stability < m.stability)
    }

    @Test
    fun higherRetentionMeansShorterIntervals() {
        val strict = Fsrs(desiredRetention = 0.95, enableFuzz = false)
        val relaxed = Fsrs(desiredRetention = 0.85, enableFuzz = false)
        assertTrue(strict.nextIntervalDays(20.0) < relaxed.nextIntervalDays(20.0))
        // При удержании 0.9 интервал ≈ стабильности (определение S в FSRS).
        assertEquals(20, Fsrs(desiredRetention = 0.9).nextIntervalDays(20.0))
    }

    @Test
    fun repeatedSuccessGrowsIntervals() {
        var m = fsrs.review(Memory(), Rating.Good, t0)
        var now = m.due
        val intervals = mutableListOf<Long>()
        repeat(6) {
            val next = fsrs.review(m, Rating.Good, now)
            intervals += next.due - now
            m = next
            now = next.due
        }
        assertTrue(intervals.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun formatsIntervals() {
        assertEquals("10 мин", Fsrs.formatInterval(10 * Fsrs.MINUTE))
        assertEquals("3 дн", Fsrs.formatInterval(3 * Fsrs.DAY))
        assertEquals("2 мес", Fsrs.formatInterval(61 * Fsrs.DAY))
    }
}
