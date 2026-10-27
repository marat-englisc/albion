package com.marat.albion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Ink

/**
 * Знак Albion: шеврон «Λ» — буква A без перекладины — и линия горизонта,
 * которая проходит сквозь него и выходит за края. Буква и белые скалы
 * над морем одновременно. Координаты в поле 100×100.
 *
 * [horizon] и [chevron] (0…1) позволяют «рисовать» знак при анимации:
 * горизонт растёт от центра, ноги шеврона поднимаются к вершине.
 */
@Composable
fun AlbionLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    horizon: Float = 1f,
    chevron: Float = 1f,
    color: Color = Ink.AccentText,
    horizonColor: Color = Ink.Gold,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        val apex = p(50f, 18f)
        val left = p(22f, 82f)
        val right = p(78f, 82f)
        val legWidth = 5.5f * u

        if (chevron >= 1f) {
            val path = Path().apply {
                moveTo(left.x, left.y); lineTo(apex.x, apex.y); lineTo(right.x, right.y)
            }
            drawPath(path, color, style = Stroke(width = legWidth, join = StrokeJoin.Miter, cap = StrokeCap.Butt, miter = 8f))
        } else if (chevron > 0f) {
            val c = chevron.coerceIn(0f, 1f)
            drawLine(color, left, left + (apex - left) * c, strokeWidth = legWidth, cap = StrokeCap.Butt)
            drawLine(color, right, right + (apex - right) * c, strokeWidth = legWidth, cap = StrokeCap.Butt)
        }

        if (horizon > 0f) {
            val half = 46f * horizon.coerceIn(0f, 1f)
            drawLine(horizonColor, p(50f - half, 64f), p(50f + half, 64f), strokeWidth = 2.6f * u, cap = StrokeCap.Butt)
        }
    }
}
