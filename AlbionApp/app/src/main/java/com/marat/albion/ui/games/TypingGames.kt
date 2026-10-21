package com.marat.albion.ui.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.content.Deck
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.games.AnswerCheck
import com.marat.albion.data.games.Verdict
import com.marat.albion.ui.components.AudioButton
import com.marat.albion.ui.components.Chip
import com.marat.albion.ui.components.Eyebrow
import com.marat.albion.ui.components.GhostButton
import com.marat.albion.ui.components.Hairline
import com.marat.albion.ui.components.LevelBadge
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.lerpColor
import com.marat.albion.ui.study.rememberCardAudio
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.Serif

// ------------------------------------------------------------------ поле ввода

/** Крупное поле ввода антиквой с линией снизу, без автокоррекции. */
@Composable
internal fun AnswerField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    focus: FocusRequester,
    imeAction: ImeAction,
    onIme: () -> Unit,
    modifier: Modifier = Modifier,
    struck: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val line by animateFloatAsState(if (focused || value.isNotEmpty()) 1f else 0f, tween(260), label = "line")
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (value.isEmpty()) {
                Text(placeholder, style = TextStyle(fontFamily = Serif, fontSize = 26.sp, color = Ink.Faint), textAlign = TextAlign.Center)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = Serif,
                    fontSize = 30.sp,
                    color = if (struck) Ink.Faint else Ink.Fg,
                    textAlign = TextAlign.Center,
                    textDecoration = if (struck) TextDecoration.LineThrough else null,
                ),
                cursorBrush = SolidColor(Ink.Fg),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(onAny = { onIme() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
            )
        }
        Hairline(color = lerpColor(Ink.LineStrong, Ink.Fg, line))
    }
}

/** Правильный ответ, где пропущенные или искажённые буквы подчёркнуты. */
internal fun annotateMistakes(correct: String, answer: String): AnnotatedString {
    val mask = AnswerCheck.mistakeMask(answer, correct)
    return buildAnnotatedString {
        correct.forEachIndexed { i, ch ->
            if (mask[i]) withStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = Ink.Fg)) { append(ch) } else append(ch)
        }
    }
}

/** Скелет слова с открытыми подсказкой буквами: «a c _ _ _ _ _   f _ _». */
/**
 * Подсказка «_ _ _»: открыто [revealed] букв. Подстановки (someone, one's) видны сразу —
 * так понятно, что на их месте можно написать и sb, и конкретное слово.
 */
private fun skeleton(term: String, revealed: Int): String {
    var shown = 0
    return term.split(' ').joinToString("     ") { word ->
        if (AnswerCheck.isPlaceholder(word)) word
        else word.map { ch ->
            when {
                !ch.isLetter() -> ch.toString()
                shown < revealed -> { shown++; ch.toString() }
                else -> { shown++; "_" }
            }
        }.joinToString(" ")
    }
}

/** Сколько букв можно открыть подсказками (без подстановок). */
private fun hintLetters(term: String) =
    term.split(' ').filterNot { AnswerCheck.isPlaceholder(it) }.sumOf { w -> w.count { it.isLetter() } }

@Composable
internal fun VerdictLine(verdict: Verdict) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (verdict != Verdict.Wrong) {
            Icon(Icons.Rounded.Check, null, tint = Ink.Fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Eyebrow(
            when (verdict) {
                Verdict.Correct -> "Верно"
                Verdict.Typo -> "Почти — опечатка"
                Verdict.Wrong -> "Неверно"
            },
            color = Ink.Fg,
        )
    }
}

// ------------------------------------------------------------------ ввод ответа

