package com.marat.albion.ui.library

import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.theme.QuietAnimatedVisibility as AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.data.db.HighlightEntity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.DictEntry
import com.marat.albion.data.library.BookWordSource
import com.marat.albion.data.library.ModelState
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif

private sealed interface TranslationResult {
    data object Loading : TranslationResult
    data object Skipped : TranslationResult
    data class Done(val text: String) : TranslationResult
    data class Failed(val message: String) : TranslationResult
}

/**
 * Перевод слова или выделенного фрагмента. Слово сразу ищется во встроенном словаре
 * и в ваших карточках (с транскрипцией и озвучкой) — это работает без интернета.
 * Фразы переводит ML Kit; пока его модель не загружена, фрагмент разбирается по словам.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSheet(
    text: String,
    isWord: Boolean,
    selectionControls: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    /** Выделение, которого касается фрагмент; null — ещё не выделено. */
    highlight: HighlightEntity? = null,
    /** Создать выделение (с заметкой или без); null — выделять нельзя. */
    onHighlight: ((String) -> Unit)? = null,
    onNote: (HighlightEntity, String) -> Unit = { _, _ -> },
    onDeleteHighlight: (HighlightEntity) -> Unit = {},
    bookSource: BookWordSource? = null,
    bookExample: String = "",
    onOpenBookWords: () -> Unit = {},
    onBookWordSaved: (com.marat.albion.data.library.SavedBookWord) -> Unit = {},
) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val audio = rememberCardAudio(c.audio)
    val model by c.translator.model.collectAsStateWithLifecycle()
    var result by remember(text) { mutableStateOf<TranslationResult>(TranslationResult.Loading) }
    var cards by remember(text) { mutableStateOf<List<CardEntity>>(emptyList()) }
    var entries by remember(text) { mutableStateOf<List<DictEntry>?>(null) }
    var glosses by remember(text) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var details by rememberSaveable(text) { mutableStateOf(false) }

    var retry by remember(text) { mutableStateOf(0) }
    var chosen by remember(text) { mutableStateOf(0) }
    var choiceMenu by remember { mutableStateOf(false) }
    LaunchedEffect(text, retry, model) {
        if (isWord) {
            cards = c.library.lookup(text)
            entries = c.dictionary.lookup(text)
            // Для слова машинный перевод — дополнение: не ждём загрузки модели, если есть словарь.
            if (model != ModelState.Ready && (entries.orEmpty().isNotEmpty() || cards.isNotEmpty())) {
                result = TranslationResult.Skipped
                return@LaunchedEffect
            }
        }
        // Пока модели фраз нет, сразу разбираем фрагмент по словам встроенным словарём.
        if (!isWord && model != ModelState.Ready) {
            glosses = WORD.findAll(text).map { it.value }.distinctBy { it.lowercase() }
                .filter { it.lowercase() !in ARTICLES }.take(16).toList().mapNotNull { w ->
                    // Две первые группы значений, по два слова — коротко, но без случайного «узкого» смысла.
                    c.dictionary.lookup(w).firstOrNull()?.let { e ->
                        w to e.senses.take(2).joinToString("; ") { it.split(", ").take(2).joinToString(", ") }
                    }
                }
        }
        result = runCatching { c.translator.translate(text) }.fold(
            onSuccess = { TranslationResult.Done(it) },
            onFailure = { if (it is kotlinx.coroutines.CancellationException) throw it; TranslationResult.Failed("Не удалось получить перевод. Проверьте загрузку модели и повторите попытку.") },
        )
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = { com.marat.albion.ui.components.SheetHandle() },
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Raised,
        contentColor = Ink.Text,
        scrimColor = Ink.Fg.copy(alpha = 0.32f),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(if (isWord) "Перевод слова" else "Перевод фрагмента", Modifier.weight(1f))
                AudioButton(
                    playing = audio.playingKey == "read:$text",
                    onClick = {
                        val card = cards.firstOrNull { it.audio != null }
                        if (isWord && card != null) audio.play(Deck.byId(card.deck) ?: Deck.WORDS, card.audio, card.term, "read:$text")
                        else c.audio.speak(text, "read:$text")
                    },
                    size = 38.dp,
                    primary = false,
                )
                IconButton(onClick = onDismiss) {
                    Icon(AlbionIcons.Close, "Закрыть перевод", tint = Ink.Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text,
                style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = if (isWord) 34.sp else 20.sp, lineHeight = if (isWord) 40.sp else 29.sp),
                color = Ink.Fg,
            )
            val dictionary = entries.orEmpty()
            selectionControls?.invoke()
            val options = dictionary.flatMap { entry -> entry.senses.filter { it.isNotBlank() }.map { entry to it } }
                .distinctBy { it.first.headword.lowercase() to it.second.lowercase() }
            val selected = options.getOrNull(chosen) ?: options.firstOrNull()
            val firstEntry = selected?.first
            val dictionaryMeaning = selected?.second
            if (options.size > 1) Box {
                TextButton(onClick = { choiceMenu = true }) { Text("Выбрать значение · ${options.size}", color = Ink.AccentText) }
                DropdownMenu(choiceMenu, { choiceMenu = false }, containerColor = Ink.Raised) {
                    options.forEachIndexed { i, option -> DropdownMenuItem(text = {
                        Text("${if (chosen == i) "✓ " else ""}${option.first.headword} · ${option.first.posLabel} — ${option.second}")
                    }, onClick = { chosen = i; choiceMenu = false }) }
                }
            }
            val cardMeaning = cards.firstOrNull { it.meaningRu.isNotBlank() }?.meaningRu
            val automatic = (result as? TranslationResult.Done)?.text?.takeIf { it.isNotBlank() }
            val ipa = cards.firstOrNull { it.ipa.isNotBlank() }?.ipa
            if (isWord && (firstEntry?.posLabel?.isNotBlank() == true || ipa != null)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)) {
                    firstEntry?.posLabel?.takeIf { it.isNotBlank() }?.let { Chip(it) }
                    if (ipa != null) Text(ipa, style = AlbionType.ipa)
                }
            }
            // Значение видно раньше сохранения и заметок, даже если перевод фраз ещё загружается.
            val meaning = dictionaryMeaning ?: cardMeaning ?: automatic
            Spacer(Modifier.height(12.dp))
            if (meaning != null) {
                Text(meaning, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = 22.sp, lineHeight = 30.sp), color = Ink.Text)
                Text(when {
                    dictionaryMeaning != null -> "Словарь"
                    cardMeaning != null -> "Из ваших карточек"
                    else -> "Автоперевод"
                }, style = MaterialTheme.typography.bodySmall, color = Ink.Muted,
                    modifier = Modifier.padding(top = 6.dp))
            } else {
                when (val r = result) {
                    TranslationResult.Loading -> {
                        Text(if (isWord && entries == null) "Ищу перевод…"
                            else if (model == ModelState.Downloading) "Загружается перевод предложений…" else "Перевожу…",
                            style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp).height(2.dp),
                            color = Ink.AccentText, trackColor = Ink.Line)
                    }
                    is TranslationResult.Failed -> {
                        com.marat.albion.ui.components.FieldError(r.message)
                        TextButton(onClick = { result = TranslationResult.Loading; retry++ }) { Text("Повторить перевод") }
                    }
                    else -> Text("Перевод не найден. Значение можно указать при сохранении.",
                        style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                }
                if (model == ModelState.Missing || model == ModelState.Failed) TextButton(onClick = { scope.launch { c.translator.ensureModel() } }) { Text("Загрузить перевод предложений") }
                if (glosses.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Eyebrow("Перевод по словам")
                    WordGlosses(glosses.take(4))
                }
            }

            if (bookSource != null || onHighlight != null) {
                Spacer(Modifier.height(16.dp))
                Hairline()
                if (bookSource != null) {
                    BookWordAction(text, cards, dictionary, automatic, bookSource, bookExample, onOpenBookWords,
                        suggestedTerm = if (dictionaryMeaning != null) requireNotNull(firstEntry).headword
                            else cards.firstOrNull { it.meaningRu.isNotBlank() }?.term ?: text,
                        suggestedMeaning = meaning, onSaved = onBookWordSaved)
                }
                if (onHighlight != null) HighlightSection(highlight, onHighlight, onNote, onDeleteHighlight)
            }

            val hasDetails = dictionary.sumOf { it.senses.size } > 1 || cards.isNotEmpty() ||
                (isWord && automatic != null && !automatic.equals(meaning, ignoreCase = true)) ||
                (meaning == null && glosses.size > 4) ||
                dictionary.any { !it.headword.equals(text, ignoreCase = true) }
            if (hasDetails) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { details = !details }, modifier = Modifier.fillMaxWidth().semantics {
                    stateDescription = if (details) "Развёрнуто" else "Свёрнуто"
                }) {
                    Text("Другие значения и подробности", modifier = Modifier.weight(1f), color = Ink.AccentText)
                    Icon(AlbionIcons.ExpandMore, null, tint = Ink.AccentText,
                        modifier = Modifier.padding(start = 8.dp).rotate(if (details) 180f else 0f))
                }
                AnimatedVisibility(details) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                        dictionary.forEach { DictionaryArticle(it, showHeadword = !it.headword.equals(text, ignoreCase = true)) }
                        if (isWord && automatic != null && !automatic.equals(meaning, ignoreCase = true)) {
                            Column {
                                Eyebrow("Автоперевод")
                                Text(automatic, style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
                            }
                        }
                        if (meaning == null && glosses.size > 4) WordGlosses(glosses.drop(4))
                        if (cards.isNotEmpty()) {
                            Eyebrow("В ваших карточках")
                            cards.forEach { DictionaryEntry(it) }
                        }
                    }
                }
            }
        }
    }
}

