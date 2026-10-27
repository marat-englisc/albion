package com.marat.albion.ui.components

import com.marat.albion.ui.theme.paperSurface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.headingFamily
import com.marat.albion.ui.theme.LocalFeedback
import com.marat.albion.ui.theme.LocalReducedMotion
import com.marat.albion.ui.theme.pressScale

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ink.Muted, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text.uppercase(), modifier = modifier, style = AlbionType.eyebrow, color = color, maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Ink.Line) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(color))
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val measurer = rememberTextMeasurer()
    val naturalWidth = with(LocalDensity.current) {
        (measurer.measure(title.uppercase(), AlbionType.eyebrow, softWrap = false).size.width +
            (trailing?.let { measurer.measure(it, AlbionType.index, softWrap = false).size.width } ?: 0)).toDp() + 56.dp
    }
    BoxWithConstraints(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        val compact = naturalWidth > maxWidth
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (compact) {
                Eyebrow(title, Modifier.weight(1f), color = Ink.Soft)
            } else {
                CatalogueTab(title)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(0.5.dp).background(Ink.Line))
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    trailing,
                    style = AlbionType.index.copy(color = Ink.Soft),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.background(Ink.GoldSoft, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, eyebrow: String? = null, includeStatusBarInsets: Boolean = true, compact: Boolean = false, trailing: @Composable (() -> Unit)? = null) {
    Column(Modifier.then(if (includeStatusBarInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).padding(top = if (compact) 12.dp else 28.dp, bottom = 8.dp)) {
        if (eyebrow != null || trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (eyebrow != null) Eyebrow(eyebrow, color = Ink.GoldText)
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(title, style = (if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall)
            .copy(fontFamily = headingFamily(title)), color = Ink.Fg)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(30.dp).height(1.dp).background(Ink.Gold))
            Box(Modifier.weight(1f).height(0.5.dp).background(Ink.Line))
        }
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink.Fg,
    spin: Float = 0f,
    enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    val reduced = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val turn by animateFloatAsState(if (pressed && !reduced) spin else 0f, tween(140), label = "turn")
    Box(
        modifier.size(48.dp).pressScale(interaction, 0.96f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = { feedback.selection(); onClick() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).background(if (pressed) Ink.Surface else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = if (enabled) tint else Ink.Faint, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn })
        }
    }
}

@Composable
fun SettingsShortcut(onClick: () -> Unit) {
    RoundIconButton(AlbionIcons.Settings, "Настройки", onClick, spin = 0f)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    borderColor: Color = Ink.Line,
    color: Color = Ink.Raised,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val lifted = modifier
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = lifted.pressScale(interaction, 0.975f),
            shape = shape,
            color = color,
            border = BorderStroke(1.dp, borderColor),
            interactionSource = interaction,
        ) { Column(Modifier.padding(contentPadding), content = content) }
    } else {
        Surface(modifier = lifted, shape = shape, color = color, border = BorderStroke(1.dp, borderColor)) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

fun Modifier.softShadow(shape: androidx.compose.ui.graphics.Shape, elevation: Dp = 2.dp): Modifier =
    shadow(elevation, shape, ambientColor = Ink.Shadow.copy(alpha = 0.18f), spotColor = Ink.Shadow.copy(alpha = 0.18f))

enum class ButtonTone { Accent, Warm, Error }

@Composable
private fun RefinedButton(
    onClick: () -> Unit,
    modifier: Modifier,
    face: Color,
    pressedFace: Color,
    border: Color?,
    enabled: Boolean,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by androidx.compose.animation.animateColorAsState(if (pressed && enabled) pressedFace else face, tween(140), label = "buttonFill")
    val shape = RoundedCornerShape(4.dp)
    Surface(
        onClick = { feedback.selection(); onClick() }, enabled = enabled, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().pressScale(interaction, 0.99f),
        shape = shape, color = fill,
        border = border?.let { BorderStroke(1.dp, it) },
    ) {
        Row(Modifier.heightIn(min = 52.dp).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = horizontalArrangement,
            content = content)
    }
}

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
    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(300), label = "btn")
    val (faceOn, lipOn, fgOn) = if (inverse) Triple(Ink.Ivory, androidx.compose.ui.graphics.Color(0xFFE8DDCA), androidx.compose.ui.graphics.Color(0xFF861F39)) else when (tone) {
        ButtonTone.Accent -> Triple(Ink.Accent, Ink.AccentDeep, Ink.OnAccent)
        ButtonTone.Warm -> Triple(Ink.WarmFill, Ink.WarmDeep, Ink.OnWarm)
        ButtonTone.Error -> Triple(Ink.ErrorFill, Ink.ErrorDeep, Ink.OnError)
    }
    val fg = lerpColor(Ink.Faint, fgOn, on)
    RefinedButton(
        onClick = onClick,
        modifier = modifier,
        face = lerpColor(Ink.Elevated, faceOn, on),
        pressedFace = lerpColor(Ink.High, lipOn, on),
        border = null,
        enabled = enabled,
        horizontalArrangement = if (trailingIcon != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        Row(Modifier.weight(1f, fill = trailingIcon != null), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (trailingIcon != null) Arrangement.Start else Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = fg, textAlign = TextAlign.Center)
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(trailingIcon, null, tint = fg, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
) {
    RefinedButton(
        onClick = onClick,
        modifier = modifier,
        face = Ink.Raised,
        pressedFace = Ink.Surface,
        border = Ink.LineStrong,
        enabled = true,
        horizontalArrangement = Arrangement.Center,
        contentPadding = contentPadding,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ink.AccentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink.Text, textAlign = TextAlign.Center)
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

@Composable
fun AudioButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, primary: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val accent = Ink.Accent
    Box(
        modifier.size(size.coerceAtLeast(48.dp)).pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (playing && !LocalReducedMotion.current) {
            val t = rememberInfiniteTransition(label = "rings")
            val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "wave")
            Canvas(Modifier.size(size)) {
                for (k in 0..1) {
                    val p = (wave + k * 0.5f) % 1f
                    drawCircle(
                        accent.copy(alpha = (1f - p) * 0.6f),
                        radius = this.size.minDimension / 2f * (0.75f + p * 0.6f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        Box(
            Modifier.size(size)
                .clip(CircleShape)
                .background(if (playing) Ink.Accent else if (primary) Ink.AccentSoft else Color.Transparent)
                .border(1.dp, if (playing) Ink.Accent else if (primary) Ink.AccentSoft else Ink.LineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AlbionIcons.VolumeUp,
                contentDescription = "Озвучить",
                tint = if (playing) Ink.OnAccent else Ink.AccentText,
                modifier = Modifier.size(size * 0.46f),
            )
        }
    }
}

@Composable
fun LevelBadge(level: String?, modifier: Modifier = Modifier) {
    if (level.isNullOrBlank()) return
    Text(
        level.uppercase(),
        modifier = modifier
            .background(Ink.GoldSoft, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Ink.GoldText,
    )
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.background(Ink.Raised, RoundedCornerShape(4.dp)).border(0.5.dp, Ink.Line, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Ink.Soft,
    )
}

@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Ink.Accent,
    track: Color = Ink.High,
    height: Dp = 2.dp,
) {
    val reduced = LocalReducedMotion.current
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(if (reduced) 0 else 360, easing = com.marat.albion.ui.theme.Easing.Standard), label = "progress")
    val trackColor = if (track == Ink.Bg) Ink.Surface else track
    Canvas(modifier.fillMaxWidth().height(height).progressSemantics(progress.coerceIn(0f, 1f))) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = r)
        val w = size.width * p
        if (w > 0f) {
            val fillW = w.coerceAtLeast(size.height)
            drawRoundRect(color, size = Size(fillW, size.height), cornerRadius = r)

        }
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(AlbionIcons.Inventory2, null, tint = Ink.Gold, modifier = Modifier.size(32.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Ink.Fg)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().paperSurface().windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(AlbionIcons.ArrowBack, "Назад", onBack)
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink.Fg,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val grow by animateFloatAsState(if (dragged || pressed) 1.25f else 1f, tween(180), label = "thumb")
    val colors = androidx.compose.material3.SliderDefaults.colors(
        thumbColor = Ink.Accent,
        activeTrackColor = Ink.Accent,
        inactiveTrackColor = Ink.High,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = colors,
        interactionSource = interaction,
        thumb = {
            Box(
                Modifier.size(24.dp).graphicsLayer { scaleX = grow; scaleY = grow }
                    .softShadow(CircleShape, 3.dp)
                    .background(Ink.Raised, CircleShape).border(4.dp, Ink.Accent, CircleShape),
            )
        },
        track = { state ->
            androidx.compose.material3.SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                modifier = Modifier.height(6.dp).clip(RoundedCornerShape(3.dp)),
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}
