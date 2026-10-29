package com.marat.albion.ui.study

import com.marat.albion.ui.theme.LocalFeedback
import com.marat.albion.ui.theme.LocalReducedMotion
import com.marat.albion.ui.theme.Brand

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.gutter
import androidx.activity.compose.BackHandler
import com.marat.albion.ui.theme.QuietAnimatedContent as AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.LocalAppContainer
import com.marat.albion.audio.AudioPlayer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.fsrs.Rating
import com.marat.albion.data.settings.Settings
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.StatPill
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
import kotlin.math.PI
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
fun StudyScreen(deck: Deck?, navigator: Navigator, answerLimit: Int? = null) {
    val c = LocalAppContainer.current
    val vm = viewModel(key = "study-${deck?.id ?: "all"}-${answerLimit ?: "full"}") { StudyViewModel(c.study, deck, answerLimit) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val audio = rememberCardAudio(c.audio)
    val feedback = LocalFeedback.current
    val reduced = LocalReducedMotion.current

    val current = state.current
    LaunchedEffect(current?.card?.id, state.revealed) {
        val card = current?.card ?: return@LaunchedEffect
        val d = Deck.byId(card.deck) ?: deck ?: Deck.WORDS
        if ((!state.revealed && settings.autoplayFront) || (state.revealed && settings.autoplayBack)) {
            c.audio.playDelayed(d, card.audio, card.term, "term:${card.id}", delayMs = 180)
        }
    }

    val rate: (Rating) -> Unit = { rating ->
        feedback.selection()
        c.audio.stop()
        vm.answer(rating)
    }

    BackHandler { c.audio.stop(); navigator.back() }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissError,
            containerColor = Ink.Raised,
            title = { Text("Не удалось записать изменения") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("Понятно") } },
        )
    }

    Column(
        Modifier.fillMaxSize().paperSurface()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        TopRow(deck, state, onClose = { c.audio.stop(); navigator.back() }, onUndo = vm::undo)
        ThinProgress(state.progress, modifier = Modifier.padding(horizontal = gutter))

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.marat.albion.ui.components.LoadingState("Готовим карточки")
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
                            (fadeIn(tween(180, delayMillis = 30)) +
                                scaleIn(tween(240, easing = Easing.Emphasized), initialScale = if (reduced) 1f else 0.995f) +
                                slideInVertically(tween(240, easing = Easing.Emphasized)) { if (reduced) 0 else it / 40 }) togetherWith
                                (slideOutHorizontally(tween(220, easing = Easing.EmphasizedAccelerate)) { if (reduced) 0 else dir * it / 7 } +
                                    fadeOut(tween(160)) +
                                    scaleOut(tween(220), targetScale = if (reduced) 1f else 0.995f))
                        },
                        label = "card",
                    ) { sc ->
                        val isCurrent = sc.card.id == current.card.id
                        FlipCard(
                            revealed = if (isCurrent) state.revealed else true,
                            onReveal = { feedback.reveal(); vm.reveal() },
                            front = { CardFront(sc.card, sc.irregular, audio, settings.showIpaOnFront) },
                            back = { CardBack(sc.card, sc.irregular, audio, showRussian = settings.showRussian) },
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    AnimatedContent(
                        targetState = state.revealed,
                        transitionSpec = {
                            (fadeIn(tween(160, delayMillis = 30)) + slideInVertically(tween(200, easing = Easing.Emphasized)) { if (reduced) 0 else it / 12 }) togetherWith
                                (fadeOut(tween(100)) + slideOutVertically(tween(140)) { if (reduced) 0 else it / 12 })
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
private fun TopRow(deck: Deck?, state: StudyState, onClose: () -> Unit, onUndo: () -> Unit) {
    // В общем занятии подписываем колоду текущей карточки: от неё зависит, что вспоминать.
    val title = deck?.title ?: listOfNotNull("Все колоды", state.current?.let { Deck.byId(it.card.deck)?.title }).joinToString(" · ")
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            com.marat.albion.ui.components.RoundIconButton(AlbionIcons.Close, "Закрыть", onClose, spin = 90f)
            Box(Modifier.weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                Text(title.uppercase(), style = AlbionType.eyebrow, color = Ink.Soft, textAlign = TextAlign.Center, maxLines = 2)
            }
            com.marat.albion.ui.components.FeatureHelp("study.ratings", "Как оценить ответ",
                "Снова — не вспомнили.\n\nТрудно — вспомнили с усилием.\n\nХорошо — вспомнили уверенно.\n\nЛегко — вспомнили сразу. Для многозначного слова оцените знание всех значений.", showOnFirstUse = false)
            com.marat.albion.ui.components.RoundIconButton(AlbionIcons.Undo, "Отменить ответ", onUndo, spin = -60f, enabled = state.canUndo)
        }
        if (state.answerLimit != null) {
            Row(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Короткое занятие", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                Text("${state.answered} / ${state.targetAnswers} ответов", style = MaterialTheme.typography.labelMedium, color = Ink.Fg)
            }
        } else Row(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QueueCount(state.newCount + if (state.current?.isNew == true) 1 else 0, "новые", Modifier.weight(1f))
            QueueCount(state.learningCount + if (state.current?.fromLearning == true) 1 else 0, "учу", Modifier.weight(1f))
            QueueCount(
                state.reviewCount + if (state.current?.let { !it.isNew && !it.fromLearning } == true) 1 else 0,
                "повтор", Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QueueCount(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.labelLarge, color = if (value > 0) Ink.Fg else Ink.Faint)
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
                .border(1.dp, Ink.Line.copy(alpha = 1f - k * 0.3f), RoundedCornerShape(6.dp))
                .background(Ink.Raised, RoundedCornerShape(6.dp)),
        )
    }
}

/** Карточка: 3D-переворот по оси Y с лёгким «проседанием» в середине поворота. */
@Composable
private fun FlipCard(
    revealed: Boolean,
    onReveal: () -> Unit,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(if (reduced) 0 else 380, easing = Easing.Standard),
        label = "flip",
    )
    val dip = 1f - 0.008f * sin((rotation / 180f) * PI).toFloat()
    val shape = RoundedCornerShape(6.dp)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = if (reduced) 0f else rotation
                scaleX = dip; scaleY = dip
                cameraDistance = 16f * density
            }
            .clip(shape)
            .clickable(enabled = !revealed, interactionSource = remember { MutableInteractionSource() }, indication = androidx.compose.foundation.LocalIndication.current, onClick = onReveal),
        shape = shape,
        color = Ink.Raised,
        border = BorderStroke(1.dp, Ink.LineStrong),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { front() }
                }
                Text("Нажмите, чтобы перевернуть", Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Ink.Faint)
            } else {
                val scroll = rememberScrollState()
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = if (reduced) 0f else 180f }) {
                    Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 28.dp)) { back() }
                    if (scroll.canScrollForward) Text("Ещё ниже ↓", Modifier.align(Alignment.BottomCenter)
                        .background(Ink.Raised).padding(horizontal = 16.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall, color = Ink.Muted)
                }
            }
        }
    }
}


