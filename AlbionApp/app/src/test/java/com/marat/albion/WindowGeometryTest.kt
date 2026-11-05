package com.marat.albion

import androidx.compose.ui.unit.IntRect
import com.marat.albion.ui.nav.*
import org.junit.Assert.*
import org.junit.Test

class WindowGeometryTest {
    @Test fun verticalHingeUsesWindowCoordinatesAfterInsetsAndRail() {
        val area = IntRect(160, 80, 1280, 880)
        val fold = WindowFold(IntRect(600, 0, 624, 900), FoldAxis.Vertical)
        val split = usablePanePartition(area, fold, 1f)!!
        assertEquals(IntRect(160, 80, 600, 880), split.first)
        assertEquals(IntRect(624, 80, 1280, 880), split.second)
    }
    @Test fun separatingCreaseMayHaveZeroWidth() {
        val split = usablePanePartition(IntRect(0, 0, 1000, 700),
            WindowFold(IntRect(500, 0, 500, 700), FoldAxis.Vertical), 1f)!!
        assertEquals(500, split.first.right)
        assertEquals(500, split.second.left)
    }
    @Test fun tabletopPlacesPanelsAboveAndBelowTheHinge() {
        val split = usablePanePartition(IntRect(104, 60, 1000, 900),
            WindowFold(IntRect(0, 430, 1000, 450), FoldAxis.Horizontal), 1f)!!
        assertEquals(IntRect(104, 60, 1000, 430), split.first)
        assertEquals(IntRect(104, 450, 1000, 900), split.second)
    }
    @Test fun narrowPanelsFallBackToTheLargerUnbrokenArea() {
        val area = IntRect(0, 0, 700, 700)
        val fold = WindowFold(IntRect(250, 0, 270, 700), FoldAxis.Vertical)
        assertNull(usablePanePartition(area, fold, 1f))
        assertEquals(IntRect(270, 0, 700, 700), largestUnbrokenArea(area, listOf(fold)))
    }
    @Test fun resizedWindowOutsideTheFoldUsesItsEntireArea() {
        val area = IntRect(0, 0, 400, 700)
        val fold = WindowFold(IntRect(500, 0, 520, 900), FoldAxis.Vertical)
        assertNull(partitionAtFold(area, fold))
        assertEquals(area, largestUnbrokenArea(area, listOf(fold)))
    }
    @Test fun hingeAtWindowEdgeStillExcludesItsOccludedPixels() {
        val area = IntRect(100, 50, 700, 750)
        val fold = WindowFold(IntRect(80, 0, 120, 900), FoldAxis.Vertical)
        assertNull(usablePanePartition(area, fold, 1f))
        assertEquals(IntRect(120, 50, 700, 750), largestUnbrokenArea(area, listOf(fold)))
    }
    @Test fun multipleHingesLeaveAContinuousRegion() {
        val area = IntRect(0, 0, 1000, 800)
        val folds = listOf(WindowFold(IntRect(300, 0, 320, 800), FoldAxis.Vertical),
            WindowFold(IntRect(650, 0, 670, 800), FoldAxis.Vertical))
        assertEquals(IntRect(320, 0, 650, 800), largestUnbrokenArea(area, folds))
        val other = listOf(WindowFold(IntRect(400, 0, 420, 800), FoldAxis.Vertical),
            WindowFold(IntRect(700, 0, 720, 800), FoldAxis.Vertical))
        assertEquals(IntRect(0, 0, 400, 800), largestUnbrokenArea(area, other))
        assertEquals(largestUnbrokenArea(area, other), largestUnbrokenArea(area, other.reversed()))
    }
    @Test fun physicalPixelsAreComparedWithDensityScaledMinimums() {
        val area = IntRect(0, 0, 2000, 1400)
        assertNotNull(usablePanePartition(area, WindowFold(IntRect(700, 0, 720, 1400), FoldAxis.Vertical), 2f))
        assertNull(usablePanePartition(area, WindowFold(IntRect(620, 0, 640, 1400), FoldAxis.Vertical), 2f))
    }
}
