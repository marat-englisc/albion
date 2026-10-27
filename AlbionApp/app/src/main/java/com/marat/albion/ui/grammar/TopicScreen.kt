package com.marat.albion.ui.grammar

import com.marat.albion.ui.theme.paperSurface
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.CatalogueTab
import com.marat.albion.ui.theme.headingFamily
import androidx.compose.foundation.background
import com.marat.albion.ui.theme.pressScale
import com.marat.albion.data.grammar.MdRule
import com.marat.albion.data.grammar.MdParagraph
import com.marat.albion.data.grammar.MdBlock
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.grammar.GrammarBook
import com.marat.albion.data.grammar.GrammarReview
import com.marat.albion.data.grammar.GrammarQuizSelection
import com.marat.albion.data.db.GrammarProgressEntity
import com.marat.albion.data.grammar.MdDocument
import com.marat.albion.data.grammar.MdHeading
import com.marat.albion.data.grammar.Topic
import com.marat.albion.data.grammar.TopicKind
import com.marat.albion.data.settings.Settings
import com.marat.albion.ui.components.AlbionLogo
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.ThinSlider
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.ThinProgress
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.nav.GrammarRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.nav.TopicRoute
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.flow.first
import com.marat.albion.ui.nav.TopicQuizRoute
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal
import kotlinx.coroutines.launch

