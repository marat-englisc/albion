package com.marat.albion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.marat.albion.data.grammar.GrammarNextStep
import com.marat.albion.data.repo.DeckSummary
import com.marat.albion.ui.components.*
import com.marat.albion.ui.nav.*
import com.marat.albion.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HomeState(
    val decks: List<DeckSummary> = Deck.entries.map { DeckSummary(it) },
    val streak: Int = 0,
    val answersToday: Int = 0,
    val daysAway: Long? = null,
    val grammar: GrammarBook = GrammarBook.EMPTY,
    val grammarRead: Int = 0,
    val continueTopic: Topic? = null,
    val dailyGoal: Int = 30,
    val sessionSize: Int = 10,
    val grammarStep: GrammarNextStep.Step? = null,
    val loaded: Boolean = false,
)

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val settings = c.settings
    fun setSessionSize(count: Int) { viewModelScope.launch { settings.setShortSessionAnswers(count) } }

    val importStatus: StateFlow<ImportStatus> = c.importer.status

    val state: StateFlow<HomeState> = combine(
        c.study.observeSummaries(),
        c.study.observeStats().map { Triple(it.streak, it.answersToday, it.lastActiveDay?.let { day -> it.today - day }) },
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        combine(c.settings.settings, c.grammar.observeQuizTopics()) { s, q -> s to q.toSet() },
    ) { decks, (streak, today, away), book, progress, (s, quizTopics) ->
        val read = progress.count { it.readAt != null && book.topic(it.topicId) != null }
        val step = GrammarNextStep.recommend(book, progress.associateBy { it.topicId }, quizTopics, System.currentTimeMillis(), lastId = s.lastGrammarTopic)
        HomeState(decks, streak, today, away, book, read, step?.topic, s.dailyGoal,
            sessionSize = s.shortSessionAnswers, grammarStep = step, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning."
    in 12..17 -> "Good afternoon."
    in 18..23 -> "Good evening."
    else -> "Welcome back."
}

@Composable
fun HomeScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel { HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.importStatus.collectAsStateWithLifecycle()
    val reveal = rememberReveal()
    var sessionSize by rememberSaveable { mutableIntStateOf(state.sessionSize) }
    LaunchedEffect(state.sessionSize) { sessionSize = state.sessionSize }
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("ru")).format(Date())

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink.Bg)) {
        val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.2f
        val overview: LazyListScope.() -> Unit = {
            item {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Albion", style = TextStyle(fontFamily = Brand, fontSize = 32.sp, lineHeight = 36.sp), color = Ink.AccentText)
                        Text("BRITISH ENGLISH", style = AlbionType.eyebrow.copy(fontSize = 8.sp, letterSpacing = 2.sp), color = Ink.Muted)
                    }
                    if (state.streak > 0) {
                        Icon(Icons.Outlined.WorkspacePremium, null, tint = Ink.Gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("${state.streak} дн.", style = MaterialTheme.typography.labelMedium, color = Ink.GoldText)
                        Spacer(Modifier.width(8.dp))
                    }
                    SettingsShortcut(navigator::openSettings)
                }
                Hairline()
            }
            item {
                Column(Modifier.padding(top = 16.dp, bottom = 16.dp).reveal(reveal, 0)) {
                    Text(greeting(), style = TextStyle(fontFamily = Brand, fontSize = 28.sp, lineHeight = 34.sp), color = Ink.Fg)
                    if (!compact) {
                        Spacer(Modifier.height(2.dp))
                        Text(date.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    }
                }
            }
            item {
                when (val s = status) {
                    is ImportStatus.Importing -> Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink.Accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Готовим материалы · ${s.index}/${s.total}", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    }
                    is ImportStatus.Failed -> Text("Не удалось подготовить материалы. Проверьте их в настройках.", color = Ink.Error, style = MaterialTheme.typography.bodyMedium)
                    ImportStatus.Empty -> EmptyState("Материалы недоступны", "Откройте настройки, чтобы проверить учебные материалы.")
                    else -> Unit
                }
            }
            item {
                TodayCard(state, sessionSize, Modifier.reveal(reveal, 1),
                    onSize = { sessionSize = it; vm.setSessionSize(it) },
                    onStart = { navigator.open(StudyRoute(answerLimit = minOf(sessionSize, state.decks.sumOf { it.toStudy }).coerceAtLeast(1))) },
                    onRead = { navigator.switchTab(LibraryRoute) })
            }
            item { SectionHeader("Продолжить", Modifier.padding(top = 10.dp)) }
            item { ReadingBlock(navigator) }
            item {
                val topic = state.continueTopic
                val step = state.grammarStep
                EditorialLink(
                    title = topic?.title ?: "Открыть учебник",
                    description = topic?.let { listOfNotNull(step?.label, it.number?.let { n -> "Урок $n" }, it.level).joinToString(" · ") }
                        ?: "Короткие уроки и проверка знаний",
                    eyebrow = "Учебник", icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = {
                        if (topic != null && step != null && step.kind != GrammarNextStep.Kind.Read && topic.number != null)
                            navigator.open(TopicQuizRoute(topic.number, topic.id, topic.title))
                        else if (topic != null) navigator.open(TopicRoute(topic.id)) else navigator.switchTab(GrammarRoute)
                    },
                )
            }
        }
        val decks: LazyListScope.() -> Unit = {
            item {
                SectionHeader("Колоды", Modifier.padding(top = 10.dp))
                if (state.loaded) Text("Доступно: ${state.decks.sumOf { it.due }} повторений · ${state.decks.sumOf { it.newAvailable }} новых",
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(bottom = 4.dp))
            }
            itemsIndexed(state.decks, key = { _, s -> s.deck.id }) { i, summary ->
                EditorialLink(
                    title = summary.deck.title, index = "%02d".format(i + 1),
                    description = when {
                        summary.total == 0 -> summary.deck.subtitle
                        summary.toStudy == 0 -> "${summary.started} из ${summary.total} начато · на сегодня всё"
                        else -> "${summary.due} повторить · ${summary.newAvailable} новых доступно"
                    },
                    progress = summary.progress.takeIf { summary.started > 0 },
                    onClick = { navigator.open(DeckRoute(summary.deck)) },
                )
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(28.dp).height(1.dp).background(Ink.Gold))
                    Spacer(Modifier.height(12.dp))
                    Text("A little, every day.", style = TextStyle(fontFamily = Brand, fontSize = 23.sp, fontStyle = FontStyle.Italic), color = Ink.Muted)
                }
            }
        }
        val padding = PaddingValues(start = gutter, end = gutter, bottom = 28.dp)
        if (maxWidth >= 900.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(Modifier.weight(1f), contentPadding = padding) { overview() }
            LazyColumn(Modifier.weight(1f), contentPadding = padding) {
                item { Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(24.dp)) }
                decks()
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) { overview(); decks() }
    }
}

