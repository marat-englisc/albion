package com.marat.albion.ui.games

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.components.AlbionIcons
import com.marat.albion.ui.components.RibbonBookmark
import com.marat.albion.ui.components.romanNumeral
import com.marat.albion.ui.theme.*

internal val Game.editionTitle: String get() = when (this) {
    Game.TYPE_ANSWER -> "The Writing Desk"
    Game.IRREGULAR -> "The Verb Ledger"
    Game.GRAMMAR_CHOICE -> "Grammar Review"
    Game.GRAMMAR_GAP -> "Mind the Gap"
    Game.SENTENCE -> "Sentence Craft"
    Game.TENSES -> "The Tense Table"
    Game.PRONUNCIATION -> "The Spoken Word"
    Game.SHADOWING -> "Listen & Echo"
}

internal val Game.editionNumber: Int get() = when (this) {
    Game.TYPE_ANSWER -> 1
    Game.IRREGULAR -> 2
    Game.GRAMMAR_CHOICE -> 3
    Game.GRAMMAR_GAP -> 4
    Game.SENTENCE -> 5
    Game.TENSES -> 6
    Game.PRONUNCIATION -> 7
    Game.SHADOWING -> 8
}

internal fun practiceTransition(reduced: Boolean): ContentTransform =
    fadeIn(tween(if (reduced) 0 else 180)) togetherWith fadeOut(tween(if (reduced) 0 else 120))

/** Printer's corners and a fine gilt rule; content stays native, selectable and scalable. */
@Composable
internal fun PracticeSheet(
    modifier: Modifier = Modifier, padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(3.dp)
    Column(modifier.fillMaxWidth().background(Ink.Raised, shape).border(.65.dp, Ink.Line, shape).drawBehind {
        val inset = 4.dp.toPx(); val length = 12.dp.toPx(); val line = .55.dp.toPx()
        drawLine(Ink.Gold, Offset(inset, inset + length), Offset(inset, inset), line)
        drawLine(Ink.Gold, Offset(inset, inset), Offset(inset + length, inset), line)
        drawLine(Ink.Gold, Offset(size.width - inset - length, size.height - inset), Offset(size.width - inset, size.height - inset), line)
        drawLine(Ink.Gold, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - length), line)
    }.padding(padding), content = content)
}

/** A rectangular, labelled choice with a full 48 dp touch target. */
@Composable
internal fun PracticeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(2.dp)
    Box(Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).pressScale(interaction, .99f)
        .background(if (selected) Ink.AccentSoft else Ink.Raised, shape)
        .border(if (selected) 1.dp else .65.dp, if (selected) Ink.Accent else Ink.LineStrong, shape)
        .selectable(selected, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.RadioButton) {
            feedback.selection(); onClick()
        }.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = if (selected) Ink.AccentText else Ink.Soft)
    }
}

@Composable
internal fun PracticeIconButton(
    icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    size: Dp = 48.dp, active: Boolean = false, enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(3.dp)
    Box(modifier.size(size.coerceAtLeast(48.dp)).pressScale(interaction, .99f)
        .background(if (active) Ink.Accent else Ink.Raised, shape)
        .border(.65.dp, if (active) Ink.Accent else Ink.LineStrong, shape)
        .clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Button) { feedback.selection(); onClick() },
        contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = if (!enabled) Ink.Faint else if (active) Ink.OnAccent else Ink.AccentText,
            modifier = Modifier.size(21.dp))
    }
}

@Composable
internal fun PracticeAudioButton(playing: Boolean, onClick: () -> Unit, size: Dp = 48.dp, primary: Boolean = true) {
    com.marat.albion.ui.components.AudioButton(playing, onClick, size = size.coerceAtLeast(48.dp), primary = primary)
}

@Composable
internal fun PracticeModeCard(game: Game, last: Boolean, onClick: () -> Unit) {
    val feedback = LocalFeedback.current
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Ink.Raised, RoundedCornerShape(3.dp))
        .border(.65.dp, Ink.Line, RoundedCornerShape(3.dp)).clickable(role = Role.Button) { feedback.selection(); onClick() }
        .padding(start = 14.dp, end = 15.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Icon(game.icon, null, tint = Ink.GoldText, modifier = Modifier.size(23.dp))

        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(game.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = headingFamily(game.title)), color = Ink.Fg)
            Text(game.tagline, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)

        }
        Spacer(Modifier.width(10.dp))
        if (last) RibbonBookmark(width = 9.dp, height = 24.dp)
        else Icon(AlbionIcons.ArrowForward, null, tint = Ink.Soft, modifier = Modifier.padding(top = 5.dp).size(17.dp))
    }
}
