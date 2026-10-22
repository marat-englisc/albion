package com.marat.albion.ui.games

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.marat.albion.data.content.Deck
import com.marat.albion.ui.browse.TextTab
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.FeatureHelp
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.serialization.Serializable

/** Мини-игры раздела «Игры». */
@Serializable
enum class Game(
    val title: String,
    val tagline: String,
    val description: String,
    val decks: List<Deck>,
) {
    TYPE_ANSWER(
        "Ввод ответа",
        "Напишите слово по значению",
        "Видите русское значение и часть речи — напечатайте английское слово. Мелкая опечатка засчитывается как «почти», " +
            "ошибки подсвечиваются по буквам. Подсказка открывает буквы по одной.",
        listOf(Deck.WORDS, Deck.PHRASAL, Deck.IDIOMS, Deck.COLLOCATIONS),
    ),
    IRREGULAR(
        "Неправильные глаголы",
        "Впишите V2 и V3",
        "Дан инфинитив и перевод — впишите Past Simple и Past Participle. Если форм две (abided / abode), " +
            "достаточно любой. Ошибки можно сразу прогнать ещё раз.",
        listOf(Deck.IRREGULAR),
    ),
    SHADOWING(
        "Шэдоуинг",
        "Повторите за диктором — получите разбор",
        "Послушайте диктора (можно замедленно) и повторите — запись остановится сама. Телефон сравнит вашу мелодику, ритм " +
            "и темп с оригиналом, покажет график интонации и подскажет, что поправить. Всё считается на устройстве, записи удаляются после занятия.",
        listOf(Deck.WORDS, Deck.PHRASAL, Deck.IDIOMS, Deck.COLLOCATIONS, Deck.IRREGULAR),
    ),
    PRONUNCIATION(
        "Произношение",
        "Скажите слово — телефон оценит",
        "Произнесите слово вслух: встроенное распознавание речи Android проверит, узнаётся ли оно. " +
            "Без интернета работает, если в настройках Google скачан офлайн-пакет английского языка.",
        listOf(Deck.WORDS, Deck.PHRASAL, Deck.IDIOMS, Deck.COLLOCATIONS),
    ),
    GRAMMAR_CHOICE(
        "Грамматика",
        "Выберите верный вариант",
        "Задания по всем темам учебника — от A1 до C1. Выберите правильный вариант, прочитайте пояснение " +
            "и, если нужно, откройте правило. Задания, в которых вы ошибались, попадаются чаще.",
        emptyList(),
    ),
    GRAMMAR_GAP(
        "Допиши пропуск",
        "Впишите пропущенное в предложение",
        "В предложении пропуск — впишите нужную форму сами, без подсказок-вариантов. В скобках дана " +
            "начальная форма или перевод. Регистр и лишние пробелы не важны.",
        emptyList(),
    ),
    SENTENCE(
        "Собери предложение",
        "Расставьте слова по порядку",
        "Слова примера из карточки перемешаны — касайтесь их по порядку, чтобы собрать предложение. Тренирует порядок " +
            "слов, вопросы и место наречий. Нажмите на слово в ответе, чтобы вернуть его обратно.",
        listOf(Deck.WORDS, Deck.PHRASAL, Deck.IDIOMS, Deck.COLLOCATIONS),
    ),
    TENSES(
        "Тренажёр времён",
        "Какая форма глагола нужна?",
        "Предложения из таблицы неправильных глаголов с пропуском: выберите или впишите V1, V2 или V3. " +
            "После ответа — правило: почему после have нужна третья форма, а после did — первая.",
        listOf(Deck.IRREGULAR),
    );

    val isGrammar get() = this == GRAMMAR_CHOICE || this == GRAMMAR_GAP

    val icon: ImageVector
        get() = when (this) {
            TYPE_ANSWER -> Icons.Outlined.Keyboard
            IRREGULAR -> Icons.Outlined.Timeline
            SHADOWING -> Icons.Outlined.RecordVoiceOver
            PRONUNCIATION -> Icons.Outlined.Mic
            GRAMMAR_CHOICE -> Icons.AutoMirrored.Outlined.Rule
            GRAMMAR_GAP -> Icons.Outlined.EditNote
            SENTENCE -> Icons.Outlined.ViewWeek
            TENSES -> Icons.Outlined.History
        }

    /** Как называть единицу занятия: «10 слов», «10 фраз». */
    val unit: String get() = when (this) {
        SENTENCE -> "фраз"
        TENSES -> "заданий"
        else -> "слов"
    }

    /** Режимы на экране настройки (если у игры их несколько). */
    val modes: List<String> get() = if (this == TENSES) listOf("Выбор формы", "Ввод") else emptyList()
}

