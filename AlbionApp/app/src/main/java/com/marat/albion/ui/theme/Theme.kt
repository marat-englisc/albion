package com.marat.albion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    val isDark: Boolean,
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
    isDark = false,
    bg = Color(0xFFF6F1E7), raised = Color(0xFFFFFDFC), surface = Color(0xFFF0EBE2),
    elevated = Color(0xFFF0EBE2), high = Color(0xFFEEE8DE),
    line = Color(0xFFD8D1C5), lineStrong = Color(0xFFB9AFA0),
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

val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF24272B), raised = Color(0xFF2E3238), surface = Color(0xFF2E3238),
    elevated = Color(0xFF383D44), high = Color(0xFF383D44),
    line = Color(0xFF444A52), lineStrong = Color(0xFF686D73),
    fg = Color(0xFFF6F1E7), text = Color(0xFFF6F1E7), soft = Color(0xFFD8D1C5),
    muted = Color(0xFFC8C1B6), faint = Color(0xFFC8C1B6), code = Color(0xFF2E3238),
    success = Color(0xFF6E8B74), successText = Color(0xFFA9C7B0), successSoft = Color(0xFF303E35),
    accent = Color(0xFFA02A47), accentText = Color(0xFFF6F1E7),
    onAccent = Color(0xFFF6F1E7), accentDeep = Color(0xFFB33455), accentSoft = Color(0xFF432E37),
    error = Color(0xFFC96A6A), errorText = Color(0xFFFFB5AE),
    onError = Color(0xFF000000), errorDeep = Color(0xFFB75B5B), errorSoft = Color(0xFF493234),
    warm = Color(0xFFD2A85A), warmText = Color(0xFFE4BF7B),
    onWarm = Color(0xFF24272B), warmDeep = Color(0xFFBD934A), warmSoft = Color(0xFF423B2F),
    gold = Color(0xFFC6A66D), goldText = Color(0xFFD8B980), goldSoft = Color(0xFF3C372F),
    ivory = Color(0xFFF6F1E7), shadow = Color(0xFF000000),
)

fun paletteFor(dark: Boolean): Palette = if (dark) DarkPalette else LightPalette

fun lerpPalette(a: Palette, b: Palette, t: Float): Palette {
    if (t <= 0f) return a
    if (t >= 1f) return b
    fun c(x: Color, y: Color) = lerp(x, y, t)
    return Palette(
        isDark = if (t < 0.5f) a.isDark else b.isDark,
        bg = c(a.bg, b.bg), raised = c(a.raised, b.raised), surface = c(a.surface, b.surface),
        elevated = c(a.elevated, b.elevated), high = c(a.high, b.high),
        line = c(a.line, b.line), lineStrong = c(a.lineStrong, b.lineStrong),
        fg = c(a.fg, b.fg), text = c(a.text, b.text), soft = c(a.soft, b.soft),
        muted = c(a.muted, b.muted), faint = c(a.faint, b.faint), code = c(a.code, b.code),
        success = c(a.success, b.success), successText = c(a.successText, b.successText), successSoft = c(a.successSoft, b.successSoft),
        accent = c(a.accent, b.accent), accentText = c(a.accentText, b.accentText), onAccent = c(a.onAccent, b.onAccent),
        accentDeep = c(a.accentDeep, b.accentDeep), accentSoft = c(a.accentSoft, b.accentSoft),
        error = c(a.error, b.error), errorText = c(a.errorText, b.errorText), onError = c(a.onError, b.onError),
        errorDeep = c(a.errorDeep, b.errorDeep), errorSoft = c(a.errorSoft, b.errorSoft),
        warm = c(a.warm, b.warm), warmText = c(a.warmText, b.warmText), onWarm = c(a.onWarm, b.onWarm),
        warmDeep = c(a.warmDeep, b.warmDeep), warmSoft = c(a.warmSoft, b.warmSoft),
        gold = c(a.gold, b.gold), goldText = c(a.goldText, b.goldText), goldSoft = c(a.goldSoft, b.goldSoft), ivory = c(a.ivory, b.ivory), shadow = c(a.shadow, b.shadow),
    )
}

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

// Libre Baskerville covers Latin; the bundled Noto serif provides Cyrillic/IPA.
// Adobe Caslon Pro is reserved for the wordmark and brand phrases.
val Brand = FontFamily(
    Font(R.font.adobe_caslon_pro_regular, weight = FontWeight.Normal),
    Font(R.font.adobe_caslon_pro_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.adobe_caslon_pro_semibold, weight = FontWeight.SemiBold),
    Font(R.font.adobe_caslon_pro_semibold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.adobe_caslon_pro_bold, weight = FontWeight.Bold),
    Font(R.font.adobe_caslon_pro_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)
var Serif = FontFamily(Font(R.font.libre_baskerville), Font(R.font.libre_baskerville_italic, style = FontStyle.Italic))
    private set

/** Explicit Cyrillic and IPA fallback keeps Russian lesson titles in a serif. */
internal fun initializeTypography(resources: android.content.res.Resources) {
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        fun family(regular: Int, italic: Int) = android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(resources, regular).setWeight(400)
                .setSlant(android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT).build(),
        ).addFont(android.graphics.fonts.Font.Builder(resources, italic).setWeight(400)
            .setSlant(android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC).build()).build()
        val typeface = android.graphics.Typeface.CustomFallbackBuilder(
            family(R.font.libre_baskerville, R.font.libre_baskerville_italic),
        ).addCustomFallback(family(R.font.noto_serif_regular, R.font.noto_serif_italic))
            .setSystemFallback("serif").build()
        Serif = FontFamily(typeface)
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
)

private fun schemeOf(p: Palette): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.accentText,
        onPrimary = if (p.isDark) p.bg else p.onAccent,
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
        inversePrimary = if (p.isDark) LightPalette.accent else DarkPalette.accent,
        scrim = Color.Black,
    )
}

private val base = Typography()

private val typography get() = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Sans, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.25.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = Sans, fontSize = 17.sp, lineHeight = 27.sp, letterSpacing = 0.1.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Sans, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

object AlbionType {
        val eyebrow = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
        val ipa get() = TextStyle(fontFamily = Sans, fontSize = 17.sp, fontStyle = FontStyle.Normal, color = Ink.Muted)
        val number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.5).sp)
    val numberSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp)
        val index get() = TextStyle(fontFamily = Serif, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = Ink.Faint)
}

@Composable
fun AlbionTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val palette = animatedPalette(paletteFor(dark))
    SideEffect { if (Ink.palette != palette) Ink.palette = palette }
    MaterialTheme(colorScheme = schemeOf(palette), typography = typography, shapes = shapes, content = content)
}

@Composable
private fun animatedPalette(target: Palette): Palette {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val t = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = lerpPalette(from, to, t.value)
        to = target
        t.snapTo(0f)
        t.animateTo(1f, tween(520, easing = Easing.Standard))
    }
    return lerpPalette(from, to, t.value)
}