@Composable
fun TopicScreen(topicId: String, navigator: Navigator, fromQuiz: Boolean = false) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var book by remember { mutableStateOf<GrammarBook?>(null) }
    var doc by remember(topicId) { mutableStateOf<MdDocument?>(null) }
    var missing by remember(topicId) { mutableStateOf(false) }
    val settings by c.settings.settings.collectAsStateWithLifecycle(Settings())
    val progress by remember(topicId) { c.grammar.observeProgress(topicId) }.collectAsStateWithLifecycle(null)
    var quizCount by remember(topicId) { mutableStateOf(0) }

    LaunchedEffect(topicId) {
        val b = c.grammar.book()
        book = b
        val topic = b.topic(topicId)
        doc = if (topic?.kind == TopicKind.Contents) b.intro else c.grammar.document(topicId)
        missing = doc == null
        if (topic?.kind == TopicKind.Lesson) {
            topic.number?.let { n -> quizCount = c.study.observeTopicTaskCount(n).first() }
            c.grammar.markOpened(topicId)
            c.settings.setLastGrammarTopic(topicId)
        }
    }

    val topic = book?.topic(topicId)
    val listState = rememberLazyListState()
    var tocMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    // Строку «← Предыдущая тема · Оглавление · Следующая тема →» из заметки
    // показываем не текстом, а рядом стрелок внизу темы.
    val (blocks, navLinks) = remember(doc) { splitNavigation(doc?.blocks.orEmpty()) }
    val headingIndex = remember(blocks) {
        blocks.withIndex().filter { (it.value as? MdHeading)?.level == 2 }.map { it.index to (it.value as MdHeading).text }
    }
    val reveal = rememberReveal()

    val env = remember(settings.grammarTextScale, book) {
        MdEnv(settings.grammarTextScale) { target ->
            val id = target.substringBefore('#').trim()
            val b = book
            when {
                b?.topic(id)?.kind == TopicKind.Contents -> navigator.open(GrammarRoute)
                b?.topic(id) != null -> navigator.open(TopicRoute(id))
            }
        }
    }

    // Отмечаем тему прочитанной, когда пользователь долистал до конца.
    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(reachedEnd, topic) {
        if (reachedEnd && topic?.kind == TopicKind.Lesson && progress?.readAt == null && blocks.isNotEmpty()) {
            c.grammar.setRead(topicId, true)
        }
    }

    val readProgress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount <= 1) 0f
            else ((listState.firstVisibleItemIndex + info.visibleItemsInfo.size).toFloat() / info.totalItemsCount).coerceIn(0f, 1f)
        }
    }

    Column(Modifier.fillMaxSize().paperSurface()) {
        TopBar(onBack = navigator::back) {
            if (headingIndex.isNotEmpty()) {
                Box {
                    com.marat.albion.ui.components.RoundIconButton(AlbionIcons.List, "Разделы темы", { tocMenu = true })
                    DropdownMenu(expanded = tocMenu, onDismissRequest = { tocMenu = false }, containerColor = Ink.Raised) {
                        headingIndex.forEach { (index, title) ->
                            DropdownMenuItem(
                                text = { Text(title, color = Ink.Text) },
                                onClick = {
                                    tocMenu = false
                                    scope.launch { listState.animateScrollToItem(index + 1) }
                                },
                            )
                        }
                    }
                }
            }
            Box {
                com.marat.albion.ui.components.RoundIconButton(AlbionIcons.TextFields, "Размер текста", { sizeMenu = true })
                DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }, containerColor = Ink.Raised) {
                    Column(Modifier.width(240.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Eyebrow("Размер текста · ${(settings.grammarTextScale * 100).toInt()}%")
                        ThinSlider(
                            value = settings.grammarTextScale,
                            onValueChange = { v -> scope.launch { c.settings.setGrammarTextScale(v) } },
                            valueRange = 0.85f..1.4f,
                            steps = 10,
                        )
                    }
                }
            }
            if (topic?.kind == TopicKind.Lesson || topic?.kind == TopicKind.Reference) {
                com.marat.albion.ui.components.RoundIconButton(
                    if (progress?.bookmarked == true) AlbionIcons.Bookmark else AlbionIcons.BookmarkBorder, "Закладка",
                    { scope.launch { c.grammar.toggleBookmark(topicId) } },
                    tint = if (progress?.bookmarked == true) Ink.AccentText else Ink.Fg,
                )
            }
        }
        ThinProgress(readProgress, track = Ink.Bg)

        when {
            missing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Тема не найдена", color = Ink.Muted) }
            doc == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AlbionLogo(size = 36.dp, color = Ink.Faint) }
            else -> ProvideMdEnv(env) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "header") { TopicHeader(topic, doc!!, Modifier.reveal(reveal, 0),
                            book?.lessons.orEmpty().filter { it.number in topic?.prerequisites.orEmpty() }) }
                        itemsIndexed(blocks, key = { i, _ -> "b$i" }) { i, block ->
                            MdBlockView(block, Modifier.reveal(reveal, 1 + i, step = 45))
                        }
                        if (topic?.kind == TopicKind.Lesson) {
                            item(key = "footer") {
                                val prev = book?.previous(topicId)
                                val next = book?.next(topicId)
                                val number = topic.number
                                TopicFooter(
                                    quizCount = quizCount,
                                    onQuiz = if (fromQuiz) navigator::back else number?.let { n -> { navigator.open(TopicQuizRoute(n, topicId, topic.title)) } },
                                    fromQuiz = fromQuiz,
                                    progress = progress,
                                    onToggleRead = { scope.launch { c.grammar.setRead(topicId, progress?.readAt == null) } },
                                    onPrev = prev?.let { t -> { navigator.replace(TopicRoute(t.id)) } },
                                    onContents = { navigator.open(GrammarRoute) },
                                    onNext = next?.let { t -> { navigator.replace(TopicRoute(t.id)) } },
                                )
                            }
                        } else if (navLinks != null) {
                            item(key = "nav") {
                                NavArrows(
                                    onPrev = navLinks.prev?.let { t -> { env.onWikiLink(t) } },
                                    onContents = navLinks.contents?.let { t -> { env.onWikiLink(t) } },
                                    onNext = navLinks.next?.let { t -> { env.onWikiLink(t) } },
                                    modifier = Modifier.padding(top = 24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicHeader(topic: Topic?, doc: MdDocument, modifier: Modifier, prerequisites: List<Topic>) {
    Column(modifier.padding(top = 8.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatalogueTab(
                when (topic?.kind) {
                    TopicKind.Reference -> "Справочник"
                    TopicKind.Contents -> "О курсе"
                    TopicKind.Sources -> "Источники"
                    else -> "Урок %03d".format(topic?.number ?: 0)
                },
            )
            Spacer(Modifier.width(12.dp))
            LevelBadge(topic?.level)
        }
        Spacer(Modifier.height(14.dp))
        val title = (doc.title ?: topic?.title.orEmpty()).removePrefix("Справочник — ")
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontFamily = headingFamily(title)), color = Ink.Fg)
        if (prerequisites.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            InlineText("Сначала: " + prerequisites.joinToString(" · ") { "[[${it.id}|${it.number}. ${it.title}]]" },
                style = MaterialTheme.typography.bodySmall.copy(color = Ink.Muted))
        }
        Spacer(Modifier.height(16.dp))
        Hairline(color = Ink.Gold.copy(alpha = .65f))
    }
}

@Composable
private fun TopicFooter(
    quizCount: Int,
    onQuiz: (() -> Unit)?,
    fromQuiz: Boolean,
    progress: GrammarProgressEntity?,
    onToggleRead: () -> Unit,
    onPrev: (() -> Unit)?,
    onContents: () -> Unit,
    onNext: (() -> Unit)?,
) {
    Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        val read = progress?.readAt != null
        val mastered = progress?.masteredAt != null
        val hasQuiz = quizCount > 0 && onQuiz != null
        val review = GrammarReview.plan(progress)
        val now = grammarReviewTime()
        if (hasQuiz) {
            Column(
                Modifier.fillMaxWidth()

                    .border(1.dp, Ink.LineStrong, RoundedCornerShape(12.dp)).padding(20.dp),
            ) {
                Eyebrow("Проверь себя", color = Ink.Soft)
                Spacer(Modifier.height(10.dp))
                Text(
                    "В тесте — ${minOf(quizCount, GrammarQuizSelection.SIZE)} из $quizCount заданий. " +
                        "Для отметки «Освоено» нужно не менее 70% верных ответов. При новой попытке набор меняется.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.Muted,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(when {
                    fromQuiz -> "Вернуться к тесту"
                    review != null -> "Повторить полный тест"
                    else -> "Начать проверку"
                }, onClick = onQuiz, trailingIcon = AlbionIcons.ArrowForward)
            }
        }
        if (progress?.quizAt != null) {
            Text(
                (if (mastered) "Тема освоена. " else "Освоение пока не подтверждено. ") +
                    "Последняя полная проверка: ${progress.quizCorrect} из ${progress.quizTotal} верно.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (mastered) Ink.Fg else Ink.Muted,
            )
        }
        if (review != null && hasQuiz) {
            Text("Следующее повторение: ${grammarReviewDate(review.dueAt, now)}. " +
                "Успешные полные тесты увеличивают интервал до 60 дней; менее 70% — повтор через день.",
                style = MaterialTheme.typography.bodyMedium, color = Ink.Soft)
        }
        GhostButton(if (read) "Тема прочитана" else "Отметить как прочитанную", onClick = onToggleRead, icon = AlbionIcons.Check)
        NavArrows(onPrev, onContents, onNext)
    }
}

