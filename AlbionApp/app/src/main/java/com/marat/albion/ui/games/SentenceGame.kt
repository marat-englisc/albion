package com.marat.albion.ui.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.games.Verdict
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif
import com.marat.albion.ui.theme.pressScale

/**
 * «Собери предложение»: плашки со словами примера перемешаны внизу, касание переносит
 * слово в ответ, касание слова в ответе — возвращает его. Подсказка ставит следующее
 * верное слово, но такой ответ уже не засчитывается как «без ошибок».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceRound(vm: GameViewModel, card: CardEntity) {
    val puzzle = remember(card.id) { vm.puzzle(card) } ?: return
    val c = LocalAppContainer.current
    val audio = rememberCardAudio(c.audio)
    val order = remember(card.id) { vm.shuffledOrder(card, puzzle.tokens.size) }
    // Индексы плашек (в puzzle.tokens) в том порядке, как их поставил пользователь.
    val placed = remember(card.id) { mutableStateListOf<Int>() }
    var verdict by remember(card.id) { mutableStateOf<Verdict?>(null) }
    var hinted by remember(card.id) { mutableStateOf(false) }
    var shake by remember(card.id) { mutableIntStateOf(0) }
    val answer = placed.map { puzzle.tokens[it] }
    val wrong = if (verdict == Verdict.Wrong) puzzle.wrongPositions(answer) else emptySet()
    val speakKey = "sentence:${card.id}"

    fun check() {
        if (verdict != null) return
        if (placed.size < puzzle.tokens.size) { shake++; return }
        val ok = puzzle.isCorrect(answer)
        verdict = if (ok) Verdict.Correct else Verdict.Wrong
        if (!ok) shake++
        val clean = ok && !hinted
        vm.record(clean, if (clean) null else Mistake(card.term, if (ok) "с подсказкой" else answer.joinToString(" ") + puzzle.ending, puzzle.original))
        c.audio.speak(puzzle.original, speakKey)
    }

    /** Убирает неверно поставленный хвост и добавляет следующее верное слово. */
    fun hint() {
        if (verdict != null) return
        hinted = true
        val prefix = answer.zip(puzzle.tokens).takeWhile { (a, b) -> a.equals(b, ignoreCase = true) }.size
        while (placed.size > prefix) placed.removeAt(placed.lastIndex)
        val need = puzzle.tokens.getOrNull(prefix) ?: return
        order.firstOrNull { it !in placed && puzzle.tokens[it].equals(need, ignoreCase = true) }?.let { placed += it }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
            Eyebrow("Соберите предложение")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(card.term)
                if (card.meaningRu.isNotBlank()) {
                    Text(card.meaningRu, style = MaterialTheme.typography.bodySmall, color = Ink.Muted, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
                }
            }
            Spacer(Modifier.height(22.dp))

            // Ответ: поставленные плашки на линиях «тетради».
            Column(Modifier.fillMaxWidth().heightIn(min = 132.dp).shakeOn(shake).animateContentSize(tween(220))) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    placed.forEachIndexed { pos, tokenIndex ->
                        WordTile(
                            text = puzzle.tokens[tokenIndex],
                            style = when {
                                verdict == Verdict.Correct -> TileStyle.Correct
                                pos in wrong -> TileStyle.Wrong
                                else -> TileStyle.Placed
                            },
                            onClick = { if (verdict == null) placed.removeAt(pos) },
                        )
                    }
                    if (puzzle.ending.isNotEmpty() && placed.size == puzzle.tokens.size) {
                        Text(puzzle.ending, style = TileText, color = Ink.Muted, modifier = Modifier.padding(top = 9.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Hairline(color = Ink.LineStrong)
            }

            AnimatedContent(
                targetState = verdict != null,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
                label = "pool",
            ) { answered ->
                if (!answered) {
                    // Запас слов: поставленные остаются пустыми местами, чтобы плашки не прыгали.
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        order.forEach { tokenIndex ->
                            val used = tokenIndex in placed
                            WordTile(
                                text = puzzle.tokens[tokenIndex],
                                style = if (used) TileStyle.Ghost else TileStyle.Idle,
                                onClick = { if (!used) placed += tokenIndex },
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                        val v = verdict ?: Verdict.Wrong
                        VerdictLine(v)
                        if (v == Verdict.Correct && hinted) {
                            Spacer(Modifier.height(4.dp))
                            Text("Собрано с подсказкой — в итоге не засчитано как «без ошибок».", style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                puzzle.original,
                                style = TextStyle(fontFamily = Serif, fontSize = 22.sp, lineHeight = 31.sp),
                                color = Ink.Fg,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            AudioButton(
                                playing = audio.playingKey == speakKey,
                                onClick = { c.audio.speak(puzzle.original, speakKey) },
                                size = 40.dp,
                                primary = false,
                            )
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = verdict != null,
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            label = "actions",
            modifier = Modifier.padding(vertical = 14.dp),
        ) { answered ->
            if (answered) {
                PrimaryButton("Дальше", onClick = { vm.next() }, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
            } else {
                GameAnswerActions(onCheck = { check() }, onSecondary = { hint() }, secondaryLabel = "Подсказка")
            }
        }
    }
}

private enum class TileStyle { Idle, Placed, Ghost, Correct, Wrong }

private val TileText = TextStyle(fontFamily = Serif, fontSize = 19.sp, lineHeight = 24.sp)

/** Плашка со словом. Ghost — пустое место, откуда слово уже взято. */
@Composable
private fun WordTile(text: String, style: TileStyle, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        when (style) {
            TileStyle.Correct -> Ink.Fg
            TileStyle.Placed -> Ink.Elevated
            else -> Ink.Bg
        },
        tween(240), label = "tileBg",
    )
    val fg by animateColorAsState(
        when (style) {
            TileStyle.Correct -> Ink.Bg
            TileStyle.Wrong -> Ink.Muted
            else -> Ink.Fg
        },
        tween(240), label = "tileFg",
    )
    val border = when (style) {
        TileStyle.Idle -> Ink.LineStrong
        TileStyle.Placed -> Ink.LineStrong
        TileStyle.Ghost -> Ink.Line
        TileStyle.Correct -> Ink.Fg
        TileStyle.Wrong -> Ink.Muted
    }
    Box(
        Modifier
            .pressScale(interaction, 0.94f)
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .clickable(interaction, indication = null, enabled = style == TileStyle.Idle || style == TileStyle.Placed, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = TileText.copy(textDecoration = if (style == TileStyle.Wrong) TextDecoration.LineThrough else null),
            color = fg,
            modifier = Modifier.alpha(if (style == TileStyle.Ghost) 0f else 1f),
        )
    }
}
