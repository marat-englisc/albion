package com.marat.albion.ui.intro

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.theme.QuietAnimatedVisibility as AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Brand
import com.marat.albion.ui.theme.LocalReducedMotion
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Загрузочный экран. Знак «рисуется»: линия горизонта растёт от центра,
 * ноги шеврона поднимаются к вершине, затем проявляется словесный знак
 * со сжимающейся разрядкой. Если идёт первичная загрузка таблиц —
 * под знаком появляется тонкая линия прогресса.
 */
@Composable
fun IntroScreen(status: ImportStatus, full: Boolean = true, onFinished: () -> Unit) {
    val animate = full && !LocalReducedMotion.current
    val horizon = remember { Animatable(if (animate) 0f else 1f) }
    val chevron = remember { Animatable(if (animate) 0f else 1f) }
    val word = remember { Animatable(if (animate) 0f else 1f) }
    val tagline = remember { Animatable(if (animate) 0f else 1f) }
    var showProgress by remember { mutableStateOf(false) }
    val currentStatus by rememberUpdatedState(status)
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        if (animate) coroutineScope {
            launch { horizon.animateTo(1f, tween(320, easing = Easing.Emphasized)) }
            launch { delay(120); chevron.animateTo(1f, tween(360, easing = Easing.Emphasized)) }
            launch { delay(280); word.animateTo(1f, tween(400, easing = Easing.EmphasizedDecelerate)) }
            launch { delay(400); tagline.animateTo(1f, tween(300, easing = Easing.EmphasizedDecelerate)) }
        }
        delay(if (full) 80 else 100)
        val busy = { s: ImportStatus -> s is ImportStatus.Checking || s is ImportStatus.Importing }
        if (busy(currentStatus)) {
            showProgress = true
            snapshotFlow { currentStatus }.first { !busy(it) }
            delay(80)
        }
        finish()
    }

    BoxWithConstraints(Modifier.fillMaxSize().paperSurface(), contentAlignment = Alignment.Center) {
        val compact = maxHeight < 480.dp || LocalDensity.current.fontScale > 1.3f
        val brandSize = if (compact) 40.sp else 76.sp
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            AlbionLogo(size = if (compact) 48.dp else 86.dp, horizon = horizon.value, chevron = chevron.value, color = Ink.AccentText, horizonColor = Ink.Gold)
            Spacer(Modifier.height(28.dp))
            val w = word.value
            Text(
                "Albion",
                style = TextStyle(
                    fontFamily = Brand,
                    fontSize = brandSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (1.2f - 1.2f * w).sp,
                ),
                color = Ink.Fg,
                modifier = Modifier.graphicsLayer {
                    alpha = w
                    translationY = (1f - w) * 8f
                },
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "THE ENGLISH COMPANION",
                style = AlbionType.eyebrow.copy(letterSpacing = 2.sp),
                color = Ink.Muted,
                modifier = Modifier.graphicsLayer { alpha = tagline.value },
            )
            Spacer(Modifier.height(if (compact) 16.dp else 40.dp))
            Text("A little, every day.", style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Brand, fontSize = 23.sp, fontStyle = FontStyle.Italic),
                color = Ink.Muted, modifier = Modifier.graphicsLayer { alpha = tagline.value })
        }

        }
        AnimatedVisibility(
            visible = showProgress,
            enter = fadeIn(tween(180)),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val s = status
                val progress = if (s is ImportStatus.Importing) (s.index - 0.5f) / s.total else 0.05f
                ThinProgress(progress, modifier = Modifier.width(160.dp), track = Ink.High)
                Text(
                    when (s) {
                        is ImportStatus.Importing -> "Готовлю карточки · ${s.deck.title}"
                        else -> "Проверяю материалы"
                    },
                    style = AlbionType.eyebrow.copy(letterSpacing = 1.5.sp),
                    color = Ink.Faint,
                )
            }
        }
        }
    }
}
