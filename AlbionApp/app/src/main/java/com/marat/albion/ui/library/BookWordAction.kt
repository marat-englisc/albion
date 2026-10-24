package com.marat.albion.ui.library

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.marat.albion.ui.components.Mascot
import com.marat.albion.ui.components.Finn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.BookWordDraft
import com.marat.albion.data.library.BookWordSource
import com.marat.albion.data.library.BookWordsRepository
import com.marat.albion.data.library.DictEntry
import com.marat.albion.data.library.SavedBookWord
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun BookWordAction(
    text: String,
    cards: List<CardEntity>,
    entries: List<DictEntry>,
    translation: String?,
    source: BookWordSource,
    example: String,
    onOpenDeck: () -> Unit,
) {
    val c = LocalAppContainer.current
    val exact = cards.firstOrNull { BookWordsRepository.key(it.term) == BookWordsRepository.key(text) }
    val term = exact?.term ?: entries.firstOrNull()?.headword ?: cards.firstOrNull()?.term ?: text
    val matching = cards.filter { BookWordsRepository.key(it.term) == BookWordsRepository.key(term) }
    val prototype = matching.firstOrNull { it.deck == Deck.WORDS.id } ?: matching.firstOrNull()
    val meanings = (matching.map { it.meaningRu } + entries.filter {
        BookWordsRepository.key(it.headword) == BookWordsRepository.key(term)
    }.flatMap { it.senses } + listOfNotNull(translation)).filter { it.isNotBlank() }
        .distinctBy(BookWordsRepository::key)
    val existing by remember(term) { c.bookWords.observeTerm(term) }.collectAsStateWithLifecycle(emptyList())
    var editing by remember(text) { mutableStateOf(false) }
    var saved by remember(text) { mutableStateOf<SavedBookWord?>(null) }

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            if (existing.isEmpty()) "Добавить в «Слова из книг»" else "Добавить другое значение",
            onClick = { editing = true }, icon = Icons.Rounded.Add,
        )
        saved?.let { result ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Финн убирает новое слово в свою тетрадь.
                if (result.added) {
                    Mascot(Finn.WORD_SAVED, 64.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (result.added) "Добавлено: ${result.card.term}" else "Это слово с таким переводом уже есть в колоде.",
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
            }
        }
        if (existing.isNotEmpty() || saved != null) {
            TextButton(onClick = onOpenDeck) { Text("Открыть «Слова из книг»", color = Ink.Fg) }
        }
    }
    if (editing) {
        BookWordEditor(term, meanings, prototype, source, example,
            onDismiss = { editing = false }, onSaved = { saved = it; editing = false })
    }
}

@Composable
private fun BookWordEditor(
    initialTerm: String,
    meanings: List<String>,
    prototype: CardEntity?,
    source: BookWordSource,
    initialExample: String,
    onDismiss: () -> Unit,
    onSaved: (SavedBookWord) -> Unit,
) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var term by rememberSaveable { mutableStateOf(initialTerm) }
    var meaning by rememberSaveable { mutableStateOf(meanings.firstOrNull().orEmpty()) }
    var example by rememberSaveable { mutableStateOf(initialExample) }
    var choices by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = term.isNotBlank() && term.length <= 200 && meaning.isNotBlank() && meaning.length <= 2000 && example.length <= 4000

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Ink.Elevated,
        title = { Text("Слово из книги", style = MaterialTheme.typography.headlineSmall, color = Ink.Fg) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(term, { term = it }, label = { Text("Слово или выражение") },
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), isError = term.length > 200)
                OutlinedTextField(meaning, { meaning = it }, label = { Text("Перевод") },
                    enabled = !saving, modifier = Modifier.fillMaxWidth(), isError = meaning.length > 2000)
                if (meanings.size > 1) {
                    Box {
                        TextButton(onClick = { choices = true }, enabled = !saving) { Text("Выбрать значение", color = Ink.Fg) }
                        DropdownMenu(choices, onDismissRequest = { choices = false }, containerColor = Ink.Elevated) {
                            meanings.forEach { option ->
                                DropdownMenuItem(text = { Text(option, maxLines = 4, overflow = TextOverflow.Ellipsis) },
                                    onClick = { meaning = option; choices = false })
                            }
                        }
                    }
                }
                OutlinedTextField(example, { example = it }, label = { Text("Пример из книги") },
                    enabled = !saving, modifier = Modifier.fillMaxWidth(), isError = example.length > 4000)
                Text(listOf(source.title, source.chapter).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                val warning = when {
                    term.length > 200 -> "Сократите слово или выражение до 200 символов."
                    meaning.length > 2000 -> "Сократите перевод до 2000 символов."
                    example.length > 4000 -> "Сократите пример до 4000 символов."
                    else -> error
                }
                warning?.let { Text(it, color = Ink.Fg, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !saving, onClick = {
                saving = true
                error = null
                scope.launch {
                    try {
                        val sameTerm = prototype != null && BookWordsRepository.key(term) == BookWordsRepository.key(prototype.term)
                        val sameMeaning = sameTerm && BookWordsRepository.key(meaning) == BookWordsRepository.key(prototype.meaningRu)
                        onSaved(c.bookWords.add(BookWordDraft(term, meaning, example, source,
                            ipa = if (sameTerm) prototype.ipa else "",
                            pos = if (sameTerm) prototype.pos else "word",
                            meaningEn = if (sameMeaning) prototype.meaningEn else "",
                            level = if (sameTerm) prototype.level else null)))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = "Не удалось сохранить слово. Попробуйте ещё раз."
                    } finally {
                        saving = false
                    }
                }
            }) { Text(if (saving) "Сохраняю…" else "Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена", color = Ink.Muted) } },
    )
}
