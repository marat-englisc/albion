package com.marat.albion.ui.home

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import com.marat.albion.ui.components.ConfettiBurst
import com.marat.albion.ui.components.FinnLedge
import com.marat.albion.ui.components.HeroCard
import com.marat.albion.ui.components.Mascot
import com.marat.albion.ui.components.SpeechBubble
import com.marat.albion.ui.components.StatPill
import com.marat.albion.ui.components.Finn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.AppContainer
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.content.ImportStatus
import com.marat.albion.data.grammar.GrammarBook
import com.marat.albion.data.grammar.Topic
import com.marat.albion.data.repo.DeckSummary
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.EmptyState
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.nav.DeckRoute
import com.marat.albion.ui.nav.GrammarRoute
import com.marat.albion.ui.nav.LibraryRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.ReaderRoute
import com.marat.albion.ui.nav.StudyRoute
import com.marat.albion.ui.nav.TopicRoute
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HomeState(
    val decks: List<DeckSummary> = Deck.entries.map { DeckSummary(it) },
    val streak: Int = 0,
    /** Ответы за учебный день: карточки и практика — по ним считается цель. */
    val answersToday: Int = 0,
    /** Сколько учебных дней прошло с последнего занятия; null — занятий ещё не было. */
    val daysAway: Long? = null,
    val grammar: GrammarBook = GrammarBook.EMPTY,
    val grammarRead: Int = 0,
    val continueTopic: Topic? = null,
    val dailyGoal: Int = 30,
    /** Данные загружены — до этого Финн не показывается, чтобы не мелькнула случайная поза. */
    val loaded: Boolean = false,
) {
    /** Финн на главной отражает день: знакомство, возвращение, цель, «всё повторено». */
    val finn: Finn get() = when {
        daysAway == null && answersToday == 0 -> Finn.WELCOME
        answersToday >= dailyGoal -> Finn.DAILY_GOAL
        daysAway != null && daysAway >= 3 && answersToday == 0 -> Finn.WELCOME_BACK
        decks.sumOf { it.toStudy } == 0 && decks.any { it.total > 0 } -> Finn.ALL_DONE
        else -> Finn.TODAY
    }

    /** Первая реплика Финна — о том, что сегодня за день. */
    val finnCaption: String get() = when (finn) {
        Finn.WELCOME -> "Привет! Я Финн. Первое занятие займёт пару минут."
        Finn.WELCOME_BACK -> "С возвращением! Карточки ждали вас."
        Finn.DAILY_GOAL -> "Цель дня выполнена — горжусь!"
        Finn.ALL_DONE -> "На сегодня всё повторено. Можно почитать."
        else -> decks.sumOf { it.toStudy }.let { n ->
            if (n > 0) "Сегодня нас ждут $n ${pluralCards(n)}. Начнём?" else "Готов, когда вы готовы."
        }
    }

    /** Реплики по касанию: первая — о дне, дальше — короткие советы по кругу. */
    val finnLines: List<String> get() = listOf(finnCaption) + FINN_TIPS
}

private val FINN_TIPS = listOf(
    "Немного каждый день работает лучше, чем много за раз.",
    "Слова с ошибками я покажу чаще — так они запомнятся.",
    "Слушайте озвучку и повторяйте вслух — это тоже тренировка.",
    "В «Чтении» коснитесь слова — я подскажу перевод.",
    "Хватит меня тыкать, щекотно! Лучше откроем карточки.",
)

private fun pluralCards(n: Int) = when {
    n % 100 in 11..14 -> "карточек"
    n % 10 == 1 -> "карточка"
    n % 10 in 2..4 -> "карточки"
    else -> "карточек"
}

class HomeViewModel(c: AppContainer) : ViewModel() {
    val importStatus: StateFlow<ImportStatus> = c.importer.status

