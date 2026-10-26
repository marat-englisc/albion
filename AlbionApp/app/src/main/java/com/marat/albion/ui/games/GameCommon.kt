package com.marat.albion.ui.games

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.components.HeroCard
import com.marat.albion.ui.components.LocalNarrow
import com.marat.albion.ui.components.RoundIconButton
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.peekingFinn
import com.marat.albion.ui.components.Mascot
import com.marat.albion.ui.components.ButtonTone
import com.marat.albion.ui.components.ConfettiBurst
import com.marat.albion.ui.components.FinnLedge
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.theme.pressScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.zIndex
import com.marat.albion.ui.components.Finn
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
                "\n\nОшибки сохраняются. Сложные слова встретятся первыми, а после двух верных ответов подряд уйдут из списка ошибок.",
                mascot = game.mascot)
        }
        SetupBody(
            start = {
                PrimaryButton("Начать", onClick = onStart, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
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
                            game.modes.forEachIndexed { i, m -> TextTab(m, mode == i) { onMode(i) } }
                        }
                    }
                }
                if (mistakes > 0) {
                    Column(Modifier.reveal(reveal, 2)) {
                        SectionHeader("Ошибки", Modifier.padding(top = 4.dp), trailing = "%02d".format(mistakes))
                        ChipFlow {
                            TextTab("Все слова", !onlyMistakes) { onOnlyMistakes(false) }
                            TextTab("Только ошибки", onlyMistakes) { onOnlyMistakes(true) }
                        }
                    }
                }
                Column(Modifier.reveal(reveal, 2)) {
                    SectionHeader("Длина", Modifier.padding(top = 4.dp))
                    ChipFlow {
                        counts.forEach { n -> TextTab(if (n == Int.MAX_VALUE) "Все" else "$n ${game.unit}", count == n) { onCount(n) } }
                    }
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

/** Варианты в столбик: выбранный — на мягкой зелёной плашке с отметкой. */
@Composable
private fun OptionGrid(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            val t by animateFloatAsState(if (isSel) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "opt")
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth()
                    .pressScale(interaction, 0.97f)
                    .background(lerpColor(Ink.Surface, Ink.AccentSoft, t), RoundedCornerShape(16.dp))
                    .clickable(interaction, indication = null) { onSelect(i) }
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(22.dp).border(2.dp, lerpColor(Ink.LineStrong, Ink.Accent, t), CircleShape).padding(5.dp)
                        .graphicsLayer { scaleX = t; scaleY = t }.background(Ink.Accent, CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (LocalNarrow.current) 18.sp else 20.sp,
                        lineHeight = if (LocalNarrow.current) 24.sp else 26.sp,
                    ),
                    color = lerpColor(Ink.Soft, Ink.Fg, t),
                )
            }
        }
    }
}

