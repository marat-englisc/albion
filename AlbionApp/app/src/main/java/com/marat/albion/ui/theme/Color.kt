package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Albion / Iris — холодный графит, бумага с голубоватым подтоном и один электрический акцент.
 * Контраст (WCAG): основной текст ≥ 16:1, второстепенный ≥ 5.3:1 на любых поверхностях,
 * акцент как текст ≥ 5.5:1, текст на акценте ≥ 6:1.
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

/** Акцентная плита (карточка дня, продолжение): одинакова в обеих темах, белый текст ≥ 5:1. */
object Hero {
    val Base = Color(0xFF4A30E0)
    val Content = Color.White
    /** Тёмный тон акцента — текст на белых кнопках внутри плиты. */
    val Deep = Color(0xFF1B1250)
    val Gradient = Brush.linearGradient(
        listOf(Color(0xFF6E52FF), Color(0xFF4A30E0), Color(0xFF2C1C9C)),
    )
}
