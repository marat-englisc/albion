package com.marat.albion.ui.home

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel { HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val status by vm.importStatus.collectAsStateWithLifecycle()
    val reveal = rememberReveal()
    var showDecks by rememberSaveable { mutableStateOf(false) }
    var sessionSize by rememberSaveable { mutableIntStateOf(state.sessionSize) }
    LaunchedEffect(state.sessionSize) { sessionSize = state.sessionSize }
    val date = SimpleDateFormat("d MMMM · EEEE", Locale.forLanguageTag("ru")).format(Date())

    BoxWithConstraints(Modifier.fillMaxSize().paperSurface()) {
        val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.2f
        val showTagline = !compact && maxWidth >= 500.dp
        val overview: LazyListScope.() -> Unit = {
            item {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (!compact) { AlbionLogo(size = 28.dp); Spacer(Modifier.width(12.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Albion", style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Brand, fontSize = if (compact) 22.sp else 28.sp, lineHeight = if (compact) 27.sp else 32.sp), color = Ink.Fg, maxLines = 1, softWrap = false)
                        if (showTagline) Text("THE ENGLISH COMPANION", style = AlbionType.eyebrow.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp), color = Ink.Muted)
                    }
                    androidx.compose.material3.TextButton(onClick = { showDecks = true }) { Text("Колоды", color = Ink.AccentText) }
                    SettingsShortcut(navigator::openSettings)
                }
                Hairline(color = Ink.Gold.copy(alpha = 0.65f))
            }
            if (!compact) item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp).reveal(reveal, 0), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(date.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                        Spacer(Modifier.height(3.dp))
                        Text(greeting(), style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Brand, fontSize = 24.sp, lineHeight = 29.sp), color = Ink.Fg)
                    }
                    LondonPostmark(Modifier.width(80.dp))
                }
            } else item { Spacer(Modifier.height(16.dp)) }
            item {
                when (val s = status) {
                    is ImportStatus.Importing -> Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.dp, color = Ink.Accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Готовим материалы · ${s.index}/${s.total}", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    }
                    is ImportStatus.Failed -> Column {
                        Text("Не удалось подготовить материалы.", color = Ink.Error)
                        GhostButton("Открыть настройки", navigator::openSettings)
                    }
                    ImportStatus.Empty -> EmptyState("Материалы недоступны", "Откройте настройки, чтобы проверить учебные материалы.")
                    else -> Unit
                }
            }
            item {
                TodayCard(state, sessionSize, Modifier.reveal(reveal, 1), compact,
                    onSize = { sessionSize = it; vm.setSessionSize(it) },
                    onStart = { navigator.open(StudyRoute(answerLimit = minOf(sessionSize, state.decks.sumOf { it.toStudy }).coerceAtLeast(1))) },
                    onRead = { navigator.switchTab(LibraryRoute) })
            }
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    val done = state.answersToday >= state.dailyGoal
                    val progress = if (state.dailyGoal > 0) state.answersToday.toFloat() / state.dailyGoal else 0f
                    Text(if (done) "✓ Цель выполнена" else "Цель дня", style = MaterialTheme.typography.labelMedium,
                        color = if (done) Ink.Success else Ink.Muted)
                    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (done) "${state.answersToday} ответов · цель ${state.dailyGoal}" else "${state.answersToday} из ${state.dailyGoal} ответов",
                            style = MaterialTheme.typography.bodySmall, color = Ink.Soft)
                        if (state.streak > 0) Text("Серия: ${state.streak} дн.", style = MaterialTheme.typography.bodySmall, color = Ink.Soft)
                    }
                    if (done && state.answersToday > state.dailyGoal) Text("Ещё ${answerLabel(state.answersToday - state.dailyGoal)} сверх цели",
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    Spacer(Modifier.height(6.dp))
                    ThinProgress(progress, color = if (done) Ink.Success else Ink.GoldText)
                }
            }
            item { ReadingBlock(navigator) }
            item {
                val topic = state.continueTopic
                val step = state.grammarStep
                CatalogueCard(
                    title = topic?.title ?: "Открыть учебник",
                    description = topic?.let { listOfNotNull(step?.label, it.number?.let { n -> "Урок $n" }, it.level).joinToString(" · ") }
                        ?: "Короткие уроки и проверка знаний",
                    label = "Грамматика", compact = true,
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
                SectionHeader("Словарная коллекция", Modifier.padding(top = 14.dp), trailing = "II")
                if (state.loaded) Text("${state.decks.sumOf { it.due }} повторений · ${state.decks.sumOf { it.newAvailable }} новых слов",
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
                Column(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FolioDivider(label = "ALBION · LONDON")
                    Spacer(Modifier.height(15.dp))
                    Text("A little, every day.", style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Brand, fontSize = 25.sp, fontStyle = FontStyle.Italic), color = Ink.Muted)
                    Spacer(Modifier.height(5.dp))
                    Text("Маленькие шаги. Большие возможности.", style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Cyrillic, fontSize = 18.sp), color = Ink.Muted, textAlign = TextAlign.Center)
                }
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = { com.marat.albion.ui.components.SheetHandle() },onDismissRequest = { showDecks = false },
        containerColor = Ink.Raised, scrimColor = Ink.Fg.copy(alpha = 0.32f),
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
private fun TodayCard(state: HomeState, sessionSize: Int, modifier: Modifier, compact: Boolean, onSize: (Int) -> Unit, onStart: () -> Unit, onRead: () -> Unit) {
    val available = state.decks.sumOf { it.toStudy }
    val planned = minOf(sessionSize, available)
    val feedback = LocalFeedback.current
    val ivory = Ink.Ivory
    // The four identity colours stay exact in both themes.
    val crimson = androidx.compose.ui.graphics.Color(0xFF861F39)
    BoundVolume(modifier.fillMaxWidth(), compact = true) {
        Text("На сегодня", style = TextStyle(fontFamily = Cyrillic, fontSize = if (compact) 24.sp else 28.sp,
            lineHeight = if (compact) 29.sp else 33.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = ivory)
        if (!state.loaded || available > 0) {
            Spacer(Modifier.height(8.dp))
            Text(if (state.loaded && available < sessionSize) "Доступно ${answerLabel(planned)}" else answerLabel(sessionSize),
                style = MaterialTheme.typography.bodySmall, color = ivory)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20).forEach { count ->
                    val selected = count == sessionSize
                    Box(Modifier.weight(1f).heightIn(min = 48.dp)
                        .background(if (selected) ivory else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(3.dp))
                        .border(0.5.dp, if (selected) ivory else ivory.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                        .selectable(selected, role = Role.RadioButton, onClick = { feedback.selection(); onSize(count) })
                        .padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("$count", style = MaterialTheme.typography.titleMedium, color = if (selected) crimson else ivory)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(if (!state.loaded) "Готовим занятие…" else "Начать", onClick = onStart,
                enabled = state.loaded && planned > 0, trailingIcon = AlbionIcons.ArrowForward, inverse = true)
        } else {
            Spacer(Modifier.height(10.dp))
            Text("Всё повторено. Продолжим с хорошей книгой?", style = MaterialTheme.typography.bodyMedium, color = ivory)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Открыть библиотеку", onRead, inverse = true, trailingIcon = AlbionIcons.ArrowForward)
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
    CatalogueCard(
        label = "Чтение", compact = true, title = current?.book?.title ?: "Открыть библиотеку",
        description = if (current != null) "Продолжить чтение · ${(current.progress * 100).toInt()}%" else "Книги с переводом касанием",
        ribbon = true, progress = current?.progress,
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = { if (current != null) navigator.open(ReaderRoute(current.book.id)) else navigator.switchTab(LibraryRoute) },
    )
}
