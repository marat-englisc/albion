package com.marat.albion.ui.library

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Heritage
import com.marat.albion.ui.theme.Hero

/** Botanical tooling, inverted to green ink on ivory in the dark reading room. */
internal fun DrawScope.drawIlluminatedInitial(frame: Rect, darkTheme: Boolean) {
    val foil = if (darkTheme) Hero.Base else Heritage.Champagne
    val corner = CornerRadius(3.dp.toPx())
    if (darkTheme) drawRoundRect(Heritage.Cream, frame.topLeft, frame.size, corner)
    else drawRoundRect(Brush.linearGradient(Hero.GradientStops, frame.topLeft, frame.bottomRight),
        frame.topLeft, frame.size, corner)
    drawRoundRect(foil.copy(alpha = .8f), frame.topLeft, frame.size, corner, style = Stroke(.7.dp.toPx()))
    val inset = 3.5.dp.toPx()
    drawRoundRect(foil.copy(alpha = .5f), frame.topLeft + Offset(inset, inset),
        Size(frame.width - inset * 2, frame.height - inset * 2), CornerRadius(1.dp.toPx()), style = Stroke(.45.dp.toPx()))

    val unit = 1.dp.toPx()
    for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
        val anchor = Offset(if (sx > 0) frame.left + 7 * unit else frame.right - 7 * unit,
            if (sy > 0) frame.top + 7 * unit else frame.bottom - 7 * unit)
        fun point(x: Float, y: Float) = anchor + Offset(x * sx * unit, y * sy * unit)
        val stem = Path().apply {
            val a = point(0f, 14f); val b = point(-1f, 6f); val c = point(6f, -1f); val d = point(14f, 0f)
            moveTo(a.x, a.y); cubicTo(b.x, b.y, c.x, c.y, d.x, d.y)
        }
        val leaves = Path().apply {
            for ((x, y) in listOf(2f to 7f, 7f to 2f)) {
                val a = point(x, y); val b = point(x - 4f, y - 3f)
                val c = point(x - 2f, y - 6f); val d = point(x + 3f, y - 4f)
                moveTo(a.x, a.y); quadraticTo(b.x, b.y, c.x, c.y)
                quadraticTo(d.x, d.y, a.x, a.y); close()
            }
        }
        drawPath(leaves, foil.copy(alpha = .12f))
        drawPath(leaves, foil.copy(alpha = .7f), style = Stroke(.45.dp.toPx()))
        drawPath(stem, foil.copy(alpha = .65f), style = Stroke(.5.dp.toPx()))
    }
    for (y in listOf(frame.top + 4 * unit, frame.bottom - 4 * unit)) {
        val x = frame.center.x
        val diamond = Path().apply {
            moveTo(x, y - 1.6f * unit); lineTo(x + 1.6f * unit, y)
            lineTo(x, y + 1.6f * unit); lineTo(x - 1.6f * unit, y); close()
        }
        drawPath(diamond, foil.copy(alpha = .8f), style = Stroke(.45.dp.toPx()))
    }
}
