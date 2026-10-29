package com.marat.albion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.R

@Immutable
data class Palette(
    val bg: Color,
    val raised: Color,
    val surface: Color,
    val elevated: Color,
    val high: Color,
    val line: Color,
    val lineStrong: Color,
    val fg: Color,
    val text: Color,
    val soft: Color,
    val muted: Color,
    val faint: Color,
    val code: Color,
    val success: Color,
    val successText: Color,
    val successSoft: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val error: Color,
    val errorText: Color,
    val onError: Color,
    val errorDeep: Color,
    val errorSoft: Color,
    val warm: Color,
    val warmText: Color,
    val onWarm: Color,
    val warmDeep: Color,
    val warmSoft: Color,
    val gold: Color,
    val goldText: Color,
    val goldSoft: Color,
    val ivory: Color,
    val shadow: Color,
)

/** Modern British Elegance — warm paper, crimson, antique gold and charcoal. */
val LightPalette = Palette(
    bg = Color(0xFFF6F1E7), raised = Color(0xFFFBF8F1), surface = Color(0xFFF0EBE2),
    elevated = Color(0xFFF0EBE2), high = Color(0xFFEEE8DE),
    line = Color(0xFFD8D1C5), lineStrong = Color(0xFF8F8475),
    fg = Color(0xFF24272B), text = Color(0xFF24272B), soft = Color(0xFF575149),
    muted = Color(0xFF6E675F), faint = Color(0xFF6E675F), code = Color(0xFFF0EBE2),
    success = Color(0xFF4F6B57), successText = Color(0xFF3D5945), successSoft = Color(0xFFE6EDE5),
    accent = Color(0xFF861F39), accentText = Color(0xFF861F39),
    onAccent = Color(0xFFF6F1E7), accentDeep = Color(0xFF6E192F), accentSoft = Color(0xFFF4E5E7),
    error = Color(0xFFA13A3A), errorText = Color(0xFF963131),
    onError = Color(0xFFFFFDFC), errorDeep = Color(0xFF852C2C), errorSoft = Color(0xFFF6E5E2),
    warm = Color(0xFFB98A3D), warmText = Color(0xFF79571F),
    onWarm = Color(0xFF24272B), warmDeep = Color(0xFFC3974A), warmSoft = Color(0xFFF0E6D3),
    gold = Color(0xFFC6A66D), goldText = Color(0xFF765A2C), goldSoft = Color(0xFFF2EAD9),
    ivory = Color(0xFFF6F1E7), shadow = Color(0xFF24272B),
)

object Ink {
    val palette = LightPalette

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
    val Success get() = palette.successText
    val SuccessFill get() = palette.success
    val SuccessSoft get() = palette.successSoft

    val Accent get() = palette.accent
    val AccentText get() = palette.accentText
    val OnAccent get() = palette.onAccent
    val AccentDeep get() = palette.accentDeep
    val AccentSoft get() = palette.accentSoft
    val Error get() = palette.errorText
    val ErrorFill get() = palette.error
    val OnError get() = palette.onError
    val ErrorDeep get() = palette.errorDeep
    val ErrorSoft get() = palette.errorSoft
    val Warm get() = palette.warmText
    val WarmFill get() = palette.warm
    val OnWarm get() = palette.onWarm
    val WarmDeep get() = palette.warmDeep
    val WarmSoft get() = palette.warmSoft
    val Gold get() = palette.gold
    val GoldText get() = palette.goldText
    val GoldSoft get() = palette.goldSoft
    val Ivory get() = palette.ivory
    val Shadow get() = palette.shadow
}

