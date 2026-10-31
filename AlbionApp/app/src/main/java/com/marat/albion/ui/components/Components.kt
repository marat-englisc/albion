package com.marat.albion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.LocalFeedback

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text, modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color, maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = DividerDefaults.color) {
    HorizontalDivider(modifier, color = color)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, eyebrow: String? = null, includeStatusBarInsets: Boolean = true, compact: Boolean = false, trailing: @Composable (() -> Unit)? = null) {
    Column(Modifier.then(if (includeStatusBarInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).padding(top = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) Eyebrow(eyebrow)
                Text(title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
            }
            trailing?.invoke()
        }
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    spin: Float = 0f,
    enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    IconButton(onClick = { feedback.selection(); onClick() }, modifier = modifier, enabled = enabled) {
        Icon(icon, contentDescription, tint = if (enabled) tint else LocalContentColor.current)
    }
}

@Composable
fun SettingsShortcut(onClick: () -> Unit) {
    RoundIconButton(AlbionIcons.Settings, "Настройки", onClick)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    borderColor: Color = Color.Unspecified,
    color: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = if (color == Color.Unspecified) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = color)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors) { Column(Modifier.padding(contentPadding), content = content) }
    } else {
        Card(modifier = modifier, colors = colors) { Column(Modifier.padding(contentPadding), content = content) }
    }
}

fun Modifier.softShadow(shape: androidx.compose.ui.graphics.Shape, elevation: Dp = 2.dp): Modifier =
    shadow(elevation, shape)

enum class ButtonTone { Accent, Warm, Error }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.Accent,
    inverse: Boolean = false,
) {
    val feedback = LocalFeedback.current
    val colors = when (tone) {
        ButtonTone.Accent -> ButtonDefaults.buttonColors()
        ButtonTone.Warm -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
        ButtonTone.Error -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
    }
    Button(onClick = { feedback.selection(); onClick() }, modifier = modifier.fillMaxWidth(), enabled = enabled, colors = colors) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(text, textAlign = TextAlign.Center)
        if (trailingIcon != null) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Icon(trailingIcon, null, Modifier.size(ButtonDefaults.IconSize))
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    val feedback = LocalFeedback.current
    OutlinedButton(onClick = { feedback.selection(); onClick() }, modifier = modifier.fillMaxWidth(), contentPadding = contentPadding) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(text, textAlign = TextAlign.Center)
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

@Composable
fun AudioButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, primary: Boolean = true) {
    val player = com.marat.albion.LocalAppContainer.current.audio
    IconButton(onClick = { if (playing) player.stop() else onClick() }, modifier = modifier) {
        Icon(
            if (playing) AlbionIcons.Stop else AlbionIcons.VolumeUp,
            contentDescription = if (playing) "Остановить озвучку" else "Озвучить",
        )
    }
}

@Composable
fun LevelBadge(level: String?, modifier: Modifier = Modifier) {
    if (level.isNullOrBlank()) return
    Text(level.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelMedium)
}

@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    track: Color = ProgressIndicatorDefaults.linearTrackColor,
    height: Dp = 4.dp,
) {
    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth(), color = color, trackColor = track)
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { if (title != null) Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = { RoundIconButton(AlbionIcons.ArrowBack, "Назад", onBack) },
        actions = actions,
    )
}

@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
) {
    Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps, modifier = modifier)
}
