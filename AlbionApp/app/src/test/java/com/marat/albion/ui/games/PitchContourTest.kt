package com.marat.albion.ui.games

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchContourTest {
    @Test fun shortTrackingGapsAreJoinedWithoutChangingMeasurements() {
        val source = FloatArray(28) { if (it in 8..19) Float.NaN else it / 10f }
        val before = source.copyOf()
        val contour = preparePitchContour(source)

        assertTrue(contour.available)
        assertEquals(1, contour.segments.size)
        assertEquals((0..27).toList(), contour.segments.single().map { it.frame })
        assertArrayEquals(before, source, 0f)
        assertTrue(contour.segments.single().zipWithNext().all { (a, b) -> a.semitones <= b.semitones })
    }

    @Test fun pausesRemainGapsAtTheirOriginalTimePositions() {
        val source = FloatArray(45) {
            when (it) {
                in 3..14 -> 2f
                in 28..39 -> -2f
                else -> Float.NaN
            }
        }
        val runs = preparePitchContour(source).segments
        assertEquals(2, runs.size)
        assertEquals((3..14).toList(), runs[0].map { it.frame })
        assertEquals((28..39).toList(), runs[1].map { it.frame })
    }

    @Test fun largePitchJumpsAreNotBridged() {
        val source = FloatArray(25) { if (it < 12) -5f else if (it == 12) Float.NaN else 5f }
        val runs = preparePitchContour(source).segments
        assertEquals(2, runs.size)
        assertEquals(11, runs.first().last().frame)
        assertEquals(13, runs.last().first().frame)
    }

    @Test fun evenShortSilentPausesAreNotBridged() {
        val source = FloatArray(28) { if (it in 10..12) Float.NaN else 1f }
        val energy = FloatArray(28) { if (it in 10..12) 0f else 1f }
        assertEquals(2, preparePitchContour(source, energy).segments.size)
        assertEquals(1, preparePitchContour(source, FloatArray(28) { 1f }).segments.size)
    }

    @Test fun isolatedPitchSpikesDoNotStretchTheChart() {
        val source = FloatArray(30) { if (it == 15) 12f else 0f }
        val points = preparePitchContour(source).segments.single()
        assertTrue(points.all { it.semitones == 0f })
    }

    @Test fun sparseStrokesAreReportedAsUnavailable() {
        val source = FloatArray(120) { if (it % 25 in 0..2) 2f else Float.NaN }
        assertFalse(preparePitchContour(source).available)
        assertFalse(preparePitchContour(FloatArray(120) { if (it < 8) 0f else Float.NaN }).available)
    }

    @Test fun silenceAndNonFiniteMeasurementsAreUnavailable() {
        for (source in listOf(floatArrayOf(), FloatArray(30) { Float.NaN }, FloatArray(30) { Float.POSITIVE_INFINITY })) {
            assertFalse(preparePitchContour(source).available)
        }
    }

    @Test fun smoothingKeepsFlatSpeechFlatAndPreservesMelodyRange() {
        assertTrue(preparePitchContour(FloatArray(20) { -3f }).segments.single().all { it.semitones == -3f })
        val source = FloatArray(40) { 4f - it / 5f }
        val points = preparePitchContour(source).segments.single()
        assertTrue(points.zipWithNext().all { (a, b) -> a.semitones >= b.semitones })
        assertTrue(points.all { it.semitones in source.min()..source.max() })
        assertTrue(points.first().semitones - points.last().semitones > 7f)
    }
}