    val state: StateFlow<HomeState> = combine(
        c.study.observeSummaries(),
        c.study.observeStats().map { Triple(it.streak, it.answersToday, it.lastActiveDay?.let { day -> it.today - day }) },
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        c.settings.settings.map { it.lastGrammarTopic to it.dailyGoal },
    ) { decks, (streak, today, away), book, progress, (lastTopic, goal) ->
        val read = progress.count { it.readAt != null && book.topic(it.topicId) != null }
        val last = lastTopic?.let(book::topic)
            ?: book.lessons.firstOrNull { l -> progress.none { it.topicId == l.id && it.readAt != null } }
        HomeState(decks, streak, today, away, book, read, last, goal, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning."
    in 12..17 -> "Good afternoon."
    in 18..22 -> "Good evening."
    else -> "Good night."
}

@Composable
fun HomeScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    val vm = viewModel { HomeViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.importStatus.collectAsStateWithLifecycle()
    val reveal = rememberReveal()
    val date = remember { SimpleDateFormat("EEEE · d MMMM", Locale.forLanguageTag("ru")).format(Date()) }
    val shortDate = remember { SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru")).format(Date()) }
    var line by rememberSaveable { mutableIntStateOf(0) }

    // Высота верхней строки — чтобы колоды справа начинались вровень с Финном.
    var headerPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerPx.toDp() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink.Bg)) {
        val compact = maxWidth < 600.dp &&
            (maxWidth < 400.dp || maxHeight < 680.dp || LocalDensity.current.fontScale > 1.2f)
        // Занятие на сегодня и продолжение — одна колонка; колоды — вторая, если на экране есть место.
        val overview: LazyListScope.() -> Unit = {
            item {
                Row(
                    Modifier.onSizeChanged { headerPx = it.height }.windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 8.dp).reveal(reveal, 0),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbionLogo(size = 22.dp, color = Ink.Accent)
                    Spacer(Modifier.width(10.dp))
                    Eyebrow("Albion", color = Ink.Fg)
                    Spacer(Modifier.weight(1f))
                    if (state.streak > 0) StreakChip(state.streak)
                    SettingsShortcut(navigator::openSettings)
                }
            }

            item {
                AnimatedVisibility(status is ImportStatus.Importing || status is ImportStatus.Failed || status == ImportStatus.Empty) {
                    Column(Modifier.padding(top = 16.dp)) {
                        when (val s = status) {
                            is ImportStatus.Importing -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Ink.Accent)
                                Spacer(Modifier.width(12.dp))
                                Eyebrow("Загружаю «${s.deck.title}» · ${s.index}/${s.total}")
                            }
                            is ImportStatus.Failed -> Text("Не удалось загрузить материалы: ${s.message}", color = Ink.Warm, style = MaterialTheme.typography.bodyMedium)
                            ImportStatus.Empty -> Panel {
                                EmptyState(
                                    title = "Материалы не добавлены",
                                    message = "Сборка собрана без таблиц и озвучки. Включите albion.bundleContent=true и пересоберите приложение.",
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            item {
                val lines = state.finnLines
                if (compact) {
                    CompactToday(state, shortDate, Modifier.padding(top = 8.dp).reveal(reveal, 1)) {
                        navigator.open(StudyRoute(it))
                    }
                } else FinnLedge(
                    pose = if (state.loaded) state.finn else null,
                    line = if (state.loaded) lines[line % lines.size] else null,
                    onTap = { line++ },
                    modifier = Modifier.padding(top = 4.dp).reveal(reveal, 1),
                    header = {
                        Eyebrow(date)
                        Spacer(Modifier.height(4.dp))
                        Text(greeting(), style = MaterialTheme.typography.headlineMedium, color = Ink.Fg)
                    },
                ) {
                    TodayBlock(state) { navigator.open(StudyRoute(it)) }
                }
            }

            item { SectionHeader("Продолжить", Modifier.padding(top = 14.dp).reveal(reveal, 2)) }
            item { ReadingBlock(Modifier.reveal(reveal, 3), navigator) }
            item {
                GrammarBlock(
                    state,
                    Modifier.reveal(reveal, 4),
                    onOpenBook = { navigator.switchTab(GrammarRoute) },
                    onContinue = { navigator.open(TopicRoute(it.id)) },
                )
            }

        }
        val decks: LazyListScope.() -> Unit = {
            item { SectionHeader("Колоды", Modifier.padding(top = 8.dp).reveal(reveal, 5), trailing = "0${Deck.entries.size}") }
            itemsIndexed(state.decks, key = { _, it -> it.deck.id }) { i, summary ->
                DeckRow(i + 1, summary, Modifier.padding(vertical = 5.dp).reveal(reveal, 6 + i)) { navigator.open(DeckRoute(summary.deck)) }
            }
        }

        val padding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)
        if (maxWidth >= TWO_COLUMNS_MIN_WIDTH) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = padding) { overview() }
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = padding) {
                    item { Spacer(Modifier.height(headerHeight)) }
                    decks()
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                overview()
                decks()
            }
        }
    }
}

/** С какой ширины главная делится на две колонки. */
private val TWO_COLUMNS_MIN_WIDTH = 900.dp

