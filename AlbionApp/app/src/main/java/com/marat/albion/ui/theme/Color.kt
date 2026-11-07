package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Albion / Mono: строгая монохромная система — чернила, графит и бумага, без оттенка. */

/** Нейтрали: почти чёрный и холодная бумага. */
val Ink = Color(0xFF0A0A0A)
val Paper = Color(0xFFF4F4F3)

/** Акцент действий — сам контраст: чёрный на бумаге, белый на чернилах. */
internal val EstateLight = Ink
internal val EstateDark = Color(0xFFF2F2F2)

/** Нейтральные «металлы» для служебной графики; цвета в интерфейсе нет. */
object Heritage {
    val Brass = Color(0xFF8A8A8A)
    val Champagne = Color(0xFFD9D9D9)
    val Cream = Color(0xFFFAFAFA)
}

/** Сигналы ответа: приглушённые, всегда вместе с подписью и иконкой — единственный цвет в системе. */
internal val SuccessLight = Color(0xFF256B3E)
internal val SuccessDark = Color(0xFF8FCB9F)
internal val ErrorLight = Color(0xFFB0302F)
internal val ErrorDark = Color(0xFFF09A96)

/** Акцентная плита: чёрный монолит с белым текстом, одинаковый в обеих темах. */
object Hero {
    val Base = Color(0xFF111111)
    val Content = Heritage.Cream
    /** Текст на белых кнопках внутри плиты. */
    val Deep = Ink
    /** Граница плиты: в светлой теме незаметна, в тёмной отделяет монолит от фона. */
    val Edge = Color(0x2EFFFFFF)
    internal val GradientStops = listOf(Color(0xFF1A1A1A), Base, Color(0xFF0B0B0B))
    internal val Glow = Color.White.copy(alpha = .05f)
    val Gradient = Brush.linearGradient(GradientStops)
}
