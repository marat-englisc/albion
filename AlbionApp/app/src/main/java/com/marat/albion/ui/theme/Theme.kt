package com.marat.albion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.marat.albion.data.settings.ThemeMode

/** Светлая: белые панели на холодной бумаге, графитовый текст, ирис — действия и прогресс. */
private val LightScheme = lightColorScheme(
    primary = IrisLight, onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE6FF), onPrimaryContainer = Color(0xFF22138A),
    inversePrimary = IrisDark,
    secondary = Color(0xFF6B7080), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E7F7), onSecondaryContainer = Color(0xFF16171D),
    tertiary = SuccessLight, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF3EA), onTertiaryContainer = Color(0xFF06331F),
    error = ErrorLight, onError = Color.White,
    errorContainer = Color(0xFFFCE4E7), onErrorContainer = Color(0xFF5A0E1A),
    background = Paper, onBackground = Color(0xFF16171D),
    surface = Paper, onSurface = Color(0xFF16171D),
    surfaceVariant = Color(0xFFECEEF2), onSurfaceVariant = Color(0xFF5C6170),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE4E6EB), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFF1F4),
    surfaceContainerHigh = Color(0xFFE8EAEF),
    surfaceContainerHighest = Color(0xFFDFE2E8),
    inverseSurface = Color(0xFF16171D), inverseOnSurface = Color(0xFFF5F6F8),
    outline = Color(0xFF858A97), outlineVariant = Color(0xFFE0E2E8),
    scrim = Color.Black,
)

/** Тёмная: графит с холодным подтоном, мягко-белый текст без бликов, светлый ирис. */
private val DarkScheme = darkColorScheme(
    primary = IrisDark, onPrimary = Color(0xFF140E33),
    primaryContainer = Color(0xFF262052), onPrimaryContainer = Color(0xFFDCD6FF),
    inversePrimary = IrisLight,
    secondary = Color(0xFF8A90A0), onSecondary = Ink,
    secondaryContainer = Color(0xFF1F1D33), onSecondaryContainer = Color(0xFFECEEF4),
    tertiary = SuccessDark, onTertiary = Color(0xFF00281A),
    tertiaryContainer = Color(0xFF0E2A21), onTertiaryContainer = Color(0xFFC5F5E2),
    error = ErrorDark, onError = Color(0xFF3A0610),
    errorContainer = Color(0xFF34141A), onErrorContainer = Color(0xFFFFD9DB),
    background = Ink, onBackground = Color(0xFFECEEF4),
    surface = Ink, onSurface = Color(0xFFECEEF4),
    surfaceVariant = Color(0xFF1C1F28), onSurfaceVariant = Color(0xFF9DA3B2),
    surfaceTint = Color.Transparent,
    surfaceDim = Ink, surfaceBright = Color(0xFF262A34),
    surfaceContainerLowest = Color(0xFF07080B),
    surfaceContainerLow = Color(0xFF12141A),
    surfaceContainer = Color(0xFF171920),
    surfaceContainerHigh = Color(0xFF1D2028),
    surfaceContainerHighest = Color(0xFF262A34),
    inverseSurface = Color(0xFFECEEF4), inverseOnSurface = Ink,
    outline = Color(0xFF5A6070), outlineVariant = Color(0xFF252832),
    scrim = Color.Black,
)

private fun scheme(dark: Boolean): ColorScheme = if (dark) DarkScheme else LightScheme

private val AlbionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
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
