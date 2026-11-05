package com.marat.albion.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        // The ornament stays in the frame's existing margin, away from content.
        if (embellished && inner != null && size.width > 120.dp.toPx()) {
            val radius = 1.5.dp.toPx()
            for (y in listOf(edge + 1.5.dp.toPx(), size.height - edge - 1.5.dp.toPx())) {
                drawLozenge(Offset(size.width / 2, y), radius, color.copy(alpha = color.alpha * .7f))
                for (direction in listOf(-1, 1)) {
                    val x = size.width / 2 + direction * 8.dp.toPx()
                    drawLine(color.copy(alpha = color.alpha * .35f), Offset(x, y),
                        Offset(x + direction * 12.dp.toPx(), y), .5.dp.toPx(), StrokeCap.Round)
                }
            }
        }
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
    val ink = color.copy(alpha = color.alpha * .055f)
    val length = .8.dp.toPx()
    val width = .4.dp.toPx()
    onDrawBehind {
        fibres.forEach { point -> drawLine(ink, point, point + Offset(length, length * .3f), width) }
    }
}

/** A quiet light across laid paper or leather. Drawing never changes measurement. */
fun Modifier.engravedSurface(color: Color): Modifier = drawWithCache {
    val wash = Brush.linearGradient(
        listOf(color.copy(alpha = color.alpha * .055f), Color.Transparent, color.copy(alpha = color.alpha * .025f)),
        start = Offset.Zero, end = Offset(size.width * .7f, size.height),
    )
    onDrawBehind { drawRect(wash) }
}

/** The bevel of a small brass fitting; suitable for existing icon and audio targets. */
fun Modifier.medallionRim(color: Color): Modifier = drawWithCache {
    val radius = size.minDimension / 2 - 3.dp.toPx()
    onDrawBehind {
        if (radius > 0f) drawCircle(color.copy(alpha = color.alpha * .32f), radius,
            style = Stroke(.5.dp.toPx()))
    }
}

/** A printer's rule in the bottom padding already reserved by a screen heading. */
fun Modifier.headingRule(color: Color): Modifier = drawWithCache {
    val y = size.height - 3.dp.toPx()
    val length = 44.dp.toPx().coerceAtMost(size.width / 4)
    val wash = Brush.horizontalGradient(listOf(color.copy(alpha = color.alpha * .6f), color.copy(alpha = 0f)), endX = length)
    onDrawBehind {
        drawLine(wash, Offset(0f, y), Offset(length, y), .6.dp.toPx())
        drawLozenge(Offset(2.dp.toPx(), y), 1.5.dp.toPx(), color.copy(alpha = color.alpha * .7f))
    }
}

/** Laurel tooling within an existing square slot. The centre is reserved for the skill icon. */
fun Modifier.engravedSeal(color: Color): Modifier = drawWithCache {
    val center = Offset(size.width / 2, size.height / 2)
    val diameter = size.minDimension
    val radius = diameter * .4f
    val ink = color.copy(alpha = color.alpha * .6f)
    val stem = Path()
    val leaves = Path()
    for (side in listOf(-1f, 1f)) {
        fun point(angle: Float, r: Float): Offset = center + Offset(side * sin(angle) * r, cos(angle) * r)
        val base = point(.18f, radius)
        stem.moveTo(base.x, base.y)
        for (i in 1..32) {
            val p = point(.18f + i * 2.45f / 32, radius)
            stem.lineTo(p.x, p.y)
        }
        for (i in 0..6) {
            val angle = .38f + i * .32f
            val root = point(angle, radius)
            val tip = point(angle + .2f, radius + diameter * .075f)
            val shoulder = point(angle + .05f, radius + diameter * .09f)
            val inner = point(angle + .17f, radius + diameter * .01f)
            leaves.moveTo(root.x, root.y)
            leaves.quadraticTo(shoulder.x, shoulder.y, tip.x, tip.y)
            leaves.quadraticTo(inner.x, inner.y, root.x, root.y)
        }
    }
    onDrawBehind {
        drawPath(stem, ink, style = Stroke(.65.dp.toPx(), cap = StrokeCap.Round))
        drawPath(leaves, ink, style = Stroke(.5.dp.toPx()))
        drawCircle(ink.copy(alpha = ink.alpha * .35f), diameter * .29f, style = Stroke(.5.dp.toPx()))
        drawLozenge(center - Offset(0f, diameter * .43f), diameter * .025f, ink)
    }
}

/** A small bookbinder's stamp only in the measured empty area after a wordmark. */
fun Modifier.wordmarkSeal(color: Color, textWidth: Float): Modifier = drawWithCache {
    val diameter = 38.dp.toPx()
    val stamp = Offset(size.width - diameter / 2 - 2.dp.toPx(), size.height / 2)
    val radius = diameter * .42f
    onDrawBehind {
        if (size.width - textWidth > diameter + 28.dp.toPx()) {
            drawCircle(color.copy(alpha = color.alpha * .45f), radius, stamp, style = Stroke(.6.dp.toPx()))
            drawCircle(color.copy(alpha = color.alpha * .2f), radius - 3.dp.toPx(), stamp, style = Stroke(.5.dp.toPx()))
            drawLozenge(stamp, 3.dp.toPx(), color.copy(alpha = color.alpha * .7f))
            for (i in 0..7) {
                val angle = i * PI.toFloat() / 4
                val direction = Offset(cos(angle), sin(angle))
                drawLine(color.copy(alpha = color.alpha * .5f), stamp + direction * (radius - 7.dp.toPx()),
                    stamp + direction * (radius - 5.dp.toPx()), .6.dp.toPx())
            }
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = color.alpha * .35f))),
                Offset(textWidth + 16.dp.toPx(), stamp.y), stamp - Offset(radius + 8.dp.toPx(), 0f), .5.dp.toPx())
        }
    }
}

private fun DrawScope.drawLozenge(point: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(point.x, point.y - radius)
        lineTo(point.x + radius, point.y)
        lineTo(point.x, point.y + radius)
        lineTo(point.x - radius, point.y)
        close()
    }
    drawPath(path, color, style = Stroke(.5.dp.toPx()))
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
        drawLine(color.copy(alpha = color.alpha * .55f), Offset(0f, center.y), Offset((center.x - gap).coerceAtLeast(0f), center.y), stroke)
        drawLine(color.copy(alpha = color.alpha * .55f), Offset(center.x + gap, center.y), Offset(size.width, center.y), stroke)
        drawPath(diamond, color, style = Stroke(stroke))
        drawCircle(color.copy(alpha = color.alpha * .75f), radius * .55f, center - Offset(radius * 3f, 0f), style = Stroke(stroke))
        drawCircle(color.copy(alpha = color.alpha * .75f), radius * .55f, center + Offset(radius * 3f, 0f), style = Stroke(stroke))
    }
}