/** Навигация между темами: ← в контурном круге · оглавление · → в белом круге. */
@Composable
private fun NavArrows(onPrev: (() -> Unit)?, onContents: (() -> Unit)?, onNext: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundArrow(AlbionIcons.West, "Предыдущая тема", filled = false, onClick = onPrev)
        Hairline(Modifier.weight(1f).padding(horizontal = 14.dp))
        if (onContents != null) {
            val interaction = remember { MutableInteractionSource() }
            Icon(
                AlbionIcons.GridView,
                contentDescription = "Оглавление",
                tint = Ink.Soft,
                modifier = Modifier.size(44.dp).pressScale(interaction, 0.88f)
                    .clip(CircleShape)
                    .clickable(interactionSource = interaction, indication = null, onClick = onContents)
                    .padding(12.dp),
            )
            Hairline(Modifier.weight(1f).padding(horizontal = 14.dp))
        }
        RoundArrow(AlbionIcons.East, "Следующая тема", filled = true, onClick = onNext)
    }
}

@Composable
private fun RoundArrow(icon: ImageVector, description: String, filled: Boolean, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(58.dp)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .background(if (filled && enabled) Ink.Fg else Ink.Bg)
            .border(1.dp, if (enabled) (if (filled) Ink.Fg else Ink.LineStrong) else Ink.Line, CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = when {
                !enabled -> Ink.Faint
                filled -> Ink.Bg
                else -> Ink.Fg
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

private data class NavLinks(val prev: String?, val contents: String?, val next: String?)

private val NAV_LINK = Regex("\\[\\[([^\\]|]+)\\|([^\\]]+)]]")
private val NAV_LINE = Regex("^\\s*(\\[\\[[^\\]]+]]\\s*·?\\s*)+$")

/** Отделяет от заметки финальную строку навигации (и линию перед ней). */
private fun splitNavigation(blocks: List<MdBlock>): Pair<List<MdBlock>, NavLinks?> {
    val last = blocks.lastOrNull() as? MdParagraph ?: return blocks to null
    if (!NAV_LINE.matches(last.text)) return blocks to null
    var prev: String? = null
    var contents: String? = null
    var next: String? = null
    NAV_LINK.findAll(last.text).forEach { m ->
        val target = m.groupValues[1].trim()
        val alias = m.groupValues[2]
        when {
            "←" in alias || alias.contains("Предыдущ", true) -> prev = target
            "→" in alias || alias.contains("Следующ", true) -> next = target
            else -> contents = target
        }
    }
    if (prev == null && next == null && contents == null) return blocks to null
    var rest = blocks.dropLast(1)
    if (rest.lastOrNull() == MdRule) rest = rest.dropLast(1)
    return rest to NavLinks(prev, contents, next)
}
