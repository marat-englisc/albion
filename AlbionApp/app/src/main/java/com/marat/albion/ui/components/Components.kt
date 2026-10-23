package com.marat.albion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Easing
import com.marat.albion.ui.theme.Ink
import com.marat.albion.ui.theme.pressScale

/** Подпись секции: мелкие заглавные с широкой разрядкой. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ink.Muted) {
    Text(text.uppercase(), modifier = modifier, style = AlbionType.eyebrow, color = color)
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Ink.Line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Заголовок секции в духе журнальной вёрстки: «КОЛОДЫ ————————— 05». */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val measurer = rememberTextMeasurer()
    val naturalWidth = with(LocalDensity.current) {
        (measurer.measure(title.uppercase(), AlbionType.eyebrow, softWrap = false).size.width +
            (trailing?.let { measurer.measure(it, AlbionType.index, softWrap = false).size.width } ?: 0)).toDp() + 40.dp
    }
    BoxWithConstraints(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        val compact = naturalWidth > maxWidth
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (compact) {
                Eyebrow(title, Modifier.weight(1f), color = Ink.Soft)
            } else {
                Eyebrow(title, color = Ink.Soft)
                Spacer(Modifier.width(14.dp))
                Hairline(Modifier.weight(1f))
            }
            if (trailing != null) {
                Spacer(Modifier.width(14.dp))
                Text(trailing, style = AlbionType.index, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Шапка вкладки: крупный антиквенный заголовок и подзаголовок. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, eyebrow: String? = null, includeStatusBarInsets: Boolean = true, compact: Boolean = false, trailing: @Composable (() -> Unit)? = null) {
    Column(Modifier.then(if (includeStatusBarInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier).padding(top = if (compact) 12.dp else 28.dp, bottom = 8.dp)) {
        if (eyebrow != null || trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (eyebrow != null) Eyebrow(eyebrow)
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(title, style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall, color = Ink.Fg)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
        }
    }
}

@Composable
fun SettingsShortcut(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Settings, contentDescription = "Настройки", tint = Ink.Fg, modifier = Modifier.size(24.dp))
    }
}

/** Панель: почти чёрная поверхность с тонкой рамкой и пружинным нажатием. */
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
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(interaction, 0.985f),
            shape = shape,
            color = color,
            border = BorderStroke(1.dp, borderColor),
            interactionSource = interaction,
        ) { Column(Modifier.padding(contentPadding), content = content) }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = BorderStroke(1.dp, borderColor)) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Главная кнопка: белая плашка с чёрным текстом. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateFloatAsState(if (enabled) 1f else 0f, tween(300), label = "btn")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.small)
            .background(lerpColor(Ink.Elevated, Ink.Fg, bg))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailingIcon != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        val fg = lerpColor(Ink.Faint, Ink.Bg, bg)
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

/** Второстепенная кнопка: прозрачная с тонкой рамкой. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, Ink.LineStrong, MaterialTheme.shapes.small)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ink.Text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink.Text, textAlign = TextAlign.Center)
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

/** Кнопка озвучки: пока звучит — от неё расходятся тонкие кольца. */
@Composable
fun AudioButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, primary: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.size(size.coerceAtLeast(48.dp)).pressScale(interaction, 0.94f)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            val t = rememberInfiniteTransition(label = "rings")
            val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "wave")
            Canvas(Modifier.size(size)) {
                for (k in 0..1) {
                    val p = (wave + k * 0.5f) % 1f
                    drawCircle(
                        Ink.Fg.copy(alpha = (1f - p) * 0.5f),
                        radius = this.size.minDimension / 2f * (0.75f + p * 0.6f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }
        Box(
            Modifier.size(size)
                .clip(CircleShape)
                .background(if (playing) Ink.Fg else if (primary) Ink.Elevated else Color.Transparent)
                .border(1.dp, if (playing) Ink.Fg else Ink.LineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Озвучить",
                tint = if (playing) Ink.Bg else Ink.Fg,
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
            .border(1.dp, Ink.LineStrong, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Ink.Soft,
    )
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.border(1.dp, Ink.LineStrong, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Ink.Soft,
    )
}

/** Тонкая линия прогресса — 2 dp, без скруглений. */
@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier, color: Color = Ink.Fg, track: Color = Ink.Line) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300, easing = Easing.Emphasized), label = "progress")
    Box(modifier.fillMaxWidth().height(2.dp).background(track)) {
        Box(Modifier.fillMaxWidth(p).height(2.dp).background(color))
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

/** Пустое состояние: например, материалы не вшиты в сборку. */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Inventory2, null, tint = Ink.Faint, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Ink.Fg)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
    }
}

/** Верхняя панель внутренних экранов: стрелка назад, необязательный заголовок и действия. */
@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().background(Ink.Bg).windowInsetsPadding(WindowInsets.statusBars).height(60.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Ink.Fg)
        }
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink.Fg,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

/** Строгий слайдер: линия в 2 dp и маленькая круглая ручка, без делений. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
) {
    val colors = androidx.compose.material3.SliderDefaults.colors(
        thumbColor = Ink.Fg,
        activeTrackColor = Ink.Fg,
        inactiveTrackColor = Ink.LineStrong,
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
        thumb = {
            Box(Modifier.size(18.dp).background(Ink.Fg, CircleShape).border(3.dp, Ink.Bg, CircleShape))
        },
        track = { state ->
            androidx.compose.material3.SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                modifier = Modifier.height(2.dp),
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 0.dp,
            )
        },
    )
}
