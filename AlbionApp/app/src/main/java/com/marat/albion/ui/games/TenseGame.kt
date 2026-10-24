package com.marat.albion.ui.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.games.AnswerCheck
import com.marat.albion.data.games.TenseTask
import com.marat.albion.data.games.Verdict
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif

/**
 * Тренажёр времён: пример с пропуском вместо формы неправильного глагола.
 * Режим выбора (V1 / V2 / V3 / «goed») или ввода; после ответа — правило и все формы.
 */
@Composable
fun TenseRound(vm: GameViewModel, card: CardEntity) {
    val task = remember(card.id) { vm.tense(card) } ?: return
    val data = remember(card.id) { vm.irregular(card) }
    val c = LocalAppContainer.current
    val audio = rememberCardAudio(c.audio)
    val typing = vm.mode == 1
    val options = remember(card.id) { vm.tenseOptions(card, task) }
    var picked by rememberSaveable(card.id) { mutableStateOf<String?>(null) }
    var input by rememberSaveable(card.id) { mutableStateOf("") }
    var verdict by rememberSaveable(card.id) { mutableStateOf<Verdict?>(null) }
    var shake by remember(card.id) { mutableIntStateOf(0) }
    val focus = remember(card.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val prompt = "${task.before.trim()} ___ (${task.infinitive}) ${task.after.trim()}".trim()

    // Озвучка формы, которая стоит в примере.
    val answerVariant = data?.forms?.getOrNull(task.formIndex)?.variants?.firstOrNull { it.text.equals(task.answer, ignoreCase = true) }
    fun playAnswer() = c.audio.play(Deck.IRREGULAR, answerVariant?.audio?.firstOrNull(), task.answer, "tense:${card.id}")

    LaunchedEffect(card.id, typing) { if (typing) runCatching { focus.requestFocus() } }

    fun finish(v: Verdict, answer: String) {
        verdict = v
        if (v == Verdict.Wrong) shake++
        vm.record(v == Verdict.Correct, Mistake(prompt, answer, task.answer))
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        playAnswer()
    }

    fun checkTyped(giveUp: Boolean = false) {
        if (verdict != null) return
        if (!giveUp && input.isBlank()) { shake++; return }
        finish(if (giveUp) Verdict.Wrong else AnswerCheck.checkGrammar(input, task.accepted), input)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Какая форма нужна?", Modifier.weight(1f))
                LevelBadge(card.level)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                sentence(task, filled = verdict?.let { shownAnswer(task, if (typing) input else picked, it) }),
                style = TextStyle(fontFamily = Serif, fontSize = 26.sp, lineHeight = 36.sp),
                color = Ink.Fg,
                modifier = Modifier.shakeOn(shake),
            )
            Spacer(Modifier.height(26.dp))

            if (typing) {
                AnswerField(
                    value = input,
                    onChange = { if (verdict == null) input = it },
                    placeholder = "Форма глагола",
                    enabled = verdict == null,
                    focus = focus,
                    imeAction = ImeAction.Done,
                    onIme = { checkTyped() },
                    struck = verdict == Verdict.Wrong,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { option ->
                        val isAnswer = task.accepted.any { it.equals(option, ignoreCase = true) }
                        OptionButton(
                            text = option,
                            state = when {
                                verdict == null -> OptionState.Idle
                                isAnswer -> OptionState.Correct
                                option == picked -> OptionState.Wrong
                                else -> OptionState.Dim
                            },
                            onClick = {
                                if (verdict == null) {
                                    picked = option
                                    finish(if (isAnswer) Verdict.Correct else Verdict.Wrong, option)
                                }
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(verdict != null, enter = fadeIn(tween(300)) + expandVertically()) {
                val v = verdict ?: Verdict.Wrong
                Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                    VerdictLine(v)
                    if (typing && v != Verdict.Correct) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            AnnotatedString(task.answer),
                            style = TextStyle(fontFamily = Serif, fontSize = 26.sp),
                            color = Ink.Fg,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth().border(1.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(16.dp)) {
                        Eyebrow(task.formLabel, color = Ink.Soft)
                        Spacer(Modifier.height(8.dp))
                        Text(task.rule, style = MaterialTheme.typography.bodyMedium, color = Ink.Text)
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(forms(task, data), style = TextStyle(fontFamily = Serif, fontSize = 20.sp), color = Ink.Muted, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            AudioButton(playing = audio.playingKey == "tense:${card.id}", onClick = ::playAnswer, size = 40.dp, primary = false)
                        }
                        answerVariant?.ipa?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("${task.answer}  $it", style = AlbionType.ipa.copy(fontSize = 15.sp))
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
            when {
                answered -> PrimaryButton("Дальше", onClick = { vm.next() }, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
                typing -> GameAnswerActions(onCheck = { checkTyped() }, onSecondary = { checkTyped(giveUp = true) })
                else -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

/** Что вписать в пропуск после ответа: ваш вариант, если он верный (burned вместо burnt). */
private fun shownAnswer(task: TenseTask, answer: String?, verdict: Verdict): String =
    if (verdict == Verdict.Wrong || answer == null) task.answer
    else task.accepted.firstOrNull { AnswerCheck.normalizeGrammar(it) == AnswerCheck.normalizeGrammar(answer) } ?: task.answer

/** Предложение: пропуск-линия с подсказкой «(go)», после ответа — вписанная форма. */
private fun sentence(task: TenseTask, filled: String?): AnnotatedString = buildAnnotatedString {
    append(task.before)
    if (filled == null) {
        withStyle(SpanStyle(color = Ink.Muted)) { append(GAP_MARK) }
        withStyle(SpanStyle(color = Ink.Muted, fontSize = 20.sp)) { append(" (${task.infinitive})") }
    } else {
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold, color = Ink.Fg)) { append(filled) }
    }
    append(task.after)
}

/** «arise · arose · arisen» — нужная форма выделена. */
private fun forms(task: TenseTask, data: com.marat.albion.data.content.IrregularData?): AnnotatedString = buildAnnotatedString {
    val list = data?.forms.orEmpty()
    list.forEachIndexed { i, f ->
        if (i > 0) append("  ·  ")
        val text = f.variants.joinToString(" / ") { it.text }
        if (i == task.formIndex) withStyle(SpanStyle(color = Ink.Fg, fontWeight = FontWeight.SemiBold)) { append(text) } else append(text)
    }
}
