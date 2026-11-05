package com.marat.albion.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Decorative ink only: no layout, input or accessibility nodes are added. */
fun Modifier.folioFrame(color: Color, inset: Dp = 6.dp, embellished: Boolean = false): Modifier = drawWithCache {
    val edge = inset.toPx()
    val corner = 4.dp.toPx()
    fun outline(pad: Float): Path {
        val l = pad
        val t = pad
        val r = size.width - pad
        val b = size.height - pad
        val c = corner.coerceAtMost((r - l) / 8).coerceAtMost((b - t) / 8)
        return Path().apply {
            moveTo(l + c * 2, t)
            lineTo(r - c * 2, t)
            quadraticTo(r - c, t, r - c, t + c)
            quadraticTo(r - c, t + c * 2, r, t + c * 2)
            lineTo(r, b - c * 2)
            quadraticTo(r - c, b - c * 2, r - c, b - c)
            quadraticTo(r - c, b, r - c * 2, b)
            lineTo(l + c * 2, b)
            quadraticTo(l + c, b, l + c, b - c)
            quadraticTo(l + c, b - c * 2, l, b - c * 2)
            lineTo(l, t + c * 2)
            quadraticTo(l + c, t + c * 2, l + c, t + c)
            quadraticTo(l + c, t, l + c * 2, t)
            close()
        }
    }
    val frame = if (size.minDimension > edge * 2 + corner * 4) outline(edge) else null
    val inner = if (embellished && size.minDimension > edge * 2 + corner * 6) outline(edge + 3.dp.toPx()) else null
    val hairline = Stroke(.55.dp.toPx())
    onDrawBehind {
        frame?.let { drawPath(it, color.copy(alpha = color.alpha * .55f), style = hairline) }
        inner?.let { drawPath(it, color.copy(alpha = color.alpha * .24f), style = hairline) }
    }
}

/** Sparse, deterministic paper fibres. Cached with the surface size, never animated. */
fun Modifier.paperGrain(color: Color): Modifier = drawWithCache {
    val step = 13.dp.toPx()
    val rows = (size.height / step).toInt()
    val columns = (size.width / step).toInt()
    val fibres = buildList {
        for (row in 0..rows) for (column in 0..columns) {
            val seed = ((row * 73 + column * 37) % 101) / 101f
            add(Offset((column + seed) * step, (row + (seed * 7f) % 1f) * step))
        }
    }
    val ink = color.copy(alpha = color.alpha * .045f)
    val length = .8.dp.toPx()
    val width = .4.dp.toPx()
    onDrawBehind {
        fibres.forEach { point -> drawLine(ink, point, point + Offset(length, length * .3f), width) }
    }
}

/** A printer's fleuron uses the existing divider allocation. */
fun Modifier.folioRule(color: Color): Modifier = drawWithCache {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = 2.dp.toPx()
    val gap = 11.dp.toPx()
    val stroke = .6.dp.toPx()
    val diamond = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    onDrawBehind {
        drawLine(color.copy(alpha = .55f), Offset(0f, center.y), Offset((center.x - gap).coerceAtLeast(0f), center.y), stroke)
        drawLine(color.copy(alpha = .55f), Offset(center.x + gap, center.y), Offset(size.width, center.y), stroke)
        drawPath(diamond, color, style = Stroke(stroke))
        drawCircle(color.copy(alpha = .75f), radius * .55f, center - Offset(radius * 3f, 0f), style = Stroke(stroke))
        drawCircle(color.copy(alpha = .75f), radius * .55f, center + Offset(radius * 3f, 0f), style = Stroke(stroke))
    }
}
