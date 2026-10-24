package com.marat.albion.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.marat.albion.R
import kotlin.math.roundToInt

/**
 * Финн, выглядывающий из-за края. Картинки собирает scripts/convert_mascots.py.
 *
 * [HOLD] и [SIDE] — отдельные рисунки: Финн держится крыльями за верхнюю кромку и выглядывает
 * из-за бокового края. Остальные — голова с шарфом из обычных поз: для «вниз головой» и «из угла».
 */
enum class FinnPeek(@DrawableRes val res: Int) {
    HOLD(R.drawable.finn_peek_hold),
    SIDE(R.drawable.finn_peek_side),
    HELLO(R.drawable.finn_peek_hello),
    CHEER(R.drawable.finn_peek_cheer),
}

/** Откуда выглядывает Финн относительно элемента. */
enum class PeekFrom {
    /** Из-за верхнего края — над карточкой или кнопкой. */
    Top,
    /** Из-за левого края наружу. */
    Start,
    /** Из-за правого края наружу. */
    End,
    /** Изнутри снизу — поднимается над нижним краем экрана или панели. */
    InBottom,
    /** Изнутри слева — заглядывает в экран с левого края. */
    InStart,
    /** Изнутри справа — заглядывает в экран с правого края. */
    InEnd,
    /** Свисает вниз головой с верхнего края. */
    Ceiling,
    /** По диагонали из правого нижнего угла. */
    CornerBottomEnd,
    /** По диагонали из левого нижнего угла. */
    CornerBottomStart,
}

/**
 * Где на рисунке проходит край, за который держится Финн: выше — то, что видно над кромкой,
 * ниже — крылья, перекинутые через неё. Печатает scripts/convert_mascots.py.
 */
private const val HOLD_EDGE = 0.8834f

/** Где на рисунке [FinnPeek.SIDE] проходит боковой край: левее — Финн, правее — крыло на кромке. */
private const val SIDE_EDGE = 0.9474f

/** «Дыхание» рисунков со срезом: на сколько они растягиваются от кромки на пике покачивания (на единицу sway). */
private const val BREATH = 0.006f

/**
 * Тело на рисунке HOLD чуть продлено за линию края (scripts/convert_mascots.py), чтобы срез был
 * непрозрачным при любом округлении. Под кромкой рисуются только крылья — вне этой полосы по ширине.
 */
private const val HOLD_BODY_FROM = 0.4417f
private const val HOLD_BODY_TO = 0.7669f

/** На рисунке SIDE за край заходит только крыло — в этой полосе по высоте; тело продлено за край вне её. */
private const val SIDE_WING_FROM = 0.6089f
private const val SIDE_WING_TO = 0.8101f

/** Голова из обычной позы срезана с запасом — эта доля остаётся спрятанной за краем. */
private const val HEAD_SINK = 0.16f

/**
 * Финн выглядывает из-за края элемента. То, что «за» краем, срезается ровно по нему,
 * а крылья, которые держатся за кромку, ложатся поверх элемента. Работает с любым фоном
 * и порядком модификаторов. Рисуется поверх соседей — подбирайте [at] так, чтобы не закрыть текст.
 *
 * [at] — положение вдоль края (0 — начало, 1 — конец), [height] — высота рисунка,
 * [mirror] — смотреть в другую сторону, [outset] — насколько край лежит за границей элемента
 * (элемент в отступе 24dp и outset = 24.dp — Финн выглядывает из-за края экрана).
 * Появляется с задержкой [delayMs]: сначала выезжает из-за края, затем перекидывает крылья
 * через кромку, потом едва заметно покачивается.
 */
fun Modifier.peekingFinn(
    peek: FinnPeek = FinnPeek.HOLD,
    from: PeekFrom = PeekFrom.Top,
    at: Float = 0.82f,
    height: Dp = 64.dp,
    mirror: Boolean = false,
    delayMs: Int = 350,
    outset: Dp = 0.dp,
): Modifier = composed {
    val image = ImageBitmap.imageResource(peek.res)
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    val outsetPx = with(density) { outset.toPx() }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val sway by rememberInfiniteTransition(label = "peek").animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )

    drawWithContent {
        drawContent()
        val p = appear.value
        if (p <= 0f) return@drawWithContent
        val h = heightPx
        val w = h * image.width / image.height
        if (peek == FinnPeek.SIDE) drawSide(image, from, at, w, h, outsetPx, mirror, p, sway)
        else drawOverEdge(image, peek, from, at, w, h, outsetPx, mirror, p, sway)
    }
}

/**
 * Рисунок, у которого край проходит горизонтально (держится за кромку или голова из позы).
 * В локальной системе край — ось x, «наружу» — вверх; поворот ставит её на нужную сторону элемента.
 */
