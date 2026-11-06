package com.marat.albion.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A single quiet rule in the divider's existing allocation. */
fun Modifier.folioRule(color: Color): Modifier = drawBehind {
    val y = size.height / 2
    drawLine(color.copy(alpha = color.alpha * .55f), Offset(0f, y), Offset(size.width, y), .6.dp.toPx())
}
