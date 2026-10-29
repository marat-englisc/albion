package com.marat.albion.ui.games

import com.marat.albion.ui.components.AlbionIcons
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.components.softShadow
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.theme.QuietAnimatedContent as AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.audio.SpeechChecker
import com.marat.albion.audio.SpeechState
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.games.AnswerCheck
import com.marat.albion.data.games.Verdict
import androidx.compose.foundation.shape.RoundedCornerShape
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale
import kotlinx.coroutines.flow.first

/** Термин крупно на карточке задания — общий заголовок голосовых игр. */
@Composable
internal fun TermBlock(card: CardEntity, showMeaning: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    TaskCard(centered = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (card.pos.isNotBlank()) Chip(card.pos)
            LevelBadge(card.level)
        }
        Spacer(Modifier.height(12.dp))
        Text(card.term, style = termStyle(card.term), color = Ink.Fg)
        if (card.ipa.isNotBlank() || trailing != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.ipa.isNotBlank()) Text(card.ipa, style = AlbionType.ipa, modifier = Modifier.weight(1f, fill = false))
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
        }
        if (showMeaning && card.meaningRu.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(card.meaningRu, style = MaterialTheme.typography.bodyMedium, color = Ink.Soft, textAlign = TextAlign.Center)
        }
    }
}

/** Framed playback control with a label aligned to the recording control. */
@Composable
internal fun PlaybackAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 60.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    slot: Dp = size,
) {
    val interaction = remember { MutableInteractionSource() }
    val on by animateFloatAsState(if (active) 1f else 0f, tween(if (com.marat.albion.ui.theme.LocalReducedMotion.current) 0 else 220), label = "active")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = size + 20.dp)) {
        // Кнопка центрируется в «слоте» высотой с соседний микрофон — подписи стоят на одной линии.
        Box(Modifier.height(slot), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).pressScale(interaction, 0.9f)
                .background(lerpColor(Ink.Raised, Ink.Accent, on), RoundedCornerShape(3.dp))
                .border(.75.dp, if (!enabled) Ink.Line else lerpColor(Ink.Gold, Ink.Accent, on), RoundedCornerShape(3.dp))
                .clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, label,
                tint = if (!enabled) Ink.Faint else lerpColor(Ink.AccentText, Ink.OnAccent, on),
                modifier = Modifier.size(size * 0.42f),
            )
        }
        }
        Spacer(Modifier.height(8.dp))
        // Подпись не переносится по буквам («Медленн-о»): при крупном шрифте кнопка просто шире.
        Text(
            label, style = MaterialTheme.typography.labelMedium, color = if (enabled) Ink.Soft else Ink.Faint,
            textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
        )
    }
}

/**
 * Кнопка микрофона. Во время записи меняет цвет и показывает уровень голоса.
 */
@Composable
internal fun MicButton(
    active: Boolean,
    level: Float,
    onClick: () -> Unit,
    label: String? = null,
    size: Dp = 84.dp,
    enabled: Boolean = true,
) {
    val reduced = com.marat.albion.ui.theme.LocalReducedMotion.current
    val feedback = com.marat.albion.ui.theme.LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val amp by animateFloatAsState(if (active && !reduced) level.coerceIn(0f, 1f) else 0f, tween(160), label = "voiceLevel")
    val face = if (active) Ink.Accent else Ink.AccentSoft
    val foreground = if (active) Ink.OnAccent else Ink.AccentText
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).pressScale(interaction, 0.985f), contentAlignment = Alignment.Center) {
            if (active) Canvas(Modifier.size(size)) {
                drawRect(Ink.Gold.copy(alpha = .7f), style = Stroke(.65.dp.toPx()))
                if (!reduced) drawLine(Ink.Gold, Offset(7.dp.toPx(), this.size.height - 3.dp.toPx()),
                    Offset(7.dp.toPx() + (this.size.width - 14.dp.toPx()) * amp, this.size.height - 3.dp.toPx()), 2.dp.toPx())
            }
            Box(Modifier.size(size - 12.dp).background(face, RoundedCornerShape(3.dp))
                .border(.75.dp, if (active) Ink.Gold else Ink.Gold.copy(alpha = .7f), RoundedCornerShape(3.dp))
                .clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Button) { feedback.selection(); onClick() },
                contentAlignment = Alignment.Center) {
                Icon(if (active) AlbionIcons.Stop else AlbionIcons.Mic,
                    if (active) "Остановить" else "Говорить", tint = foreground, modifier = Modifier.size(size * 0.36f))
            }
        }
        if (label != null) {
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.Soft)
        }
    }
}

// Шэдоуинг — в ShadowingGame.kt.

/** Кнопка записи шэдоуинга. */
@Composable
internal fun RecordButton(recording: Boolean, level: Float, onClick: () -> Unit, size: Dp = 84.dp, enabled: Boolean = true) {
    MicButton(recording, level, onClick, label = if (recording) "Стоп" else if (enabled) "Запись" else "Подождите", size = size, enabled = enabled)
}

/** Высота кнопки микрофона вместе с кромкой — для выравнивания соседних кнопок. */
internal val MicLip = 0.dp

// ------------------------------------------------------------------ произношение