private fun DrawScope.drawOverEdge(
    image: ImageBitmap, peek: FinnPeek, from: PeekFrom, at: Float,
    w: Float, h: Float, outset: Float, mirror: Boolean, p: Float, sway: Float,
) {
    // У карточки рисунок не вылезает за концы края; у края экрана (In*) это не нужно.
    val inside = from == PeekFrom.InBottom || from == PeekFrom.InStart || from == PeekFrom.InEnd
    val x = if (inside) size.width * at else (size.width * at).coerceIn(w / 2, (size.width - w / 2).coerceAtLeast(w / 2))
    val y = if (inside) size.height * at else (size.height * at).coerceIn(w / 2, (size.height - w / 2).coerceAtLeast(w / 2))
    val (pivot, angle) = when (from) {
        PeekFrom.Top -> Offset(x, -outset) to 0f
        PeekFrom.InBottom -> Offset(x, size.height + outset) to 0f
        PeekFrom.Ceiling -> Offset(x, -outset) to 180f
        PeekFrom.Start -> Offset(-outset, y) to -90f
        PeekFrom.InStart -> Offset(-outset, y) to 90f
        PeekFrom.End -> Offset(size.width + outset, y) to 90f
        PeekFrom.InEnd -> Offset(size.width + outset, y) to -90f
        // Из угла точка вращения — сам угол, лишнее срезают края элемента.
        PeekFrom.CornerBottomEnd -> Offset(size.width, size.height) to -35f
        PeekFrom.CornerBottomStart -> Offset(0f, size.height) to 35f
    }
    val corner = from == PeekFrom.CornerBottomEnd || from == PeekFrom.CornerBottomStart
    val hold = peek == FinnPeek.HOLD
    // Над краем: у HOLD — всё до кромки, у головы — без запаса; из угла голова выходит целиком.
    val above = h * when {
        hold -> HOLD_EDGE
        corner -> 1.05f
        else -> 1f - HEAD_SINK
    }
    val big = (size.width + size.height + h) * 4
    clipRect(
        if (corner) 0f else -big, if (corner) 0f else -big,
        if (corner) size.width else big, if (corner) size.height else big,
    ) {
        // Рисунок со срезом (HOLD) нельзя наклонять и поднимать выше места: срез оторвётся
        // от кромки и под ним мелькнёт фон. Он поднимается без перелёта и «дышит», растягиваясь
        // от кромки; голове из позы, у которой за краем есть запас, можно и покачиваться.
        val rise = if (hold) p.coerceAtMost(1f) else p
        val tilt = if (hold) 0f else sway * p
        val breath = if (hold) 1f + sway * BREATH * rise else 1f
        translate(pivot.x, pivot.y) {
            rotate(angle + tilt, pivot = Offset.Zero) {
                scale(if (mirror) -1f else 1f, breath, pivot = Offset.Zero) {
                    // Выезжает из-за края: видимая часть растёт, всё ниже кромки срезано.
                    clipRect(-w, -h * 2, w, if (corner) h * 2 else 0f) {
                        drawArt(image, -w / 2, -above * rise, w, h)
                    }
                    // Крылья перекидываются через кромку из-за края, когда Финн почти выглянул.
                    if (hold) {
                        val t = wingsProgress(p)
                        if (t > 0f) {
                            val y = -above - (1f - t) * h * (1f - HOLD_EDGE)
                            // Слева и справа от тела: продлённый срез тела под кромкой не рисуем.
                            clipRect(-w, 0f, -w / 2 + w * HOLD_BODY_FROM, h) { drawArt(image, -w / 2, y, w, h, alpha = t) }
                            clipRect(-w / 2 + w * HOLD_BODY_TO, 0f, w, h) { drawArt(image, -w / 2, y, w, h, alpha = t) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [FinnPeek.SIDE]: край проходит вертикально справа от Финна. Без поворота подходит
 * для левого края элемента снаружи и правого края экрана изнутри; для остальных — зеркально.
 */
private fun DrawScope.drawSide(
    image: ImageBitmap, from: PeekFrom, at: Float,
    w: Float, h: Float, outset: Float, mirror: Boolean, p: Float, sway: Float,
) {
    val edgeOnRight = when (from) {
        PeekFrom.Start, PeekFrom.InEnd -> true
        PeekFrom.End, PeekFrom.InStart -> false
        else -> true
    }
    val edgeX = when (from) {
        PeekFrom.Start, PeekFrom.InStart -> -outset
        else -> size.width + outset
    }
    val centerY = size.height * at
    val flip = (if (edgeOnRight) 1f else -1f) * (if (mirror) -1f else 1f)
    val left = w * SIDE_EDGE
    // Как у HOLD: без перелёта и наклона, иначе срез отходит от края и за Финном видна пустота.
    val slide = p.coerceAtMost(1f)
    val breath = 1f + sway * BREATH * slide
    translate(edgeX, centerY) {
        scale(flip * breath, 1f, pivot = Offset.Zero) {
            // Финн слева от края; выезжает из-за него, лишнее справа срезано.
            clipRect(-w * 2, -h, 0f, h) {
                drawArt(image, -left * slide, -h / 2, w, h)
            }
            // Крыло перекидывается через край следом, когда Финн почти выглянул.
            val t = wingsProgress(p)
            // За краем — только полоса крыла; продлённый срез тела остаётся спрятанным.
            if (t > 0f) clipRect(0f, -h / 2 + h * SIDE_WING_FROM, w, -h / 2 + h * SIDE_WING_TO) {
                drawArt(image, -left - (1f - t) * w * (1f - SIDE_EDGE), -h / 2, w, h, alpha = t)
            }
        }
    }
}

/** Крылья появляются в последней трети подъёма — после того, как Финн выглянул из-за края. */
private fun wingsProgress(p: Float): Float = ((p - 0.7f) / 0.3f).coerceIn(0f, 1f)

private fun DrawScope.drawArt(image: ImageBitmap, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f) {
    drawImage(
        image,
        dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.High,
    )
}
