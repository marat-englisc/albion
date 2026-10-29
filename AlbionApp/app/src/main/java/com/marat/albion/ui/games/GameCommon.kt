package com.marat.albion.ui.games

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.components.HeroCard
import com.marat.albion.ui.components.LocalNarrow
import com.marat.albion.ui.components.RoundIconButton
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.ButtonTone
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.theme.pressScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.marat.albion.ui.components.CatalogueTab
import com.marat.albion.ui.components.FolioDivider
import com.marat.albion.ui.components.romanNumeral
import com.marat.albion.ui.theme.headingFamily
import com.marat.albion.ui.theme.Brand
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.FeatureHelp
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.LocalReducedMotion
import com.marat.albion.ui.theme.LocalFeedback
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
        "Проверьте, узнаётся ли слово",
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
            TYPE_ANSWER -> AlbionIcons.Keyboard
            IRREGULAR -> AlbionIcons.Timeline
            SHADOWING -> AlbionIcons.RecordVoiceOver
            PRONUNCIATION -> AlbionIcons.Mic
            GRAMMAR_CHOICE -> AlbionIcons.Rule
            GRAMMAR_GAP -> AlbionIcons.EditNote
            SENTENCE -> AlbionIcons.ViewWeek
            TENSES -> AlbionIcons.History
        }

    /** Как называть единицу занятия: «10 слов», «10 фраз». */
    val unit: String get() = when (this) {
        SENTENCE -> "предложений"
        IRREGULAR -> "глаголов"
        TENSES, GRAMMAR_CHOICE, GRAMMAR_GAP -> "заданий"
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
    available: Int? = null,
) {
    val reveal = rememberReveal()
    Column(Modifier.fillMaxSize().paperSurface()) {
        TopBar(onBack = onBack) {
            FeatureHelp("practice.${game.name}", "Как заниматься", game.description +
                "\n\nОшибки сохраняются. Сложные слова встретятся первыми, а после двух верных ответов подряд уйдут из списка ошибок.")
        }
        SetupBody(
            start = {
                PrimaryButton("Начать", onClick = onStart, trailingIcon = AlbionIcons.ArrowForward,
                    modifier = Modifier.reveal(reveal, 3))
            },
        ) {
            GameSetupHeader(game, Modifier.reveal(reveal, 0)) {
                if (game.decks.size > 1) {
                    Column(Modifier.reveal(reveal, 1)) {
                        SectionHeader("Колода", Modifier.padding(top = 0.dp))
                        OptionGrid(game.decks.map { it.title }, game.decks.indexOf(deck)) { onDeck(game.decks[it]) }
                    }
                }
                if (game.modes.isNotEmpty()) {
                    Column(Modifier.reveal(reveal, 1)) {
                        SectionHeader("Режим", Modifier.padding(top = 8.dp))
                        ChipFlow {
                            game.modes.forEachIndexed { i, m -> PracticeOption(m, mode == i) { onMode(i) } }
                        }
                    }
                }
                if (mistakes > 0) {
                    Column(Modifier.reveal(reveal, 2)) {
                        SectionHeader("Ошибки", Modifier.padding(top = 4.dp), trailing = "%02d".format(mistakes))
                        ChipFlow {
                            PracticeOption("Все слова", !onlyMistakes) { onOnlyMistakes(false) }
                            PracticeOption("Только ошибки", onlyMistakes) { onOnlyMistakes(true) }
                        }
                    }
                }
                Column(Modifier.reveal(reveal, 2)) {
                    SectionHeader("Длина", Modifier.padding(top = 4.dp))
                    ChipFlow {
                        counts.forEach { n -> PracticeOption(if (n == Int.MAX_VALUE) "Все · ${available ?: "…"}" else "$n ${game.unit}", count == n) { onCount(n) } }
                    }
                    if (count == Int.MAX_VALUE) Text("${available ?: "Все"} ${game.unit} · около ${((available ?: 0) / 4).coerceAtLeast(1)} мин. Можно закончить раньше.",
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                }
            }
        }
    }
}

/**
 * Тело экрана настройки: параметры прокручиваются, кнопка «Начать» закреплена внизу —
 * на маленьком экране её не нужно искать прокруткой.
 */
@Composable
internal fun ColumnScope.SetupBody(start: (@Composable () -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = gutter).padding(bottom = 16.dp),
        content = content,
    )
    if (start != null) {
        com.marat.albion.ui.components.Hairline(color = Ink.Gold.copy(alpha = .65f))
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = gutter).padding(top = 4.dp, bottom = 16.dp)) {
            start()
        }
    } else {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/** Ряд фишек-вариантов, который переносится на новую строку вместо горизонтальной прокрутки. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** Catalogue entries: rectangular choices with an explicit selection mark. */
@Composable
private fun OptionGrid(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        GhostButton(options.getOrNull(selected) ?: "Выберите колоду", { open = true }, icon = AlbionIcons.ExpandMore)
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            options.forEachIndexed { index, label -> androidx.compose.material3.DropdownMenuItem(
                text = { Text(label) }, modifier = Modifier.semantics { this.selected = index == selected },
                onClick = { onSelect(index); open = false }) }
        }
    }
}

