package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Albion / Iris — холодный графит, бумага с голубоватым подтоном и один электрический акцент.
 * Контраст проверяется для конкретных пар ролей и составных фонов, включая свечение акцента.
 */

/** Нейтрали: графит и холодная бумага. */
val Ink = Color(0xFF0A0B0F)
val Paper = Color(0xFFF5F6F8)

/** Электрический ирис — единственный акцент интерфейса. */
internal val IrisLight = Color(0xFF5B3FF5)
internal val IrisDark = Color(0xFFB0A4FF)

/** Сигналы ответа: мята — верно, коралл — ошибка; всегда вместе с подписью и иконкой. */
internal val SuccessLight = Color(0xFF0F7A55)
internal val SuccessDark = Color(0xFF5FE3B0)
internal val ErrorLight = Color(0xFFC9283E)
internal val ErrorDark = Color(0xFFFF8A8F)

/** Акцентная плита: непрозрачный белый текст и запас контраста даже поверх блика и сетки. */
object Hero {
    val Base = Color(0xFF4326BD)
    val Content = Color.White
    /** Тёмный тон акцента — текст на белых кнопках внутри плиты. */
    val Deep = Color(0xFF1B1250)
    internal val GradientStops = listOf(Color(0xFF5438D2), Base, Color(0xFF291884))
    internal val Glow = Color(0xFF7FE7FF).copy(alpha = .10f)
    val Gradient = Brush.linearGradient(GradientStops)
}
