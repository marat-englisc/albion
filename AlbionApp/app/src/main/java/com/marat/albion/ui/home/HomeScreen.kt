package com.marat.albion.ui.home

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
    val reviewsToday: Int = 0,
    val grammar: GrammarBook = GrammarBook.EMPTY,
    val grammarRead: Int = 0,
    val continueTopic: Topic? = null,
    val dailyGoal: Int = 30,
)

class HomeViewModel(c: AppContainer) : ViewModel() {
    val importStatus: StateFlow<ImportStatus> = c.importer.status

    val state: StateFlow<HomeState> = combine(
        c.study.observeSummaries(),
        c.study.observeStats().map { it.streak to it.reviewsToday },
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        c.settings.settings.map { it.lastGrammarTopic to it.dailyGoal },
    ) { decks, (streak, today), book, progress, (lastTopic, goal) ->
        val read = progress.count { it.readAt != null && book.topic(it.topicId) != null }
        val last = lastTopic?.let(book::topic)
            ?: book.lessons.firstOrNull { l -> progress.none { it.topicId == l.id && it.readAt != null } }
        HomeState(decks, streak, today, book, read, last, goal)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ink.Bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        item {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp).reveal(reveal, 0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbionLogo(size = 22.dp)
                    Spacer(Modifier.width(10.dp))
                    Eyebrow("Albion", Modifier.weight(1f), color = Ink.Fg)
                    SettingsShortcut(navigator::openSettings)
                }
                if (state.streak > 0) {
                    val days = when {
                        state.streak % 100 in 11..14 -> "дней"
                        state.streak % 10 == 1 -> "день"
                        state.streak % 10 in 2..4 -> "дня"
                        else -> "дней"
                    }
                    Text("${state.streak} $days подряд", style = MaterialTheme.typography.labelMedium, color = Ink.Muted)
                }
                Spacer(Modifier.height(12.dp))
                Eyebrow(date)
                Spacer(Modifier.height(4.dp))
                Text(greeting(), style = MaterialTheme.typography.headlineMedium, color = Ink.Fg)
            }
        }

        item {
            AnimatedVisibility(status is ImportStatus.Importing || status is ImportStatus.Failed || status == ImportStatus.Empty) {
                Column(Modifier.padding(top = 24.dp)) {
                    when (val s = status) {
                        is ImportStatus.Importing -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Ink.Fg)
                            Spacer(Modifier.width(12.dp))
                            Eyebrow("Загружаю «${s.deck.title}» · ${s.index}/${s.total}")
                        }
                        is ImportStatus.Failed -> Text("Не удалось загрузить материалы: ${s.message}", color = Ink.Soft, style = MaterialTheme.typography.bodyMedium)
                        ImportStatus.Empty -> Panel {
                            EmptyState(
                                "Материалы не добавлены",
                                "Сборка собрана без таблиц и озвучки. Включите albion.bundleContent=true и пересоберите приложение.",
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        item {
            TodayBlock(state, Modifier.padding(top = 16.dp).reveal(reveal, 1)) { navigator.open(StudyRoute(it)) }
        }

        item { SectionHeader("Продолжить", Modifier.padding(top = 6.dp).reveal(reveal, 2)) }
        item { ReadingBlock(Modifier.reveal(reveal, 3), navigator) }
        item {
            GrammarBlock(
                state,
                Modifier.reveal(reveal, 4),
                onOpenBook = { navigator.switchTab(GrammarRoute) },
                onContinue = { navigator.open(TopicRoute(it.id)) },
            )
        }

        item { SectionHeader("Колоды", Modifier.padding(top = 8.dp).reveal(reveal, 5), trailing = "0${Deck.entries.size}") }
        itemsIndexed(state.decks, key = { _, it -> it.deck.id }) { i, summary ->
            DeckRow(i + 1, summary, Modifier.reveal(reveal, 6 + i)) { navigator.open(DeckRoute(summary.deck)) }
        }
    }
}

@Composable
private fun TodayBlock(state: HomeState, modifier: Modifier, onStart: (Deck) -> Unit) {
    val due = state.decks.sumOf { it.due }
    val new = state.decks.sumOf { it.newAvailable }
    val next = state.decks.firstOrNull { it.due > 0 } ?: state.decks.firstOrNull { it.newAvailable > 0 }
    val measurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) {
        measurer.measure("ПОВТОРИТЬ", AlbionType.eyebrow).size.width.toDp() + 12.dp
    }
    Column(modifier) {
        Eyebrow("Сегодня", color = Ink.Soft)
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints {
            if (labelWidth * 3 <= maxWidth) {
                Row {
                    Metric(due, "повторить", Modifier.weight(1f))
                    Metric(new, "доступно\nновых", Modifier.weight(1f))
                    Metric(state.reviewsToday, "ответов", Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Повторить" to due, "Доступно новых" to new, "Ответов" to state.reviewsToday).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink.Muted, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(16.dp))
                            AnimatedNumber(value, AlbionType.numberSmall, color = Ink.Fg)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GoalBar(state.reviewsToday, state.dailyGoal)
        Spacer(Modifier.height(14.dp))
        if (next != null) {
            PrimaryButton(
                text = (if (next.started == 0) "Начать · " else "Продолжить · ") + next.deck.title,
                onClick = { onStart(next.deck) },
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        } else if (state.decks.any { it.total > 0 }) {
            Hairline()
            Spacer(Modifier.height(14.dp))
            Text(
                "На сегодня всё. Следующее повторение появится здесь.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.Muted,
            )
        }
    }
}

@Composable
private fun Metric(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        AnimatedNumber(value, AlbionType.numberSmall, color = if (value == 0) Ink.Faint else Ink.Fg)
        Spacer(Modifier.height(2.dp))
        Eyebrow(label)
    }
}

@Composable
private fun DeckRow(index: Int, summary: DeckSummary, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(index), style = AlbionType.index, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.deck.title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        summary.total == 0 -> summary.deck.subtitle
                        summary.toStudy == 0 -> "${summary.started} из ${summary.total} · на сегодня всё"
                        else -> "${summary.due} повторить · ${summary.newAvailable} новых"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.toStudy > 0) Ink.Soft else Ink.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Ink.Faint)
        }
        ThinProgress(summary.progress, track = Ink.Line)
    }
}

@Composable
private fun GrammarBlock(state: HomeState, modifier: Modifier, onOpenBook: () -> Unit, onContinue: (Topic) -> Unit) {
    val total = state.grammar.lessons.size
    val topic = state.continueTopic
    Panel(modifier.padding(top = 8.dp), onClick = { if (topic != null) onContinue(topic) else onOpenBook() }, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Грамматика", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Spacer(Modifier.height(4.dp))
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
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink.Fg, modifier = Modifier.size(20.dp))
        }
        if (total > 0) {
            Spacer(Modifier.height(10.dp))
            ThinProgress(state.grammarRead.toFloat() / total)
        }
    }
}