/** Шапка во время игры: закрыть, название, «3 / 10», линия прогресса. */
@Composable
fun GameTopRow(game: Game?, index: Int, total: Int, onClose: () -> Unit, title: String = game?.title.orEmpty()) {
    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = gutter, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Кнопка не принимает фокус: клавиатура по «Готово» не должна перескакивать на неё.
            RoundIconButton(Icons.Rounded.Close, "Закрыть", onClose, Modifier.focusProperties { canFocus = false }, spin = 90f)
            Spacer(Modifier.width(6.dp))
            Eyebrow(title, Modifier.weight(1f), color = Ink.Soft, maxLines = 1)
            Spacer(Modifier.width(10.dp))
            // Счётчик — маленькая «фишка», как счётчики секций.
            Text(
                "${(index + 1).coerceAtMost(total)} / $total",
                style = AlbionType.index.copy(color = Ink.Soft),
                maxLines = 1,
                modifier = Modifier.background(Ink.Surface, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        ThinProgress(if (total == 0) 0f else index.toFloat() / total, modifier = Modifier.padding(horizontal = gutter), height = 8.dp)
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

/** Поза Финна для упражнения: пишет, слушает, говорит в микрофон, раскладывает слова. */
val Game.mascot: Finn get() = when (this) {
    Game.TYPE_ANSWER -> Finn.WRITING
    Game.IRREGULAR -> Finn.FLASHCARDS
    Game.SHADOWING -> Finn.LISTENING
    Game.PRONUNCIATION -> Finn.SPEAKING
    Game.GRAMMAR_CHOICE -> Finn.GRAMMAR
    Game.GRAMMAR_GAP -> Finn.WRITING
    Game.SENTENCE -> Finn.SENTENCE_BUILDER
    Game.TENSES -> Finn.THINKING
}

/**
 * Шапка настройки упражнения: название слева, Финн в позе упражнения стоит на карточке
 * с параметрами и коротко говорит, чем займёмся.
 */
@Composable
internal fun GameSetupHeader(game: Game, modifier: Modifier = Modifier, options: @Composable ColumnScope.() -> Unit) {
    FinnLedge(
        pose = game.mascot,
        line = game.tagline,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        delayMs = 120,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp),
        header = {
            Eyebrow("Упражнение · %02d".format(game.ordinal + 1), color = Ink.Accent)
            Spacer(Modifier.height(6.dp))
            Text(game.title, style = MaterialTheme.typography.headlineLarge, color = Ink.Fg)
        },
        content = options,
    )
}

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
    LaunchedEffect(Unit) { ring.animateTo(ratio, tween(400, easing = Easing.Emphasized)) }
    val reveal = rememberReveal()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(Modifier.fillMaxSize().background(Ink.Bg)) {
    // Хороший результат — салют цветами Финна поверх итога.
    if (total > 0 && ratio >= 0.8f) ConfettiBurst(Unit, Modifier.fillMaxSize().zIndex(1f))
    Column(
        Modifier.fillMaxSize().background(Ink.Bg).windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()).padding(horizontal = gutter)
            .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp, top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Финн стоит рядом с результатом и реагирует на него.
        val pose = when {
            total == 0 -> Finn.THINKING
            ratio >= 1f -> Finn.MILESTONE
            ratio >= 0.8f -> Finn.CORRECT
            ratio >= 0.5f -> Finn.SESSION_COMPLETE
            else -> Finn.TRY_AGAIN
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Кольцо растёт с системным шрифтом, Финн уступает ему место на узком экране.
        val finnSize = if (maxWidth < 300.dp) 96.dp else 128.dp
        val ringSize = minOf(150.dp * fontScale, maxWidth - finnSize - 8.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Mascot(pose, finnSize, Modifier.reveal(reveal, 0), delayMs = 260)
            Spacer(Modifier.width(8.dp))
            val track = Ink.High
            val arc = if (ratio >= 0.5f) Ink.Accent else Ink.Beak
            Box(Modifier.size(ringSize).reveal(reveal, 0), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 10.dp.toPx()
                    drawCircle(track, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
                    drawArc(
                        arc, -90f, 360f * ring.value, false,
                        topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                        style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
                Column(Modifier.padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNumber(
                        (ratio * 100).toInt(),
                        if (ringSize < 140.dp) AlbionType.numberSmall else AlbionType.number,
                        color = Ink.Fg, suffix = "%",
                    )
                    Text(
                        "$correct ИЗ $total\n${scoreLabel.uppercase()}",
                        style = AlbionType.eyebrow,
                        color = Ink.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
            style = if (LocalNarrow.current) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
            color = Ink.Fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.reveal(reveal, 1),
        )
        Spacer(Modifier.height(6.dp))
        Eyebrow(title, Modifier.reveal(reveal, 2))

        if (mistakes.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().reveal(reveal, 3)) {
                // Ошибки — тёплым цветом клюва: заметно, но не тревожно.
                SectionHeader("Ошибки", Modifier.padding(top = 20.dp), trailing = "%02d".format(mistakes.size))
                Text(
                    "Сохранены — в следующем занятии эти слова попадутся первыми.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mistakes.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().background(Ink.Raised, MaterialTheme.shapes.medium)
                                .border(1.dp, Ink.Line, MaterialTheme.shapes.medium)
                                .height(IntrinsicSize.Min)
                                .padding(end = 14.dp, top = 12.dp, bottom = 12.dp),
                        ) {
                            // Тёплая «закладка» слева — ошибка, к которой вернёмся.
                            Box(Modifier.padding(start = 8.dp, end = 12.dp).width(4.dp).fillMaxHeight().background(Ink.Beak.copy(alpha = 0.7f), CircleShape))
                            Column(Modifier.weight(1f)) {
                                if (m.prompt.isNotBlank()) {
                                    Text(m.prompt, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(m.correct, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                                if (m.answer.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        m.answer,
                                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                        color = Ink.Warm,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Column(Modifier.reveal(reveal, 4), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onRetryMistakes != null && mistakes.isNotEmpty()) PrimaryButton("Повторить ошибки", onClick = onRetryMistakes, tone = ButtonTone.Warm)
            if (onRetryMistakes != null && mistakes.isNotEmpty()) GhostButton(restartLabel, onClick = onRestart)
            else PrimaryButton(restartLabel, onClick = onRestart)
            GhostButton("Готово", onClick = onDone)
        }
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

// ------------------------------------------------------------------ общие элементы раунда

/**
 * Карточка задания: то, на что нужно ответить, лежит на мягкой карточке с градиентом шарфа —
 * как карточки на главной и в занятии. Поля ужимаются на узком экране.
 */
@Composable
internal fun TaskCard(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pad = if (LocalNarrow.current) 16.dp else 20.dp
    HeroCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(pad)) {
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
    val shape = MaterialTheme.shapes.large
    val bg = when (tone) {
        NoteTone.Neutral -> Ink.Surface
        NoteTone.Good -> Ink.AccentSoft
        NoteTone.Bad -> Ink.WarmSoft
    }
    Column(
        modifier.fillMaxWidth().clip(shape).background(bg)
            .then(if (tone == NoteTone.Neutral) Modifier.border(1.dp, Ink.Line, shape) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/** Текст задания антиквой: на узком экране чуть мельче, чтобы предложение не превращалось в столбик. */
internal val promptStyle: TextStyle
    @Composable get() = if (LocalNarrow.current) TextStyle(fontFamily = Serif, fontSize = 22.sp, lineHeight = 31.sp)
    else TextStyle(fontFamily = Serif, fontSize = 26.sp, lineHeight = 36.sp)

/** Крупный термин (слово, ответ): размер зависит от длины и ширины экрана. */
@Composable
internal fun termStyle(text: String, max: Int = 48): TextStyle {
    val size = when {
        text.length <= 10 -> max
        text.length <= 20 -> max * 4 / 5
        else -> max * 5 / 8
    }.let { if (LocalNarrow.current) it * 5 / 6 else it }
    return TextStyle(fontFamily = Serif, fontSize = size.sp, lineHeight = (size * 1.18f).sp, textAlign = TextAlign.Center)
}