/** На небольшом телефоне сначала доступно занятие, затем счётчики и цель. */
@Composable
private fun CompactToday(state: HomeState, date: String, modifier: Modifier, onStart: (Deck?) -> Unit) {
    var tip by rememberSaveable { mutableIntStateOf(-1) }
    HeroCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Text(date, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
            Spacer(Modifier.width(8.dp))
            if (state.loaded) Mascot(state.finn, 64.dp, onTap = { tip++ })
            else Spacer(Modifier.size(64.dp))
        }
        Spacer(Modifier.height(12.dp))
        StudyStartAction(state, onStart, concise = true)
        Spacer(Modifier.height(12.dp))
        Text(
            "${state.decks.sumOf { it.due }} повторить · ${state.decks.sumOf { it.newAvailable }} новых",
            style = MaterialTheme.typography.bodyMedium, color = Ink.Soft,
        )
        Spacer(Modifier.height(12.dp))
        GoalBar(state.answersToday, state.dailyGoal, state.loaded, compact = true)
        if (tip >= 0 && state.loaded) {
            Spacer(Modifier.height(12.dp))
            SpeechBubble(state.finnLines[tip % state.finnLines.size])
        }
    }
}

/** Серия дней: огонёк цвета клюва, который мерцает, пока серия жива. */
@Composable
private fun StreakChip(streak: Int) {
    val days = when {
        streak % 100 in 11..14 -> "дней"
        streak % 10 == 1 -> "день"
        streak % 10 in 2..4 -> "дня"
        else -> "дней"
    }
    val flicker by rememberInfiniteTransition(label = "flame").animateFloat(
        0.92f, 1.08f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker",
    )
    Row(
        Modifier.semantics(mergeDescendants = true) {}.background(Ink.WarmSoft, CircleShape).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment, null, tint = Ink.Beak,
            modifier = Modifier.size(18.dp).graphicsLayer { scaleX = flicker; scaleY = 2f - flicker; transformOrigin = TransformOrigin(0.5f, 1f) },
        )
        Spacer(Modifier.width(4.dp))
        Text("$streak $days", style = MaterialTheme.typography.labelLarge, color = Ink.Warm)
    }
}

