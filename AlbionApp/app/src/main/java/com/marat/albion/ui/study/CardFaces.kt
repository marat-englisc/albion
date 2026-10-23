package com.marat.albion.ui.study

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.data.content.Deck
import com.marat.albion.data.content.FormVariant
import com.marat.albion.data.content.IrregularData
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.library.BookWordsRepository
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.theme.rememberReveal
import com.marat.albion.ui.theme.reveal

/** Озвучка, доступная лицевой и оборотной сторонам. */
interface CardAudio {
    val playingKey: String?
    fun play(deck: Deck, file: String?, text: String, key: String)
}

private fun deckOf(card: CardEntity) = Deck.byId(card.deck) ?: Deck.WORDS

/** Термин набран антиквой; длинные идиомы и коллокации — мельче. */
private fun termStyle(term: String, large: Boolean): TextStyle {
    val size = when {
        !large -> if (term.length <= 18) 30 else 24
        term.length <= 10 -> 54
        term.length <= 18 -> 42
        term.length <= 28 -> 34
        else -> 28
    }
    return TextStyle(fontFamily = Serif, fontSize = size.sp, lineHeight = (size * 1.15f).sp, letterSpacing = (-size / 40f).sp)
}

@Composable
private fun TermHeader(card: CardEntity, audio: CardAudio, large: Boolean, showIpa: Boolean = true) {
    val deck = deckOf(card)
    val key = "term:${card.id}"
    Column(horizontalAlignment = if (large) Alignment.CenterHorizontally else Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(card.pos)
            LevelBadge(card.level)
        }
        Spacer(Modifier.height(if (large) 28.dp else 16.dp))
        Text(card.term, style = termStyle(card.term, large), textAlign = if (large) TextAlign.Center else TextAlign.Start, color = Ink.Fg)
        Spacer(Modifier.height(if (large) 14.dp else 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showIpa && card.ipa.isNotBlank()) {
                Text(card.ipa, style = AlbionType.ipa, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(14.dp))
            }
            AudioButton(
                playing = audio.playingKey == key,
                onClick = { audio.play(deck, card.audio, card.term, key) },
                size = if (large) 46.dp else 38.dp,
            )
        }
    }
}

@Composable
fun CardFront(card: CardEntity, irregular: IrregularData?, audio: CardAudio, showIpa: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TermHeader(card, audio, large = true, showIpa = showIpa)
        if (irregular != null) {
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                irregular.forms.drop(1).forEachIndexed { i, form ->
                    Column(
                        Modifier.border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("V${i + 2}", style = AlbionType.index)
                        Spacer(Modifier.height(2.dp))
                        Text("?", style = TextStyle(fontFamily = Serif, fontSize = 26.sp), color = Ink.Faint)
                        Spacer(Modifier.height(2.dp))
                        Eyebrow(form.label, color = Ink.Faint)
                    }
                }
            }
        }
    }
}

