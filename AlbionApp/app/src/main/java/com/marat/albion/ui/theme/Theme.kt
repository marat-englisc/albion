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

/** Светлая: слоновая кость, бумажные панели, зелёные чернила и латунные контуры. */
private val LightScheme = lightColorScheme(
    primary = EstateLight, onPrimary = Heritage.Cream,
    primaryContainer = Color(0xFFE1E7D9), onPrimaryContainer = Color(0xFF203C2E),
    inversePrimary = EstateDark,
    secondary = Color(0xFF765B33), onSecondary = Heritage.Cream,
    secondaryContainer = Color(0xFFEBDEC6), onSecondaryContainer = Color(0xFF49391F),
    tertiary = SuccessLight, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E9D8), onTertiaryContainer = Color(0xFF2C452A),
    error = ErrorLight, onError = Color.White,
    errorContainer = Color(0xFFF2DEDA), onErrorContainer = Color(0xFF5D2429),
    background = Paper, onBackground = Color(0xFF292C24),
    surface = Paper, onSurface = Color(0xFF292C24),
    surfaceVariant = Color(0xFFE9DFCF), onSurfaceVariant = Color(0xFF696354),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE2D6C3), surfaceBright = Heritage.Cream,
    surfaceContainerLowest = Color(0xFFFFFAF0),
    surfaceContainerLow = Color(0xFFFAF3E7),
    surfaceContainer = Color(0xFFF0E6D7),
    surfaceContainerHigh = Color(0xFFE9DDCA),
    surfaceContainerHighest = Color(0xFFE1D4BE),
    inverseSurface = Color(0xFF26382B), inverseOnSurface = Heritage.Cream,
    outline = Color(0xFF827053), outlineVariant = Color(0xFFCDBB9A),
    scrim = Ink,
)

/** Тёмная: зелёная кожа переплёта, кремовый текст и светлая латунь. */
private val DarkScheme = darkColorScheme(
    primary = EstateDark, onPrimary = Color(0xFF282417),
    primaryContainer = Color(0xFF394535), onPrimaryContainer = Color(0xFFF1E2BF),
    inversePrimary = EstateLight,
    secondary = Color(0xFFC3AD83), onSecondary = Ink,
    secondaryContainer = Color(0xFF3C382B), onSecondaryContainer = Color(0xFFEBDDC2),
    tertiary = SuccessDark, onTertiary = Color(0xFF1B301C),
    tertiaryContainer = Color(0xFF2B3D2E), onTertiaryContainer = Color(0xFFD4E6CA),
    error = ErrorDark, onError = Color(0xFF411C21),
    errorContainer = Color(0xFF472C2F), onErrorContainer = Color(0xFFF3D4CE),
    background = Ink, onBackground = Color(0xFFF1E8D7),
    surface = Ink, onSurface = Color(0xFFF1E8D7),
    surfaceVariant = Color(0xFF30372C), onSurfaceVariant = Color(0xFFBEB7A6),
    surfaceTint = Color.Transparent,
    surfaceDim = Ink, surfaceBright = Color(0xFF373E32),
    surfaceContainerLowest = Color(0xFF101712),
    surfaceContainerLow = Color(0xFF1E2820),
    surfaceContainer = Color(0xFF263026),
    surfaceContainerHigh = Color(0xFF30392E),
    surfaceContainerHighest = Color(0xFF394234),
    inverseSurface = Color(0xFFF1E8D7), inverseOnSurface = Ink,
    outline = Color(0xFFAD9C78), outlineVariant = Color(0xFF5B5843),
    scrim = Color(0xFF0C130E),
)

private fun scheme(dark: Boolean): ColorScheme = if (dark) DarkScheme else LightScheme

/** Functional boundaries share Material's outline; outlineVariant remains decorative. */
val ColorScheme.controlBorder: Color get() = outline

/** Selection shares an estate-green / brass surface across every control. */
val ColorScheme.selectedContainer: Color get() = primaryContainer
val ColorScheme.selectedBorder: Color get() = primary
val ColorScheme.selectedContent: Color get() = onPrimaryContainer

private val AlbionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
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