// The brand voice and English editorial headings use the supplied Adobe Caslon.
val Brand = FontFamily(
    Font(R.font.adobe_caslon_pro_regular, weight = FontWeight.Normal),
    Font(R.font.adobe_caslon_pro_italic, style = FontStyle.Italic),
    Font(R.font.adobe_caslon_pro_semibold, weight = FontWeight.SemiBold),
    Font(R.font.adobe_caslon_pro_semibold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.adobe_caslon_pro_bold, weight = FontWeight.Bold),
    Font(R.font.adobe_caslon_pro_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun editorialFont(id: Int, weight: FontWeight = FontWeight.Normal, italic: Boolean = false) = Font(
    id, weight = weight, style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.weight(weight.weight),
    ),
)

val Cyrillic = FontFamily(
    editorialFont(R.font.cormorant_garamond_variable, FontWeight.Medium),
    editorialFont(R.font.cormorant_garamond_variable, FontWeight.Normal),
    editorialFont(R.font.cormorant_garamond_variable, FontWeight.SemiBold),
    editorialFont(R.font.cormorant_garamond_variable, FontWeight.Bold),
    editorialFont(R.font.cormorant_garamond_italic_variable, italic = true),
    editorialFont(R.font.cormorant_garamond_italic_variable, FontWeight.SemiBold, italic = true),
)
val Reading = FontFamily(
    editorialFont(R.font.literata_variable),
    editorialFont(R.font.literata_variable, FontWeight.Medium),
    editorialFont(R.font.literata_variable, FontWeight.SemiBold),
    editorialFont(R.font.literata_variable, FontWeight.Bold),
    editorialFont(R.font.literata_italic_variable, italic = true),
    editorialFont(R.font.literata_italic_variable, FontWeight.Medium, italic = true),
    editorialFont(R.font.literata_italic_variable, FontWeight.Bold, italic = true),
)

var Serif: FontFamily = Cyrillic
    private set

/** Caslon for Latin, Cormorant for Cyrillic, Inter for symbols and IPA. */
internal fun initializeTypography(resources: android.content.res.Resources) {
    InitialFonts.initialize(resources)
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        fun family(regular: Int, italic: Int, variable: Boolean = false, semibold: Int? = null, semiboldItalic: Int? = null): android.graphics.fonts.FontFamily {
            val builder = android.graphics.fonts.FontFamily.Builder(
                android.graphics.fonts.Font.Builder(resources, regular).setWeight(400)
                    .apply { if (variable) setFontVariationSettings("'wght' 400") }.build(),
            )
            builder.addFont(android.graphics.fonts.Font.Builder(resources, italic).setWeight(400)
                .setSlant(android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC)
                .apply { if (variable) setFontVariationSettings("'wght' 400") }.build())
            if (variable || semibold != null) {
                listOf(500, 600, 700).forEach { weight ->
                    builder.addFont(android.graphics.fonts.Font.Builder(resources, semibold ?: regular)
                        .setWeight(weight).apply { if (variable) setFontVariationSettings("'wght' $weight") }.build())
                    builder.addFont(android.graphics.fonts.Font.Builder(resources, semiboldItalic ?: italic)
                        .setWeight(weight).setSlant(android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC)
                        .apply { if (variable) setFontVariationSettings("'wght' $weight") }.build())
                }
            }
            return builder.build()
        }
        Serif = FontFamily(android.graphics.Typeface.CustomFallbackBuilder(
            family(R.font.adobe_caslon_pro_regular, R.font.adobe_caslon_pro_italic,
                semibold = R.font.adobe_caslon_pro_semibold, semiboldItalic = R.font.adobe_caslon_pro_semibold_italic),
        ).addCustomFallback(family(R.font.cormorant_garamond_variable, R.font.cormorant_garamond_italic_variable, true))
            .addCustomFallback(family(R.font.inter_variable, R.font.inter_italic_variable, true))
            .setSystemFallback("serif").build())
    }
}

/** Explicit script selection also preserves the intended face on Android 8 and 9. */
fun headingFamily(text: String): FontFamily {
    val cyrillic = text.any { it in '\u0400'..'\u052f' }
    val latin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
    return when {
        cyrillic && latin -> Serif
        cyrillic -> Cyrillic
        else -> Brand
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: FontWeight, italic: Boolean = false) = Font(
    if (italic) R.font.inter_italic_variable else R.font.inter_variable,
    weight = weight,
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.weight(weight.weight),
        androidx.compose.ui.text.font.FontVariation.Setting("opsz", 18f),
    ),
)
val Sans = FontFamily(
    inter(FontWeight.Light), inter(FontWeight.Normal), inter(FontWeight.Medium),
    inter(FontWeight.SemiBold), inter(FontWeight.Bold), inter(FontWeight.Normal, italic = true),
    inter(FontWeight.Medium, italic = true),
)

private fun schemeOf(p: Palette): ColorScheme {
    val base = lightColorScheme()
    return base.copy(
        primary = p.accentText,
        onPrimary = p.onAccent,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.fg,
        secondary = p.goldText,
        onSecondary = p.bg,
        secondaryContainer = p.elevated,
        onSecondaryContainer = p.fg,
        tertiary = p.successText,
        onTertiary = p.bg,
        tertiaryContainer = p.surface,
        onTertiaryContainer = p.successText,
        background = p.bg,
        onBackground = p.text,
        surface = p.raised,
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
        surfaceTint = p.accent,
        error = p.errorText,
        onError = p.onError,
        errorContainer = p.errorSoft,
        onErrorContainer = p.errorText,
        inverseSurface = p.fg,
        inverseOnSurface = p.bg,
        inversePrimary = p.accentSoft,
        scrim = Color.Black,
    )
}

private val base = Typography()

private val typography get() = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 27.sp, letterSpacing = 0.1.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

object AlbionType {
        val eyebrow = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        val ipa get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 17.sp, fontStyle = FontStyle.Normal, color = Ink.Muted)
        val number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.5).sp)
    val numberSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp)
        val index get() = TextStyle(fontFamily = Brand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontStyle = FontStyle.Italic, color = Ink.Faint)
}

/** Albion has one permanent ivory Reading Room appearance, independent of system night mode. */
@Composable
fun AlbionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeOf(LightPalette), typography = typography, shapes = shapes, content = content)
}
