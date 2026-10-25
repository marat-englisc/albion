package com.marat.albion.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.R
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Финн — тупик в зелёном шарфе, маскот Albion. Каждая поза — свой момент:
 * приветствие, итог занятия, подсказка, пустой список. Исходники — папка mascot,
 * в APK их кладёт scripts/convert_mascots.py.
 */
enum class Finn(@DrawableRes val res: Int) {
    WELCOME(R.drawable.finn_welcome),
    WELCOME_BACK(R.drawable.finn_welcome_back),
    TODAY(R.drawable.finn_today),
    DAILY_GOAL(R.drawable.finn_daily_goal),
    ALL_DONE(R.drawable.finn_all_done),
    REST(R.drawable.finn_rest),
    SESSION_COMPLETE(R.drawable.finn_session_complete),
    MILESTONE(R.drawable.finn_milestone),
    CORRECT(R.drawable.finn_correct),
    TRY_AGAIN(R.drawable.finn_try_again),
    THINKING(R.drawable.finn_thinking),
    HINT(R.drawable.finn_hint),
    FLASHCARDS(R.drawable.finn_flashcards),
    GRAMMAR(R.drawable.finn_grammar),
    WRITING(R.drawable.finn_writing),
    LISTENING(R.drawable.finn_listening),
    SPEAKING(R.drawable.finn_speaking),
    SENTENCE_BUILDER(R.drawable.finn_sentence_builder),
    READING(R.drawable.finn_reading),
    IMPORT_BOOK(R.drawable.finn_import_book),
    EMPTY_LIBRARY(R.drawable.finn_empty_library),
    NO_RESULTS(R.drawable.finn_no_results),
    OFFLINE(R.drawable.finn_offline),
    WORD_SAVED(R.drawable.finn_word_saved),
}

/**
 * Сцена Финна: на экране он всегда один. Каждый рисунок Финна (поза, выглядывание, Финн в диалоге)
 * встаёт в очередь при появлении, и виден только последний. Открылся диалог со справкой — Финн
 * «переходит» в него, закрылся — возвращается на страницу; при переходе между экранами новый
 * Финн сменяет старого, а не появляется рядом.
 */
object FinnStage {
    private val owners = mutableStateListOf<Any>()

    internal fun enter(owner: Any) {
        owners.remove(owner)
        owners.add(owner)
    }

    internal fun leave(owner: Any) {
        owners.remove(owner)
    }

    internal fun isFront(owner: Any) = owners.lastOrNull() === owner
}

/** Занимает место на сцене, пока composable жив; возвращает плавную видимость 0…1. */
@Composable
internal fun rememberStagePresence(): Float {
    val token = remember { Any() }
    DisposableEffect(token) {
        FinnStage.enter(token)
        onDispose { FinnStage.leave(token) }
    }
    val front = FinnStage.isFront(token)
    val presence by animateFloatAsState(
        if (front) 1f else 0f,
        if (front) spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow) else tween(160),
        label = "stage",
    )
    return presence
}

/**
 * Финн в позе [pose] — живой элемент интерфейса, а не наклейка.
 *
 * Он «дышит» и чуть покачивается, стоит на мягкой тени, на тёмной теме — в тёплом ореоле.
 * Касание — прыжок со сжатием при приземлении и россыпь искр цвета шарфа и клюва;
 * [onTap] даёт экрану ответить (сменить реплику, позу). Появляется, приподнимаясь;
 * смена позы — плавная. [delayMs] согласует появление с каскадом экрана.
 *
 * Рядом всегда есть текст, который говорит то же самое, поэтому для экранного диктора
 * картинка декоративная.
 */
