package com.marat.albion.ui.grammar

import com.marat.albion.ui.nav.LocalBesideDetail
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.marat.albion.ui.components.selectedRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marat.albion.AppContainer
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.db.GrammarProgressEntity
import com.marat.albion.data.grammar.GrammarBook
import com.marat.albion.data.grammar.GrammarLearningPath
import com.marat.albion.data.grammar.GrammarSearch
import com.marat.albion.data.grammar.GrammarReview
import com.marat.albion.data.grammar.Topic
import com.marat.albion.data.grammar.TopicKind
import com.marat.albion.ui.browse.SearchField
import com.marat.albion.ui.browse.TextTab
import com.marat.albion.ui.components.EmptyState
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.TopicRoute
import com.marat.albion.ui.nav.TopicQuizRoute
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.AnimatedNumber
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class GrammarHomeState(
    val loading: Boolean = true,
    val book: GrammarBook = GrammarBook.EMPTY,
    val progress: Map<String, GrammarProgressEntity> = emptyMap(),
    val lastTopic: String? = null,
    val reviews: List<GrammarReview.Item> = emptyList(),
)

class GrammarViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<GrammarHomeState> = combine(
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        c.settings.settings,
        c.grammar.observeQuizTopics(),
    ) { book, progress, s, quizTopics ->
        val byId = progress.associateBy { it.topicId }
        GrammarHomeState(false, book, byId, s.lastGrammarTopic, GrammarReview.scheduled(book, byId, quizTopics.toSet()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrammarHomeState())
}

private val levels = listOf("A1", "A2", "B1", "B2", "C1")

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
    var openChapters by rememberSaveable { mutableStateOf(listOf<String>()) }
    val reveal = rememberReveal()
    val listState = rememberLazyListState()
    LaunchedEffect(reviewsOnly) {
        if (reviewsOnly) listState.animateScrollToItem(2)
    }

    val book = state.book
    val lessons = book.lessons
    val readCount = lessons.count { state.progress[it.id]?.readAt != null }
    val masteredCount = lessons.count { state.progress[it.id]?.masteredAt != null }
    val recommendation = GrammarLearningPath.recommend(book.topics,
        state.progress.filterValues { it.masteredAt != null }.keys, level, state.lastTopic)
    LaunchedEffect(book, recommendation?.id) {
        if (openChapters.isEmpty()) book.chapters.firstOrNull { chapter -> chapter.topics.any { it.id == recommendation?.id } }
            ?.let { openChapters = listOf(it.title) }
    }
    val now = grammarReviewTime()
    val due = state.reviews.takeWhile { it.dueAt <= now }
    val dueIds = due.map { it.topic.id }.toSet()
    val filtering = query.isNotBlank() || level != null || bookmarksOnly || reviewsOnly
    val open: (Topic) -> Unit = { navigator.openDetail(TopicRoute(it.id)) }
    val shownTopic = (navigator.shownDetail as? TopicRoute)?.topicId
    val q = query.trim().lowercase()
    val found = if (!filtering) emptyList() else (if (reviewsOnly) due.map { it.topic } else book.topics).filter { t ->
        (t.kind == TopicKind.Lesson || t.kind == TopicKind.Reference) &&
            GrammarSearch.matches(t, q) &&
            (level == null || t.level == level) &&
            (!bookmarksOnly || state.progress[t.id]?.bookmarked == true) &&
            (!reviewsOnly || t.id in dueIds)
    }

    /** Номер строки темы в списке ниже; порядок элементов — тот же, что в LazyColumn. */
    fun topicItemIndex(id: String): Int? {
        var i = 1 // шапка
        if (filtering) {
            i += 2 // поиск, «Найдено»
            return found.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { i + it }
        }
        i += 2 // обзор, поиск
        if (!state.loading && state.reviews.isNotEmpty()) i++ // повторение грамматики
        if (book.topics.any { it.kind == TopicKind.Contents }) i++ // «Как заниматься»
        i++ // «Главы»
        for (chapter in book.chapters) {
            i++
            if (chapter.title !in openChapters) continue
            val pos = chapter.topics.indexOfFirst { it.id == id }
            if (pos >= 0) return i + pos
            i += chapter.topics.size
        }
        return null
    }

    // На планшете тема открыта справа: оглавление держит её строку на виду —
    // раскрывает главу и прокручивает, если тему открыли кнопкой или ссылкой из другой темы.
    val besideDetail = LocalBesideDetail.current
    LaunchedEffect(shownTopic, besideDetail, book, filtering) {
        val id = shownTopic ?: return@LaunchedEffect
        if (!besideDetail) return@LaunchedEffect
        val chapter = book.chapters.firstOrNull { ch -> ch.topics.any { it.id == id } }
        if (!filtering && chapter != null && chapter.title !in openChapters) {
            val before = listState.layoutInfo.totalItemsCount
            openChapters = openChapters + chapter.title
            // Ждём, пока список покажет строки раскрытой главы.
            withTimeoutOrNull(1_000) { snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it != before } }
        }
        val key: Any = if (filtering) "f-$id" else "t-${chapter?.title}-$id"
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        val viewport = listState.layoutInfo.viewportEndOffset
        if (visible != null && visible.offset >= 0 && visible.offset + visible.size <= viewport) return@LaunchedEffect
        val index = topicItemIndex(id) ?: return@LaunchedEffect
        listState.animateScrollToItem(index, scrollOffset = -viewport / 3)
    }
    val quiz: (Topic) -> Unit = { t -> t.number?.let { navigator.open(TopicQuizRoute(it, t.id, t.title)) } }

    LazyColumn(
        Modifier.fillMaxSize().background(Ink.Bg).imePadding(),
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        item(key = "header") {
            Box(Modifier.reveal(reveal, 0)) {
                ScreenHeader("Грамматика", eyebrow = "Короткие уроки · A1–C1", compact = true, trailing = { SettingsShortcut(navigator::openSettings) })
            }
        }

        if (!state.loading && lessons.isEmpty()) {
            item {
                Panel(Modifier.padding(top = 24.dp)) {
                    EmptyState("Учебник ещё не добавлен", "Темы берутся из папки grammar. Включите albion.bundleContent=true и пересоберите приложение.")
                }
            }
            return@LazyColumn
        }

        if (!filtering) item(key = "overview") {
            Column(Modifier.padding(top = 10.dp).reveal(reveal, 1)) {
                Eyebrow("Освоено")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedNumber(masteredCount, AlbionType.numberSmall, color = Ink.Fg)
                        Text(" / ${lessons.size} тем", style = MaterialTheme.typography.bodyMedium, color = Ink.Faint,
                            maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        levels.forEach { lv ->
                            val inLevel = lessons.filter { it.level == lv }
                            if (inLevel.isNotEmpty()) LevelMeter(lv, inLevel.count { state.progress[it.id]?.masteredAt != null }, inLevel.size)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ThinProgress(if (lessons.isEmpty()) 0f else masteredCount.toFloat() / lessons.size)
                Text("Прочитано: $readCount из ${lessons.size}", style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                    modifier = Modifier.padding(top = 6.dp))

                val continueTopic = recommendation
                if (continueTopic != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(if (continueTopic.id == state.lastTopic) "Продолжить урок" else "Следующий шаг",
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(bottom = 6.dp))
                    PrimaryButton(
                        text = "${continueTopic.number}. ${continueTopic.title}",
                        onClick = { open(continueTopic) },
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                }
            }
        }

        item(key = "search") {
            Column(Modifier.padding(top = 18.dp).reveal(reveal, 2)) {
                SearchField(query, { query = it }, "Правило, пример или тема…")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    TextTab("Все", level == null && !bookmarksOnly && !reviewsOnly) { level = null; bookmarksOnly = false; reviewsOnly = false }
                    levels.forEach { lv -> TextTab(lv, level == lv) { level = if (level == lv) null else lv } }
                    TextTab("Закладки", bookmarksOnly) { bookmarksOnly = !bookmarksOnly }
                    TextTab("К повторению (${due.size})", reviewsOnly) { reviewsOnly = !reviewsOnly }
                }
            }
        }

        if (!state.loading && !filtering && state.reviews.isNotEmpty()) item {
            Column(Modifier.padding(top = 8.dp)) {
                SectionHeader("Повторение грамматики", trailing = due.size.toString())
                Panel(contentPadding = PaddingValues(18.dp)) {
                    when {
                        due.isNotEmpty() -> {
                            val first = due.first().topic
                            PrimaryButton("Повторить: ${first.number}. ${first.title}", onClick = { quiz(first) },
                                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { open(first) }, modifier = Modifier.weight(1f)) { Text("Открыть правило", color = Ink.Fg) }
                                TextButton(onClick = {
                                    reviewsOnly = true; query = ""; level = null; bookmarksOnly = false
                                }, modifier = Modifier.weight(1f)) { Text("Очередь (${due.size})", color = Ink.Fg) }
                            }
                        }
                        state.reviews.isNotEmpty() -> {
                            val next = state.reviews.first()
                            Text("Следующее повторение: ${grammarReviewDate(next.dueAt, now)}", style = MaterialTheme.typography.titleMedium, color = Ink.Fg)
                            Text("${next.topic.number}. ${next.topic.title}",
                                style = MaterialTheme.typography.bodyMedium, color = Ink.Muted,
                                modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = { open(next.topic) }) { Text("Открыть тему", color = Ink.Fg) }
                        }
                        else -> {
                            Text("Пройдите тест в конце темы — здесь появится её ближайшее повторение.",
                                style = MaterialTheme.typography.bodyMedium, color = Ink.Muted,
                            )
                        }
                    }
                    TextButton(onClick = { reviewDetails = !reviewDetails }, modifier = Modifier.semantics {
                        stateDescription = if (reviewDetails) "Развёрнуто" else "Свёрнуто"
                    }) {
                        Text("Как работает повторение", color = Ink.Fg, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ExpandMore, null, tint = Ink.Muted,
                            modifier = Modifier.size(18.dp).rotate(if (reviewDetails) 180f else 0f))
                    }
                    AnimatedVisibility(reviewDetails) {
                        Text("Сначала повторяются темы с самой ранней датой. Полный тест обновляет план. " +
                            "Интервалы после успешных тестов: 1, 3, 7, 14, 30 и 60 дней. " +
                            "Менее 70% — повтор через день. Досрочный тест и повтор ошибок не увеличивают интервал.",
                            style = MaterialTheme.typography.bodySmall, color = Ink.Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        if (!filtering) {
            book.topics.firstOrNull { it.kind == TopicKind.Contents }?.let { contents ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { open(contents) }.padding(vertical = 16.dp).reveal(reveal, 5),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Как заниматься · темы по уровням", style = MaterialTheme.typography.bodyMedium,
                            color = Ink.Soft, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null,
                            tint = Ink.Soft, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item { SectionHeader("Главы", Modifier.reveal(reveal, 6), trailing = "%02d".format(book.chapters.size)) }
            book.chapters.forEachIndexed { ci, chapter ->
                val isOpen = chapter.title in openChapters
                val done = chapter.topics.count { state.progress[it.id]?.masteredAt != null }
                item(key = "chapter-${chapter.title}") {
                    ChapterRow(ci + 1, chapter.title, done, chapter.topics.size, isOpen, Modifier.animateItem().reveal(reveal, 7 + ci)) {
                        openChapters = if (isOpen) openChapters - chapter.title else openChapters + chapter.title
                    }
                }
                if (isOpen) {
                    items(chapter.topics, key = { "t-${chapter.title}-${it.id}" }) { t ->
                        TopicRow(t, state.progress[t.id], Modifier.animateItem(), now, selected = t.id == shownTopic, onClick = { open(t) })
                    }
                }
            }
            val refs = book.references
            if (refs.isNotEmpty()) {
                item { SectionHeader("Справочники", trailing = "%02d".format(refs.size)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(refs, key = { it.id }) { ref -> ReferenceCard(ref) { open(ref) } }
                    }
                }
            }
        } else {
            item { Eyebrow("Найдено: ${found.size}", Modifier.padding(vertical = 8.dp)) }
            items(found, key = { "f-${it.id}" }) { t ->
                Column {
                    TopicRow(t, state.progress[t.id], Modifier.animateItem(), now, selected = t.id == shownTopic, onClick = { open(t) })
                    if (q.isNotBlank()) Text(GrammarSearch.snippet(t, q),
                        style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                        modifier = Modifier.padding(start = 36.dp, bottom = 12.dp))
                    if (reviewsOnly) GhostButton("Повторить тему ${t.number}", onClick = { quiz(t) }, modifier = Modifier.padding(start = 36.dp))
                }
            }
            if (found.isEmpty()) item {
                Text(if (reviewsOnly) "Нет тем к повторению с выбранными фильтрами." else "Темы не найдены.",
                    style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
}

@Composable
private fun LevelMeter(level: String, done: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(3.dp).height(28.dp).background(Ink.Line), contentAlignment = Alignment.BottomCenter) {
            val p by animateFloatAsState(done.toFloat() / total, tween(900, easing = Easing.Emphasized), label = "lvl")
            Box(Modifier.width(3.dp).height(28.dp * p).background(Ink.Fg))
        }
        Spacer(Modifier.height(6.dp))
        Text(level, style = MaterialTheme.typography.labelSmall, color = Ink.Muted, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ReferenceCard(topic: Topic, onClick: () -> Unit) {
    Panel(onClick = onClick, modifier = Modifier.width(168.dp).height(112.dp), contentPadding = PaddingValues(16.dp)) {
        Text("§", style = AlbionType.index.copy(color = Ink.Fg, fontSize = 20.sp))
        Spacer(Modifier.weight(1f))
        Text(topic.title, style = MaterialTheme.typography.titleSmall, color = Ink.Text, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChapterRow(index: Int, title: String, done: Int, total: Int, open: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val arrow by animateFloatAsState(if (open) 180f else 0f, tween(360, easing = Easing.Emphasized), label = "chapter")
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth().pressScale(interaction, 0.985f).clickable(interaction, indication = null, onClick = onToggle)) {
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%02d".format(index), style = AlbionType.index, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink.Fg)
                Spacer(Modifier.height(4.dp))
                Text(if (done == total) "Освоено" else "Освоено $done из $total", style = MaterialTheme.typography.bodySmall, color = if (done == total) Ink.Fg else Ink.Muted)
            }
            Icon(Icons.Rounded.ExpandMore, null, tint = Ink.Muted, modifier = Modifier.rotate(arrow))
        }
        ThinProgress(done.toFloat() / total.coerceAtLeast(1))
    }
}

@Composable
private fun TopicRow(t: Topic, progress: GrammarProgressEntity?, modifier: Modifier, now: Long, selected: Boolean, onClick: () -> Unit) {
    val read = progress?.readAt != null
    val mastered = progress?.masteredAt != null
    Column(modifier.fillMaxWidth().selectedRow(selected).clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 7.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.number?.takeIf { t.kind == TopicKind.Lesson }?.toString() ?: "§",
                style = AlbionType.index,
                modifier = Modifier.width(36.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.bodyLarge, color = if (mastered) Ink.Muted else Ink.Text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                val status = when {
                    mastered -> "Освоено"
                    progress?.quizAt != null -> "Проверка: ${progress.quizCorrect} из ${progress.quizTotal}"
                    read -> "Прочитано"
                    else -> null
                }
                if (status != null) Text(status, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                GrammarReview.plan(progress)?.let { plan ->
                    Text("Повторение: ${grammarReviewDate(plan.dueAt, now)}", style = MaterialTheme.typography.bodySmall, color = Ink.Soft)
                }
            }
            if (progress?.bookmarked == true) {
                Icon(Icons.Rounded.Bookmark, null, tint = Ink.Fg, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
            }
            LevelBadge(t.level)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(18.dp)
                    .background(if (mastered) Ink.Fg else Color.Transparent, CircleShape)
                    .border(1.dp, if (mastered) Ink.Fg else Ink.LineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (mastered) Icon(Icons.Rounded.Check, null, tint = Ink.Bg, modifier = Modifier.size(12.dp))
            }
        }
        Hairline()
    }
}

