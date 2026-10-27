package com.marat.albion.ui.grammar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.AppContainer
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.db.GrammarProgressEntity
import com.marat.albion.data.grammar.*
import com.marat.albion.ui.browse.SearchField
import com.marat.albion.ui.components.*
import com.marat.albion.ui.nav.LocalBesideDetail
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.TopicRoute
import com.marat.albion.ui.nav.TopicQuizRoute
import com.marat.albion.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrammarHomeState(
    val loading: Boolean = true,
    val book: GrammarBook = GrammarBook.EMPTY,
    val progress: Map<String, GrammarProgressEntity> = emptyMap(),
    val lastTopic: String? = null,
    val reviews: List<GrammarReview.Item> = emptyList(),
    val quizTopics: Set<Int> = emptySet(),
)

class GrammarViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<GrammarHomeState> = combine(
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        c.settings.settings,
        c.grammar.observeQuizTopics(),
    ) { book, progress, s, quizTopics ->
        val byId = progress.associateBy { it.topicId }
        GrammarHomeState(false, book, byId, s.lastGrammarTopic, GrammarReview.scheduled(book, byId, quizTopics.toSet()), quizTopics.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrammarHomeState())
}

@Composable
fun GrammarScreen(navigator: Navigator) {
    val c = LocalAppContainer.current
    val vm = viewModel { GrammarViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf<String?>(null) }
    var bookmarksOnly by rememberSaveable { mutableStateOf(false) }
    var reviewsOnly by rememberSaveable { mutableStateOf(false) }
    var reviewDetails by rememberSaveable { mutableStateOf(false) }
    val reveal = rememberReveal()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reduced = LocalReducedMotion.current

    val book = state.book
    val now = grammarReviewTime()
    val stages = remember(book) { grammarStages(book) }
    val nextStep = GrammarNextStep.recommend(book, state.progress, state.quizTopics, now, lastId = state.lastTopic)
    val recommendation = nextStep?.topic
    val activeStage = stages.firstOrNull { it.level == level }
        ?: stages.firstOrNull { it.level == recommendation?.level }
        ?: stages.firstOrNull { stage -> stage.topics.any { state.progress[it.id]?.masteredAt == null } }
        ?: stages.lastOrNull()
    val route = activeStage?.topics.orEmpty()
    val chapterByTopic = remember(book) {
        book.chapters.flatMap { chapter -> chapter.topics.map { it.id to chapter.title } }.toMap()
    }
    val due = state.reviews.takeWhile { it.dueAt <= now }
    val filtering = query.isNotBlank() || bookmarksOnly || reviewsOnly
    val showNext = !filtering && nextStep != null && recommendation?.level == activeStage?.level
    val q = query.trim().lowercase()
    // Search and saved/review lists cover the whole book, independent of the stage being explored.
    val found = if (!filtering) emptyList() else (if (reviewsOnly) due.map { it.topic } else book.topics).filter { topic ->
        (topic.kind == TopicKind.Lesson || topic.kind == TopicKind.Reference) &&
            GrammarSearch.matches(topic, q) &&
            (!bookmarksOnly || state.progress[topic.id]?.bookmarked == true)
    }
    val open: (Topic) -> Unit = { navigator.openDetail(TopicRoute(it.id)) }
    val quiz: (Topic) -> Unit = { topic ->
        topic.number?.let { navigator.open(TopicQuizRoute(it, topic.id, topic.title)) }
    }
    val shownTopic = (navigator.shownDetail as? TopicRoute)?.topicId
    val besideDetail = LocalBesideDetail.current

    fun selectStage(selected: String) {
        level = selected
        query = ""
        bookmarksOnly = false
        reviewsOnly = false
    }

    // A shared detail pane follows the topic's stage, including links from another lesson.
    LaunchedEffect(shownTopic, besideDetail, book) {
        if (besideDetail) book.topic(shownTopic.orEmpty())?.takeIf { it.kind == TopicKind.Lesson }?.let { level = it.level }
    }
    LaunchedEffect(shownTopic, besideDetail, book, filtering, activeStage?.level, showNext, state.reviews.isNotEmpty()) {
        val id = shownTopic ?: return@LaunchedEffect
        if (!besideDetail) return@LaunchedEffect
        val topic = book.topic(id) ?: return@LaunchedEffect
        if (!filtering && topic.kind == TopicKind.Lesson && topic.level != activeStage?.level) {
            return@LaunchedEffect
        }
        val position = (if (filtering) found else route).indexOfFirst { it.id == id }
        if (position < 0) return@LaunchedEffect
        val key = if (filtering) "f-$id" else "route-$id"
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        val viewport = listState.layoutInfo.viewportEndOffset
        if (visible != null && visible.offset >= 0 && visible.offset + visible.size <= viewport) return@LaunchedEffect
        val preceding = if (filtering) 3 else 4 + (if (showNext) 1 else 0) + (if (state.reviews.isNotEmpty()) 1 else 0)
        listState.requestScrollToItem(preceding + position, scrollOffset = -viewport / 3)
    }

    LazyColumn(
        Modifier.fillMaxSize().paperSurface().imePadding(), state = listState,
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp),
    ) {
        item(key = "header") {
            Box(Modifier.reveal(reveal, 0)) {
                ScreenHeader("Грамматика", eyebrow = "The Grammar Atlas", compact = true,
                    trailing = { SettingsShortcut(navigator::openSettings) })
            }
        }
        if (state.loading) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { AlbionLogo(size = 36.dp, color = Ink.Faint) } }
            return@LazyColumn
        }
        if (book.lessons.isEmpty()) {
            item { Panel(Modifier.padding(top = 24.dp)) { EmptyState("Учебник ещё не добавлен", "Материалы грамматики пока недоступны.") } }
            return@LazyColumn
        }
        if (!filtering) {
            item(key = "atlas") {
                GrammarAtlas(stages, activeStage?.level, state.progress, book.lessons.size,
                    book.lessons.count { state.progress[it.id]?.masteredAt != null }, ::selectStage)
            }
            if (showNext) item(key = "next") {
                GrammarNextCard(nextStep, onClick = {
                    if (nextStep.kind == GrammarNextStep.Kind.Read) open(nextStep.topic) else quiz(nextStep.topic)
                }, onRead = { open(nextStep.topic) })
            }
        }
        item(key = "search") {
            Column(Modifier.padding(top = 18.dp)) {
                SearchField(query, { query = it }, "Найти правило или пример…")
                GrammarViewTabs(if (bookmarksOnly) 1 else if (reviewsOnly) 2 else if (!filtering) 0 else -1, due.size) { selected ->
                    when (selected) {
                        0 -> { query = ""; bookmarksOnly = false; reviewsOnly = false }
                        1 -> { bookmarksOnly = !bookmarksOnly; reviewsOnly = false }
                        2 -> { reviewsOnly = !reviewsOnly; bookmarksOnly = false }
                    }
                }
            }
        }
        if (filtering) {
            item(key = "results") { Eyebrow("Найдено: ${found.size}", Modifier.padding(vertical = 12.dp)) }
            items(found, key = { "f-${it.id}" }) { topic ->
                GrammarSearchResult(topic, state.progress[topic.id], q, now, topic.id == shownTopic, open)
                if (reviewsOnly && topic.number in state.quizTopics) {
                    GhostButton("Повторить тему ${topic.number}", onClick = { quiz(topic) }, modifier = Modifier.padding(start = 36.dp))
                }
            }
            if (found.isEmpty()) item {
                val noBookmarks = bookmarksOnly && book.topics.none { state.progress[it.id]?.bookmarked == true }
                val noReviews = reviewsOnly && due.isEmpty()
                EmptyState(if (noReviews) "Все повторения выполнены" else if (noBookmarks) "Пока нет закладок" else "Темы не найдены",
                    if (noReviews) "Новые темы появятся здесь по плану повторения." else if (noBookmarks) "Сохраните нужное правило кнопкой закладки в уроке." else "Попробуйте другое слово или пример.")
            }
        } else {
            if (state.reviews.isNotEmpty()) item(key = "review") {
                GrammarReviewCard(state.reviews, due, now, reviewDetails, { reviewDetails = !reviewDetails },
                    open = open, onQueue = { reviewsOnly = true; bookmarksOnly = false; query = "" })
            }
            activeStage?.let { stage ->
                item(key = "stage-${stage.level}") {
                    GrammarStageHeading(stage, stages.indexOf(stage), stage.topics.count { state.progress[it.id]?.masteredAt != null },
                        stage.topics.count { state.progress[it.id]?.readAt != null })
                }
                itemsIndexed(route, key = { _, topic -> "route-${topic.id}" }) { index, topic ->
                    GrammarRouteLesson(topic, state.progress[topic.id], current = topic.id == recommendation?.id,
                        selected = topic.id == shownTopic, first = index == 0, last = index == route.lastIndex,
                        chapter = chapterByTopic[topic.id], now = now, onClick = { open(topic) })
                }
                val nextStage = stages.getOrNull(stages.indexOf(stage) + 1)
                if (nextStage != null) item(key = "next-stage") {
                    CatalogueCard("Следующий этап", "${nextStage.level} · ${nextStage.title}", nextStage.name, onClick = {
                        selectStage(nextStage.level)
                        scope.launch { if (reduced) listState.scrollToItem(1) else listState.animateScrollToItem(1) }
                    }, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
                } else item {
                    FolioDivider(Modifier.padding(vertical = 22.dp), label = "ALBION")
                }
            }
            if (book.references.isNotEmpty()) {
                item { SectionHeader("Справочники", trailing = "%02d".format(book.references.size)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(book.references, key = { it.id }) { ref -> ReferenceCard(ref) { open(ref) } }
                    }
                }
            }
            book.topics.firstOrNull { it.kind == TopicKind.Contents }?.let { contents ->
                item {
                    Row(Modifier.fillMaxWidth().clickable { open(contents) }.padding(vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Как устроен курс", style = MaterialTheme.typography.bodyMedium, color = Ink.Soft, modifier = Modifier.weight(1f))
                        Icon(AlbionIcons.ArrowForward, null, tint = Ink.Soft, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarReviewCard(
    reviews: List<GrammarReview.Item>, due: List<GrammarReview.Item>, now: Long, expanded: Boolean,
    onExpand: () -> Unit, open: (Topic) -> Unit, onQueue: () -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        SectionHeader("Повторение грамматики", trailing = due.size.toString())
        Panel(contentPadding = PaddingValues(16.dp)) {
            if (due.isNotEmpty()) {
                Text("${due.size} ${com.marat.albion.ui.library.pluralRu(due.size, "тема", "темы", "тем")} ждут повторения.",
                    style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { open(due.first().topic) }, modifier = Modifier.weight(1f)) { Text("Открыть правило", color = Ink.AccentText) }
                    TextButton(onClick = onQueue, modifier = Modifier.weight(1f)) { Text("Очередь (${due.size})", color = Ink.Fg) }
                }
            } else {
                val next = reviews.first()
                Text("Следующее повторение: ${grammarReviewDate(next.dueAt, now)}", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                Text(next.topic.title, style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = { open(next.topic) }) { Text("Открыть тему", color = Ink.AccentText) }
            }
            TextButton(onClick = onExpand, modifier = Modifier.semantics { stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто" }) {
                Text("Как работает повторение", color = Ink.Fg, modifier = Modifier.weight(1f, fill = false))
                Icon(AlbionIcons.ExpandMore, null, tint = Ink.Muted, modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f))
            }
            AnimatedVisibility(expanded) {
                Text("Сначала повторяются темы с самой ранней датой. Полный тест обновляет план. " +
                    "Интервалы после успешных тестов: 1, 3, 7, 14, 30 и 60 дней. Менее 70% — повтор через день. " +
                    "Досрочный тест и повтор ошибок не увеличивают интервал.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
        }
    }
}

@Composable
private fun ReferenceCard(topic: Topic, onClick: () -> Unit) {
    Panel(onClick = onClick, modifier = Modifier.width(180.dp).heightIn(min = 124.dp), contentPadding = PaddingValues(16.dp)) {
        Text("§", style = AlbionType.index.copy(color = Ink.GoldText, fontSize = 22.sp))
        Spacer(Modifier.height(12.dp))
        Text(topic.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(topic.title), fontSize = 22.sp, lineHeight = 27.sp), color = Ink.Text)
    }
}

@Composable
private fun GrammarSearchResult(
    topic: Topic, progress: GrammarProgressEntity?, query: String, now: Long, selected: Boolean, open: (Topic) -> Unit,
) {
    Column(Modifier.fillMaxWidth().selectedRow(selected).clickable { open(topic) }.padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(topic.number?.toString() ?: "§", style = AlbionType.index, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.bodyLarge, color = Ink.Fg)
                val status = when { progress?.masteredAt != null -> "Освоено"; progress?.readAt != null -> "Прочитано"; else -> null }
                if (status != null) Text(status, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                GrammarReview.plan(progress)?.let { Text("Повторение: ${grammarReviewDate(it.dueAt, now)}", style = MaterialTheme.typography.bodySmall, color = Ink.Soft) }
            }
            Spacer(Modifier.width(8.dp))
            if (progress?.bookmarked == true) Icon(AlbionIcons.Bookmark, null, tint = Ink.Accent, modifier = Modifier.size(14.dp))
            LevelBadge(topic.level)
        }
        if (query.isNotBlank()) Text(GrammarSearch.snippet(topic, query), style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
            modifier = Modifier.padding(start = 36.dp, top = 7.dp))
        Hairline(Modifier.padding(top = 14.dp))
    }
}

