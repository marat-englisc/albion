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

/**
 * Набор тонов одной темы. Палитра выведена из Финна: кремовое оперение, тёмно-графитовая спинка,
 * зелёный вязаный шарф с бежевой полоской и оранжевый клюв. Иерархия текста строится яркостью,
 * акцент (шарф) отмечает действие и успех, тёплый (клюв) — ошибки, серию и праздники.
 */
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
    val success: Color,
    /** Шарф Финна — главное действие, выбор, прогресс. */
    val accent: Color,
    val onAccent: Color,
    /** Нижняя «кромка» объёмных кнопок. */
    val accentDeep: Color,
    /** Мягкая заливка выбранного и карточек с акцентом. */
    val accentSoft: Color,
    /** Клюв — ошибки, серия, предупреждения; читается как текст. */
    val warm: Color,
    val onWarm: Color,
    val warmDeep: Color,
    val warmSoft: Color,
    /** Чистый оранжевый клюва — только для декора: конфетти, огонёк серии. */
    val beak: Color,
    /** Бежевая полоска шарфа — блики и праздничные искры. */
    val stripe: Color,
    /** Тень под карточками. */
    val shadow: Color,
)

/** Светлая тема — «утро у моря»: кремовая бумага, графитовые чернила, зелёный шарф. */
val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFFBF6EE), raised = Color(0xFFFFFDF8), surface = Color(0xFFF6EFE3),
    elevated = Color(0xFFEFE6D6), high = Color(0xFFE7DCC8),
    line = Color(0xFFEADFCC), lineStrong = Color(0xFFD5C6AD),
    fg = Color(0xFF272325), text = Color(0xFF2E2A2C), soft = Color(0xFF4A4345),
    muted = Color(0xFF5F5658), faint = Color(0xFF655B5D), code = Color(0xFFF3EADB),
    success = Color(0xFF2C6249),
    accent = Color(0xFF2C6249), onAccent = Color(0xFFFFFDF7), accentDeep = Color(0xFF1C4232), accentSoft = Color(0xFFDDEBDF),
    warm = Color(0xFF9C4217), onWarm = Color(0xFFFFFDF7), warmDeep = Color(0xFF6E2C0C), warmSoft = Color(0xFFFBE4D4),
    beak = Color(0xFFEE7A3F), stripe = Color(0xFFE6CC94),
    shadow = Color(0xFF6B4F2A),
)

/** Тёмная тема — «ночной фьорд»: глубокая сине-зелёная вода, кремовый текст, светящийся шарф. */
val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF111917), raised = Color(0xFF16201D), surface = Color(0xFF1A2522),
    elevated = Color(0xFF212E2A), high = Color(0xFF283733),
    line = Color(0xFF26332F), lineStrong = Color(0xFF3B4C47),
    fg = Color(0xFFF7F0E4), text = Color(0xFFEEE7DB), soft = Color(0xFFD3CBBE),
    muted = Color(0xFFB3AB9E), faint = Color(0xFFA8A094), code = Color(0xFF1E2A27),
    success = Color(0xFF86CFA4),
    accent = Color(0xFF86CFA4), onAccent = Color(0xFF0E1A14), accentDeep = Color(0xFF4C8D69), accentSoft = Color(0xFF1D362B),
    warm = Color(0xFFF49A68), onWarm = Color(0xFF1A0F08), warmDeep = Color(0xFFB8653A), warmSoft = Color(0xFF3A261A),
    beak = Color(0xFFF08A52), stripe = Color(0xFFE9D3A1),
    shadow = Color(0xFF000000),
)

fun paletteFor(dark: Boolean): Palette = if (dark) DarkPalette else LightPalette

/** Промежуточная палитра — для плавной смены темы. */
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
        success = c(a.success, b.success),
        accent = c(a.accent, b.accent), onAccent = c(a.onAccent, b.onAccent),
        accentDeep = c(a.accentDeep, b.accentDeep), accentSoft = c(a.accentSoft, b.accentSoft),
        warm = c(a.warm, b.warm), onWarm = c(a.onWarm, b.onWarm),
        warmDeep = c(a.warmDeep, b.warmDeep), warmSoft = c(a.warmSoft, b.warmSoft),
        beak = c(a.beak, b.beak), stripe = c(a.stripe, b.stripe), shadow = c(a.shadow, b.shadow),
    )
}

/**
 * Текущая палитра Albion.
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
    val Success get() = palette.success

    val Accent get() = palette.accent
    val OnAccent get() = palette.onAccent
    val AccentDeep get() = palette.accentDeep
    val AccentSoft get() = palette.accentSoft
    val Warm get() = palette.warm
    val OnWarm get() = palette.onWarm
    val WarmDeep get() = palette.warmDeep
    val WarmSoft get() = palette.warmSoft
    val Beak get() = palette.beak
    val Stripe get() = palette.stripe
    val Shadow get() = palette.shadow
}

/** Антиква — для заголовков, терминов и цифр; гротеск — для интерфейса. */
val Serif = FontFamily(
    Font(R.font.noto_serif_regular),
    Font(R.font.noto_serif_italic, style = FontStyle.Italic),
)
val Sans = FontFamily(
    Font(R.font.noto_sans_light, weight = FontWeight.Light),
    Font(R.font.noto_sans_regular),
    Font(R.font.noto_sans_medium, weight = FontWeight.Medium),
    Font(R.font.noto_sans_semibold, weight = FontWeight.SemiBold),
    Font(R.font.noto_sans_italic, style = FontStyle.Italic),
)

private fun schemeOf(p: Palette): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.accent,
        onPrimary = p.onAccent,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.fg,
        secondary = p.soft,
        onSecondary = p.bg,
        secondaryContainer = p.elevated,
        onSecondaryContainer = p.fg,
        tertiary = p.muted,
        onTertiary = p.bg,
        tertiaryContainer = p.elevated,
        onTertiaryContainer = p.fg,
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
        error = p.warm,
        onError = p.onWarm,
        errorContainer = p.warmSoft,
        onErrorContainer = p.fg,
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

/** Мягкие, «галечные» скругления — как у игрушки, а не у таблицы. */
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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

/**
 * Палитра переключается целиком (при смешивании светлой и тёмной тем текст сливался с фоном),
 * но не скачком: цвета перетекают за полсекунды, как будто над фьордом сменилось освещение.
 */
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