/** Шапка во время игры: закрыть, название, «3 / 10», линия прогресса. */
@Composable
fun GameTopRow(game: Game?, index: Int, total: Int, onClose: () -> Unit, title: String = game?.title.orEmpty()) {
    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = gutter, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Кнопка не принимает фокус: клавиатура по «Готово» не должна перескакивать на неё.
            PracticeIconButton(AlbionIcons.Close, "Закрыть", onClose, Modifier)
            Spacer(Modifier.width(12.dp))
            Eyebrow(title, Modifier.weight(1f), color = Ink.Soft, maxLines = 1)
            Spacer(Modifier.width(10.dp))
            // Счётчик — маленькая «фишка», как счётчики секций.
            Text(
                "${(index + 1).coerceAtMost(total)} / $total",
                style = AlbionType.index.copy(color = Ink.Soft),
                maxLines = 1,
                modifier = Modifier.border(.65.dp, Ink.LineStrong, RoundedCornerShape(2.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        ThinProgress(if (total == 0) 0f else index.toFloat() / total, modifier = Modifier.padding(horizontal = gutter), color = Ink.GoldText, height = 1.dp)
    }
}

/** Равные кнопки ответа; при крупном системном шрифте встают друг под другом. */
@Composable
internal fun GameAnswerActions(
    onCheck: () -> Unit,
    onSecondary: () -> Unit,
    secondaryLabel: String = "Не знаю",
) {
    ButtonPair(
        listOf("Проверить", secondaryLabel),
        first = { m -> PrimaryButton("Проверить", onCheck, m) },
        second = { m -> GhostButton(secondaryLabel, onSecondary, m, contentPadding = PairPadding) },
    )
}

private val PairPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)

/**
 * Две кнопки рядом, если подписи помещаются в половину ширины целиком; иначе — друг под другом.
 * Так слово никогда не рвётся по буквам («Проверит-ь») при крупном шрифте и на узком экране.
 */
@Composable
internal fun ButtonPair(
    labels: List<String>,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    val style = MaterialTheme.typography.titleMedium
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = LocalDensity.current
    val widest = labels.maxOf { measurer.measure(it, style, softWrap = false).size.width }
    // Поля основной кнопки — по 20 dp с каждой стороны, плюс запас на округление.
    val need = with(density) { widest.toDp() } + 48.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if ((maxWidth - 10.dp) / 2 >= need) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.weight(1f).fillMaxHeight())
                second(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier)
                second(Modifier)
            }
        }
    }
}

data class Mistake(val prompt: String, val answer: String, val correct: String)

/** Editorial exercise introduction and configuration. */
@Composable
internal fun GameSetupHeader(game: Game, modifier: Modifier = Modifier, options: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(game.title, style = MaterialTheme.typography.headlineLarge.copy(fontFamily = headingFamily(game.title)), color = Ink.Fg)
        Text(game.editionTitle, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Brand), color = Ink.GoldText)
        Spacer(Modifier.height(8.dp))
        Text(game.tagline, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), content = options)
    }
}

