package com.marat.albion.ui.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.audio.SpeechChecker
import com.marat.albion.audio.SpeechState
import com.marat.albion.audio.VoiceRecorder
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
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Термин крупно — общий заголовок голосовых игр. */
@Composable
internal fun TermBlock(card: CardEntity, showMeaning: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Chip(card.pos)
        Spacer(Modifier.height(22.dp))
        val size = when {
            card.term.length <= 10 -> 48
            card.term.length <= 20 -> 38
            else -> 30
        }
        Text(
            card.term,
            style = TextStyle(fontFamily = Serif, fontSize = size.sp, lineHeight = (size * 1.15f).sp),
            color = Ink.Fg,
            textAlign = TextAlign.Center,
        )
        if (card.ipa.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(card.ipa, style = AlbionType.ipa)
        }
        if (showMeaning && card.meaningRu.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(card.meaningRu, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
        }
    }
}

/** Круглая кнопка с подписью снизу. */
@Composable
internal fun RoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    size: Dp = 64.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size).pressScale(interaction, 0.9f)
                .background(if (filled || active) Ink.Fg else Ink.Bg, CircleShape)
                .border(1.dp, if (!enabled) Ink.Line else if (filled || active) Ink.Fg else Ink.LineStrong, CircleShape)
                .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = when {
                    !enabled -> Ink.Faint
                    filled || active -> Ink.Bg
                    else -> Ink.Fg
                },
                modifier = Modifier.size(size * 0.4f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Eyebrow(label, color = if (enabled) Ink.Soft else Ink.Faint)
    }
}

// Шэдоуинг — в ShadowingGame.kt.

/** Большая кнопка записи: во время записи пульсирует в такт голосу. */
@Composable
internal fun RecordButton(recording: Boolean, level: Float, onClick: () -> Unit) {
    val pulse by animateFloatAsState(if (recording) 1f + level * 0.35f else 1f, spring(stiffness = 500f), label = "pulse")
    val interaction = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            if (recording) {
                Box(Modifier.size(88.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.border(1.dp, Ink.Fg.copy(alpha = 0.5f), CircleShape))
            }
            Box(
                Modifier.size(80.dp).pressScale(interaction, 0.9f)
                    .background(Ink.Fg, CircleShape)
                    .clickable(interaction, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(targetState = recording, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "rec") { rec ->
                    Icon(if (rec) Icons.Rounded.Stop else Icons.Rounded.Mic, if (rec) "Остановить" else "Записать", tint = Ink.Bg, modifier = Modifier.size(34.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Eyebrow(if (recording) "Стоп" else "Запись", color = Ink.Fg)
    }
}

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

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TermBlock(card, showMeaning = false)
            Spacer(Modifier.height(16.dp))
            AudioButton(
                playing = playing == "term:${card.id}",
                onClick = { c.audio.play(deck, card.audio, card.term, "term:${card.id}") },
                size = 40.dp,
                primary = false,
            )
            Spacer(Modifier.height(40.dp))

            MicButton(listening = listening, level = level, onClick = { if (listening) checker.stopListening() else listen() })
            Spacer(Modifier.height(18.dp))

            val s = state
            Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.TopCenter) {
                when {
                    micDenied -> Message("Нет доступа к микрофону — разрешите его в настройках телефона.")
                    !checker.isAvailable -> Message("На телефоне нет сервиса распознавания речи. Установите приложение Google и офлайн-пакет английского.")
                    s is SpeechState.Listening -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow("Слушаю…", color = Ink.Fg)
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
        AnimatedContent(targetState = attempts > 0, label = "actions", modifier = Modifier.padding(vertical = 14.dp)) { tried ->
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(
            when {
                score >= 0.999f -> "Отлично — узнано точно"
                score >= PASS -> "Почти — узнано с расхождением"
                else -> "Ещё раз — пока не узнаётся"
            },
            color = Ink.Fg,
        )
        Spacer(Modifier.height(10.dp))
        Text("Распознано: «$heard»", style = MaterialTheme.typography.bodyLarge, color = Ink.Soft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        // Шкала сходства.
        Box(Modifier.width(180.dp).height(2.dp).background(Ink.Line)) {
            val p by animateFloatAsState(score, tween(700), label = "score")
            Box(Modifier.fillMaxWidth(p).height(2.dp).background(Ink.Fg))
        }
    }
}

/** Микрофон: в режиме прослушивания вокруг расходятся кольца, их размах — от громкости голоса. */
@Composable
private fun MicButton(listening: Boolean, level: Float, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "mic")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    val amp by animateFloatAsState(level, spring(stiffness = 400f), label = "amp")
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        if (listening) {
            Canvas(Modifier.size(150.dp)) {
                for (k in 0..2) {
                    val p = (wave + k / 3f) % 1f
                    val r = size.minDimension / 2f * (0.55f + p * (0.3f + amp * 0.15f))
                    drawCircle(Ink.Fg.copy(alpha = (1f - p) * 0.45f), radius = r, style = Stroke(1.dp.toPx()))
                }
            }
        }
        Box(
            Modifier.size(84.dp).pressScale(interaction, 0.9f)
                .graphicsLayer { val s = 1f + amp * 0.08f; scaleX = s; scaleY = s }
                .background(lerpColor(Ink.Bg, Ink.Fg, if (listening) 1f else 0f), CircleShape)
                .border(1.dp, Ink.Fg, CircleShape)
                .clickable(interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Mic, if (listening) "Остановить" else "Говорить", tint = if (listening) Ink.Bg else Ink.Fg, modifier = Modifier.size(36.dp))
        }
    }
}
