package com.marat.albion.ui.grammar

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.marat.albion.data.grammar.Topic
import com.marat.albion.data.grammar.TopicKind
import com.marat.albion.ui.browse.SearchField
import com.marat.albion.ui.browse.TextTab
import com.marat.albion.ui.components.EmptyState
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.ScreenHeader
import com.marat.albion.ui.components.SettingsShortcut
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.TopicRoute
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
)

class GrammarViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<GrammarHomeState> = combine(
        flow { emit(c.grammar.book()) },
        c.grammar.observeProgress(),
        c.settings.settings,
    ) { book, progress, s ->
        GrammarHomeState(false, book, progress.associateBy { it.topicId }, s.lastGrammarTopic)
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
    var openChapters by rememberSaveable { mutableStateOf(listOf<String>()) }
    val reveal = rememberReveal()

    val book = state.book
    val lessons = book.lessons
    val readCount = lessons.count { state.progress[it.id]?.readAt != null }
    val filtering = query.isNotBlank() || level != null || bookmarksOnly
    val open: (Topic) -> Unit = { navigator.open(TopicRoute(it.id)) }

    LazyColumn(
        Modifier.fillMaxSize().background(Ink.Bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        item {
            Box(Modifier.reveal(reveal, 0)) {
                ScreenHeader("Грамматика", eyebrow = "Учебник · от нуля до C1", trailing = { SettingsShortcut(navigator::openSettings) })
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

        item {
            Column(Modifier.padding(top = 24.dp).reveal(reveal, 1)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedNumber(readCount, AlbionType.number, color = Ink.Fg)
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
                            if (inLevel.isNotEmpty()) LevelMeter(lv, inLevel.count { state.progress[it.id]?.readAt != null }, inLevel.size)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                ThinProgress(if (lessons.isEmpty()) 0f else readCount.toFloat() / lessons.size)

                val continueTopic = state.lastTopic?.let(book::topic)?.takeIf { it.kind == TopicKind.Lesson }
                    ?: lessons.firstOrNull { state.progress[it.id]?.readAt == null }
                if (continueTopic != null) {
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(
                        text = "${continueTopic.number}. ${continueTopic.title}",
                        onClick = { open(continueTopic) },
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(top = 18.dp).reveal(reveal, 2)) {
                SearchField(query, { query = it }, "Найти тему…")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    TextTab("Все", level == null && !bookmarksOnly) { level = null; bookmarksOnly = false }
                    levels.forEach { lv -> TextTab(lv, level == lv) { level = if (level == lv) null else lv } }
                    TextTab("Закладки", bookmarksOnly) { bookmarksOnly = !bookmarksOnly }
                }
            }
        }

        if (!filtering) {
            val refs = book.references
            if (refs.isNotEmpty()) {
                item { SectionHeader("Справочники", Modifier.reveal(reveal, 3), trailing = "%02d".format(refs.size)) }
                item {
                    LazyRow(Modifier.reveal(reveal, 4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(refs, key = { it.id }) { ref -> ReferenceCard(ref) { open(ref) } }
                    }
                }
            }
            book.topics.firstOrNull { it.kind == TopicKind.Contents }?.let { contents ->
                item {
                    Text(
                        "Как устроен учебник и условные обозначения →",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.Soft,
                        modifier = Modifier.fillMaxWidth().clickable { open(contents) }.padding(vertical = 16.dp).reveal(reveal, 5),
                    )
                }
            }

            item { SectionHeader("Главы", Modifier.reveal(reveal, 6), trailing = "%02d".format(book.chapters.size)) }
            book.chapters.forEachIndexed { ci, chapter ->
                val isOpen = chapter.title in openChapters
                val done = chapter.topics.count { state.progress[it.id]?.readAt != null }
                item(key = "chapter-${chapter.title}") {
                    ChapterRow(ci + 1, chapter.title, done, chapter.topics.size, isOpen, Modifier.animateItem().reveal(reveal, 7 + ci)) {
                        openChapters = if (isOpen) openChapters - chapter.title else openChapters + chapter.title
                    }
                }
                if (isOpen) {
                    items(chapter.topics, key = { "t-${chapter.title}-${it.id}" }) { t ->
                        TopicRow(t, state.progress[t.id], Modifier.animateItem(), onClick = { open(t) })
                    }
                }
            }
        } else {
            val q = query.trim().lowercase()
            val found = book.topics.filter { t ->
                (t.kind == TopicKind.Lesson || t.kind == TopicKind.Reference) &&
                    (q.isEmpty() || t.title.lowercase().contains(q) || t.number?.toString() == q) &&
                    (level == null || t.level == level) &&
                    (!bookmarksOnly || state.progress[t.id]?.bookmarked == true)
            }
            item { Eyebrow("Найдено: ${found.size}", Modifier.padding(vertical = 8.dp)) }
            items(found, key = { "f-${it.id}" }) { t -> TopicRow(t, state.progress[t.id], Modifier.animateItem(), onClick = { open(t) }) }
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
                Text(if (done == total) "Пройдено" else "$done из $total", style = MaterialTheme.typography.bodySmall, color = if (done == total) Ink.Fg else Ink.Muted)
            }
            Icon(Icons.Rounded.ExpandMore, null, tint = Ink.Muted, modifier = Modifier.rotate(arrow))
        }
        ThinProgress(done.toFloat() / total.coerceAtLeast(1))
    }
}

@Composable
private fun TopicRow(t: Topic, progress: GrammarProgressEntity?, modifier: Modifier, onClick: () -> Unit) {
    val read = progress?.readAt != null
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 36.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.number?.takeIf { t.kind == TopicKind.Lesson }?.toString() ?: "§",
                style = AlbionType.index,
                modifier = Modifier.width(34.dp),
            )
            Text(
                t.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (read) Ink.Muted else Ink.Text,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress?.bookmarked == true) {
                Icon(Icons.Rounded.Bookmark, null, tint = Ink.Fg, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
            }
            LevelBadge(t.level)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(18.dp)
                    .background(if (read) Ink.Fg else Ink.Bg, CircleShape)
                    .border(1.dp, if (read) Ink.Fg else Ink.LineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (read) Icon(Icons.Rounded.Check, null, tint = Ink.Bg, modifier = Modifier.size(12.dp))
            }
        }
        Hairline(Modifier.padding(start = 36.dp))
    }
}

