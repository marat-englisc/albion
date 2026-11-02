package com.marat.albion.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import com.marat.albion.ui.theme.pressScale
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
import com.marat.albion.ui.components.romanNumeral
import com.marat.albion.ui.components.SignalLabel
import com.marat.albion.ui.components.Panel
import com.marat.albion.ui.theme.*

internal val Game.editionTitle: String get() = when (this) {
    Game.TYPE_ANSWER -> "WORD RECALL"
    Game.IRREGULAR -> "VERB FORMS"
    Game.GRAMMAR_CHOICE -> "GRAMMAR"
    Game.GRAMMAR_GAP -> "GAP FILL"
    Game.SENTENCE -> "WORD ORDER"
    Game.TENSES -> "TENSES"
    Game.PRONUNCIATION -> "SPEAKING"
    Game.SHADOWING -> "SHADOWING"
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

/** Secondary exercise content uses the same outlined panels as the rest of Albion. */
@Composable
internal fun PracticeSheet(
    modifier: Modifier = Modifier, padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel(modifier.fillMaxWidth(), contentPadding = PaddingValues(padding)) {
        content()
    }
}

/** The work surface used by every exercise, with restrained instrument-like corners. */
@Composable
internal fun PracticeStage(
    label: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    Column(modifier.fillMaxWidth().clip(shape).background(scheme.surfaceContainerLow)
        .border(.75.dp, scheme.outlineVariant, shape)
        .drawBehind {
            val inset = 9.dp.toPx()
            val length = 8.dp.toPx()
            for (x in listOf(inset, size.width - inset)) for (y in listOf(inset, size.height - inset)) {
                val dx = if (x < size.width / 2) length else -length
                val dy = if (y < size.height / 2) length else -length
                drawLine(scheme.onSurface.copy(alpha = .28f), Offset(x, y), Offset(x + dx, y), .75.dp.toPx())
                drawLine(scheme.onSurface.copy(alpha = .28f), Offset(x, y), Offset(x, y + dy), .75.dp.toPx())
            }
        }.padding(if (compact) 14.dp else 20.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        if (!compact) {
            SignalLabel(label, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
        }
        content()
    }
}

@Composable
internal fun PracticeSectionLabel(number: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        SignalLabel(number, Modifier.padding(end = 10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** Fixed actions have a clear boundary from the scrollable work area. */
@Composable
internal fun PracticeActionShelf(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth().drawBehind {
        drawLine(line, Offset.Zero, Offset(size.width, 0f), .75.dp.toPx())
    }.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
internal fun PracticeIconButton(
    icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    size: Dp = 48.dp, active: Boolean = false, enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    Box(modifier.size(size.coerceAtLeast(48.dp)).pressScale(interaction, 0.9f)
        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, shape).clip(shape)
        .clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Button) { feedback.selection(); onClick() },
        contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant else if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
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
    androidx.compose.material3.Card(onClick = { feedback.selection(); onClick() }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        androidx.compose.material3.ListItem(
            headlineContent = { Text(game.title) },
            supportingContent = { Text(game.tagline) },
            leadingContent = { Icon(game.icon, null) },
            trailingContent = { Icon(AlbionIcons.ArrowForward, null) },
        )
    }
}