/** Дневная цель: линия заполняется, при выполнении — галочка с пружинным появлением. */
@Composable
private fun GoalBar(done: Int, goal: Int) {
    val reached = goal > 0 && done >= goal
    val check by androidx.compose.animation.core.animateFloatAsState(
        if (reached) 1f else 0f,
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 300f),
        label = "goal",
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (check > 0.01f) {
                Icon(
                    Icons.Rounded.Check, null, tint = Ink.Success,
                    modifier = Modifier.size(16.dp).graphicsLayer { scaleX = check; scaleY = check },
                )
                Spacer(Modifier.width(8.dp))
            }
            Eyebrow(if (reached) "Цель дня выполнена" else "Цель дня", Modifier.weight(1f), color = if (reached) Ink.Success else Ink.Muted)
            Text("${minOf(done, 9999)} / $goal", style = AlbionType.index.copy(color = Ink.Soft))
        }
        Spacer(Modifier.height(10.dp))
        ThinProgress(if (goal == 0) 0f else done.toFloat() / goal, color = if (reached) Ink.Success else Ink.Fg)
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
                com.marat.albion.ui.library.BookCover(current.book.title, Modifier.size(width = 32.dp, height = 46.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Чтение", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Spacer(Modifier.height(4.dp))
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
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink.Fg, modifier = Modifier.size(20.dp))
        }
        if (current != null && current.lastRead != null) {
            Spacer(Modifier.height(10.dp))
            ThinProgress(current.progress)
        }
    }
}