/** Экран перед началом: описание, выбор колоды и длины занятия. */
@Composable
fun GameSetup(
    game: Game,
    deck: Deck,
    count: Int,
    onDeck: (Deck) -> Unit,
    onCount: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    counts: List<Int> = listOf(10, 20, 30),
    mode: Int = 0,
    onMode: (Int) -> Unit = {},
    mistakes: Int = 0,
    onlyMistakes: Boolean = false,
    onOnlyMistakes: (Boolean) -> Unit = {},
) {
    val reveal = rememberReveal()
    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        TopBar(onBack = onBack) {
            FeatureHelp("practice.${game.name}", "Как заниматься", game.description +
                "\n\nОшибки сохраняются. Сложные слова встретятся первыми, а после двух верных ответов подряд уйдут из списка ошибок.")
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp),
        ) {
            Column(Modifier.reveal(reveal, 0)) {
                Icon(game.icon, null, tint = Ink.Fg, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(12.dp))
                Eyebrow("Упражнение · %02d".format(game.ordinal + 1))
                Spacer(Modifier.height(8.dp))
                Text(game.title, style = MaterialTheme.typography.headlineLarge, color = Ink.Fg)
                Spacer(Modifier.height(6.dp))
                Text(game.tagline, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
            }
            if (game.decks.size > 1) {
                Column(Modifier.reveal(reveal, 1)) {
                    SectionHeader("Колода", Modifier.padding(top = 8.dp))
                    OptionGrid(game.decks.map { it.title }, game.decks.indexOf(deck)) { onDeck(game.decks[it]) }
                }
            }
            if (game.modes.isNotEmpty()) {
                Column(Modifier.reveal(reveal, 1)) {
                    SectionHeader("Режим", Modifier.padding(top = 20.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        game.modes.forEachIndexed { i, m -> TextTab(m, mode == i) { onMode(i) } }
                    }
                }
            }
            if (mistakes > 0) {
                Column(Modifier.reveal(reveal, 2)) {
                    SectionHeader("Ошибки", Modifier.padding(top = 12.dp), trailing = "%02d".format(mistakes))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        TextTab("Все слова", !onlyMistakes) { onOnlyMistakes(false) }
                        TextTab("Только ошибки", onlyMistakes) { onOnlyMistakes(true) }
                    }
                }
            }
            Column(Modifier.reveal(reveal, 2)) {
                SectionHeader("Длина", Modifier.padding(top = 12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    counts.forEach { n -> TextTab(if (n == Int.MAX_VALUE) "Все" else "$n ${game.unit}", count == n) { onCount(n) } }
                }
            }
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Начать", onClick = onStart, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward, modifier = Modifier.reveal(reveal, 3))
        }
    }
}

/** Варианты в столбик: выбранный отмечен белой чертой слева. */
@Composable
private fun OptionGrid(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            Row(
                Modifier.fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(2.dp).height(20.dp).background(if (isSel) Ink.Fg else Ink.Bg))
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isSel) Ink.Fg else Ink.Faint,
                )
            }
            if (i < options.lastIndex) Hairline()
        }
    }
}

/** Шапка во время игры: закрыть, название, «3 / 10», линия прогресса. */
@Composable
fun GameTopRow(game: Game?, index: Int, total: Int, onClose: () -> Unit, title: String = game?.title.orEmpty()) {
    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Кнопка не принимает фокус: клавиатура по «Готово» не должна перескакивать на неё.
            IconButton(onClick = onClose, modifier = Modifier.focusProperties { canFocus = false }) {
                Icon(Icons.Rounded.Close, "Закрыть", tint = Ink.Fg)
            }
            Eyebrow(title, Modifier.weight(1f), color = Ink.Soft)
            Text("${(index + 1).coerceAtMost(total)} / $total", style = AlbionType.index.copy(color = Ink.Soft), modifier = Modifier.padding(end = 16.dp))
        }
        ThinProgress(if (total == 0) 0f else index.toFloat() / total, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

/** Равные кнопки ответа; высота растёт вместе с системным размером текста. */
@Composable
internal fun GameAnswerActions(
    onCheck: () -> Unit,
    onSecondary: () -> Unit,
    secondaryLabel: String = "Не знаю",
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrimaryButton("Проверить", onCheck, Modifier.weight(1f).fillMaxHeight())
        GhostButton(secondaryLabel, onSecondary, Modifier.weight(1f).fillMaxHeight())
    }
}

