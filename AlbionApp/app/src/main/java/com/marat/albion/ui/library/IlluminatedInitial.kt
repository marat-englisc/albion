package com.marat.albion.ui.library

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Heritage
import com.marat.albion.ui.theme.Hero

/** The initial remains a literary accent, with one fine edge and no miniature ornaments. */
internal fun DrawScope.drawIlluminatedInitial(frame: Rect, darkTheme: Boolean) {
    val edge = if (darkTheme) Hero.Base else Heritage.Champagne
    val corner = CornerRadius(3.dp.toPx())
    if (darkTheme) drawRoundRect(Heritage.Cream, frame.topLeft, frame.size, corner)
    else drawRoundRect(Brush.linearGradient(Hero.GradientStops, frame.topLeft, frame.bottomRight),
        frame.topLeft, frame.size, corner)
    drawRoundRect(edge.copy(alpha = .55f), frame.topLeft, frame.size, corner, style = Stroke(.7.dp.toPx()))
}
