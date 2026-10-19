package com.marat.albion.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.DictEntry
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
    onDismiss: () -> Unit,
    /** Выделение, которого касается фрагмент; null — ещё не выделено. */
    highlight: HighlightEntity? = null,
    /** Создать выделение (с заметкой или без); null — выделять нельзя. */
    onHighlight: ((String) -> Unit)? = null,
    onNote: (HighlightEntity, String) -> Unit = { _, _ -> },
    onDeleteHighlight: (HighlightEntity) -> Unit = {},
) {
    val c = LocalAppContainer.current
    val audio = rememberCardAudio(c.audio)
    val model by c.translator.model.collectAsStateWithLifecycle()
    var result by remember(text) { mutableStateOf<TranslationResult>(TranslationResult.Loading) }
    var cards by remember(text) { mutableStateOf<List<CardEntity>>(emptyList()) }
    var entries by remember(text) { mutableStateOf<List<DictEntry>?>(null) }
    var glosses by remember(text) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(text) {
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
            onFailure = { TranslationResult.Failed(it.message ?: "Ошибка перевода") },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Raised,
        contentColor = Ink.Text,
        scrimColor = Ink.Bg.copy(alpha = 0.4f),
        dragHandle = { Hairline(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp), color = Ink.LineStrong) },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(if (isWord) "Слово" else "Фрагмент", Modifier.weight(1f))
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
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text,
                style = TextStyle(fontFamily = Serif, fontSize = if (isWord) 34.sp else 20.sp, lineHeight = if (isWord) 40.sp else 29.sp),
                color = Ink.Fg,
            )
            if (onHighlight != null) {
                HighlightSection(highlight, onHighlight, onNote, onDeleteHighlight)
            }
            Spacer(Modifier.height(18.dp))
            Hairline()

            if (isWord) {
                Spacer(Modifier.height(16.dp))
                Eyebrow("Словарь")
                Spacer(Modifier.height(8.dp))
                val list = entries
                when {
                    list == null -> Text("Ищу…", style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                    list.isEmpty() -> Text("Во встроенном словаре этого слова нет.", style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                    else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        list.forEach { e -> DictionaryArticle(e, showHeadword = !e.headword.equals(text, ignoreCase = true)) }
                    }
                }
            }

            if (result != TranslationResult.Skipped) {
                Spacer(Modifier.height(if (isWord) 20.dp else 16.dp))
                Eyebrow(if (isWord) "Машинный перевод · ML Kit" else "Перевод · ML Kit")
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = result,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "translation",
                ) { r ->
                    when (r) {
                        TranslationResult.Loading, TranslationResult.Skipped -> Column {
                            Text(
                                if (model == ModelState.Downloading) "Модель перевода фраз загружается в фоне — перевод появится, как только она будет готова."
                                else "Перевожу…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink.Muted,
                            )
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Ink.Fg, trackColor = Ink.Line)
                        }
                        is TranslationResult.Done -> Text(
                            r.text,
                            style = TextStyle(fontFamily = Serif, fontSize = if (isWord) 22.sp else 19.sp, lineHeight = 28.sp),
                            color = Ink.Text,
                        )
                        is TranslationResult.Failed -> Text(r.message, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
                    }
                }
            }

            if (glosses.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Eyebrow("По словам · встроенный словарь")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    glosses.forEach { (w, t) ->
                        Row {
                            Text(w, style = TextStyle(fontFamily = Serif, fontSize = 17.sp), color = Ink.Fg, modifier = Modifier.width(120.dp))
                            Text(t, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                        }
                    }
                }
            }

            if (cards.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Eyebrow("В ваших карточках")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.forEach { card -> DictionaryEntry(card) }
                }
            }
        }
    }
}

private val ARTICLES = setOf("a", "an", "the")
private val WORD = Regex("[A-Za-z][A-Za-z'’-]*")

@Composable
private fun DictionaryArticle(entry: DictEntry, showHeadword: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showHeadword) Text(entry.headword, style = TextStyle(fontFamily = Serif, fontSize = 20.sp), color = Ink.Fg)
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
    Column(Modifier.fillMaxWidth().border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.term, style = TextStyle(fontFamily = Serif, fontSize = 20.sp), color = Ink.Fg)
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
        if (card.meaningRu.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(card.meaningRu, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
        }
        if (card.meaningEn.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(card.meaningEn, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
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
            Column(Modifier.fillMaxWidth().background(Ink.Fg.copy(alpha = 0.06f), RoundedCornerShape(4.dp)).padding(14.dp)) {
                Eyebrow("Выделено")
                Spacer(Modifier.height(6.dp))
                Text(highlight.text, style = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp), color = Ink.Text, maxLines = 4)
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
                placeholder = { Text("Ваша заметка", color = Ink.Faint) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink.Fg, unfocusedBorderColor = Ink.LineStrong, cursorColor = Ink.Fg,
                    focusedTextColor = Ink.Fg, unfocusedTextColor = Ink.Text,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                editing -> {
                    SmallAction("Сохранить", Icons.Rounded.Check) {
                        if (highlight != null) onNote(highlight, draft) else { onHighlight(draft); justAdded = true }
                        editing = false
                    }
                    SmallAction("Отмена", Icons.Rounded.Close) { editing = false; draft = highlight?.note.orEmpty() }
                }
                highlight != null -> {
                    SmallAction(if (highlight.note.isBlank()) "Добавить заметку" else "Изменить заметку", Icons.Rounded.EditNote) { editing = true }
                    SmallAction("Убрать", Icons.Rounded.Close) { onDelete(highlight); justAdded = false }
                }
                justAdded -> Eyebrow("Выделено", Modifier.padding(vertical = 8.dp))
                else -> {
                    SmallAction("Выделить", Icons.Rounded.BorderColor) { onHighlight(""); justAdded = true }
                    SmallAction("Заметка", Icons.Rounded.EditNote) { editing = true }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.border(1.dp, Ink.LineStrong, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ink.Fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink.Fg)
    }
}
