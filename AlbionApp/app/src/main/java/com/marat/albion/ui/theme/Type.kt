package com.marat.albion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.marat.albion.R

@OptIn(ExperimentalTextApi::class)
private fun variable(id: Int, weight: FontWeight, italic: Boolean = false, opticalSize: Float? = null) = Font(
    id, weight = weight, style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        *listOfNotNull(FontVariation.weight(weight.weight), opticalSize?.let { FontVariation.Setting("opsz", it) }).toTypedArray(),
    ),
)

private val weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

object AlbionFonts {
    /** Интерфейс: строгий современный гротеск с родной кириллицей. */
    val Sans = FontFamily(weights.map { variable(R.font.onest, it) })

    /** Акцент: крупные заголовки экранов и цифры. */
    val Display = FontFamily(weights.map { variable(R.font.unbounded, it) })

    /** Технические подписи, индексы и счётчики: тот же широкий гротеск, мелким кеглем — «приборная» нота. */
    val Tech = Display

    /** Книги и учебник: шрифт, созданный для долгого чтения с экрана. */
    val Reading = FontFamily(
        weights.map { variable(R.font.literata, it, opticalSize = 12f) } +
            weights.map { variable(R.font.literata_italic, it, italic = true, opticalSize = 12f) },
    )
}

private val trim = PlatformTextStyle(includeFontPadding = false)
private val centered = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Medium) = TextStyle(
    fontFamily = AlbionFonts.Display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = (-0.02).em, platformStyle = trim, lineHeightStyle = centered,
)

private fun sans(size: Float, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = AlbionFonts.Sans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em, platformStyle = trim, lineHeightStyle = centered,
)

val Typography = Typography(
    displayLarge = display(52, 58, FontWeight.Light),
    displayMedium = display(38, 44, FontWeight.Light),
    displaySmall = display(28, 34, FontWeight.Normal),
    headlineLarge = sans(30f, 36, FontWeight.SemiBold, -0.025),
    headlineMedium = sans(26f, 32, FontWeight.SemiBold, -0.022),
    headlineSmall = sans(22f, 28, FontWeight.SemiBold, -0.018),
    titleLarge = sans(20f, 26, FontWeight.SemiBold, -0.012),
    titleMedium = sans(16f, 22, FontWeight.SemiBold, -0.005),
    titleSmall = sans(14f, 20, FontWeight.SemiBold),
    bodyLarge = sans(16f, 25, FontWeight.Normal, 0.003),
    bodyMedium = sans(15f, 22, FontWeight.Normal, 0.003),
    bodySmall = sans(13f, 19, FontWeight.Normal, 0.005),
    labelLarge = sans(15f, 20, FontWeight.SemiBold, 0.005),
    labelMedium = sans(12.5f, 16, FontWeight.Medium, 0.02),
    labelSmall = sans(11f, 14, FontWeight.Medium, 0.03),
)

/** Текст книг и правил: Literata, щедрый интерлиньяж. */
val ReadingStyle = TextStyle(
    fontFamily = AlbionFonts.Reading, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 29.sp,
)