@Composable
fun PronunciationRound(vm: GameViewModel, card: CardEntity) {
    val c = LocalAppContainer.current
    val context = LocalContext.current
    val checker = remember { SpeechChecker(context.applicationContext) }
    val state by checker.state.collectAsStateWithLifecycle()
    val playing by c.audio.playing.collectAsStateWithLifecycle()
    val deck = Deck.byId(card.deck) ?: Deck.WORDS
    var best by rememberSaveable(card.id) { mutableFloatStateOf(-1f) }
    var attempts by rememberSaveable(card.id) { mutableIntStateOf(0) }
    var lastScore by rememberSaveable(card.id) { mutableFloatStateOf(-1f) }
    var lastHeard by rememberSaveable(card.id) { mutableStateOf("") }
    var micDenied by remember { mutableStateOf(false) }
    val micAccess = rememberMicAccess(onDenied = { micDenied = true })

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) micDenied = false
    }
    DisposableEffect(Unit) { onDispose { checker.release() } }
    LaunchedEffect(card.id) { checker.reset() }

    // Оцениваем результат: лучший из вариантов распознавания.
    LaunchedEffect(state) {
        val s = state
        if (s is SpeechState.Heard) {
            // «drive me up the wall» для «drive someone up the wall» — полное совпадение.
            val scored = s.variants.map { v ->
                v to if (AnswerCheck.checkTerm(v, card.term) == Verdict.Correct) 1f else AnswerCheck.similarity(v, card.term)
            }.maxBy { it.second }
            lastHeard = scored.first
            lastScore = scored.second
            best = maxOf(best, scored.second)
            attempts++
        }
    }

    val listening = state is SpeechState.Listening || state is SpeechState.Processing
    val level = (state as? SpeechState.Listening)?.level ?: 0f

    fun listen() = micAccess { c.audio.stop(); checker.listen() }

    Column(Modifier.fillMaxSize().padding(horizontal = gutter)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TermBlock(card, showMeaning = false) {
                PracticeAudioButton(
                    playing = playing == "term:${card.id}",
                    onClick = { c.audio.play(deck, card.audio, card.term, "term:${card.id}") },
                    size = 40.dp,
                )
            }
            Spacer(Modifier.height(20.dp))

            Spacer(Modifier.height(24.dp))
            MicButton(active = listening, level = level, onClick = { if (listening) checker.stopListening() else listen() })
            Spacer(Modifier.height(28.dp))

            val s = state
            Box(Modifier.fillMaxWidth().heightIn(min = 110.dp), contentAlignment = Alignment.TopCenter) {
                when {
                    micDenied -> Column {
                        Message("Для проверки нужен доступ к микрофону.")
                        com.marat.albion.ui.components.AppSettingsButton()
                    }
                    !checker.isAvailable -> Message("На телефоне нет сервиса распознавания речи. Установите приложение Google и офлайн-пакет английского.")
                    s is SpeechState.Listening -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("Слушаю…", color = Ink.Error)
                        if (s.partial.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("«${s.partial}»", style = MaterialTheme.typography.bodyLarge, color = Ink.Muted)
                        }
                    }
                    s is SpeechState.Processing -> Eyebrow("Распознаю…", color = Ink.Muted)
                    s is SpeechState.Failed -> Message(s.message)
                    lastScore >= 0f -> ScoreView(lastScore, lastHeard)
                    else -> Message("Нажмите на микрофон и произнесите вслух")
                }
            }
        }

        val done = best >= 0.999f || attempts >= 3
        AnimatedContent(targetState = attempts > 0, label = "actions", modifier = Modifier.padding(vertical = 12.dp)) { tried ->
            if (!tried) {
                GhostButton("Пропустить", onClick = {
                    checker.reset()
                    vm.record(false, Mistake(card.ipa, "пропущено", card.term))
                    vm.next()
                })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!done) GhostButton("Ещё попытка · ${3 - attempts}", onClick = ::listen, icon = AlbionIcons.Mic)
                    PrimaryButton("Дальше", onClick = {
                        checker.reset()
                        val ok = best >= PASS
                        vm.record(ok, Mistake(card.ipa, lastHeard, card.term))
                        vm.next()
                    }, trailingIcon = AlbionIcons.ArrowForward)
                }
            }
        }
    }
}

/** Порог «узнано»: небольшие расхождения распознавания прощаются. */
private const val PASS = 0.75f

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
}

@Composable
private fun ScoreView(score: Float, heard: String) {
    val good = score >= PASS
    NoteCard(tone = if (good) NoteTone.Good else NoteTone.Bad) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    score >= 0.999f -> "Отлично — узнано точно"
                    good -> "Почти — узнано с расхождением"
                    else -> "Ещё раз — пока не узнаётся"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (good) Ink.Success else Ink.Error,
                modifier = Modifier.weight(1f),
            )
            Text("${(score * 100).toInt()}%", style = AlbionType.index.copy(color = Ink.Fg))
        }
        Spacer(Modifier.height(8.dp))
        Text("Совпадение распознавания: ${(score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        Text("Распознано: «$heard»", style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
        Spacer(Modifier.height(10.dp))
        Text("Сравниваем распознанный текст со словом; это не фонетическая оценка.",
            style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        ThinProgress(score, color = if (good) Ink.Accent else Ink.Gold, track = Ink.Raised, height = 8.dp)
    }
}
