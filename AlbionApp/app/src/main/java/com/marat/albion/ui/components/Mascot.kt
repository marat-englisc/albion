package com.marat.albion.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.R
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink

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

/** Тёплый «бумажный» свет за Финном на тёмных темах — чтобы тёмное оперение не тонуло в фоне. */
private val Glow = Color(0xFFF3ECDD)

/**
 * Финн в позе [pose]. Рядом всегда есть текст, который говорит то же самое,
 * поэтому для экранного диктора картинка декоративная.
 *
 * На тёмной теме за ним мягкое свечение, на любой — тень под лапами: иллюстрация
 * стоит на странице, а не висит наклейкой. Появляется, чуть приподнимаясь;
 * смена позы — плавная. [delayMs] согласует появление с каскадом экрана.
 */
@Composable
fun Mascot(pose: Finn, size: Dp, modifier: Modifier = Modifier, delayMs: Int = 0) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }
    val dark = Ink.palette.isDark
    Box(
        modifier.size(size).graphicsLayer {
            val p = appear.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * rise
            val s = 0.92f + 0.08f * p
            scaleX = s; scaleY = s
            transformOrigin = TransformOrigin(0.5f, 0.95f)
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (dark) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Glow.copy(alpha = 0.16f), Glow.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(this.size.width / 2, this.size.height * 0.5f),
                        radius = this.size.minDimension * 0.5f,
                    ),
                    radius = this.size.minDimension * 0.5f,
                    center = Offset(this.size.width / 2, this.size.height * 0.5f),
                )
            }
            // Тень под лапами: на светлой теме — тёмная, на тёмной — светлая.
            val shadow = if (dark) Glow.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
            val w = this.size.width * 0.5f
            val h = this.size.height * 0.05f
            drawOval(
                Brush.radialGradient(listOf(shadow, Color.Transparent),
                    center = Offset(this.size.width / 2, this.size.height * 0.94f), radius = w / 2),
                topLeft = Offset((this.size.width - w) / 2, this.size.height * 0.94f - h / 2),
                size = Size(w, h),
            )
        }
        AnimatedContent(
            targetState = pose,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + scaleIn(tween(320, easing = Easing.Emphasized), initialScale = 0.94f)) togetherWith
                    fadeOut(tween(140))
            },
            label = "finn",
        ) { p ->
            Image(
                bitmap = ImageBitmap.imageResource(p.res),
                contentDescription = null,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
