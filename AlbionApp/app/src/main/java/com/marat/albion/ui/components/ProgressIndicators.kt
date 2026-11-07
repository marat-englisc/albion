package com.marat.albion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.LocalReducedMotion

/** Static indeterminate symbols convey waiting without implying a completion percentage. */
@Composable
fun QuietCircularProgressIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, strokeWidth: Dp = 4.dp) {
    if (LocalReducedMotion.current) {
        Icon(AlbionIcons.Hourglass, null, modifier.size(40.dp).progressSemantics(), tint = color)
    } else androidx.compose.material3.CircularProgressIndicator(modifier, color = color, strokeWidth = strokeWidth, strokeCap = StrokeCap.Butt)
}

@Composable
fun QuietLinearProgressIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    if (LocalReducedMotion.current) {
        Canvas(modifier.fillMaxWidth().height(4.dp).progressSemantics()) {
            drawRect(trackColor)
            val step = 12.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(color, Offset(x, 0f), Size(minOf(step / 2, size.width - x), size.height))
                x += step
            }
        }
    } else androidx.compose.material3.LinearProgressIndicator(modifier, color = color, trackColor = trackColor, strokeCap = StrokeCap.Butt, gapSize = 0.dp)
}

/**
 * Кольцо прогресса в квадратной геометрии: линия идёт по периметру квадрата
 * от середины верхней стороны по часовой стрелке. [inset] — отступ от краёв холста до оси линии.
 */
fun DrawScope.drawSquareProgress(track: Color?, color: Color, fraction: Float, strokeWidth: Float, inset: Float = strokeWidth / 2) {
    val l = inset; val t = inset; val r = size.width - inset; val b = size.height - inset
    val outline = Path().apply {
        moveTo((l + r) / 2, t); lineTo(r, t); lineTo(r, b); lineTo(l, b); lineTo(l, t); lineTo((l + r) / 2, t)
    }
    val stroke = Stroke(strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
    if (track != null) drawRect(track, Offset(l, t), Size(r - l, b - t), style = stroke)
    val f = fraction.coerceIn(0f, 1f)
    if (f <= 0f) return
    val measure = PathMeasure().apply { setPath(outline, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * f, part, true)
    drawPath(part, color, style = stroke)
}
