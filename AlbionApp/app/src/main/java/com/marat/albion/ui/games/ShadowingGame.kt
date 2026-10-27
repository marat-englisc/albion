package com.marat.albion.ui.games

import com.marat.albion.ui.components.AlbionIcons
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import com.marat.albion.ui.components.LocalNarrow
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.softShadow
import com.marat.albion.ui.components.gutter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.audio.AudioDecoder
import com.marat.albion.audio.VoiceRecorder
import com.marat.albion.data.audio.Prosody
import com.marat.albion.data.audio.ProsodyAnalyzer
import com.marat.albion.data.audio.ShadowReport
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.romanNumeral
import com.marat.albion.ui.theme.Brand
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Порог «похоже» для итога игры. */
private const val PASS = 60

/**
 * Шэдоуинг: послушать диктора (можно медленно), повторить — запись остановится сама,
 * когда вы замолчите, — и получить разбор: интонация, ритм, темп, график мелодики и советы.
 * Всё считается на телефоне; записи удаляются после занятия.
 */
@Composable
fun ShadowingRound(vm: GameViewModel, card: CardEntity) {
    val c = LocalAppContainer.current
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    val recording by recorder.recording.collectAsStateWithLifecycle()
    val level by recorder.level.collectAsStateWithLifecycle()
    val playing by c.audio.playing.collectAsStateWithLifecycle()
    val deck = Deck.byId(card.deck) ?: Deck.WORDS
    val file = remember(card.id) { recorder.fileFor(card.id) }
    val scope = rememberCoroutineScope()
    var micDenied by remember { mutableStateOf(false) }
    val micAccess = rememberMicAccess(onDenied = { micDenied = true })

    var original by remember(card.id) { mutableStateOf<Prosody?>(null) }
    var originalReady by remember(card.id) { mutableStateOf(false) }
    var hasTake by rememberSaveable(card.id) { mutableStateOf(false) }
    var analyzing by remember(card.id) { mutableStateOf(false) }
    var report by remember(card.id) { mutableStateOf<ShadowReport?>(null) }
    var unclear by remember(card.id) { mutableStateOf(false) }
    var best by rememberSaveable(card.id) { mutableStateOf<Int?>(null) }
    var takes by rememberSaveable(card.id) { mutableStateOf(0) }
    // Скользящее окно уровней для «волны» во время записи.
    val wave = remember(card.id) { mutableStateListOf<Float>().apply { repeat(32) { add(0f) } } }

    DisposableEffect(Unit) { onDispose { recorder.clear() } }
    LaunchedEffect(card.id) {
        c.audio.playDelayed(deck, card.audio, card.term, "orig:${card.id}", delayMs = 350)
    }
    // Мелодика диктора — один раз на карточку, в фоне.
    LaunchedEffect(card.id) {
        original = card.audio?.let { name ->
            withContext(Dispatchers.Default) { AudioDecoder.decodeCardAudio(context, deck, name)?.let(ProsodyAnalyzer::analyze) }
        }
        originalReady = true
    }
    LaunchedEffect(level, recording) {
        if (recording) { wave.removeAt(0); wave.add(level) }
    }

    fun playOriginal(rate: Float = 1f, key: String = "orig:${card.id}") = c.audio.play(deck, card.audio, card.term, key, rate)
    fun playMine() = c.audio.playFile(file, "mine:${card.id}")

    fun onTake() {
        val samples = recorder.samples ?: return
        hasTake = samples.isNotEmpty() && file.exists()
        if (!hasTake) return
        takes++
        val orig = original
        if (orig == null) { playMine(); return }
        analyzing = true
        scope.launch {
            val mine = withContext(Dispatchers.Default) { ProsodyAnalyzer.analyze(samples) }
            val r = mine?.let { withContext(Dispatchers.Default) { ProsodyAnalyzer.compare(orig, it) } }
            unclear = r == null
            report = r
            r?.let { best = maxOf(best ?: 0, it.overall) }
            analyzing = false
        }
    }

    fun toggleRecord() {
        if (recording) { recorder.stop(); return }
        micAccess {
            c.audio.stop()
            wave.indices.forEach { wave[it] = 0f }
            report = null
            unclear = false
            if (!recorder.start(file, onStopped = ::onTake)) unclear = true
        }
    }

    fun compare() {
        scope.launch {
            playOriginal()
            c.audio.playFileAfterCurrent(file, "mine:${card.id}")
        }
    }

    fun next() {
        recorder.stop(); c.audio.stop()
        val score = best
        val ok = when {
            score != null -> score >= PASS
            else -> hasTake && original == null // сравнивать не с чем — засчитываем саму запись
        }
        vm.record(ok, if (ok) null else Mistake(card.meaningRu, score?.let { "$it%" } ?: "нет записи", card.term))
        vm.next()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = gutter)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TermBlock(card)

            val micSize = if (LocalNarrow.current) 76.dp else 84.dp
            FlowRow(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlaybackAction(AlbionIcons.PlayArrow, "Диктор", { playOriginal() }, active = playing == "orig:${card.id}", slot = micSize + MicLip)
                RecordButton(recording = recording, level = level, onClick = ::toggleRecord, size = micSize)
                PlaybackAction(
                    AlbionIcons.SlowMotionVideo, "Медленно",
                    { playOriginal(0.7f, "slow:${card.id}") },
                    enabled = card.audio != null, active = playing == "slow:${card.id}", slot = micSize + MicLip,
                )
            }

            // Волна: живая во время записи — красная, как кнопка записи.
            AnimatedVisibility(recording || (!hasTake && report == null)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        wave.forEach { v ->
                            val h by animateFloatAsState(if (recording) v.coerceAtLeast(0.1f) else 0.1f, tween(160), label = "bar")
                            Box(Modifier.weight(1f).fillMaxHeight(h).background(if (recording) Ink.Error else Ink.LineStrong, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            when {
                micDenied -> ShadowStatus("Нет доступа к микрофону — разрешите его в настройках телефона.", warm = true)
                recording -> ShadowStatus("Говорите — запись остановится, когда вы замолчите", warm = true)
                analyzing -> ShadowStatus("Сравниваю с диктором…")
                unclear -> ShadowStatus("Не удалось разобрать запись — говорите громче и ближе к телефону", warm = true)
                report != null -> ShadowStatus("Попытка $takes" + (best?.takeIf { takes > 1 }?.let { " · лучшая $it%" } ?: ""))
                hasTake && original == null -> ShadowStatus("Для этой карточки нет записи диктора — сравните на слух")
                else -> ShadowSteps()
            }

            AnimatedVisibility(report != null && !recording, enter = fadeIn(tween(320)) + expandVertically()) {
                report?.let { ReportCard(it) }
            }

            AnimatedVisibility(hasTake && !recording, enter = fadeIn()) {
                PlaybackActions(onCompare = ::compare, onPlayMine = ::playMine)
            }
        }
        PrimaryButton(
            "Дальше",
            onClick = ::next,
            trailingIcon = AlbionIcons.ArrowForward,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun PlaybackActions(onCompare: () -> Unit, onPlayMine: () -> Unit) {
    val compareLabel = "Диктор → я"
    val mineLabel = "Моя запись"
    val padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
    Box(Modifier.padding(top = 14.dp)) {
        ButtonPair(
            listOf(compareLabel, mineLabel),
            first = { m -> GhostButton(compareLabel, onCompare, m, contentPadding = padding) },
            second = { m -> GhostButton(mineLabel, onPlayMine, m, contentPadding = padding) },
        )
    }
}

/** Подсказка-статус под кнопками: обычный текст, красный — когда идёт запись или что-то не так. */
@Composable
private fun ShadowStatus(text: String, warm: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (warm) Ink.Error else Ink.Soft,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

/** Три шага занятия — пронумерованные «фишки», переносятся на узком экране. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShadowSteps() {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Послушайте", "Повторите", "Сравните").forEachIndexed { i, step ->
            Row(
                Modifier.border(.65.dp, Ink.Line, RoundedCornerShape(2.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    Text(romanNumeral(i + 1), style = MaterialTheme.typography.labelMedium.copy(fontFamily = Brand), color = Ink.GoldText)
                }
                Spacer(Modifier.width(6.dp))
                Text(step, style = MaterialTheme.typography.labelMedium, color = Ink.Soft)
            }
        }
    }
}

/** Разбор попытки: общий процент, три шкалы, график мелодики и советы — на мягкой карточке. */
@Composable
internal fun ReportCard(r: ShadowReport) {
    val good = r.overall >= PASS
    val shape = RoundedCornerShape(3.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp)
            .background(Ink.Raised, shape).border(1.dp, Ink.Line, shape)
            .padding(if (LocalNarrow.current) 16.dp else 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Result label uses the same square corners as the exercise sheet.
            Box(
                Modifier.background(if (good) Ink.GoldSoft else Ink.ErrorSoft, RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                AnimatedNumber(r.overall, AlbionType.numberSmall, color = if (good) Ink.AccentText else Ink.Error, suffix = "%")
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        r.overall >= 85 -> "Очень близко"
                        r.overall >= 70 -> "Хорошо"
                        r.overall >= PASS -> "Неплохо"
                        else -> "Ещё попытка"
                    },
                    style = TextStyle(fontFamily = com.marat.albion.ui.theme.Sans, fontSize = 22.sp, lineHeight = 26.sp),
                    color = Ink.Fg,
                )
                Text("сходство с диктором", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        r.intonation?.let { Scale("Интонация", it, null) }
        Scale("Ритм", r.rhythm, null)
        Scale(
            "Темп", r.tempo,
            when {
                r.tempoRatio > 1.12f -> "медленнее ×${"%.1f".format(r.tempoRatio)}"
                r.tempoRatio < 0.88f -> "быстрее ×${"%.1f".format(1 / r.tempoRatio)}"
                else -> "как у диктора"
            },
        )

        Spacer(Modifier.height(14.dp))
        Eyebrow("Мелодика", color = Ink.Soft)
        Spacer(Modifier.height(8.dp))
        MelodyChart(r)

        if (r.tips.isNotEmpty() || r.overall >= 85) {
            Spacer(Modifier.height(14.dp))
            val tips = r.tips.ifEmpty { listOf("Мелодика, ритм и темп совпадают с диктором. Попробуйте следующее слово.") }
            NoteCard(tone = NoteTone.Neutral) {
                Eyebrow("Советы", color = Ink.AccentText)
                Spacer(Modifier.height(4.dp))
                tips.forEach { tip ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Box(Modifier.padding(top = 9.dp, end = 10.dp).size(6.dp).background(Ink.Accent, CircleShape))
                        Text(tip, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                    }
                }
            }
        }
    }
}

/** Result scales use crimson and muted gold, with explicit numeric values. */
@Composable
private fun Scale(label: String, value: Int, note: String?) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Пояснение идёт сразу за подписью и переносится целыми словами при крупном шрифте.
            Text(
                buildAnnotatedString {
                    append(label)
                    if (note != null) withStyle(SpanStyle(color = Ink.Muted, fontSize = MaterialTheme.typography.bodySmall.fontSize)) { append(" · $note") }
                },
                style = MaterialTheme.typography.bodyMedium, color = Ink.Text, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text("$value%", style = AlbionType.index.copy(color = Ink.Fg))
        }
        Spacer(Modifier.height(6.dp))
        ThinProgress(value / 100f, color = if (value >= PASS) Ink.Accent else Ink.GoldText, height = 3.dp)
    }
}
