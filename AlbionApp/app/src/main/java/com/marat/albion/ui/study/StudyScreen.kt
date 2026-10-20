package com.marat.albion.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.LocalAppContainer
import com.marat.albion.audio.AudioPlayer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.settings.Settings
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun rememberCardAudio(player: AudioPlayer): CardAudio {
    val playing by player.playing.collectAsStateWithLifecycle()
    return remember(player, playing) {
        object : CardAudio {
            override val playingKey: String? = playing
            override fun play(deck: Deck, file: String?, text: String, key: String) = player.play(deck, file, text, key)
        }
    }
}

@Composable
fun StudyScreen(deck: Deck, navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel(key = "study-${deck.id}") { StudyViewModel(c.study, deck) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val audio = rememberCardAudio(c.audio)
    val haptic = LocalHapticFeedback.current

    val current = state.current
    LaunchedEffect(current?.card?.id, state.revealed) {
        val card = current?.card ?: return@LaunchedEffect
        val d = Deck.byId(card.deck) ?: deck
        if ((!state.revealed && settings.autoplayFront) || (state.revealed && settings.autoplayBack)) {
            delay(180) // даём карточке появиться
            c.audio.play(d, card.audio, card.term, "term:${card.id}")
        }
    }

    val rate: (Rating) -> Unit = { rating ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        c.audio.stop()
        vm.answer(rating)
    }

    BackHandler { c.audio.stop(); navigator.back() }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissError,
            containerColor = Ink.Elevated,
            title = { Text("Не удалось записать изменения") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("Понятно") } },
        )
    }

    Column(
        Modifier.fillMaxSize().background(Ink.Bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        TopRow(deck, state, onClose = { c.audio.stop(); navigator.back() }, onUndo = vm::undo)
        ThinProgress(state.progress, modifier = Modifier.padding(horizontal = 24.dp))

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AlbionLogo(size = 40.dp, color = Ink.Faint)
            }
            state.finished -> Finished(state, onDone = { navigator.back() }, onUndo = vm::undo)
            current != null -> {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    CardStackShadow(remaining = state.remaining)
                    AnimatedContent(
                        targetState = current,
                        contentKey = { it.card.id + it.isNew },
                        transitionSpec = {
                            val dir = if (state.lastRating == Rating.Again) -1 else 1
                            (fadeIn(tween(260, delayMillis = 90)) +
                                scaleIn(tween(520, easing = Easing.Emphasized), initialScale = 0.92f) +
                                slideInVertically(tween(520, easing = Easing.Emphasized)) { it / 12 }) togetherWith
                                (slideOutHorizontally(tween(420, easing = Easing.EmphasizedAccelerate)) { dir * it } +
                                    fadeOut(tween(300)) +
                                    scaleOut(tween(420), targetScale = 0.9f))
                        },
                        label = "card",
                    ) { sc ->
                        val isCurrent = sc.card.id == current.card.id
                        FlipCard(
                            cardKey = sc.card.id + sc.isNew,
                            revealed = if (isCurrent) state.revealed else true,
                            onReveal = vm::reveal,
                            onSwipe = rate,
                            front = { CardFront(sc.card, sc.irregular, audio, settings.showIpaOnFront) },
                            back = { CardBack(sc.card, sc.irregular, audio, showRussian = settings.showRussian) },
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    AnimatedContent(
                        targetState = state.revealed,
                        transitionSpec = {
                            (fadeIn(tween(260, delayMillis = 120)) + slideInVertically(tween(420, easing = Easing.Emphasized)) { it / 2 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically(tween(200)) { it / 3 })
                        },
                        label = "actions",
                    ) { revealed ->
                        if (!revealed) {
                            GhostButton("Показать ответ", onClick = vm::reveal)
                        } else {
                            RatingBar(current.intervals, rate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRow(deck: Deck, state: StudyState, onClose: () -> Unit, onUndo: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Закрыть", tint = Ink.Fg) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow(deck.title, color = Ink.Soft)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                QueueCount(state.newCount + if (state.current?.isNew == true) 1 else 0, "новые")
                QueueCount(state.learningCount, "учу")
                QueueCount(state.reviewCount + if (state.current?.isNew == false) 1 else 0, "повтор")
            }
        }
        IconButton(onClick = onUndo, enabled = state.canUndo) {
            Icon(Icons.AutoMirrored.Rounded.Undo, "Отменить ответ", tint = if (state.canUndo) Ink.Fg else Ink.Faint)
        }
    }
}

@Composable
private fun QueueCount(value: Int, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$value", style = MaterialTheme.typography.labelLarge, color = if (value > 0) Ink.Fg else Ink.Faint)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.Faint)
    }
}

/** Края карточек «под» текущей — ощущение колоды, тает по мере прохождения. */
@Composable
private fun CardStackShadow(remaining: Int) {
    val layers = remaining.coerceIn(0, 2)
    for (k in layers downTo 1) {
        val inset = (k * 10).dp
        Box(
            Modifier.fillMaxSize()
                .padding(horizontal = inset)
                .offset(y = (k * 8).dp)
                .border(1.dp, Ink.Line.copy(alpha = 1f - k * 0.3f), RoundedCornerShape(10.dp))
                .background(Ink.Raised, RoundedCornerShape(10.dp)),
        )
    }
}

/**
 * Карточка: 3D-переворот по оси Y с лёгким «проседанием» в середине поворота.
 * После переворота её можно смахнуть: влево — «Снова», вправо — «Хорошо».
 */
@Composable
private fun FlipCard(
    cardKey: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onSwipe: (Rating) -> Unit,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(620, easing = Easing.Emphasized),
        label = "flip",
    )
    val dip = 1f - 0.07f * sin((rotation / 180f) * PI).toFloat()
    val drag = remember(cardKey) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(10.dp)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val threshold = width * 0.28f
        // Handle swipes on a stationary container so the card's transforms cannot mirror the drag.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cardKey, revealed) {
                    if (!revealed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val v = drag.value
                                if (abs(v) > threshold) {
                                    drag.animateTo(if (v > 0) width * 1.4f else -width * 1.4f, tween(220))
                                    onSwipe(if (v > 0) Rating.Good else Rating.Again)
                                } else {
                                    drag.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { drag.animateTo(0f, spring()) } },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            scope.launch { drag.snapTo(drag.value + amount) }
                        },
                    )
                },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = drag.value
                        rotationZ = drag.value / width * 12f
                    }
                    .graphicsLayer {
                        rotationY = rotation
                        scaleX = dip; scaleY = dip
                        cameraDistance = 16f * density
                    }
                    .clip(shape)
                    .clickable(enabled = !revealed, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onReveal),
                shape = shape,
                color = Ink.Surface,
                border = BorderStroke(1.dp, Ink.LineStrong),
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (rotation <= 90f) {
                        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { front() }
                        }
                        Eyebrow("Нажмите, чтобы перевернуть", Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), color = Ink.Faint)
                    } else {
                        Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { back() }
                            // Подсказка при смахивании.
                            val p = (abs(drag.value) / threshold).coerceIn(0f, 1f)
                            if (p > 0f) {
                                val label = if (drag.value > 0) "ХОРОШО" else "СНОВА"
                                Text(
                                    label,
                                    style = AlbionType.eyebrow.copy(letterSpacing = 3.sp),
                                    color = if (drag.value > 0) Ink.Bg else Ink.Fg,
                                    modifier = Modifier
                                        .align(if (drag.value > 0) Alignment.TopStart else Alignment.TopEnd)
                                        .padding(18.dp)
                                        .graphicsLayer { alpha = p; rotationZ = if (drag.value > 0) -8f else 8f }
                                        .background(if (drag.value > 0) Ink.Fg else Ink.Bg, RoundedCornerShape(2.dp))
                                        .border(1.dp, Ink.Fg, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Оценки: чем легче ответ, тем светлее кнопка. */
@Composable
private fun RatingBar(intervals: Map<Rating, String>, onRate: (Rating) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rating.entries.forEachIndexed { i, r ->
            val (bg, fg, border) = when (r) {
                Rating.Again -> Triple(Color.Transparent, Ink.Soft, Ink.LineStrong)
                Rating.Hard -> Triple(Ink.Elevated, Ink.Text, Ink.Elevated)
                Rating.Good -> Triple(Ink.Soft, Ink.Bg, Ink.Soft)
                Rating.Easy -> Triple(Ink.Fg, Ink.Bg, Ink.Fg)
            }
            val interaction = remember { MutableInteractionSource() }
            val appear = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(i * 45L)
                appear.animateTo(1f, tween(420, easing = Easing.EmphasizedDecelerate))
            }
            Column(
                Modifier.weight(1f).height(66.dp)
                    .graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 40f }
                    .pressScale(interaction, 0.92f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(4.dp))
                    .clickable(interactionSource = interaction, indication = null) { onRate(r) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(intervals[r].orEmpty(), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text(r.label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** Итог занятия: галочка «прорисовывается», цифры набегают. */
@Composable
private fun Finished(state: StudyState, onDone: () -> Unit, onUndo: () -> Unit) {
    val circle = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        circle.animateTo(1f, tween(700, easing = Easing.Emphasized))
        tick.animateTo(1f, tween(380, easing = Easing.EmphasizedDecelerate))
    }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(84.dp)) {
            val stroke = 2.dp.toPx()
            drawArc(Ink.Fg, -90f, 360f * circle.value, false, style = Stroke(stroke, cap = StrokeCap.Round))
            val path = Path().apply {
                moveTo(size.width * 0.3f, size.height * 0.52f)
                lineTo(size.width * 0.45f, size.height * 0.66f)
                lineTo(size.width * 0.71f, size.height * 0.38f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val part = Path()
            measure.getSegment(0f, measure.length * tick.value, part, true)
            drawPath(part, Ink.Fg, style = Stroke(stroke * 1.4f, cap = StrokeCap.Round))
        }
        Spacer(Modifier.height(28.dp))
        Text("Well done.", style = MaterialTheme.typography.displayMedium, color = Ink.Fg)
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.answered == 0) "Сейчас в этой колоде нечего повторять." else "Занятие завершено. Следующие карточки FSRS покажет точно в срок.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.Muted,
            textAlign = TextAlign.Center,
        )
        if (state.answered > 0) {
            Spacer(Modifier.height(36.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(state.answered, "ответов", "", Modifier.weight(1f))
                Stat((state.answered - state.again) * 100 / state.answered, "вспомнил", "%", Modifier.weight(1f))
                Stat((state.elapsedMs / 60000).toInt().coerceAtLeast(1), "минут", "", Modifier.weight(1f))
            }
        }
        if (state.learningCount > 0) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Ещё ${state.learningCount} — на коротких шагах. Вернитесь через несколько минут.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.Soft,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(40.dp))
        PrimaryButton("Готово", onClick = onDone)
        if (state.canUndo) {
            Spacer(Modifier.height(10.dp))
            GhostButton("Отменить последний ответ", onClick = onUndo)
        }
    }
}

@Composable
private fun Stat(value: Int, label: String, suffix: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedNumber(value, AlbionType.numberSmall, color = Ink.Fg, suffix = suffix)
        Spacer(Modifier.height(2.dp))
        Eyebrow(label)
    }
}

