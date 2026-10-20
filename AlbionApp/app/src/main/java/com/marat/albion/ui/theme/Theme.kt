package com.marat.albion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Набор тонов одной темы. Иерархия строится только яркостью: важнее — контрастнее. */
@Immutable
data class Palette(
    val isDark: Boolean,
    /** Фон («бумага»). */
    val bg: Color,
    val raised: Color,
    val surface: Color,
    val elevated: Color,
    val high: Color,
    val line: Color,
    val lineStrong: Color,
    /** Самый контрастный цвет («чернила»). */
    val fg: Color,
    val text: Color,
    val soft: Color,
    val muted: Color,
    val faint: Color,
    val code: Color,
)

val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF000000),
    raised = Color(0xFF0A0A0A),
    surface = Color(0xFF111111),
    elevated = Color(0xFF191919),
    high = Color(0xFF222222),
    line = Color(0xFF232323),
    lineStrong = Color(0xFF3A3A3A),
    fg = Color(0xFFFFFFFF),
    text = Color(0xFFEDEDED),
    soft = Color(0xFFC4C4C4),
    muted = Color(0xFFA8A8A8),
    faint = Color(0xFF929292),
    code = Color(0xFF1C1C1C),
)

val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFFFFFFF),
    raised = Color(0xFFF8F8F8),
    surface = Color(0xFFF3F3F3),
    elevated = Color(0xFFEBEBEB),
    high = Color(0xFFE2E2E2),
    line = Color(0xFFE6E6E6),
    lineStrong = Color(0xFFC8C8C8),
    fg = Color(0xFF000000),
    text = Color(0xFF151515),
    soft = Color(0xFF3B3B3B),
    muted = Color(0xFF555555),
    faint = Color(0xFF626262),
    code = Color(0xFFEFEFEF),
)

/**
 * Albion Mono — строгая монохромная палитра текущей темы.
 * Значения читаются из snapshot-состояния, поэтому при смене темы
 * всё, что их использует (композиция и отрисовка), обновляется само.
 */
object Ink {
    var palette by mutableStateOf(DarkPalette)
        internal set

    val Bg get() = palette.bg
    val Raised get() = palette.raised
    val Surface get() = palette.surface
    val Elevated get() = palette.elevated
    val High get() = palette.high
    val Line get() = palette.line
    val LineStrong get() = palette.lineStrong

    val Fg get() = palette.fg
    val Text get() = palette.text
    val Soft get() = palette.soft
    val Muted get() = palette.muted
    val Faint get() = palette.faint
    val Code get() = palette.code
}

/** Антиква — для заголовков, терминов и цифр; гротеск — для интерфейса. */
val Serif = FontFamily.Serif
val Sans = FontFamily.SansSerif

private fun schemeOf(p: Palette): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.fg,
        onPrimary = p.bg,
        primaryContainer = p.high,
        onPrimaryContainer = p.fg,
        secondary = p.soft,
        onSecondary = p.bg,
        secondaryContainer = p.elevated,
        onSecondaryContainer = p.fg,
        tertiary = p.muted,
        onTertiary = p.bg,
        background = p.bg,
        onBackground = p.text,
        surface = p.bg,
        onSurface = p.text,
        surfaceVariant = p.elevated,
        onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.bg,
        surfaceContainerLow = p.raised,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.elevated,
        surfaceContainerHighest = p.high,
        surfaceBright = p.high,
        surfaceDim = p.bg,
        outline = p.lineStrong,
        outlineVariant = p.line,
        error = p.fg,
        onError = p.bg,
        inverseSurface = p.fg,
        inverseOnSurface = p.bg,
        inversePrimary = p.bg,
        scrim = Color.Black,
    )
}

private val base = Typography()

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.25.sp),
    labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp, letterSpacing = 0.1.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

object AlbionType {
    /** Мелкие заглавные с широкой разрядкой — подписи секций. */
    val eyebrow = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
    /** Транскрипция. */
    val ipa get() = TextStyle(fontFamily = Serif, fontSize = 18.sp, fontStyle = FontStyle.Italic, color = Ink.Muted)
    /** Крупные цифры статистики — тонкий гротеск. */
    val number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.5).sp)
    val numberSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp)
    /** Порядковые номера «01», «02». */
    val index get() = TextStyle(fontFamily = Serif, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = Ink.Faint)
}

/** Палитра переключается целиком: при смешивании светлой и тёмной тем текст сливался с фоном. */
@Composable
fun AlbionTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    SideEffect { if (Ink.palette != palette) Ink.palette = palette }
    MaterialTheme(colorScheme = schemeOf(palette), typography = typography, shapes = shapes, content = content)
}