@Composable
fun TypeAnswerRound(vm: GameViewModel, card: CardEntity) {
    val c = LocalAppContainer.current
    val audio = rememberCardAudio(c.audio)
    val deck = Deck.byId(card.deck) ?: Deck.WORDS
    var input by rememberSaveable(card.id) { mutableStateOf("") }
    var verdict by remember(card.id) { mutableStateOf<Verdict?>(null) }
    var hints by remember(card.id) { mutableIntStateOf(0) }
    var showEn by remember(card.id) { mutableStateOf(false) }
    var shake by remember(card.id) { mutableIntStateOf(0) }
    val focus = remember(card.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val letters = hintLetters(card.term)

    LaunchedEffect(card.id) { runCatching { focus.requestFocus() } }

    fun check(giveUp: Boolean = false) {
        if (verdict != null) return
        val v = if (giveUp) Verdict.Wrong else AnswerCheck.checkTerm(input, card.term)
        if (!giveUp && input.isBlank()) { shake++; return }
        verdict = v
        if (v == Verdict.Wrong) shake++
        vm.record(v != Verdict.Wrong, Mistake(card.meaningRu, input, card.term))
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        audio.play(deck, card.audio, card.term, "term:${card.id}")
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(card.pos)
                LevelBadge(card.level)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                card.meaningRu,
                style = TextStyle(fontFamily = Serif, fontSize = 24.sp, lineHeight = 32.sp),
                color = Ink.Fg,
                textAlign = TextAlign.Center,
            )
            AnimatedVisibility(showEn && verdict == null, enter = expandVertically() + fadeIn()) {
                Text(
                    card.meaningEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(
                skeleton(card.term, if (verdict != null) letters else hints),
                style = TextStyle(fontFamily = Serif, fontSize = 18.sp, letterSpacing = 1.sp),
                color = if (verdict != null) Ink.Faint else Ink.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            AnswerField(
                value = input,
                onChange = { if (verdict == null) input = it },
                placeholder = "Ваш ответ",
                enabled = verdict == null,
                focus = focus,
                imeAction = ImeAction.Done,
                onIme = { check() },
                modifier = Modifier.shakeOn(shake),
                struck = verdict == Verdict.Wrong,
            )

            AnimatedVisibility(
                verdict != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(420)) { it / 3 },
            ) {
                val v = verdict ?: Verdict.Wrong
                Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    VerdictLine(v)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (v == Verdict.Typo) annotateMistakes(card.term, input) else AnnotatedString(card.term),
                        style = TextStyle(fontFamily = Serif, fontSize = 34.sp, lineHeight = 40.sp),
                        color = Ink.Fg,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (card.ipa.isNotBlank()) {
                            Text(card.ipa, style = AlbionType.ipa)
                            Spacer(Modifier.width(12.dp))
                        }
                        AudioButton(
                            playing = audio.playingKey == "term:${card.id}",
                            onClick = { audio.play(deck, card.audio, card.term, "term:${card.id}") },
                            size = 40.dp,
                        )
                    }
                    if (card.example1.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(card.example1, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameAnswerActions(onCheck = { check() }, onSecondary = { check(giveUp = true) })
                    GhostButton(
                        if (hints < letters - 1) "Буква" else "Определение",
                        onClick = { if (hints < letters - 1) hints++ else showEn = true },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ неправильные глаголы

@Composable
fun IrregularRound(vm: GameViewModel, card: CardEntity) {
    val c = LocalAppContainer.current
    val audio = rememberCardAudio(c.audio)
    val data = remember(card.id) { vm.irregular(card) }
    var v2 by rememberSaveable(card.id) { mutableStateOf("") }
    var v3 by rememberSaveable(card.id) { mutableStateOf("") }
    var result by remember(card.id) { mutableStateOf<Pair<Verdict, Verdict>?>(null) }
    var shake by remember(card.id) { mutableIntStateOf(0) }
    val focus2 = remember(card.id) { FocusRequester() }
    val focus3 = remember(card.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(card.id) { runCatching { focus2.requestFocus() } }

    val past = data?.forms?.getOrNull(1)?.variants.orEmpty()
    val participle = data?.forms?.getOrNull(2)?.variants.orEmpty()

    fun check(giveUp: Boolean = false) {
        if (result != null) return
        if (!giveUp && v2.isBlank() && v3.isBlank()) { shake++; return }
        val r = if (giveUp) Verdict.Wrong to Verdict.Wrong
            else AnswerCheck.checkForm(v2, past.map { it.text }) to AnswerCheck.checkForm(v3, participle.map { it.text })
        result = r
        val ok = r.first == Verdict.Correct && r.second == Verdict.Correct
        if (!ok) shake++
        vm.record(
            ok,
            Mistake(
                prompt = "${card.term} — ${card.meaningRu}",
                answer = listOf(v2, v3).joinToString(" · ") { it.ifBlank { "—" } },
                correct = past.joinToString(" / ") { it.text } + " · " + participle.joinToString(" / ") { it.text },
            ),
        )
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow("V1 · Infinitive")
            Spacer(Modifier.height(14.dp))
            Text(card.term, style = TextStyle(fontFamily = Serif, fontSize = 48.sp, lineHeight = 54.sp), color = Ink.Fg)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.ipa.isNotBlank()) {
                    Text(card.ipa, style = AlbionType.ipa)
                    Spacer(Modifier.width(12.dp))
                }
                AudioButton(
                    playing = audio.playingKey == "term:${card.id}",
                    onClick = { audio.play(Deck.IRREGULAR, card.audio, card.term, "term:${card.id}") },
                    size = 38.dp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(card.meaningRu, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(34.dp))

            Column(Modifier.shakeOn(shake), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                FormInput(
                    label = "V2 · Past Simple",
                    value = v2,
                    onChange = { if (result == null) v2 = it },
                    focus = focus2,
                    imeAction = ImeAction.Next,
                    onIme = { runCatching { focus3.requestFocus() } },
                    verdict = result?.first,
                    correct = past.map { it.text },
                    onPlay = { i -> audio.play(Deck.IRREGULAR, past[i].audio.firstOrNull(), past[i].text, "v2:${card.id}:$i") },
                    playingKey = audio.playingKey,
                    keyPrefix = "v2:${card.id}",
                )
                FormInput(
                    label = "V3 · Past Participle",
                    value = v3,
                    onChange = { if (result == null) v3 = it },
                    focus = focus3,
                    imeAction = ImeAction.Done,
                    onIme = { check() },
                    verdict = result?.second,
                    correct = participle.map { it.text },
                    onPlay = { i -> audio.play(Deck.IRREGULAR, participle[i].audio.firstOrNull(), participle[i].text, "v3:${card.id}:$i") },
                    playingKey = audio.playingKey,
                    keyPrefix = "v3:${card.id}",
                )
            }
        }
        AnimatedContent(
            targetState = result != null,
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            label = "actions",
            modifier = Modifier.padding(vertical = 14.dp),
        ) { answered ->
            if (answered) PrimaryButton("Дальше", onClick = { vm.next() }, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
            else GameAnswerActions(onCheck = { check() }, onSecondary = { check(giveUp = true) }, secondaryLabel = "Я не знаю")
        }
    }
}

@Composable
private fun FormInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    focus: FocusRequester,
    imeAction: ImeAction,
    onIme: () -> Unit,
    verdict: Verdict?,
    correct: List<String>,
    onPlay: (Int) -> Unit,
    playingKey: String?,
    keyPrefix: String,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(label, Modifier.weight(1f))
            if (verdict != null) VerdictLine(verdict)
        }
        AnswerField(
            value = value,
            onChange = onChange,
            placeholder = "…",
            enabled = verdict == null,
            focus = focus,
            imeAction = imeAction,
            onIme = onIme,
            struck = verdict == Verdict.Wrong,
        )
        AnimatedVisibility(verdict != null, enter = expandVertically() + fadeIn()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                correct.forEachIndexed { i, text ->
                    if (i > 0) Text("  /  ", color = Ink.Faint)
                    Text(
                        if (verdict == Verdict.Typo) annotateMistakes(text, value) else AnnotatedString(text),
                        style = TextStyle(fontFamily = Serif, fontSize = 24.sp),
                        color = Ink.Fg,
                    )
                    Spacer(Modifier.width(8.dp))
                    AudioButton(playing = playingKey == "$keyPrefix:$i", onClick = { onPlay(i) }, size = 32.dp, primary = false)
                }
            }
        }
    }
}