/** Итог: кольцо результата прорисовывается, проценты набегают. */
@Composable
fun GameSummary(
    game: Game?, correct: Int, total: Int, mistakes: List<Mistake>,
    onRestart: () -> Unit, onRetryMistakes: (() -> Unit)?, onDone: () -> Unit,
    scoreLabel: String = "верно", title: String = game?.title.orEmpty(), restartLabel: String = "Ещё раз",
) {
    val feedback = LocalFeedback.current
    LaunchedEffect(Unit) { if (total > 0) feedback.complete() }
    Column(Modifier.fillMaxSize().paperSurface().windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = gutter)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp)) {
            if (total == 0) com.marat.albion.ui.components.EmptyState("Нет заданий для этих параметров", "Измените колоду, уровень или режим упражнения.")
            else {
                Eyebrow("Итоги практики", color = Ink.GoldText)
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink.Fg)
                Spacer(Modifier.height(16.dp))
                Text("${correct * 100 / total}%", style = AlbionType.number, color = Ink.Fg)
                Text("$correct из $total · $scoreLabel", style = MaterialTheme.typography.bodyLarge, color = Ink.Muted)
                ThinProgress(correct.toFloat() / total, Modifier.padding(top = 12.dp), color = Ink.Accent)
                Spacer(Modifier.height(16.dp))
                Text(when { correct == total -> "Excellent."; correct * 10 >= total * 7 -> "Well done."; else -> "Keep going." },
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Brand), color = Ink.Fg)
                if (mistakes.isNotEmpty()) {
                    SectionHeader("Ошибки", trailing = mistakes.size.toString())
                    Text("Сохранены для следующей практики.", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    mistakes.take(3).forEach { SummaryMistake(it) }
                    if (mistakes.size > 3) com.marat.albion.ui.components.Disclosure("Ещё ошибок: ${mistakes.size - 3}") {
                        mistakes.drop(3).forEach { SummaryMistake(it) }
                    }
                }
            }
        }
        if (total == 0) PrimaryButton("Другие упражнения", onDone, Modifier.padding(bottom = 16.dp))
        else {
            if (mistakes.isNotEmpty() && onRetryMistakes != null) ButtonPair(listOf("Повторить ошибки", "Готово"),
                first = { PrimaryButton("Повторить ошибки", onRetryMistakes, it) },
                second = { GhostButton("Готово", onDone, it) })
            else PrimaryButton("Готово", onDone)
            androidx.compose.material3.TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text(restartLabel) }
        }
    }
}

@Composable
private fun SummaryMistake(m: Mistake) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (m.prompt.isNotBlank()) Text(m.prompt, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        Text(m.correct, style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
        if (m.answer.isNotBlank()) Text("Ваш ответ: ${m.answer}", style = MaterialTheme.typography.bodyMedium, color = Ink.Error)
        Hairline(Modifier.padding(top = 10.dp))
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
    val reduced = LocalReducedMotion.current
    val offset = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 3.dp.toPx() }
    LaunchedEffect(trigger) {
        if (trigger == 0 || reduced) return@LaunchedEffect
        for (k in listOf(1f, -0.5f, 0f)) offset.animateTo(k * px, tween(55))
    }
    graphicsLayer { translationX = offset.value }
}

// ------------------------------------------------------------------ общие элементы раунда

/**
 * Нейтральная карточка задания. Поля уменьшаются на узком экране.
 */
@Composable
internal fun TaskCard(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pad = if (LocalNarrow.current) 16.dp else 20.dp
    PracticeSheet(modifier.fillMaxWidth(), padding = pad) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            content = content,
        )
    }
}

/** Тон плашки разбора. */
internal enum class NoteTone { Neutral, Good, Bad }

/** Плашка разбора после ответа: правило, пояснение, верный ответ. Мягкая заливка без жёстких рамок. */
@Composable
internal fun NoteCard(
    modifier: Modifier = Modifier,
    tone: NoteTone = NoteTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(3.dp)
    val bg = when (tone) {
        NoteTone.Neutral -> Ink.Surface
        NoteTone.Good -> Ink.SuccessSoft
        NoteTone.Bad -> Ink.ErrorSoft
    }
    Column(
        modifier.fillMaxWidth().clip(shape).background(bg)
            .border(.65.dp, if (tone == NoteTone.Neutral) Ink.Line else if (tone == NoteTone.Good) Ink.Success.copy(alpha = .4f) else Ink.Error.copy(alpha = .4f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/** Текст задания антиквой: на узком экране чуть мельче, чтобы предложение не превращалось в столбик. */
internal val promptStyle: TextStyle
    @Composable get() = if (LocalNarrow.current) TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFamily = com.marat.albion.ui.theme.Sans, fontSize = 22.sp, lineHeight = 31.sp)
    else TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFamily = com.marat.albion.ui.theme.Sans, fontSize = 26.sp, lineHeight = 36.sp)

/** Крупный термин (слово, ответ): размер зависит от длины и ширины экрана. */
@Composable
internal fun termStyle(text: String, max: Int = 48): TextStyle {
    val size = when {
        text.length <= 10 -> max
        text.length <= 20 -> max * 4 / 5
        else -> max * 5 / 8
    }.let { if (LocalNarrow.current) it * 5 / 6 else it }
    return TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFamily = com.marat.albion.ui.theme.Sans, fontSize = size.sp, lineHeight = (size * 1.18f).sp, textAlign = TextAlign.Center)
}
