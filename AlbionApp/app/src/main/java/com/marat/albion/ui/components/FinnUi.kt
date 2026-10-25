package com.marat.albion.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.marat.albion.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/** С какой стороны облачка «хвостик», указывающий на Финна. */
enum class Tail { Start, End, Bottom }

/**
 * Реплика Финна. Облачко выскакивает пружиной из хвостика, текст печатается по буквам —
 * так подсказка звучит голосом персонажа, а не системным сообщением. Новая реплика
 * «подпрыгивает» и печатается заново. Раскладка считается по полному тексту, поэтому
 * облачко не растёт во время печати.
 */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tail: Tail = Tail.End,
    tailAt: Float = 0.62f,
    delayMs: Int = 0,
    color: Color = Ink.Raised,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = Ink.Text,
) {
    val pop = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        if (pop.value < 1f) {
            delay(delayMs.toLong())
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        } else {
            pop.snapTo(0.9f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
    LaunchedEffect(text) {
        shown = 0
        delay(delayMs.toLong() + 90)
        while (shown < text.length) {
            delay(if (text[shown] in ".,!?—") 70 else 14)
            shown++
        }
    }
    val border = Ink.Line
    val shadowColor = Ink.Shadow
    val dark = Ink.palette.isDark
    val tailSize = 10.dp
    val origin = when (tail) {
        Tail.End -> TransformOrigin(1f, tailAt)
        Tail.Start -> TransformOrigin(0f, tailAt)
        Tail.Bottom -> TransformOrigin(tailAt, 1f)
    }
    Box(
        modifier
            .graphicsLayer {
                val p = pop.value
                alpha = p.coerceIn(0f, 1f)
                scaleX = 0.6f + 0.4f * p
                scaleY = 0.6f + 0.4f * p
                transformOrigin = origin
            }
            .drawBehind {
                val t = tailSize.toPx()
                val body = when (tail) {
                    Tail.End -> Size(size.width - t, size.height)
                    Tail.Start -> Size(size.width - t, size.height)
                    Tail.Bottom -> Size(size.width, size.height - t)
                }
                val left = if (tail == Tail.Start) t else 0f
                val radius = 18.dp.toPx()
                val shape = Path().apply {
                    addRoundRect(RoundRect(left, 0f, left + body.width, body.height, CornerRadius(radius)))
                }
                val tailPath = Path().apply {
                    when (tail) {
                        Tail.End -> {
                            val y = (size.height * tailAt).coerceIn(radius, size.height - radius)
                            moveTo(body.width - 2f, y - t * 0.8f)
                            quadraticTo(body.width + t * 0.3f, y - t * 0.1f, size.width, y + t * 0.5f)
                            quadraticTo(body.width - t * 0.2f, y + t * 0.6f, body.width - 2f, y + t * 0.7f)
                            close()
                        }
                        Tail.Start -> {
                            val y = (size.height * tailAt).coerceIn(radius, size.height - radius)
                            moveTo(t + 2f, y - t * 0.8f)
                            quadraticTo(t * 0.7f, y - t * 0.1f, 0f, y + t * 0.5f)
                            quadraticTo(t * 1.2f, y + t * 0.6f, t + 2f, y + t * 0.7f)
                            close()
                        }
                        Tail.Bottom -> {
                            val x = (size.width * tailAt).coerceIn(radius, size.width - radius)
                            moveTo(x - t * 0.8f, body.height - 2f)
                            quadraticTo(x - t * 0.1f, body.height + t * 0.3f, x + t * 0.5f, size.height)
                            quadraticTo(x + t * 0.6f, body.height - t * 0.2f, x + t * 0.7f, body.height - 2f)
                            close()
                        }
                    }
                }
                val bubble = Path().apply { op(shape, tailPath, PathOperation.Union) }
                if (!dark) {
                    // Мягкая тёплая тень — облачко лежит над страницей.
                    translate(0f, 3.dp.toPx()) { drawPath(bubble, shadowColor.copy(alpha = 0.08f)) }
                }
                drawPath(bubble, color)
                drawPath(bubble, border, style = Stroke(1.dp.toPx()))
            }
            .padding(
                start = 14.dp + if (tail == Tail.Start) tailSize else 0.dp,
                end = 14.dp + if (tail == Tail.End) tailSize else 0.dp,
                top = 10.dp,
                bottom = 10.dp + if (tail == Tail.Bottom) tailSize else 0.dp,
            )
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            buildAnnotatedString {
                append(text.take(shown))
                withStyle(SpanStyle(color = Color.Transparent)) { append(text.drop(shown)) }
            },
            style = style,
            color = textColor,
        )
    }
}

/**
 * Финн стоит на верхней кромке карточки, как на уступе скалы, и говорит репликой [line].
 * Лапы касаются кромки, тень ложится на карточку — персонаж и интерфейс один объект.
 * Касание Финна — прыжок и [onTap] (например, следующая реплика).
 */
@Composable
fun FinnLedge(
    pose: Finn?,
    line: String?,
    modifier: Modifier = Modifier,
    finnSize: Dp = 112.dp,
    onTap: (() -> Unit)? = null,
    delayMs: Int = 180,
    cardColor: Color = Ink.Raised,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().zIndex(1f), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).padding(bottom = 14.dp), verticalArrangement = Arrangement.Bottom) {
                header?.invoke(this)
                if (line != null) {
                    SpeechBubble(line, Modifier.padding(top = 10.dp), tail = Tail.End, tailAt = 0.55f, delayMs = delayMs + 260)
                }
            }
            if (pose != null) {
                // Лапы чуть заходят на кромку карточки — Финн стоит на ней, а не парит над ней.
                Mascot(pose, finnSize, Modifier.offset(y = finnSize * 0.07f), delayMs = delayMs, onTap = onTap)
            } else {
                Box(Modifier.padding(finnSize / 2))
            }
        }
        HeroCard(Modifier.fillMaxWidth(), color = cardColor, contentPadding = contentPadding, content = content)
    }
}

