package com.marat.albion.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.RoundIconButton
import com.marat.albion.ui.components.SegmentedPicker
import com.marat.albion.ui.theme.AlbionFonts
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.appear
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel { HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.importStatus.collectAsStateWithLifecycle()
    var showDecks by rememberSaveable { mutableStateOf(false) }
    var sessionSize by rememberSaveable { mutableIntStateOf(state.sessionSize) }
    LaunchedEffect(state.sessionSize) { sessionSize = state.sessionSize }
    val date = SimpleDateFormat("d MMMM · EEEE", Locale.forLanguageTag("ru")).format(Date())

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxWidth < 360.dp || maxHeight < 640.dp || LocalDensity.current.fontScale > 1.2f
        val overview: LazyListScope.() -> Unit = {
            item {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    
                    AlbionMark(Modifier.appear(0))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).appear(0)) {
                        Text("albion", style = MaterialTheme.typography.titleLarge.copy(fontFamily = AlbionFonts.Display), maxLines = 1)
                        if (!compact) SignalLabel("АНГЛИЙСКИЙ КАЖДЫЙ ДЕНЬ", Modifier.padding(top = 3.dp))
                    }
                    RoundIconButton(AlbionIcons.Inventory2, "Колоды", { showDecks = true }, filled = true)
                    Spacer(Modifier.width(4.dp))
                    SettingsShortcut(navigator::openSettings)
                }
            }
            if (!compact) item {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp).appear(1)) {
                    SignalLabel(date.uppercase(Locale.forLanguageTag("ru")))
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ваш язык.\nВаш ритм.", Modifier.weight(1f),
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 41.sp))
                        if (!compact) OrbitalField(Modifier.size(120.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                AnimatedNumber(state.answersToday, MaterialTheme.typography.titleLarge)
                                Text(" / ${state.dailyGoal}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("цель дня", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${state.streak} дн.", style = MaterialTheme.typography.titleLarge)
                            Text("ваша серия", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            SignalLabel(if (state.answersToday >= state.dailyGoal) "ВЫПОЛНЕНО" else "В ПРОЦЕССЕ")
                            Spacer(Modifier.height(8.dp))
                            ThinProgress(if (state.dailyGoal > 0) state.answersToday.toFloat() / state.dailyGoal else 0f, Modifier.width(76.dp), height = 2.dp)
                        }
                    }
                }
            }
            item {
                when (val s = status) {
                    is ImportStatus.Importing -> Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Готовим материалы · ${s.index}/${s.total}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ImportStatus.Failed -> Column {
                        Text("Не удалось подготовить материалы.", color = MaterialTheme.colorScheme.error)
                        GhostButton("Открыть настройки", navigator::openSettings)
                    }
                    ImportStatus.Empty -> EmptyState("Материалы недоступны", "Откройте настройки, чтобы проверить учебные материалы.")
                    else -> Unit
                }
            }
            item {
                TodayCard(state, sessionSize, Modifier.appear(2), compact, status is ImportStatus.Checking || status is ImportStatus.Importing,
                    onSize = { sessionSize = it; vm.setSessionSize(it) },
                    onStart = { navigator.open(StudyRoute(answerLimit = minOf(sessionSize, state.decks.sumOf { it.toStudy }).coerceAtLeast(1))) },
                    onRead = { navigator.switchTab(LibraryRoute) })
            }
            if (compact) item {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Цель дня · ${state.answersToday} / ${state.dailyGoal}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(if (state.dailyGoal > 0) state.answersToday.toFloat() / state.dailyGoal else 0f, height = 2.dp)
                }
            }
            item {
                ModuleHeading("02", "План на сегодня", modifier = Modifier.appear(3))
            }
            item {
                val topic = state.continueTopic
                val step = state.grammarStep
                PlanRow(
                    AlbionIcons.MenuBook,
                    title = topic?.title ?: "Открыть учебник",
                    subtitle = topic?.let { listOfNotNull(step?.label, it.number?.let { n -> "Урок $n" }, it.level).joinToString(" · ") }
                        ?: "Короткие уроки и проверка знаний",
                    done = false,
                    modifier = Modifier.padding(bottom = 10.dp).appear(4),
                ) {
                    if (topic != null && step != null && step.kind != GrammarNextStep.Kind.Read && topic.number != null)
                        navigator.open(TopicQuizRoute(topic.number, topic.id, topic.title))
                    else if (topic != null) navigator.open(TopicRoute(topic.id)) else navigator.switchTab(GrammarRoute)
                }
            }
            item { Box(Modifier.padding(bottom = 10.dp).appear(5)) { ReadingBlock(navigator) } }
            item { Box(Modifier.appear(6)) { PracticeBlock(navigator) } }
        }
        val decks: LazyListScope.() -> Unit = {
            item {
                ModuleHeading("03", "Ваши колоды", if (state.loaded) "${state.decks.size} " + com.marat.albion.ui.library.pluralRu(state.decks.size, "колода", "колоды", "колод") else null)
            }
            items(state.decks.size, key = { state.decks[it].deck.id }) { i ->
                val summary = state.decks[i]
                DeckRow(summary, Modifier.padding(bottom = 8.dp).appear(7 + i)) { navigator.open(DeckRoute(summary.deck)) }
            }
        }
        val padding = PaddingValues(start = gutter, end = gutter, bottom = 28.dp)
        if (maxWidth >= 900.dp) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            LazyColumn(Modifier.weight(1f), contentPadding = padding) { overview() }
            LazyColumn(Modifier.weight(1f), contentPadding = padding) {
                item { Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(24.dp)) }
                decks()
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) { overview(); decks() }
    }
    if (showDecks) androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { showDecks = false },
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = gutter)) {
            SheetHeader("Все колоды", { showDecks = false })
            state.decks.forEach { deck -> EditorialLink(deck.deck.title,
                "${deck.due} повторить · ${deck.newAvailable} новых", onClick = {
                    showDecks = false; navigator.open(DeckRoute(deck.deck))
                }) }
        }
    }

}