/** Оборот. Секции проявляются каскадом сразу после переворота. */
@Composable
fun CardBack(card: CardEntity, irregular: IrregularData?, audio: CardAudio, modifier: Modifier = Modifier, showRussian: Boolean = true) {
    val reveal = rememberReveal()
    var i = 0
    Column(modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp)) {
        TermHeader(card, audio, large = false)

        if (irregular != null) {
            Spacer(Modifier.height(22.dp))
            IrregularForms(card, irregular, audio, Modifier.reveal(reveal, ++i, step = 70))
        }

        Spacer(Modifier.height(18.dp))
        Hairline(Modifier.reveal(reveal, ++i, step = 70))
        Spacer(Modifier.height(16.dp))

        if (card.meaningEn.isNotBlank()) {
            Column(Modifier.reveal(reveal, ++i, step = 70)) {
                Eyebrow("Meaning")
                Spacer(Modifier.height(8.dp))
                Text(card.meaningEn, style = MaterialTheme.typography.bodyLarge, color = Ink.Fg)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (showRussian && card.meaningRu.isNotBlank()) {
            Column(Modifier.reveal(reveal, ++i, step = 25)) {
                Eyebrow("Значение")
                Spacer(Modifier.height(6.dp))
                Text(card.meaningRu, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp), color = Ink.Soft)
            }
        }

        if (irregular != null) {
            irregular.forms.forEachIndexed { fi, form ->
                if (form.examples.isEmpty()) return@forEachIndexed
                Column(Modifier.reveal(reveal, ++i, step = 70)) {
                    Spacer(Modifier.height(20.dp))
                    Eyebrow("Examples · V${fi + 1} ${form.label}")
                    Spacer(Modifier.height(8.dp))
                    form.examples.forEach { ExampleLine(it, form.variants.map(FormVariant::text) + card.term) }
                }
            }
            if (irregular.note.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                Column(
                    Modifier.fillMaxWidth().reveal(reveal, ++i, step = 70)
                        .border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(14.dp),
                ) {
                    Eyebrow("Примечание")
                    Spacer(Modifier.height(6.dp))
                    Text(irregular.note, style = MaterialTheme.typography.bodyMedium, color = Ink.Soft)
                }
            }
        } else {
            val examples = listOf(card.example1, card.example2).filter { it.isNotBlank() }
            if (examples.isNotEmpty()) {
                Column(Modifier.reveal(reveal, ++i, step = 70)) {
                    Spacer(Modifier.height(20.dp))
                    Eyebrow("Examples")
                    Spacer(Modifier.height(8.dp))
                    examples.forEach { ExampleLine(it, listOf(card.term)) }
                }
            }
        }
        val source = remember(card.id, card.extra) { BookWordsRepository.source(card) }
        if (source != null) {
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Eyebrow("Из книги")
            Text(listOf(source.title, source.chapter).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IrregularForms(card: CardEntity, data: IrregularData, audio: CardAudio, modifier: Modifier) {
    val deck = deckOf(card)
    Column(modifier.fillMaxWidth().border(1.dp, Ink.Line, RoundedCornerShape(6.dp))) {
        data.forms.forEachIndexed { index, form ->
            if (index > 0) Hairline()
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("V${index + 1}", style = AlbionType.index.copy(color = Ink.Fg, fontSize = 18.sp))
                    Text(form.label, style = MaterialTheme.typography.labelSmall, color = Ink.Muted, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    form.variants.forEach { v ->
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column {
                                Text(v.text, style = TextStyle(fontFamily = Serif, fontSize = 20.sp), color = Ink.Fg)
                                if (v.ipa.isNotBlank()) Text(v.ipa, style = AlbionType.ipa.copy(fontSize = 14.sp))
                            }
                            v.audio.ifEmpty { listOf(null) }.forEachIndexed { k, file ->
                                val key = "form:${card.id}:${form.label}:${v.text}:$k"
                                AudioButton(
                                    playing = audio.playingKey == key,
                                    onClick = { audio.play(deck, file, v.text, key) },
                                    size = 32.dp,
                                    primary = k == 0,
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
private fun ExampleLine(text: String, highlight: List<String>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text("—", style = MaterialTheme.typography.bodyLarge, color = Ink.Faint)
        Spacer(Modifier.width(12.dp))
        Text(highlightTerms(text, highlight), style = MaterialTheme.typography.bodyLarge, color = Ink.Soft, modifier = Modifier.weight(1f))
    }
}

/**
 * Выделяет в примере изучаемое выражение. Сначала ищем фразу целиком,
 * затем — отдельные слова по основе (account for → accounts … for).
 */
fun highlightTerms(text: String, terms: List<String>): AnnotatedString {
    val ranges = mutableListOf<IntRange>()
    for (term in terms.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
        val phrase = Regex("(?<![\\p{L}])" + Regex.escape(term) + "(?![\\p{L}])", RegexOption.IGNORE_CASE)
        val found = phrase.findAll(text).map { it.range }.toList()
        if (found.isNotEmpty()) { ranges += found; continue }
        term.split(' ', '-').filter { it.length >= 3 && it.lowercase() !in STOP }.forEach { token ->
            val stem = if (token.length > 4 && token.endsWith("e")) token.dropLast(1) else token
            Regex("(?<![\\p{L}])" + Regex.escape(stem) + "\\p{L}*", RegexOption.IGNORE_CASE)
                .findAll(text).forEach { ranges += it.range }
        }
    }
    if (ranges.isEmpty()) return AnnotatedString(text)
    val merged = ranges.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { acc, r ->
        val last = acc.lastOrNull()
        if (last != null && r.first <= last.last + 1) acc[acc.lastIndex] = last.first..maxOf(last.last, r.last) else acc += r
        acc
    }
    return buildAnnotatedString {
        var pos = 0
        for (r in merged) {
            append(text.substring(pos, r.first))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink.Fg)) { append(text.substring(r.first, r.last + 1)) }
            pos = r.last + 1
        }
        append(text.substring(pos))
    }
}

private val STOP = setOf("the", "and", "one", "someone", "something", "sth", "sb", "your", "you", "out", "off", "for", "with", "into")