@Composable
fun Mascot(
    pose: Finn,
    size: Dp,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    onTap: (() -> Unit)? = null,
) {
    val presence = rememberStagePresence()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val idle = rememberInfiniteTransition(label = "idle")
    val breath by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    val sway by idle.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway",
    )

    // Прыжок: высота (0…1) и «сплющивание» при приземлении.
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    var burstSeed by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rise = with(density) { 14.dp.toPx() }
    val jump = with(density) { (size * 0.16f).toPx() }

    fun react() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        burstSeed++
        scope.launch {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(720, easing = Easing.EmphasizedDecelerate))
        }
        scope.launch {
            squash.animateTo(1f, tween(70))
            launch { squash.animateTo(-0.6f, tween(150)) }
            hop.animateTo(1f, tween(190, easing = Easing.EmphasizedDecelerate))
            hop.animateTo(0f, tween(210, easing = Easing.EmphasizedAccelerate))
            squash.animateTo(1f, tween(60))
            squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
        onTap?.invoke()
    }

    val tapModifier = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Image,
        onClickLabel = "Погладить Финна",
        onClick = ::react,
    )

    Box(
        modifier.size(size).graphicsLayer {
            val p = appear.value * presence
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * rise
            val s = 0.9f + 0.1f * p
            scaleX = s; scaleY = s
            transformOrigin = TransformOrigin(0.5f, 0.95f)
        }.then(tapModifier),
        contentAlignment = Alignment.Center,
    ) {
        val dark = Ink.palette.isDark
        val glow = Ink.Stripe
        val accent = Ink.Accent
        val shadowColor = if (dark) Color.Black else Ink.Shadow
        Canvas(Modifier.fillMaxSize()) {
            if (dark) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.18f), accent.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(this.size.width / 2, this.size.height * 0.52f),
                        radius = this.size.minDimension * 0.52f,
                    ),
                    radius = this.size.minDimension * 0.52f,
                    center = Offset(this.size.width / 2, this.size.height * 0.52f),
                )
            }
            // Тень под лапами сжимается, когда Финн в прыжке.
            val lift = hop.value
            val w = this.size.width * 0.52f * (1f - 0.35f * lift)
            val h = this.size.height * 0.06f * (1f - 0.3f * lift)
            drawOval(
                Brush.radialGradient(
                    listOf(shadowColor.copy(alpha = (if (dark) 0.45f else 0.16f) * (1f - 0.5f * lift)), Color.Transparent),
                    center = Offset(this.size.width / 2, this.size.height * 0.95f), radius = w / 2,
                ),
                topLeft = Offset((this.size.width - w) / 2, this.size.height * 0.95f - h / 2),
                size = Size(w, h),
            )
        }
        AnimatedContent(
            targetState = pose,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + scaleIn(tween(320, easing = Easing.Emphasized), initialScale = 0.9f)) togetherWith
                    fadeOut(tween(140))
            },
            label = "finn",
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val b = breath
                val sq = squash.value
                translationY = -hop.value * jump
                scaleY = 1f + 0.018f * b - 0.09f * sq
                scaleX = 1f - 0.008f * b + 0.08f * sq
                rotationZ = sway * 1.4f
                transformOrigin = TransformOrigin(0.5f, 0.95f)
            },
        ) { p ->
            Image(
                bitmap = ImageBitmap.imageResource(p.res),
                contentDescription = null,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (burstSeed > 0) Sparkles(burst.value, burstSeed, Modifier.fillMaxSize())
    }
}

/** Россыпь искр вокруг Финна после касания: звёздочки цвета клюва, шарфа и полоски. */
@Composable
private fun Sparkles(progress: Float, seed: Int, modifier: Modifier) {
    val colors = listOf(Ink.Beak, Ink.Accent, Ink.Stripe, Ink.Beak, Ink.Stripe, Ink.Accent, Ink.Beak)
    Canvas(modifier) {
        if (progress >= 1f) return@Canvas
        val center = Offset(size.width / 2, size.height * 0.42f)
        val reach = size.minDimension * 0.62f
        colors.forEachIndexed { i, color ->
            val angle = (-PI / 2) + (i - 3) * 0.42 + ((seed * 37 + i * 13) % 10 - 5) * 0.02
            val dist = reach * (0.45f + 0.55f * progress) * (0.85f + (i % 3) * 0.1f)
            val c = Offset(center.x + (cos(angle) * dist).toFloat(), center.y + (sin(angle) * dist).toFloat())
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val r = size.minDimension * 0.035f * (1f - 0.5f * progress)
            drawStar(c, r, color.copy(alpha = alpha))
        }
    }
}

/** Четырёхлучевая искра. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(center: Offset, r: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - r * 2)
        quadraticTo(center.x, center.y, center.x + r * 2, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r * 2)
        quadraticTo(center.x, center.y, center.x - r * 2, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r * 2)
        close()
    }
    drawPath(path, color)
}

/** Бесконечное мягкое мерцание 0…1 — для бликов и «живых» мелочей рядом с Финном. */
@Composable
internal fun rememberTwinkle(periodMs: Int = 2400): Float {
    val t by rememberInfiniteTransition(label = "twinkle").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "t",
    )
    return t
}
