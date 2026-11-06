package com.marat.albion.ui.nav

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** A non-null override also lets the layout be tested with changing device postures. */
internal val LocalWindowFolds = staticCompositionLocalOf<List<WindowFold>?> { null }
internal val LocalNavigationBounds = staticCompositionLocalOf { IntRect.Zero }
internal val LocalSceneBounds = staticCompositionLocalOf { IntRect.Zero }

@Composable
private fun separatingFolds(): List<WindowFold> {
    val activity = LocalActivity.current
    val flow = remember(activity) {
        if (activity == null) flowOf(emptyList()) else WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
            .map { info -> info.displayFeatures.filterIsInstance<FoldingFeature>()
                .filter { it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL }
                .map { feature ->
                    val r = feature.bounds
                    WindowFold(IntRect(r.left, r.top, r.right, r.bottom),
                        if (feature.orientation == FoldingFeature.Orientation.VERTICAL) FoldAxis.Vertical else FoldAxis.Horizontal)
                } }
    }
    return flow.collectAsStateWithLifecycle(emptyList()).value
}

/** Background remains edge-to-edge; the entire interactive viewport consumes safe drawing insets once. */
@Composable
internal fun NavigationWindow(current: NavKey?, showChrome: Boolean, listDetailEligible: Boolean = paneOf(current ?: HomeRoute) == Pane.List, content: @Composable (Boolean, Dp) -> Unit) {
    val reported = separatingFolds()
    val folds = LocalWindowFolds.current ?: reported
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {
                val p = it.positionInWindow(); origin = IntOffset(p.x.toInt(), p.y.toInt())
            }, contentAlignment = AbsoluteAlignment.TopLeft) {
                val area = with(density) { IntRect(origin.x, origin.y, origin.x + maxWidth.roundToPx(), origin.y + maxHeight.roundToPx()) }
                val fold = folds.singleOrNull()
                val rootPartition = fold?.let { partitionAtFold(area, it) }
                val chromeHeight = with(density) {
                    (if (rootPartition?.axis == FoldAxis.Horizontal) rootPartition.first.height else area.height).toDp()
                }
                val rail = navigationRailMetrics(maxWidth, chromeHeight)
                val railPixels = with(density) { if (showChrome && rail.enabled) rail.width.roundToPx() else 0 }
                val sceneArea = if (rtl) area.copy(right = area.right - railPixels) else area.copy(left = area.left + railPixels)
                val fitsFold = fold?.let { usablePanePartition(sceneArea, it, density.density) } != null
                val canSplit = listDetailEligible && density.fontScale <= 1.3f &&
                    if (folds.isNotEmpty()) folds.size == 1 && fitsFold
                    else sceneArea.width >= with(density) { ListDetailMinWidth.roundToPx() } && sceneArea.height >= with(density) { 240.dp.roundToPx() }
                val viewport = if (canSplit || folds.isEmpty()) area else largestUnbrokenArea(area, folds)
                val navigationHeight = with(density) {
                    (if (canSplit && rootPartition?.axis == FoldAxis.Horizontal) rootPartition.first.height else viewport.height).toDp()
                }
                CompositionLocalProvider(LocalWindowFolds provides folds, LocalNavigationBounds provides viewport) {
                    Box(Modifier.absoluteOffset { IntOffset(viewport.left - area.left, viewport.top - area.top) }
                        .requiredSize(with(density) { viewport.width.toDp() }, with(density) { viewport.height.toDp() }).clipToBounds()) {
                        if (viewport.width > 0 && viewport.height > 0) content(canSplit, navigationHeight)
                    }
                }
            }
        }
    }
}