/** «Хорошо» — основное действие; остальные оценки сохраняют равную область нажатия. */
@Composable
private fun RatingBar(intervals: Map<Rating, String>, onRate: (Rating) -> Unit) {
    val measurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) {
        Rating.entries.maxOf { measurer.measure(it.label, MaterialTheme.typography.labelLarge).size.width }.toDp() + 12.dp
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (labelWidth * 4 + 24.dp <= maxWidth) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Rating.entries.chunked(columns).forEach { ratings ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ratings.forEach { r ->
                        RatingButton(r, Rating.entries.indexOf(r), intervals[r].orEmpty(), onRate, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(r: Rating, index: Int, interval: String, onRate: (Rating) -> Unit, modifier: Modifier = Modifier) {

    val (bg, fg, border) = when (r) {
        Rating.Again -> Triple(Ink.ErrorSoft, Ink.Error, Ink.ErrorSoft)
        Rating.Hard -> Triple(Ink.Surface, Ink.Text, Ink.LineStrong)
        Rating.Good -> Triple(Ink.Accent, Ink.OnAccent, Ink.AccentDeep)
        Rating.Easy -> Triple(Ink.AccentSoft, Ink.AccentText, Ink.AccentSoft)
    }
    val interaction = remember { MutableInteractionSource() }
    val reduced = LocalReducedMotion.current
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) delay(index * 20L)
        appear.animateTo(1f, tween(if (reduced) 0 else 180, easing = Easing.EmphasizedDecelerate))
    }
    Column(
        modifier.fillMaxHeight().heightIn(min = 66.dp)
            .graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 12f }
            .pressScale(interaction, 0.96f)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button) { onRate(r) }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(r.label, style = MaterialTheme.typography.labelLarge, color = fg, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(interval, style = MaterialTheme.typography.labelSmall,
            color = if (r == Rating.Good) Ink.OnAccent else fg.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        Text(when(r) { Rating.Again -> "Не вспомнил"; Rating.Hard -> "С усилием"; Rating.Good -> "Вспомнил"; Rating.Easy -> "Сразу" },
            style = MaterialTheme.typography.bodySmall, color = fg, textAlign = TextAlign.Center)
    }
}

/** Summary of the completed study session. */
@Composable
private fun Finished(state: StudyState, onDone: () -> Unit, onUndo: () -> Unit) {
    val feedback = LocalFeedback.current
    LaunchedEffect(Unit) { if (state.answered > 0) feedback.complete() }
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        com.marat.albion.ui.components.AlbionLogo(size = 56.dp, color = Ink.AccentText, horizonColor = Ink.Gold)
        Spacer(Modifier.height(24.dp))
        Text(if (state.answered == 0) "All caught up." else "Well done.", style = MaterialTheme.typography.displayMedium.copy(fontFamily = Brand), color = Ink.Fg)
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.answered == 0) "Сейчас в этой колоде нечего повторять."
            else if (state.answerLimit != null) "Короткое занятие завершено. Прогресс сохранён — к оставшимся карточкам можно вернуться позже."
            else "Занятие завершено. Следующие карточки FSRS покажет точно в срок.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink.Muted,
            textAlign = TextAlign.Center,
        )
        if (state.answered > 0) {
            Spacer(Modifier.height(28.dp))
            val metrics = listOf(
                Triple(state.answered, "ответов", ""),
                Triple((state.answered - state.again) * 100 / state.answered, "вспомнил", "%"),
                Triple((state.elapsedMs / 60000).toInt().coerceAtLeast(1), "минут", ""),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        metrics.forEach { (value, label, suffix) -> StatPill(value, label, Modifier.fillMaxWidth(), suffix) }
                    }
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.forEach { (value, label, suffix) -> StatPill(value, label, Modifier.weight(1f), suffix) }
                }
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
}

private fun answersWord(n: Int) = when {
    n % 100 in 11..14 -> "ответов"
    n % 10 == 1 -> "ответ"
    n % 10 in 2..4 -> "ответа"
    else -> "ответов"
}

@Composable
private fun Stat(value: Int, label: String, suffix: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedNumber(value, AlbionType.numberSmall, color = Ink.Fg, suffix = suffix)
        Spacer(Modifier.height(2.dp))
        Eyebrow(label)
    }
}