@Composable
private fun TodayBlock(state: HomeState, onStart: (Deck?) -> Unit) {
    val due = state.decks.sumOf { it.due }
    val new = state.decks.sumOf { it.newAvailable }
    Column {
        Eyebrow("Сегодня", color = Ink.Accent)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints {
            if (maxWidth >= 300.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(due, "повторить", Modifier.weight(1f))
                    StatPill(new, "новых", Modifier.weight(1f))
                    StatPill(state.answersToday, "ответов", Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(due, "повторить", Modifier.fillMaxWidth())
                    StatPill(new, "новых", Modifier.fillMaxWidth())
                    StatPill(state.answersToday, "ответов", Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GoalBar(state.answersToday, state.dailyGoal, state.loaded)
        Spacer(Modifier.height(16.dp))
        StudyStartAction(state, onStart)
    }
}

/** Отбор колод одинаков для полного и компактного обзора. */
@Composable
private fun StudyStartAction(state: HomeState, onStart: (Deck?) -> Unit, concise: Boolean = false) {
    val withWork = state.decks.filter { it.toStudy > 0 }
    Column {
        if (concise && withWork.isNotEmpty()) {
            Text(if (withWork.size > 1) "Все колоды" else withWork.single().deck.title,
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            Spacer(Modifier.height(6.dp))
        }
        if (withWork.size > 1) {
            PrimaryButton(
                text = if (concise) "Начать занятие" else "Занятие · все колоды",
                onClick = { onStart(null) },
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        } else if (withWork.size == 1) {
            val next = withWork.single()
            PrimaryButton(
                text = if (concise) {
                    if (next.started == 0) "Начать занятие" else "Продолжить занятие"
                } else (if (next.started == 0) "Начать · " else "Продолжить · ") + next.deck.title,
                onClick = { onStart(next.deck) },
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        } else if (state.decks.any { it.total > 0 }) {
            Text(
                "На сегодня всё. Следующее повторение появится здесь.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.Muted,
            )
        }
    }
}

/** Колода — карточка с «фишкой»-номером и полоской прогресса. */
@Composable
private fun DeckRow(index: Int, summary: DeckSummary, modifier: Modifier, onClick: () -> Unit) {
    val active = summary.toStudy > 0
    Panel(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(if (active) Ink.AccentSoft else Ink.Surface, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("%02d".format(index), style = AlbionType.index.copy(color = if (active) Ink.Accent else Ink.Faint, fontStyle = FontStyle.Normal))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.deck.title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        summary.total == 0 -> summary.deck.subtitle
                        summary.toStudy == 0 -> "${summary.started} из ${summary.total} · на сегодня всё"
                        else -> "${summary.due} повторить · ${summary.newAvailable} новых"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) Ink.Soft else Ink.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Ink.Faint)
        }
        Spacer(Modifier.height(12.dp))
        ThinProgress(summary.progress, Modifier.padding(end = 4.dp))
    }
}

/** Значок раздела в мягкой цветной «плитке». */
@Composable
private fun IconTile(icon: ImageVector, tint: Color, background: Color) {
    Box(Modifier.size(44.dp).background(background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun GrammarBlock(state: HomeState, modifier: Modifier, onOpenBook: () -> Unit, onContinue: (Topic) -> Unit) {
    val total = state.grammar.lessons.size
    val topic = state.continueTopic
    Panel(modifier.padding(top = 10.dp), onClick = { if (topic != null) onContinue(topic) else onOpenBook() }, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.AutoMirrored.Outlined.MenuBook, Ink.Accent, Ink.AccentSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Грамматика", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Spacer(Modifier.height(2.dp))
                Text(
                    topic?.let { listOfNotNull(it.number?.toString(), it.title).joinToString(". ") }
                        ?: if (total == 0) "Темы появятся вместе с материалами" else "Учебник · $total тем",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink.Accent, modifier = Modifier.size(20.dp))
        }
        if (total > 0) {
            Spacer(Modifier.height(12.dp))
            ThinProgress(state.grammarRead.toFloat() / total)
        }
    }
}

/** Дневная цель: полоска заполняется, при выполнении — галочка с пружинным появлением и салют. */
@Composable
private fun GoalBar(done: Int, goal: Int, loaded: Boolean, compact: Boolean = false) {
    val reached = goal > 0 && done >= goal
    var seen by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var celebrate by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded, reached) {
        if (!loaded) return@LaunchedEffect
        if (seen == false && reached) celebrate++
        seen = reached
    }
    val check by androidx.compose.animation.core.animateFloatAsState(
        if (reached) 1f else 0f,
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 300f),
        label = "goal",
    )
    Box {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (check > 0.01f) {
                    Box(
                        Modifier.size(20.dp).graphicsLayer { scaleX = check; scaleY = check; rotationZ = (1f - check) * -90f }
                            .background(Ink.Accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = Ink.OnAccent, modifier = Modifier.size(14.dp)) }
                    Spacer(Modifier.width(8.dp))
                }
                if (compact) {
                    Text(if (reached) "Цель выполнена · $done / $goal" else "Цель дня · $done / $goal",
                        style = MaterialTheme.typography.bodySmall, color = if (reached) Ink.Accent else Ink.Muted)
                } else {
                    Eyebrow(if (reached) "Цель дня выполнена" else "Цель дня", Modifier.weight(1f), color = if (reached) Ink.Accent else Ink.Muted)
                    Text("${minOf(done, 9999)} / $goal", style = AlbionType.index.copy(color = Ink.Soft))
                }
            }
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            ThinProgress(if (goal == 0) 0f else done.toFloat() / goal, height = if (compact) 6.dp else 10.dp)
        }
        // Салют — только когда цель выполнена «на глазах», а не при каждом открытии главной.
        if (celebrate > 0) ConfettiBurst(celebrate, Modifier.matchParentSize(), count = 40)
    }
}

/** Чтение: продолжить книгу или открыть библиотеку. */
@Composable
private fun ReadingBlock(modifier: Modifier, navigator: Navigator) {
    val c = LocalAppContainer.current
    val books by remember { c.library.observeLibrary() }.collectAsStateWithLifecycle(emptyList())
    val current = books.firstOrNull { it.lastRead != null && it.progress < 0.999f } ?: books.firstOrNull()
    Panel(modifier, onClick = {
        if (current != null && current.lastRead != null) navigator.open(ReaderRoute(current.book.id))
        else navigator.switchTab(LibraryRoute)
    }, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (current != null) {
                com.marat.albion.ui.library.BookCover(current.book.title, Modifier.size(width = 34.dp, height = 46.dp))
            } else {
                IconTile(Icons.Outlined.AutoStories, Ink.Warm, Ink.WarmSoft)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Чтение", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Spacer(Modifier.height(2.dp))
                Text(
                    current?.book?.title
                        ?: "Книги с переводом касанием",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink.Accent, modifier = Modifier.size(20.dp))
        }
        if (current != null && current.lastRead != null) {
            Spacer(Modifier.height(12.dp))
            ThinProgress(current.progress, color = Ink.Beak)
        }
    }
}
