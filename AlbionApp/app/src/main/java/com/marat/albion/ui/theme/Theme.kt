package com.marat.albion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.marat.albion.data.settings.ThemeMode

/** Светлая: холодная бумага, белые панели, чёрные чернила и графитовые контуры. */
private val LightScheme = lightColorScheme(
    primary = EstateLight, onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E3E2), onPrimaryContainer = Ink,
    inversePrimary = EstateDark,
    secondary = Color(0xFF3D3D3D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E7), onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = SuccessLight, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2EFE5), onTertiaryContainer = Color(0xFF15402A),
    error = ErrorLight, onError = Color.White,
    errorContainer = Color(0xFFF8E3E2), onErrorContainer = Color(0xFF5E1715),
    background = Paper, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = Color(0xFFE8E8E7), onSurfaceVariant = Color(0xFF575757),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE0E0DF), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFECECEB),
    surfaceContainerHigh = Color(0xFFE5E5E4),
    surfaceContainerHighest = Color(0xFFDADAD9),
    inverseSurface = Ink, inverseOnSurface = Color.White,
    outline = Color(0xFF767676), outlineVariant = Color(0xFFD9D9D8),
    scrim = Ink,
)

/** Тёмная: чернильный фон, графитовые панели, белый текст и тонкие серые контуры. */
private val DarkScheme = darkColorScheme(
    primary = EstateDark, onPrimary = Ink,
    primaryContainer = Color(0xFF2B2B2B), onPrimaryContainer = Color(0xFFF2F2F2),
    inversePrimary = EstateLight,
    secondary = Color(0xFFBDBDBD), onSecondary = Ink,
    secondaryContainer = Color(0xFF262626), onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = SuccessDark, onTertiary = Color(0xFF0D2A17),
    tertiaryContainer = Color(0xFF15281B), onTertiaryContainer = Color(0xFFCDEBD5),
    error = ErrorDark, onError = Color(0xFF3D0D0C),
    errorContainer = Color(0xFF331716), onErrorContainer = Color(0xFFFAD6D3),
    background = Ink, onBackground = Color(0xFFEDEDED),
    surface = Ink, onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1F1F1F), onSurfaceVariant = Color(0xFFA3A3A3),
    surfaceTint = Color.Transparent,
    surfaceDim = Ink, surfaceBright = Color(0xFF2E2E2E),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF202020),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    inverseSurface = Color(0xFFEDEDED), inverseOnSurface = Ink,
    outline = Color(0xFF787878), outlineVariant = Color(0xFF2C2C2C),
    scrim = Color.Black,
)

private fun scheme(dark: Boolean): ColorScheme = if (dark) DarkScheme else LightScheme

/** Functional boundaries share Material's outline; outlineVariant remains decorative. */
val ColorScheme.controlBorder: Color get() = outline

/** Выбор — полная инверсия: белая плашка на чернилах, чёрная на бумаге. */
val ColorScheme.selectedContainer: Color get() = inverseSurface
val ColorScheme.selectedBorder: Color get() = inverseSurface
val ColorScheme.selectedContent: Color get() = inverseOnSurface

/** Только прямые углы: строгая геометрия без скруглений. */
private val AlbionShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/** Тёмная ли сейчас тема приложения (с учётом выбора в настройках). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AlbionTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = scheme(darkTheme)
    CompositionLocalProvider(LocalDarkTheme provides darkTheme, LocalContentColor provides colors.onSurface) {
        MaterialTheme(colorScheme = colors, typography = Typography, shapes = AlbionShapes, content = content)
    }
}
