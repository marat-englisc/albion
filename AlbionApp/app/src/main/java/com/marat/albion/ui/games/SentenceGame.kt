package com.marat.albion.ui.games

import com.marat.albion.ui.components.AlbionIcons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import com.marat.albion.ui.components.LocalNarrow
import com.marat.albion.ui.components.gutter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.Ink
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
    val placed = rememberSaveable(card.id, saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<Int>()
    }
    var verdict by rememberSaveable(card.id) { mutableStateOf<Verdict?>(null) }
    var hinted by rememberSaveable(card.id) { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize().padding(horizontal = gutter)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 4.dp)) {
            TaskCard {
            Eyebrow("Соберите предложение", color = Ink.AccentText)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(card.term, Modifier.weight(1f, fill = false))
                if (card.meaningRu.isNotBlank()) {
                    Text(card.meaningRu, style = MaterialTheme.typography.bodySmall, color = Ink.Muted, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
            }
            Spacer(Modifier.height(18.dp))

            // Ответ: поставленные плашки на линии «тетради».
            Column(
                Modifier.fillMaxWidth().heightIn(min = 104.dp).shakeOn(shake).animateContentSize(tween(220)),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
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
                Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(2.dp).background(Ink.LineStrong, CircleShape))
            }
            }

            AnimatedContent(
                targetState = verdict != null,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
                label = "pool",
            ) { answered ->
                if (!answered) {
                    // Запас слов: поставленные остаются пустыми местами, чтобы плашки не прыгали.
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 22.dp),
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
                    NoteCard(Modifier.padding(top = 18.dp), tone = if (verdict == Verdict.Correct) NoteTone.Good else NoteTone.Bad) {
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
                                style = TextStyle(fontFamily = com.marat.albion.ui.theme.Sans, fontSize = if (LocalNarrow.current) 19.sp else 22.sp, lineHeight = if (LocalNarrow.current) 27.sp else 31.sp),
                                color = Ink.Fg,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            PracticeAudioButton(
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
            modifier = Modifier.padding(vertical = 12.dp),
        ) { answered ->
            if (answered) {
                PrimaryButton("Дальше", onClick = { vm.next() }, trailingIcon = AlbionIcons.ArrowForward)
            } else {
                GameAnswerActions(onCheck = { check() }, onSecondary = { hint() }, secondaryLabel = "Подсказка")
            }
        }
    }
}

private enum class TileStyle { Idle, Placed, Ghost, Correct, Wrong }

private val TileText = TextStyle(fontFamily = com.marat.albion.ui.theme.Sans, fontSize = 19.sp, lineHeight = 24.sp)
private val TileTextNarrow = TextStyle(fontFamily = com.marat.albion.ui.theme.Sans, fontSize = 17.sp, lineHeight = 22.sp)

/** Плашка со словом. Ghost — пустое место, откуда слово уже взято. */
@Composable
private fun WordTile(text: String, style: TileStyle, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        when (style) {
            TileStyle.Correct -> Ink.SuccessSoft
            TileStyle.Placed -> Ink.AccentSoft
            TileStyle.Wrong -> Ink.ErrorSoft
            TileStyle.Ghost -> Ink.Surface
            TileStyle.Idle -> Ink.Raised
        },
        tween(240), label = "tileBg",
    )
    val fg by animateColorAsState(
        when (style) {
            TileStyle.Correct -> Ink.Success
            TileStyle.Wrong -> Ink.Error
            else -> Ink.Fg
        },
        tween(240), label = "tileFg",
    )
    val border = when (style) {
        TileStyle.Idle -> Ink.LineStrong
        TileStyle.Placed -> Ink.Accent
        TileStyle.Ghost -> Ink.Line
        TileStyle.Correct -> Ink.Success
        TileStyle.Wrong -> Ink.Error
    }
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .widthIn(min = 48.dp).heightIn(min = 48.dp)
            .pressScale(interaction, 0.99f)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(interaction, indication = null, enabled = style == TileStyle.Idle || style == TileStyle.Placed, onClick = onClick)
            .padding(horizontal = if (LocalNarrow.current) 12.dp else 14.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            style = (if (LocalNarrow.current) TileTextNarrow else TileText).copy(textDecoration = if (style == TileStyle.Wrong) TextDecoration.LineThrough else null),
            color = fg,
            modifier = Modifier.alpha(if (style == TileStyle.Ghost) 0f else 1f),
        )
    }
}