/**
 * Главная карточка экрана: крупное скругление, тёплая тень и мягкий градиент шарфа сверху —
 * отсюда «растёт» Финн.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    color: Color = Ink.Raised,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val dark = Ink.palette.isDark
    val tint = Ink.AccentSoft
    val border = Ink.Line
    Column(
        modifier
            .then(if (dark) Modifier else Modifier.shadow(10.dp, shape, ambientColor = Ink.Shadow.copy(alpha = 0.25f), spotColor = Ink.Shadow.copy(alpha = 0.25f)))
            .drawBehind {
                val r = CornerRadius(28.dp.toPx())
                drawRoundRect(color, cornerRadius = r)
                drawRoundRect(
                    Brush.verticalGradient(listOf(tint.copy(alpha = if (dark) 0.9f else 0.75f), tint.copy(alpha = 0f)), endY = size.height * 0.7f),
                    cornerRadius = r,
                )
                drawRoundRect(border, cornerRadius = r, style = Stroke(1.dp.toPx()))
            }
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Праздничный салют: конфетти цветов Финна вылетают вверх и осыпаются.
 * Запускается каждый раз, когда меняется [trigger] (и при первом показе, если [onStart]).
 */
@Composable
fun ConfettiBurst(trigger: Any?, modifier: Modifier = Modifier, count: Int = 56, onStart: Boolean = true) {
    val progress = remember { Animatable(if (onStart) 0f else 1f) }
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(trigger) {
        if (!onStart && seed == 0) { seed = 1; return@LaunchedEffect }
        seed++
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2200, easing = androidx.compose.animation.core.LinearEasing))
    }
    val colors = listOf(Ink.Beak, Ink.Accent, Ink.Stripe, Ink.Warm, Ink.AccentDeep, Ink.Stripe)
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f || t <= 0f) return@Canvas
        val random = Random(seed * 7919)
        repeat(count) { i ->
            val startX = size.width * (0.3f + 0.4f * random.nextFloat())
            val vx = (random.nextFloat() - 0.5f) * size.width * 1.3f
            val vy = -size.height * (0.55f + 0.45f * random.nextFloat())
            val gravity = size.height * 1.6f
            val spin = (random.nextFloat() - 0.5f) * 1080f
            val w = (6 + random.nextInt(6)).dp.toPx()
            val h = w * (0.4f + random.nextFloat() * 0.5f)
            val delayT = random.nextFloat() * 0.12f
            val lt = ((t - delayT) / (1f - delayT)).coerceIn(0f, 1f)
            if (lt <= 0f) return@repeat
            val x = startX + vx * lt + sin(lt * 12f + i) * w * 1.5f
            val y = size.height * 0.35f + vy * lt + gravity * lt * lt
            val alpha = (1f - ((lt - 0.7f) / 0.3f).coerceIn(0f, 1f))
            val color = colors[i % colors.size].copy(alpha = alpha)
            rotate(spin * lt, pivot = Offset(x, y)) {
                if (i % 4 == 0) drawCircle(color, radius = w * 0.4f, center = Offset(x, y))
                else drawRoundRect(color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), cornerRadius = CornerRadius(h / 3))
            }
        }
    }
}

/** Плашка с крупной цифрой — для «сегодня», статистики, итогов. */
@Composable
fun StatPill(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    tint: Color = Ink.Surface,
    valueColor: Color = if (value == null || value == 0) Ink.Faint else Ink.Fg,
) {
    Column(
        modifier
            .drawBehind { drawRoundRect(tint, cornerRadius = CornerRadius(18.dp.toPx())) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value == null) Text("—", style = com.marat.albion.ui.theme.AlbionType.numberSmall, color = Ink.Faint)
        else com.marat.albion.ui.theme.AnimatedNumber(value, com.marat.albion.ui.theme.AlbionType.numberSmall, color = valueColor, suffix = suffix)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink.Muted, maxLines = 2)
    }
}
