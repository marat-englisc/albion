package com.marat.albion

import com.marat.albion.data.audio.ProsodyAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** Проверка анализа на синтетическом «голосе»: тон с гармониками и заданной мелодикой. */
class ProsodyTest {

    private val rate = ProsodyAnalyzer.RATE

    /**
     * [contour] — высота от 0 до 1 доли фразы (в Гц), [seconds] — длительность звучания,
     * [syllables] — число «слогов» (провалы громкости), [stressAt] — где громче всего (0…1).
     */
    private fun voice(seconds: Double, contour: (Double) -> Double, syllables: Int = 3, stressAt: Double = 0.2, pad: Double = 0.3): FloatArray {
        val n = (seconds * rate).toInt()
        val padN = (pad * rate).toInt()
        val out = FloatArray(n + 2 * padN)
        var phase = 0.0
        for (k in 0 until n) {
            val t = k.toDouble() / n
            val f0 = contour(t)
            phase += 2 * PI * f0 / rate
            val syll = sin(PI * (t * syllables % 1.0)).pow(0.6)
            val stress = 0.55 + 0.45 * kotlin.math.exp(-((t - stressAt) * (t - stressAt)) / 0.01)
            val s = sin(phase) + 0.5 * sin(2 * phase) + 0.25 * sin(3 * phase)
            out[padN + k] = (0.3 * syll * stress * s).toFloat()
        }
        // Лёгкий шум, как у микрофона.
        val rnd = java.util.Random(1)
        for (k in out.indices) out[k] += (rnd.nextGaussian() * 0.002).toFloat()
        return out
    }

    private val falling: (Double) -> Double = { t -> 220 - 70 * t }
    private val rising: (Double) -> Double = { t -> 150 + 90 * t }

    @Test
    fun yinFindsPitch() {
        val x = voice(0.5, { 200.0 }, syllables = 1, stressAt = 0.5, pad = 0.0)
        val f = ProsodyAnalyzer.yin(x, rate / 4)
        assertEquals(200f, f, 3f)
    }

    @Test
    fun yinFindsPitchWithBreathNoise() {
        val random = java.util.Random(21)
        val x = FloatArray(rate / 2) { i ->
            val phase = 2 * PI * 200 * i / rate
            (0.25 * sin(phase) + 0.12 * sin(2 * phase) + 0.1 * random.nextGaussian()).toFloat()
        }
        assertEquals(200f, ProsodyAnalyzer.yin(x, rate / 4), 5f)
    }

    @Test
    fun unvoicedNoiseDoesNotCreateMelody() {
        val random = java.util.Random(21)
        val x = FloatArray(rate * 2) { i ->
            if (i in rate / 2 until rate * 3 / 2) (random.nextGaussian() * 0.15).toFloat() else 0f
        }
        val analyzed = ProsodyAnalyzer.analyze(x)!!
        assertEquals(0, analyzed.voiced)
    }

    @Test
    fun silenceIsRejected() {
        assertNull(ProsodyAnalyzer.analyze(FloatArray(rate) { 0f }))
    }

    @Test
    fun sameMelodyDifferentVoiceScoresHigh() {
        val original = ProsodyAnalyzer.analyze(voice(1.2, falling))!!
        // «Другой голос»: на октаву ниже, но та же мелодика и темп.
        val mine = ProsodyAnalyzer.analyze(voice(1.2, { t -> falling(t) / 2 }))!!
        val r = ProsodyAnalyzer.compare(original, mine)
        assertNotNull(r.intonation)
        assertTrue("интонация ${r.intonation}", r.intonation!! >= 85)
        assertTrue("итог ${r.overall}", r.overall >= 85)
        assertTrue(r.tips.toString(), r.tips.isEmpty())
    }

    @Test
    fun risingInsteadOfFallingIsCaught() {
        val original = ProsodyAnalyzer.analyze(voice(1.2, falling))!!
        val mine = ProsodyAnalyzer.analyze(voice(1.2, rising))!!
        val r = ProsodyAnalyzer.compare(original, mine)
        assertTrue("интонация ${r.intonation}", r.intonation!! < 50)
        assertTrue(r.tips.toString(), r.tips.any { it.contains("понижает тон") })
    }

    @Test
    fun slowAndFlatSpeechGetsTips() {
        val original = ProsodyAnalyzer.analyze(voice(1.0, falling))!!
        val mine = ProsodyAnalyzer.analyze(voice(1.6, { 180.0 }))!!
        val r = ProsodyAnalyzer.compare(original, mine)
        assertTrue("темп ${r.tempoRatio}", r.tempoRatio > 1.4f)
        assertTrue(r.tips.toString(), r.tips.any { it.contains("медленнее") })
        assertTrue(r.tips.toString(), r.tips.any { it.contains("ровнее") })
    }

    @Test
    fun wrongStressIsCaught() {
        val original = ProsodyAnalyzer.analyze(voice(1.2, falling, stressAt = 0.15))!!
        val mine = ProsodyAnalyzer.analyze(voice(1.2, falling, stressAt = 0.8))!!
        val r = ProsodyAnalyzer.compare(original, mine)
        assertTrue(r.tips.toString(), r.tips.any { it.contains("ударение") })
    }

    @Test
    fun resamplesToMono16k() {
        val stereo44 = ShortArray(44_100 * 2) { 1000 }
        val out = ProsodyAnalyzer.toMono16k(stereo44, channels = 2, rate = 44_100)
        assertEquals(16_000, out.size)
        assertEquals(1000 / 32768f, out[100], 1e-4f)
    }
}