@Composable
private fun TodayCard(state: HomeState, sessionSize: Int, modifier: Modifier, onSize: (Int) -> Unit, onStart: () -> Unit, onRead: () -> Unit) {
    val available = state.decks.sumOf { it.toStudy }
    val planned = minOf(sessionSize, available)
    val reached = state.dailyGoal > 0 && state.answersToday >= state.dailyGoal
    HeroCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Короткое занятие", style = MaterialTheme.typography.headlineSmall, color = Ink.Fg, modifier = Modifier.weight(1f))
            FeatureHelp("short-study", "Короткое занятие",
                "Выберите 5, 10 или 20 ответов. Повторный показ сложного слова тоже считается ответом.\n\nСессия закончится при достижении лимита или раньше, если доступные карточки закончатся. Все ответы сохраняются в расписании повторения; оставшиеся карточки можно открыть позже.\n\nДневная цель учитывает ответы в карточках и практике за весь день.", showOnFirstUse = false)
        }
        if (!state.loaded || available > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Ответов за сессию", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20).forEach { count ->
                    val selected = count == sessionSize
                    val shape = RoundedCornerShape(8.dp)
                    Box(Modifier.weight(1f).heightIn(min = 48.dp)
                        .background(if (selected) Ink.Surface else Ink.Raised, shape)
                        .border(if (selected) 1.5.dp else 1.dp, if (selected) Ink.AccentText else Ink.Line, shape)
                        .selectable(selected, role = Role.RadioButton, onClick = { onSize(count) }).padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("$count", style = MaterialTheme.typography.titleMedium, color = if (selected) Ink.AccentText else Ink.Soft)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(if (!state.loaded) "Готовим занятие…" else "Начать · ${answerLabel(planned)}", onClick = onStart,
                enabled = state.loaded && planned > 0, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
        } else {
            Spacer(Modifier.height(10.dp))
            Text("На сегодня всё повторено. Можно продолжить чтение.", style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
            Spacer(Modifier.height(14.dp))
            GhostButton("Открыть библиотеку", onRead)
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (reached) "Цель дня выполнена" else "Дневная цель", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = if (reached) Ink.Success else Ink.Muted)
            Spacer(Modifier.width(8.dp))
            Text("${state.answersToday} / ${state.dailyGoal}", style = MaterialTheme.typography.labelMedium, color = Ink.Soft)
        }
        Spacer(Modifier.height(7.dp))
        ThinProgress(if (state.dailyGoal > 0) state.answersToday.toFloat() / state.dailyGoal else 0f)
    }
}

private fun answerLabel(count: Int): String = "$count " + when {
    count % 100 in 11..14 -> "ответов"
    count % 10 == 1 -> "ответ"
    count % 10 in 2..4 -> "ответа"
    else -> "ответов"
}

@Composable
private fun ReadingBlock(navigator: Navigator) {
    val c = LocalAppContainer.current
    val books by remember { c.library.observeLibrary() }.collectAsStateWithLifecycle(emptyList())
    val current = books.firstOrNull { it.lastRead != null && it.progress < 0.999f }
    EditorialLink(
        title = current?.book?.title ?: "Открыть библиотеку",
        description = current?.let { "Продолжить чтение · ${(it.progress * 100).toInt()}%" } ?: "Книги с переводом касанием",
        eyebrow = "Чтение", icon = Icons.Outlined.AutoStories,
        onClick = { if (current != null) navigator.open(ReaderRoute(current.book.id)) else navigator.switchTab(LibraryRoute) },
    )
}
