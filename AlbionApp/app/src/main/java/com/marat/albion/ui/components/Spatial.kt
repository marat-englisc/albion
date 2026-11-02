package com.marat.albion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.AlbionFonts
import com.marat.albion.ui.theme.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Quiet technical labels give each section a place in the same visual system. */
@Composable
fun SignalLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, modifier, color = color, style = MaterialTheme.typography.labelSmall.copy(
        fontFamily = AlbionFonts.Tech, fontWeight = FontWeight.Normal, fontSize = 9.5.sp, letterSpacing = 1.1.sp,
    ))
}

@Composable
fun AlbionMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(32.dp).clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * .14f, h * .8f)
            lineTo(w * .5f, h * .13f)
            lineTo(w * .86f, h * .8f)
            moveTo(w * .32f, h * .65f)
            lineTo(w * .68f, h * .65f)
        }
        drawPath(path, color, style = Stroke(1.7.dp.toPx()))
        drawCircle(color, 2.dp.toPx(), Offset(w * .5f, h * .86f))
    }
}

@Composable
fun ModuleHeading(number: String, title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 26.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        SignalLabel(number, Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.primary)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (trailing != null) SignalLabel(trailing)
    }
}

/** Canvas ornament: no bitmap assets, no network requests, respects reduced motion. */
@Composable
fun OrbitalField(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface, animated: Boolean = true) {
    val reduced = LocalReducedMotion.current
    val rotation = if (!reduced && animated) {
        val transition = rememberInfiniteTransition(label = "orbital-field")
        transition.animateFloat(0f, (PI * 2).toFloat(),
            infiniteRepeatable(tween(48_000, easing = LinearEasing)), label = "orbital-rotation")
    } else remember { mutableStateOf(.38f) }
    Canvas(modifier.clearAndSetSemantics {}) {
        val phase = rotation.value
        val radius = size.minDimension * .35f
        val center = Offset(size.width / 2, size.height / 2)
        val tilt = -.35f
        fun project(x: Float, y: Float, z: Float): Offset {
            val rx = x * cos(phase) + z * sin(phase)
            return center + Offset((rx * cos(tilt) - y * sin(tilt)) * radius,
                (rx * sin(tilt) + y * cos(tilt)) * radius)
        }
        drawCircle(color.copy(alpha = .1f), radius * 1.3f, center, style = Stroke(.6.dp.toPx()))
        drawCircle(color.copy(alpha = .32f), radius, center, style = Stroke(.7.dp.toPx()))
        for (meridian in 0 until 10) {
            val longitude = meridian * PI.toFloat() / 5
            val path = Path()
            for (step in 0..80) {
                val latitude = -PI.toFloat() / 2 + step * PI.toFloat() / 80
                val p = project(cos(latitude) * cos(longitude), sin(latitude), cos(latitude) * sin(longitude))
                if (step == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color.copy(alpha = .24f), style = Stroke(.55.dp.toPx()))
        }
        for (parallel in -3..3) {
            val latitude = parallel * PI.toFloat() / 9
            val path = Path()
            for (step in 0..100) {
                val longitude = step * 2 * PI.toFloat() / 100
                val p = project(cos(latitude) * cos(longitude), sin(latitude), cos(latitude) * sin(longitude))
                if (step == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color.copy(alpha = .24f), style = Stroke(.55.dp.toPx()))
        }
        for (index in 0..3) {
            val a = index * PI.toFloat() / 2
            val p = center + Offset(cos(a), sin(a)) * radius * 1.3f
            drawLine(color.copy(alpha = .5f), p - Offset(3.dp.toPx(), 0f), p + Offset(3.dp.toPx(), 0f), .7.dp.toPx())
            drawLine(color.copy(alpha = .5f), p - Offset(0f, 3.dp.toPx()), p + Offset(0f, 3.dp.toPx()), .7.dp.toPx())
        }
        val satellite = center + Offset(cos(phase * 2), sin(phase * 2)) * radius * 1.3f
        drawCircle(color.copy(alpha = .9f), 2.dp.toPx(), satellite)
    }
}

/** Sparse construction marks on feature panels; content always stays above them. */
fun Modifier.constructionLines(color: Color): Modifier = drawBehind {
    val step = 32.dp.toPx()
    var x = size.width / 2
    while (x < size.width) {
        drawLine(color.copy(alpha = .035f), Offset(x, 0f), Offset(x, size.height), .5.dp.toPx())
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(color.copy(alpha = .035f), Offset(size.width / 2, y), Offset(size.width, y), .5.dp.toPx())
        y += step
    }
}
