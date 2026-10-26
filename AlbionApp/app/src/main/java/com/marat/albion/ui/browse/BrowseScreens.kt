package com.marat.albion.ui.browse

import com.marat.albion.ui.components.gutter
import com.marat.albion.ui.components.peekingFinn
import com.marat.albion.ui.components.Finn
import com.marat.albion.ui.components.selectedRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.marat.albion.ui.theme.pressScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.fsrs.CardState
import com.marat.albion.data.fsrs.Fsrs
import com.marat.albion.data.repo.toMemory
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.EmptyState
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.components.SectionHeader
import com.marat.albion.ui.components.TopBar
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.nav.CardRoute
import com.marat.albion.ui.nav.Navigator
import com.marat.albion.ui.study.CardBack
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val filters = listOf("Все", "Новые", "Изучаются", "Повторение", "К повтору")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Composable
fun BrowseScreen(deck: Deck, navigator: Navigator) {
    val c = LocalAppContainer.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val queryFlow = remember { MutableStateFlow(query) }
    val filterFlow = remember { MutableStateFlow(filter) }
    LaunchedEffect(query) { queryFlow.value = query }
    LaunchedEffect(filter) { filterFlow.value = filter }
    val results = remember(deck) {
        combine(queryFlow.debounce(150), filterFlow) { q, f -> q to f }.flatMapLatest { (q, f) -> c.study.browse(deck, q, f) }
    }
    val cards by results.collectAsStateWithLifecycle(emptyList())
    val audio = rememberCardAudio(c.audio)
    val showRussian by remember { c.settings.settings.map { it.showRussian } }.collectAsStateWithLifecycle(true)

    Column(Modifier.fillMaxSize().background(Ink.Bg).imePadding()) {
        TopBar(onBack = navigator::back, title = deck.title)
        SearchField(query, { query = it }, "Поиск по-английски", Modifier.padding(horizontal = gutter))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            filters.forEachIndexed { i, label -> TextTab(label, filter == i) { filter = i } }
        }
        Hairline()
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("Ничего не найдено", "Измените запрос или фильтр.", mascot = Finn.NO_RESULTS) }
        } else {
            val shownCard = (navigator.shownDetail as? CardRoute)?.cardId
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Eyebrow("${cards.size} карточ.", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                }
                items(cards, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        showRussian = showRussian,
                        playing = audio.playingKey == "term:${card.id}",
                        selected = card.id == shownCard,
                        onPlay = { audio.play(deck, card.audio, card.term, "term:${card.id}") },
                        onClick = { navigator.openDetail(CardRoute(card.id)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** Строгое поле поиска: только нижняя линия, которая светлеет при вводе. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val active by animateFloatAsState(if (value.isNotEmpty()) 1f else 0f, label = "search")
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = lerpColor(Ink.Faint, Ink.Fg, active), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Ink.Faint)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink.Fg),
                    cursorBrush = SolidColor(Ink.Fg),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Clear, "Очистить", tint = Ink.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        Hairline(color = lerpColor(Ink.LineStrong, Ink.Fg, active))
    }
}

/** Фильтр — пилюля: выбранная заливается цветом шарфа с пружинным «щелчком». */
@Composable
fun TextTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val t by animateFloatAsState(if (selected) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 500f), label = "tab")
    val interaction = remember { MutableInteractionSource() }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        Modifier
            .pressScale(interaction, 0.93f)
            .background(lerpColor(Ink.Surface, Ink.Accent, t), shape)
            .border(1.dp, lerpColor(Ink.Line, Ink.Accent, t), shape)
            .clickable(interaction, indication = null, role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label.uppercase(), style = com.marat.albion.ui.theme.AlbionType.eyebrow, color = lerpColor(Ink.Soft, Ink.OnAccent, t))
    }
}