@Composable
private fun TodayCard(state: HomeState, sessionSize: Int, modifier: Modifier, compact: Boolean, preparing: Boolean, onSize: (Int) -> Unit, onStart: () -> Unit, onRead: () -> Unit) {
    val available = state.decks.sumOf { it.toStudy }
    val planned = minOf(sessionSize, available)
    val loaded = state.loaded && !preparing
    val sizes = listOf(5, 10, 20)
    val scheme = MaterialTheme.colorScheme
    Panel(modifier.fillMaxWidth(), contentPadding = PaddingValues(if (compact) 16.dp else 22.dp), borderColor = scheme.outlineVariant) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SignalLabel("01 / ФОКУС")
            if (!compact) SignalLabel("НА СЕГОДНЯ")
        }
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
        Text(when {
            !loaded -> "Готовим занятие"
            available > 0 && compact -> "На сегодня"
            available > 0 -> "Маленький шаг.\nНовый уровень."
            else -> "Отличная работа."
        }, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(when {
            !loaded -> "Собираем ваши карточки"
            available > 0 -> "${answerLabel(planned)} за занятие · $available ${com.marat.albion.ui.library.pluralRu(available, "карточка доступна", "карточки доступны", "карточек доступно")}"
            else -> "Все карточки на сегодня повторены. Время для книги."
        }, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        if (!loaded || available > 0) {
            Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
            if (!compact) {
                SignalLabel("ОБЪЁМ ЗАНЯТИЯ")
                Spacer(Modifier.height(8.dp))
            }
            SegmentedPicker(sizes.map { "$it" }, sizes.indexOf(sessionSize).coerceAtLeast(0), { onSize(sizes[it]) })
            Spacer(Modifier.height(14.dp))
            PrimaryButton(if (!loaded) "Готовим занятие…" else "Начать", onClick = onStart,
                enabled = loaded && planned > 0, trailingIcon = AlbionIcons.ArrowForward)
        } else {
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Открыть библиотеку", onRead, trailingIcon = AlbionIcons.ArrowForward)
        }
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
    PlanRow(
        AlbionIcons.AutoStories,
        title = current?.book?.title ?: "Открыть библиотеку",
        subtitle = if (current != null) "Продолжить чтение · ${(current.progress * 100).toInt()}%" else "Книги с переводом касанием",
        done = false,
        progress = current?.progress,
    ) { if (current != null) navigator.open(ReaderRoute(current.book.id)) else navigator.switchTab(LibraryRoute) }
}

/** Практика в плане дня: если накопились ошибки — повторить их, иначе открыть каталог. */
@Composable
private fun PracticeBlock(navigator: Navigator) {
    val c = LocalAppContainer.current
    val needs by remember { c.study.observePracticeNeeds() }.collectAsStateWithLifecycle(emptyList())
    val top = needs.maxByOrNull { it.count }
    val game = top?.let { n -> com.marat.albion.ui.games.Game.entries.firstOrNull { it.name == n.game } }
    PlanRow(
        AlbionIcons.EditNote,
        title = if (game != null) "Повторить ошибки" else "Практика",
        subtitle = if (game != null) "${game.title} · ${top.count}" else "Упражнения на слова, грамматику и речь",
        done = false,
    ) {
        if (game != null) navigator.open(com.marat.albion.ui.nav.GameRoute(game, com.marat.albion.data.content.Deck.byId(top.deck), onlyMistakes = true))
        else navigator.switchTab(com.marat.albion.ui.nav.GamesRoute)
    }
}
