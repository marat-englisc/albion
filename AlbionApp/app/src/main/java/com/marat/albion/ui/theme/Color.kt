package com.marat.albion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Albion / Old Money: тёплая бумага, лесные чернила и состаренная латунь. */

/** Нейтрали: зелёные чернила и слоновая кость. */
val Ink = Color(0xFF161D19)
val Paper = Color(0xFFF4EBDD)

/** Акцент действий и выбора: переплётная зелень / светлая латунь. */
internal val EstateLight = Color(0xFF284D3E)
internal val EstateDark = Color(0xFFD6BD87)

/** Декоративная фольга; функциональные границы используют контрастный outline. */
object Heritage {
    val Brass = Color(0xFF947039)
    val Champagne = Color(0xFFD6BD87)
    val Cream = Color(0xFFFFF8EB)
}

/** Сигналы ответа: шалфей и бордо; всегда вместе с подписью и иконкой. */
internal val SuccessLight = Color(0xFF416344)
internal val SuccessDark = Color(0xFFACC5A1)
internal val ErrorLight = Color(0xFF963F43)
internal val ErrorDark = Color(0xFFE3A3A0)

/** Кожаный переплёт: непрозрачный светлый текст и запас контраста поверх тиснения. */
object Hero {
    val Base = Color(0xFF263F32)
    val Content = Color.White
    /** Тёмный тон акцента — текст на белых кнопках внутри плиты. */
    val Deep = Color(0xFF182B21)
    internal val GradientStops = listOf(Color(0xFF354E3E), Base, Color(0xFF1B3025))
    internal val Glow = Heritage.Champagne.copy(alpha = .07f)
    val Gradient = Brush.linearGradient(GradientStops)
}
