package com.marat.albion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Brand
import com.marat.albion.ui.theme.Ink

/** Caslon A inside a printer's frame, shared by the masthead and launch screen. */
@Composable
fun AlbionLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    horizon: Float = 1f,
    chevron: Float = 1f,
    color: Color = Ink.AccentText,
    horizonColor: Color = Ink.Gold,
) {
    val letterSize = with(LocalDensity.current) { (size * 0.76f).toSp() }
    Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = this.size.width * 0.06f
            drawRect(horizonColor.copy(alpha = 0.7f * horizon.coerceIn(0f, 1f)),
                Offset(inset, inset), Size(this.size.width - inset * 2, this.size.height - inset * 2), style = Stroke(0.7.dp.toPx()))
            val span = this.size.width * 0.18f * horizon.coerceIn(0f, 1f)
            drawLine(horizonColor, Offset(center.x - span, this.size.height * 0.88f),
                Offset(center.x + span, this.size.height * 0.88f), 1.4.dp.toPx())
        }
        Text("A", style = TextStyle(fontFamily = Brand, fontSize = letterSize, lineHeight = letterSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false)), color = color,
            modifier = Modifier.graphicsLayer { alpha = chevron.coerceIn(0f, 1f); translationY = this.size.height * 0.035f })
    }
}
