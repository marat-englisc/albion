package com.marat.albion.ui.library

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.marat.albion.ui.theme.Heritage
import com.marat.albion.ui.theme.Hero

/** Буквица — монолитный квадрат: чёрный на бумаге, белый на чернилах, без скруглений и орнамента. */
internal fun DrawScope.drawIlluminatedInitial(frame: Rect, darkTheme: Boolean) {
    drawRect(if (darkTheme) Heritage.Cream else Hero.Base, frame.topLeft, frame.size)
}