@Composable
private fun CardRow(card: CardEntity, showRussian: Boolean, playing: Boolean, selected: Boolean, onPlay: () -> Unit, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().selectedRow(selected).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.term,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Serif),
                        color = Ink.Fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    LevelBadge(card.level)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    listOf(card.ipa, if (showRussian) card.meaningRu else card.meaningEn).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            AudioButton(playing = playing, onClick = onPlay, size = 36.dp, primary = false)
        }
        Hairline(Modifier.padding(horizontal = gutter))
    }
}

@Composable
fun CardDetailScreen(cardId: String, navigator: Navigator) {
    val c = LocalAppContainer.current
    val card by remember(cardId) { c.study.observeCard(cardId) }.collectAsStateWithLifecycle(null)
    val state by remember(cardId) { c.study.observeState(cardId) }.collectAsStateWithLifecycle(null)
    val settings by c.settings.settings.collectAsStateWithLifecycle(com.marat.albion.data.settings.Settings())
    val audio = rememberCardAudio(c.audio)
    val scope = rememberCoroutineScope()
    var confirmRemove by rememberSaveable(cardId) { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    var removeError by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Ink.Bg)) {
        TopBar(onBack = navigator::back)
        val current = card ?: return@Column
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp),
        ) {
            Panel(
                Modifier.padding(top = 46.dp).peekingFinn(at = 0.86f, height = 52.dp, delayMs = 450),
                contentPadding = PaddingValues(0.dp), color = Ink.Surface, borderColor = Ink.LineStrong,
            ) {
                CardBack(current, c.study.irregular(current), audio, showRussian = settings.showRussian)
            }
            SectionHeader("Память · FSRS", Modifier.padding(horizontal = 4.dp))
            Column(Modifier.padding(horizontal = 4.dp)) {
                val s = state
                if (s == null) {
                    Text("Карточка ещё не изучалась.", style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                } else {
                    val r = Fsrs(w = Fsrs.decodeParams(settings.fsrsParams) ?: Fsrs.DEFAULT_W).retrievability(s.toMemory(), System.currentTimeMillis())
                    InfoRow("Статус", stateName(s.state))
                    InfoRow("Следующий показ", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.due)))
                    InfoRow("Стабильность", Fsrs.formatInterval((s.stability * Fsrs.DAY).toLong()))
                    InfoRow("Сложность", "%.1f / 10".format(s.difficulty))
                    InfoRow("Вероятность вспомнить", "${(r * 100).toInt()}%")
                    InfoRow("Повторений · забываний", "${s.reps} · ${s.lapses}")
                    Spacer(Modifier.height(18.dp))
                    GhostButton("Сделать новой", onClick = { scope.launch { c.study.resetCard(cardId) } })
                }
                if (current.deck == Deck.BOOK_WORDS.id) {
                    Spacer(Modifier.height(18.dp))
                    GhostButton("Удалить из «Слова из книг»", onClick = { removeError = false; confirmRemove = true })
                }
            }
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { if (!removing) confirmRemove = false },
            containerColor = Ink.Raised,
            title = { Text("Удалить слово?", color = Ink.Fg) },
            text = {
                Text(if (removeError) "Не удалось удалить слово. Попробуйте ещё раз."
                    else "Карточка «${card?.term.orEmpty()}» и история её повторений будут удалены.", color = Ink.Muted)
            },
            confirmButton = {
                TextButton(enabled = !removing, onClick = {
                    removing = true
                    scope.launch {
                        try {
                            c.bookWords.remove(cardId)
                            navigator.back()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            removeError = true
                        } finally {
                            removing = false
                        }
                    }
                }) { Text(if (removing) "Удаляю…" else "Удалить", color = Ink.Fg) }
            },
            dismissButton = { TextButton(enabled = !removing, onClick = { confirmRemove = false }) { Text("Отмена", color = Ink.Muted) } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink.Fg)
        }
        Hairline()
    }
}

private fun stateName(s: Int) = when (s) {
    CardState.LEARNING -> "Изучается"
    CardState.REVIEW -> "Повторение"
    CardState.RELEARNING -> "Переучивается"
    else -> "Новая"
}
