package com.marat.albion.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/** Static, cached paper fibres. Never animated and never placed over glyphs. */
fun Modifier.paperSurface(color: Color? = null): Modifier {
    val base = color ?: Ink.Bg
    val grain = if (Ink.palette.isDark) Color.White.copy(alpha = 0.018f) else Color(0xFF655038).copy(alpha = 0.045f)
    return drawWithCache {
        val random = Random(86139)
        val count = ((size.width * size.height) / 1900f).toInt().coerceIn(0, 2600)
        val fibres = List(count) { Offset(random.nextFloat() * size.width, random.nextFloat() * size.height) }
        onDrawBehind {
            drawRect(base)
            fibres.forEach { p -> drawLine(grain, p, p + Offset(1.6f, 0.5f), strokeWidth = 0.65f) }
        }
    }
}