private val ARTICLES = setOf("a", "an", "the")
private val WORD = Regex("[A-Za-z][A-Za-z'’-]*")

@Composable
private fun WordGlosses(glosses: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        glosses.forEach { (word, meaning) ->
            Column {
                Text(word, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = 17.sp), color = Ink.Fg)
                Text(meaning, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
            }
        }
    }
}

@Composable
private fun DictionaryArticle(entry: DictEntry, showHeadword: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showHeadword) Text(entry.headword, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = 20.sp), color = Ink.Fg)
            if (entry.posLabel.isNotEmpty()) Chip(entry.posLabel)
        }
        Spacer(Modifier.height(6.dp))
        entry.senses.forEachIndexed { i, sense ->
            Row(Modifier.padding(vertical = 2.dp)) {
                if (entry.senses.size > 1) Text("${i + 1}", style = AlbionType.index, color = Ink.Faint, modifier = Modifier.width(20.dp).padding(top = 3.dp))
                Text(sense, style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
            }
        }
    }
}

@Composable
private fun DictionaryEntry(card: CardEntity) {
    Column(Modifier.fillMaxWidth().border(1.dp, Ink.Line, RoundedCornerShape(8.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.term, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = 20.sp), color = Ink.Fg)
            Spacer(Modifier.width(10.dp))
            if (card.ipa.isNotBlank()) Text(card.ipa, style = AlbionType.ipa.copy(fontSize = 15.sp))
            Spacer(Modifier.weight(1f))
            LevelBadge(card.level)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(card.pos)
            Chip(Deck.byId(card.deck)?.title ?: card.deck)
        }
        Spacer(Modifier.height(12.dp))
        com.marat.albion.ui.components.WordMeanings(card, showRussian = true, showDefinitions = true)
    }
}

