package com.marat.albion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Inventory2
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

/** Заголовок секции: зелёная «бусина» шарфа, подпись и мягкая линия до счётчика. */
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
            Box(Modifier.size(width = 14.dp, height = 6.dp).background(Ink.Accent, CircleShape))
            Spacer(Modifier.width(10.dp))
            if (compact) {
                Eyebrow(title, Modifier.weight(1f), color = Ink.Soft)
            } else {
                Eyebrow(title, color = Ink.Soft)
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f).height(2.dp).background(Ink.Line, CircleShape))
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    trailing,
                    style = AlbionType.index.copy(color = Ink.Soft),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.background(Ink.Surface, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
                )
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
                    if (eyebrow != null) Eyebrow(eyebrow, color = Ink.Accent)
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

/** Круглая кнопка-«камешек» для значков в шапке: при нажатии вдавливается и чуть поворачивает значок. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink.Fg,
    spin: Float = 0f,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val turn by animateFloatAsState(if (pressed) spin else 0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow), label = "turn")
    Box(
        modifier.size(48.dp).pressScale(interaction, 0.88f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).background(Ink.Surface, CircleShape).border(1.dp, Ink.Line, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn })
        }
    }
}

@Composable
fun SettingsShortcut(onClick: () -> Unit) {
    RoundIconButton(Icons.Outlined.Settings, "Настройки", onClick, spin = 75f)
}

/** Панель: мягкая карточка с крупным скруглением, тёплой тенью и пружинным нажатием. */
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
    val lifted = modifier.softShadow(shape)
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

/** Тёплая мягкая тень на светлой теме; на тёмной карточки отделяются тоном и рамкой. */
fun Modifier.softShadow(shape: androidx.compose.ui.graphics.Shape, elevation: Dp = 6.dp): Modifier =
    if (Ink.palette.isDark) this
    else shadow(elevation, shape, ambientColor = Ink.Shadow.copy(alpha = 0.18f), spotColor = Ink.Shadow.copy(alpha = 0.18f))

/** Оттенок объёмной кнопки. */
enum class ButtonTone { Accent, Warm }

/**
 * Объёмная кнопка, как в настольной игре: у плашки есть нижняя кромка, и при нажатии
 * она «проседает» на её высоту. Отклик ощущается пальцем, а не только глазом.
 */
@Composable
private fun ChunkyButton(
    onClick: () -> Unit,
    modifier: Modifier,
    face: Color,
    lip: Color,
    border: Color?,
    enabled: Boolean,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lipHeight = 4.dp
    val sink by animateDpAsState(
        if (pressed && enabled) lipHeight else 0.dp,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "sink",
    )
    val glossy = !Ink.palette.isDark
    Row(
        modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind {
                val lipPx = lipHeight.toPx()
                val sinkPx = sink.toPx()
                val faceSize = Size(size.width, size.height - lipPx)
                val r = CornerRadius(16.dp.toPx())
                drawRoundRect(lip, topLeft = Offset(0f, lipPx), size = faceSize, cornerRadius = r)
                drawRoundRect(face, topLeft = Offset(0f, sinkPx), size = faceSize, cornerRadius = r)
                if (border != null) {
                    drawRoundRect(border, topLeft = Offset(0f, sinkPx), size = faceSize, cornerRadius = r, style = Stroke(1.5.dp.toPx()))
                }
                if (glossy && border == null) {
                    // Блик по верху плашки — кнопка выглядит выпуклой.
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), startY = sinkPx, endY = sinkPx + faceSize.height * 0.55f),
                        topLeft = Offset(0f, sinkPx), size = faceSize, cornerRadius = r,
                    )
                }
            }
            .padding(bottom = lipHeight)
            .graphicsLayer { translationY = sink.toPx() }
            .heightIn(min = 52.dp)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

/** Главная кнопка: зелёная, как шарф Финна, с объёмной кромкой. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.Accent,
) {
    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(300), label = "btn")
    val (faceOn, lipOn, fgOn) = when (tone) {
        ButtonTone.Accent -> Triple(Ink.Accent, Ink.AccentDeep, Ink.OnAccent)
        ButtonTone.Warm -> Triple(Ink.Warm, Ink.WarmDeep, Ink.OnWarm)
    }
    val fg = lerpColor(Ink.Faint, fgOn, on)
    ChunkyButton(
        onClick = onClick,
        modifier = modifier,
        face = lerpColor(Ink.Elevated, faceOn, on),
        lip = lerpColor(Ink.High, lipOn, on),
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

/** Второстепенная кнопка: светлая плашка с рамкой и такой же «проседающей» кромкой. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
) {
    ChunkyButton(
        onClick = onClick,
        modifier = modifier,
        face = Ink.Raised,
        lip = Ink.LineStrong,
        border = Ink.LineStrong,
        enabled = true,
        horizontalArrangement = Arrangement.Center,
        contentPadding = contentPadding,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ink.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink.Text, textAlign = TextAlign.Center)
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

/** Кнопка озвучки: пока звучит — заливается цветом шарфа, от неё расходятся кольца. */
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
        if (playing) {
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
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Озвучить",
                tint = if (playing) Ink.OnAccent else Ink.Accent,
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
            .background(Ink.AccentSoft, CircleShape)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Ink.Accent,
    )
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.background(Ink.Surface, CircleShape).border(1.dp, Ink.Line, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Ink.Soft,
    )
}

/**
 * Полоска прогресса: скруглённая «дорожка» с заливкой цвета шарфа. По заливке
 * изредка пробегает блик — прогресс выглядит живым, а не нарисованным.
 */
@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Ink.Accent,
    track: Color = Ink.High,
    height: Dp = 6.dp,
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "progress")
    val shine by rememberInfiniteTransition(label = "shine").animateFloat(
        -0.4f, 1.8f, infiniteRepeatable(tween(3200, easing = Easing.Standard), RepeatMode.Restart), label = "shine",
    )
    val trackColor = if (track == Ink.Bg) Ink.Surface else track
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = r)
        val w = size.width * p
        if (w > 0f) {
            val fillW = w.coerceAtLeast(size.height)
            drawRoundRect(color, size = Size(fillW, size.height), cornerRadius = r)
            if (shine in 0f..1f) {
                val x = fillW * shine
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startX = x - size.height * 4, endX = x + size.height * 4,
                    ),
                    size = Size(fillW, size.height), cornerRadius = r,
                )
            }
        }
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

/**
 * Пустое состояние: Финн сам объясняет, что произошло, — заголовок над ним,
 * пояснение в облачке. Без позы — значок.
 */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, mascot: Finn? = null) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Ink.Fg)
        if (mascot != null) {
            SpeechBubble(message, tail = Tail.Bottom, tailAt = 0.5f, delayMs = 320)
            Mascot(mascot, 132.dp, delayMs = 120)
        } else {
            Icon(Icons.Rounded.Inventory2, null, tint = Ink.Faint, modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted, textAlign = TextAlign.Center)
        }
    }
}

/** Верхняя панель внутренних экранов: круглая кнопка назад, необязательный заголовок и действия. */
@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().background(Ink.Bg).windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
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

/** Слайдер: мягкая дорожка цвета шарфа и круглая ручка, которая подрастает под пальцем. */
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
    val grow by animateFloatAsState(if (dragged || pressed) 1.25f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "thumb")
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