data class Mistake(val prompt: String, val answer: String, val correct: String)

/** Итог: кольцо результата прорисовывается, проценты набегают. */
@Composable
fun GameSummary(
    game: Game?,
    correct: Int,
    total: Int,
    mistakes: List<Mistake>,
    onRestart: () -> Unit,
    onRetryMistakes: (() -> Unit)?,
    onDone: () -> Unit,
    scoreLabel: String = "верно",
    title: String = game?.title.orEmpty(),
    restartLabel: String = "Ещё раз",
) {
    val ring = remember { Animatable(0f) }
    val ratio = if (total == 0) 0f else correct.toFloat() / total
    LaunchedEffect(Unit) { ring.animateTo(ratio, tween(1100, easing = Easing.Emphasized)) }
    val reveal = rememberReveal()
    val ringSize = 150.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(
        Modifier.fillMaxSize().background(Ink.Bg).windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp, top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(ringSize).reveal(reveal, 0), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 2.dp.toPx()
                drawCircle(Ink.Line, style = Stroke(w))
                drawArc(Ink.Fg, -90f, 360f * ring.value, false, style = Stroke(w * 1.5f, cap = StrokeCap.Round))
            }
            Column(Modifier.padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber((ratio * 100).toInt(), AlbionType.number, color = Ink.Fg, suffix = "%")
                Text(
                    "$correct ИЗ $total\n${scoreLabel.uppercase()}",
                    style = AlbionType.eyebrow,
                    color = Ink.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            when {
                total == 0 -> "Нет слов для упражнения"
                ratio >= 0.9f -> "Excellent."
                ratio >= 0.7f -> "Well done."
                ratio >= 0.4f -> "Keep going."
                else -> "Practice makes perfect."
            },
            style = MaterialTheme.typography.displaySmall,
            color = Ink.Fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.reveal(reveal, 1),
        )
        Spacer(Modifier.height(6.dp))
        Eyebrow(title, Modifier.reveal(reveal, 2))

        if (mistakes.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().reveal(reveal, 3)) {
                SectionHeader("Ошибки", Modifier.padding(top = 20.dp), trailing = "%02d".format(mistakes.size))
                Text(
                    "Сохранены — в следующем занятии эти слова попадутся первыми.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                mistakes.forEach { m ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(m.prompt, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                        Spacer(Modifier.height(4.dp))
                        Text(m.correct, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                        if (m.answer.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                m.answer,
                                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                color = Ink.Faint,
                            )
                        }
                    }
                    Hairline()
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Column(Modifier.reveal(reveal, 4), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onRetryMistakes != null && mistakes.isNotEmpty()) PrimaryButton("Повторить ошибки", onClick = onRetryMistakes)
            if (onRetryMistakes != null && mistakes.isNotEmpty()) GhostButton(restartLabel, onClick = onRestart)
            else PrimaryButton(restartLabel, onClick = onRestart)
            GhostButton("Готово", onClick = onDone)
        }
    }
}

/** Запрос доступа к микрофону; [run] выполняется сразу, если доступ уже есть. */
@Composable
fun rememberMicAccess(onDenied: () -> Unit = {}): (run: () -> Unit) -> Unit {
    val context = LocalContext.current
    val holder = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) holder[0]?.invoke() else onDenied()
        holder[0] = null
    }
    return { run ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) run()
        else {
            holder[0] = run
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/** «Встряхнуть» элемент при неверном ответе: значение [trigger] меняется — элемент дрожит. */
fun Modifier.shakeOn(trigger: Int): Modifier = composed {
    val offset = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (k in listOf(1f, -0.8f, 0.6f, -0.4f, 0.2f, 0f)) offset.animateTo(k * px, tween(55))
    }
    graphicsLayer { translationX = offset.value }
}
