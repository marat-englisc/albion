package com.marat.albion.ui.nav

import androidx.compose.ui.unit.IntRect

internal enum class FoldAxis { Vertical, Horizontal }
/** Pixel bounds in the application window, as reported by WindowManager. */
internal data class WindowFold(val bounds: IntRect, val axis: FoldAxis)
internal data class PanePartition(val first: IntRect, val second: IntRect, val axis: FoldAxis)

internal fun partitionAtFold(area: IntRect, fold: WindowFold): PanePartition? = when (fold.axis) {
    FoldAxis.Vertical -> if (fold.bounds.top <= area.top && fold.bounds.bottom >= area.bottom &&
        fold.bounds.left < area.right && fold.bounds.right > area.left) {
        PanePartition(IntRect(area.left, area.top, fold.bounds.left.coerceIn(area.left, area.right), area.bottom),
            IntRect(fold.bounds.right.coerceIn(area.left, area.right), area.top, area.right, area.bottom), fold.axis)
    } else null
    FoldAxis.Horizontal -> if (fold.bounds.left <= area.left && fold.bounds.right >= area.right &&
        fold.bounds.top < area.bottom && fold.bounds.bottom > area.top) {
        PanePartition(IntRect(area.left, area.top, area.right, fold.bounds.top.coerceIn(area.top, area.bottom)),
            IntRect(area.left, fold.bounds.bottom.coerceIn(area.top, area.bottom), area.right, area.bottom), fold.axis)
    } else null
}

internal fun usablePanePartition(area: IntRect, fold: WindowFold, density: Float): PanePartition? =
    partitionAtFold(area, fold)?.takeIf { split ->
        when (split.axis) {
            FoldAxis.Vertical -> split.first.width >= 320 * density && split.second.width >= 360 * density && area.height >= 240 * density
            FoldAxis.Horizontal -> area.width >= 600 * density && split.first.height >= 240 * density && split.second.height >= 240 * density
        }
    }

/** A focused screen stays on one continuous panel when two readable panels cannot fit. */
internal fun largestUnbrokenArea(area: IntRect, folds: List<WindowFold>): IntRect {
    val regions = folds.fold(listOf(area)) { remaining, fold ->
        remaining.flatMap { region ->
            partitionAtFold(region, fold)?.let { listOf(it.first, it.second) } ?: listOf(region)
        }
    }
    return regions.maxBy { it.width.toLong() * it.height }
}
