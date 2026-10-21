package com.marat.albion.ui.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif
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
    var hasTake by remember(card.id) { mutableStateOf(false) }
    var analyzing by remember(card.id) { mutableStateOf(false) }
    var report by remember(card.id) { mutableStateOf<ShadowReport?>(null) }
    var unclear by remember(card.id) { mutableStateOf(false) }
    var best by remember(card.id) { mutableStateOf<Int?>(null) }
    var takes by remember(card.id) { mutableStateOf(0) }
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

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TermBlock(card)
            Spacer(Modifier.height(30.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                RoundAction(Icons.Rounded.PlayArrow, "Диктор", { playOriginal() }, active = playing == "orig:${card.id}")
                RecordButton(recording = recording, level = level, onClick = ::toggleRecord)
                RoundAction(
                    Icons.Rounded.SlowMotionVideo, "Медленно",
                    { playOriginal(0.7f, "slow:${card.id}") },
                    enabled = card.audio != null, active = playing == "slow:${card.id}",
                )
            }
            Spacer(Modifier.height(22.dp))

            // Волна: живая во время записи.
            AnimatedVisibility(recording || (!hasTake && report == null)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        wave.forEach { v ->
                            val h by animateFloatAsState(if (recording) v.coerceAtLeast(0.06f) else 0.06f, spring(stiffness = 600f), label = "bar")
                            Box(Modifier.weight(1f).fillMaxHeight(h).background(if (recording) Ink.Fg else Ink.LineStrong, RoundedCornerShape(1.dp)))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Eyebrow(
                when {
                    micDenied -> "Нет доступа к микрофону"
                    recording -> "Говорите — запись остановится, когда вы замолчите"
                    analyzing -> "Сравниваю с диктором…"
                    unclear -> "Не удалось разобрать запись — говорите громче и ближе к телефону"
                    report != null -> "Попытка $takes" + (best?.takeIf { takes > 1 }?.let { " · лучшая $it%" } ?: "")
                    hasTake && original == null -> "Для этой карточки нет записи диктора — сравните на слух"
                    else -> "1 · Послушайте   2 · Повторите   3 · Сравните"
                },
                color = if (recording) Ink.Fg else Ink.Muted,
            )

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
            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun PlaybackActions(onCompare: () -> Unit, onPlayMine: () -> Unit) {
    val compareLabel = "Диктор → я"
    val mineLabel = "Моя запись"
    val style = MaterialTheme.typography.titleMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = maxOf(
        measurer.measure(compareLabel, style, softWrap = false).size.width,
        measurer.measure(mineLabel, style, softWrap = false).size.width,
    )
    val requiredWidth = with(density) { labelWidth.toDp() } + 24.dp
    val padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)

    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        // При крупном системном шрифте даём каждой подписи всю ширину экрана.
        if ((maxWidth - 10.dp) / 2 >= requiredWidth) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(compareLabel, onCompare, Modifier.weight(1f), contentPadding = padding)
                GhostButton(mineLabel, onPlayMine, Modifier.weight(1f), contentPadding = padding)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(compareLabel, onCompare, contentPadding = padding)
                GhostButton(mineLabel, onPlayMine, contentPadding = padding)
            }
        }
    }
}

/** Разбор попытки: общий процент, три шкалы, график мелодики и советы. */
@Composable
private fun ReportCard(r: ShadowReport) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp).border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedNumber(r.overall, AlbionType.number, color = Ink.Fg, suffix = "%")
            Spacer(Modifier.width(14.dp))
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    when {
                        r.overall >= 85 -> "Очень близко"
                        r.overall >= 70 -> "Хорошо"
                        r.overall >= PASS -> "Неплохо"
                        else -> "Ещё попытка"
                    },
                    style = TextStyle(fontFamily = Serif, fontSize = 22.sp),
                    color = Ink.Fg,
                )
                Eyebrow("сходство с диктором")
            }
        }
        Spacer(Modifier.height(18.dp))
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

        if (r.originalPitch.count { !it.isNaN() } >= 5) {
            Spacer(Modifier.height(16.dp))
            Eyebrow("Мелодика")
            Spacer(Modifier.height(8.dp))
            ContourChart(r)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Legend(dashed = false, label = "диктор")
                Legend(dashed = true, label = "вы")
            }
        }

        if (r.tips.isNotEmpty() || r.overall >= 85) {
            Spacer(Modifier.height(16.dp))
            Hairline()
            Spacer(Modifier.height(14.dp))
            val tips = r.tips.ifEmpty { listOf("Мелодика, ритм и темп совпадают с диктором. Попробуйте следующее слово.") }
            tips.forEach { tip ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("—", color = Ink.Faint, modifier = Modifier.width(18.dp))
                    Text(tip, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                }
            }
        }
    }
}

@Composable
private fun Scale(label: String, value: Int, note: String?) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(value) { fill.animateTo(value / 100f, tween(700)) }
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Text, modifier = Modifier.weight(1f))
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Ink.Muted); Spacer(Modifier.width(10.dp)) }
            Text("$value%", style = AlbionType.index.copy(color = Ink.Fg))
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Ink.Line)) {
            Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().background(Ink.Fg))
        }
    }
}

/** Мелодика: диктор — сплошная линия, вы — пунктир; фон — громкость диктора. */
@Composable
private fun ContourChart(r: ShadowReport) {
    val fg = Ink.Fg
    val muted = Ink.Muted
    val line = Ink.Line
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val n = r.originalPitch.size
        if (n < 2) return@Canvas
        val dx = size.width / (n - 1)
        val all = (r.originalPitch.toList() + r.minePitch.toList()).filter { !it.isNaN() }
        val lo = minOf(-4f, all.minOrNull() ?: -4f)
        val hi = maxOf(4f, all.maxOrNull() ?: 4f)
        fun y(semi: Float) = size.height * (1f - (semi - lo) / (hi - lo)) * 0.86f + size.height * 0.07f

        // Громкость диктора — тонкие столбики снизу.
        val barW = maxOf(1f, dx * 0.6f)
        for (i in 0 until n) {
            val h = r.originalEnergy[i] * size.height * 0.35f
            drawLine(line, Offset(i * dx, size.height), Offset(i * dx, size.height - h), strokeWidth = barW)
        }

        fun contour(values: FloatArray): Path {
            val p = Path()
            var open = false
            for (i in 0 until n) {
                val v = values[i]
                if (v.isNaN()) { open = false; continue }
                if (!open) { p.moveTo(i * dx, y(v)); open = true } else p.lineTo(i * dx, y(v))
            }
            return p
        }
        drawPath(contour(r.originalPitch), fg, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            contour(r.minePitch), muted,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
    }
}

@Composable
private fun Legend(dashed: Boolean, label: String) {
    val fg = Ink.Fg
    val muted = Ink.Muted
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 22.dp, height = 8.dp)) {
            drawLine(
                if (dashed) muted else fg, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
    }
}
