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
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
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
    /** Интерфейс: Geist — строгий швейцарский гротеск с родной кириллицей. */
    val Sans = FontFamily(weights.map { variable(R.font.geist, it) })

    /** Заголовки, термины и цифры: тот же Geist, светлые начертания и плотная разрядка. */
    val Display = Sans

    /** Служебные подписи, индексы и счётчики: моноширинный Geist Mono. */
    val Tech = FontFamily(weights.map { variable(R.font.geist_mono, it) })

    /** Транскрипции целиком набраны одной встроенной гарнитурой, включая IPA и диакритику. */
    val Ipa = FontFamily(Font(R.font.noto_sans, FontWeight.Normal))

    /** Книги и учебник: Source Serif 4 с оптическим кеглем для долгого чтения. */
    val Reading = FontFamily(
        weights.map { variable(R.font.source_serif, it, opticalSize = 14f) } +
            weights.map { variable(R.font.source_serif_italic, it, italic = true, opticalSize = 14f) },
    )
}

private val trim = PlatformTextStyle(includeFontPadding = false)
private val centered = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Medium, tracking: Double = -0.03) = TextStyle(
    fontFamily = AlbionFonts.Display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em, platformStyle = trim, lineHeightStyle = centered,
    lineBreak = LineBreak.Heading, hyphens = Hyphens.Auto,
)

private fun sans(size: Float, line: Int, weight: FontWeight, tracking: Double = 0.0, family: FontFamily = AlbionFonts.Sans) = TextStyle(
    fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em, platformStyle = trim, lineHeightStyle = centered,
    lineBreak = LineBreak.Paragraph, hyphens = Hyphens.Auto,
)

val Typography = Typography(
    displayLarge = display(52, 56, FontWeight.Light, -0.045),
    displayMedium = display(38, 42, FontWeight.Light, -0.04),
    displaySmall = display(28, 34, FontWeight.Normal, -0.035),
    headlineLarge = display(30, 36, FontWeight.Medium),
    headlineMedium = display(26, 32, FontWeight.Medium),
    headlineSmall = display(22, 28, FontWeight.Medium, -0.025),
    titleLarge = display(20, 26, FontWeight.Medium, -0.02),
    titleMedium = sans(16f, 22, FontWeight.SemiBold, -0.01),
    titleSmall = sans(14f, 20, FontWeight.SemiBold),
    bodyLarge = sans(16f, 25, FontWeight.Normal),
    bodyMedium = sans(15f, 22, FontWeight.Normal),
    bodySmall = sans(13f, 19, FontWeight.Normal, 0.005),
    labelLarge = sans(15f, 20, FontWeight.Medium, 0.005),
    // Служебные подписи — моноширинные: индексы, счётчики, «04 / НАВЫКИ».
    labelMedium = sans(12.5f, 16, FontWeight.Medium, 0.04, AlbionFonts.Tech),
    labelSmall = sans(11f, 14, FontWeight.Medium, 0.05, AlbionFonts.Tech),
)

/** Текст книг и правил: Source Serif 4, щедрый интерлиньяж. */
val ReadingStyle = TextStyle(
    fontFamily = AlbionFonts.Reading, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 29.sp,
)

/** Отдельная роль: Noto Sans сохраняет единые метрики всех символов транскрипции. */
val IpaStyle = TextStyle(
    fontFamily = AlbionFonts.Ipa, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp,
    platformStyle = trim, lineHeightStyle = centered,
)