/**
 * Выделение и заметка: «Выделить» / «Заметка» для нового фрагмента; для уже выделенного —
 * цитата, заметка, правка и удаление.
 */
@Composable
private fun HighlightSection(
    highlight: HighlightEntity?,
    onHighlight: (String) -> Unit,
    onNote: (HighlightEntity, String) -> Unit,
    onDelete: (HighlightEntity) -> Unit,
) {
    var editing by remember(highlight?.id) { mutableStateOf(false) }
    var draft by remember(highlight?.id) { mutableStateOf(highlight?.note.orEmpty()) }
    // Только что созданное выделение — показываем «Выделено», пока не пришла запись из БД.
    var justAdded by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    // Поле заметки сразу получает фокус — клавиатура открывается без лишнего касания.
    LaunchedEffect(editing) { if (editing) runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        if (highlight != null) {
            Column(Modifier.fillMaxWidth().background(Ink.Fg.copy(alpha = 0.06f), RoundedCornerShape(8.dp)).padding(14.dp)) {
                Eyebrow("Выделено")
                Spacer(Modifier.height(6.dp))
                Text(highlight.text, style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp), color = Ink.Text, maxLines = 4)
                if (highlight.note.isNotBlank() && !editing) {
                    Spacer(Modifier.height(8.dp))
                    Text(highlight.note, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                }
            }
        }
        if (editing) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Ваша заметка") },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink.Fg, unfocusedBorderColor = Ink.LineStrong, cursorColor = Ink.Fg,
                    focusedTextColor = Ink.Fg, unfocusedTextColor = Ink.Text,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                editing -> {
                    SmallAction("Сохранить", AlbionIcons.Check) {
                        if (highlight != null) onNote(highlight, draft) else { onHighlight(draft); justAdded = true }
                        editing = false
                    }
                    SmallAction("Отмена", AlbionIcons.Close) { editing = false; draft = highlight?.note.orEmpty() }
                }
                highlight != null -> {
                    SmallAction(if (highlight.note.isBlank()) "Добавить заметку" else "Изменить заметку", AlbionIcons.EditNote) { editing = true }
                    SmallAction("Удалить выделение", AlbionIcons.Delete) { onDelete(highlight); justAdded = false }
                }
                justAdded -> Eyebrow("Выделено", Modifier.padding(vertical = 8.dp))
                else -> {
                    SmallAction("Выделить", AlbionIcons.BorderColor) { onHighlight(""); justAdded = true }
                    SmallAction("Заметка", AlbionIcons.EditNote) { editing = true }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.border(1.dp, Ink.LineStrong, RoundedCornerShape(4.dp)).clickable(onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ink.Fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Fg)
    }
}
