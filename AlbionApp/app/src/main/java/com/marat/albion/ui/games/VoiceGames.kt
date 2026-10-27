package com.marat.albion.ui.games

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
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
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
import com.marat.albion.ui.components.AudioButton
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

/** Круглая кнопка воспроизведения с подписью снизу. */
@Composable
internal fun RoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 60.dp,
    enabled: Boolean = true,
    active: Boolean = false,
    slot: Dp = size,
) {
    val interaction = remember { MutableInteractionSource() }
    val on by animateFloatAsState(if (active) 1f else 0f, tween(220), label = "active")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = size + 20.dp)) {
        // Кнопка центрируется в «слоте» высотой с соседний микрофон — подписи стоят на одной линии.
        Box(Modifier.height(slot), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).pressScale(interaction, 0.9f)
                .softShadow(CircleShape, 4.dp)
                .background(lerpColor(Ink.Raised, Ink.Accent, on), CircleShape)
                .border(1.5.dp, if (!enabled) Ink.Line else lerpColor(Ink.LineStrong, Ink.Accent, on), CircleShape)
                .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
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
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val t = rememberInfiniteTransition(label = "mic")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    val amp by animateFloatAsState(if (active) level else 0f, spring(stiffness = 400f), label = "amp")
    val on by animateFloatAsState(if (active) 1f else 0f, tween(240), label = "on")
    val face = lerpColor(Ink.Accent, Ink.ErrorFill, on)
    val lip = lerpColor(Ink.AccentDeep, Ink.ErrorDeep, on)
    val fg = lerpColor(Ink.OnAccent, Ink.OnError, on)
    val ring = Ink.Error
    val lipH = 0.dp
    val sink by animateDpAsState(if (pressed) lipH else 0.dp, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh), label = "sink")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size, size + lipH), contentAlignment = Alignment.TopCenter) {
            if (active) {
                // Кольца расходятся за пределы кнопки и не раздувают раскладку.
                Canvas(Modifier.size(size)) {
                    for (k in 0..2) {
                        val p = (wave + k / 3f) % 1f
                        val r = size.toPx() / 2f * (1f + p * (0.45f + amp * 0.2f))
                        drawCircle(ring.copy(alpha = (1f - p) * 0.5f), radius = r, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Box(
                Modifier.size(size, size + lipH)
                    .graphicsLayer { val s = 1f + amp * 0.08f; scaleX = s; scaleY = s }
                    .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
                    .drawBehind {
                        val d = this.size.width
                        drawCircle(face, radius = d / 2, center = Offset(d / 2, d / 2 + sink.toPx()))
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.size(size).graphicsLayer { translationY = sink.toPx() }, contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = active, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "rec") { rec ->
                        Icon(if (rec) Icons.Rounded.Stop else Icons.Rounded.Mic, if (rec) "Остановить" else "Говорить", tint = fg, modifier = Modifier.size(size * 0.42f))
                    }
                }
            }
        }
        if (label != null) {
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) Ink.Error else Ink.Soft)
        }
    }
}

// Шэдоуинг — в ShadowingGame.kt.

/** Кнопка записи шэдоуинга. */
@Composable
internal fun RecordButton(recording: Boolean, level: Float, onClick: () -> Unit, size: Dp = 84.dp) {
    MicButton(recording, level, onClick, label = if (recording) "Стоп" else "Запись", size = size)
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
                AudioButton(
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
                    micDenied -> Message("Нет доступа к микрофону — разрешите его в настройках телефона.")
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
                    if (!done) GhostButton("Ещё попытка · ${3 - attempts}", onClick = ::listen, icon = Icons.Rounded.Mic)
                    PrimaryButton("Дальше", onClick = {
                        checker.reset()
                        val ok = best >= PASS
                        vm.record(ok, Mistake(card.ipa, lastHeard, card.term))
                        vm.next()
                    }, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
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
        Text("Распознано: «$heard»", style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
        Spacer(Modifier.height(10.dp))
        ThinProgress(score, color = if (good) Ink.Accent else Ink.Gold, track = Ink.Raised, height = 8.dp)
    }
}
