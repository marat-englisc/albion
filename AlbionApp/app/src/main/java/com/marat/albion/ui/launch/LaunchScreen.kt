package com.marat.albion.ui.launch

import com.marat.albion.ui.theme.Typography
import com.marat.albion.ui.theme.Heritage
import com.marat.albion.ui.components.paperGrain
import com.marat.albion.ui.components.engravedSurface

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.AlbionFonts
import com.marat.albion.ui.theme.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Экран загрузки Albion. Системный сплэш Android 12+ показывает только знак «A» на зелёных чернилах
 * (drawable/splash_mark.xml); этот экран начинается с того же знака в той же точке и разворачивает
 * вокруг него мир Old Money:
 *
 *  вступление (~2,8 с) — латунное свечение, кольца прорисовываются,
 *  шкала-«прибор» набирает деления, наклонная орбита из иконки обвивает знак, стартует спутник,
 *  по буквам появляется «albion» и подпись;
 *  петля — пока приложение готовится: спутник кружит со шлейфом, свечение «дышит»,
 *  снизу статус импорта и бегущая полоса;
 *  готовность — экран сразу освобождает интерфейс, даже посреди вступления.
 *
 * «Меньше движения»: без вступления и петли — статичная композиция.
 * Экран одинаков в обеих темах, как акцентная плита (Hero).
 */

private const val INTRO_MS = 2800

private val Ink = com.marat.albion.ui.theme.Ink
private val Brass = Heritage.Brass
private val BrassDeep = com.marat.albion.ui.theme.Hero.Deep
private val Champagne = Heritage.Champagne

/** Системный сплэш без фона иконки рисует 108 единиц вектора на 288 dp — так знак совпадает пиксель в пиксель. */
private val IconBox = 288.dp
/** Наклон орбиты — как в иконке (`A30,10.5 -18`). */
private const val ORBIT_TILT = -18f * PI.toFloat() / 180f

@Composable
fun LaunchScreen(
    /** Приложение готово: материалы проверены или импортированы. */
    ready: Boolean,
    /** Строка статуса внизу во время петли, например «ГОТОВИМ МАТЕРИАЛЫ · 2/5». */
    status: String?,
    onFinished: () -> Unit,
) {
    // No loading layer or intercepted touches once the content is ready.
    if (ready) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }
    val reduced = LocalReducedMotion.current
    val intro = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) intro.snapTo(1f)
        else intro.animateTo(1f, tween(INTRO_MS, easing = LinearEasing))
    }

    // Петли идут с первого кадра: к концу вступления движение уже «разогнано» и не стартует рывком.
    val (orbit, dial, breathe, barLoop) = if (reduced) floatArrayOf(.6f, 0f, .5f, 0f) else {
        val loop = rememberInfiniteTransition(label = "launch")
        val orbit by loop.animateFloat(0f, 2 * PI.toFloat(), infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "orbit")
        val dial by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "dial")
        val breathe by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4_800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
        val bar by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing)), label = "bar")
        floatArrayOf(orbit, dial, breathe, bar)
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Ink).engravedSurface(Champagne).paperGrain(Champagne)
            // Пока материалы готовятся, касания не должны попадать в интерфейс под загрузкой.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .semantics { contentDescription = "Albion загружается" },
    ) {
        val p = if (reduced) 1f else intro.value
        val e = 0f
        val sceneRadius = min(maxWidth, maxHeight * .8f) * .30f
        val lift = 52.dp * ease(seg(p, .45f, .8f))

        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            drawScene(p, e, sceneRadius.toPx(), lift.toPx(), orbit, dial, breathe)
        }

        // Слово-знак и подпись под знаком; движутся вместе с группой.
        Column(
            Modifier.align(Alignment.Center).offset(y = sceneRadius * 1.52f + 44.dp - lift)
                .graphicsLayer { alpha = 1f - e; val s = 1f + .08f * e; scaleX = s; scaleY = s }
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                "albion".forEachIndexed { i, ch ->
                    val la = ease(seg(p, .52f + i * .035f, .7f + i * .035f))
                    Text(
                        ch.toString(),
                        Modifier.graphicsLayer { alpha = la; translationY = (1f - la) * 18.dp.toPx() },
                        style = TextStyle(fontFamily = AlbionFonts.Display, fontWeight = FontWeight.Medium, fontSize = 40.sp, color = Heritage.Cream, letterSpacing = (-.01).em),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val ta = ease(seg(p, .76f, .96f))
            Text(
                "АНГЛИЙСКИЙ КАЖДЫЙ ДЕНЬ",
                Modifier.graphicsLayer { alpha = ta },
                style = Typography.labelMedium.copy(color = Champagne, letterSpacing = (.02f + .3f * (1f - ta)).em),
            )
        }

        // Статус доступен с первого кадра, независимо от декоративного вступления.
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(status ?: "ЗАГРУЗКА", style = Typography.labelMedium.copy(color = Heritage.Cream.copy(alpha = .62f)))
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.size(132.dp, 2.dp)) {
                drawLine(Heritage.Cream.copy(alpha = .1f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                val head = barLoop * 1.3f
                val start = (head - .3f).coerceIn(0f, 1f) * size.width
                val end = head.coerceIn(0f, 1f) * size.width
                if (end > start) drawLine(Champagne, Offset(start, size.height / 2), Offset(end, size.height / 2), size.height, StrokeCap.Round)
            }
        }
    }
}

/** Вся графика одним проходом: свечение, кольца, шкала, орбита, знак и спутник. */
private fun DrawScope.drawScene(
    p: Float, e: Float, rs: Float, lift: Float,
    orbit: Float, dial: Float, breathe: Float,
) {
    val c = Offset(size.width / 2, size.height / 2 - lift)
    val unit = IconBox.toPx() / 108f
    val px = 1.dp.toPx()

    withTransform({ scale(1f + .22f * e, 1f + .22f * e, c) }) {
        // 1. Латунное свечение: разгорается во вступлении и «дышит» в петле.
        val glowIn = ease(seg(p, 0f, .5f))
        val glowR = rs * (1.1f + 1.25f * glowIn) * (.95f + .1f * breathe)
        if (glowIn > 0f) drawCircle(
            Brush.radialGradient(listOf(Brass.copy(alpha = .46f * glowIn), BrassDeep.copy(alpha = .18f * glowIn), Color.Transparent), c, glowR),
            glowR, c,
        )

        // 3. Внутреннее кольцо прорисовывается дугой сверху по часовой.
        val ring = ease(seg(p, .06f, .42f))
        if (ring > 0f) drawArc(Heritage.Cream.copy(alpha = .26f), -90f, 360f * ring, false,
            topLeft = c - Offset(rs * 1.16f, rs * 1.16f), size = androidx.compose.ui.geometry.Size(rs * 2.32f, rs * 2.32f), style = Stroke(px))

        // 4. Шкала-«прибор»: деления набираются по кругу и потом медленно плывут.
        val ticks = (96 * seg(p, .22f, .62f)).toInt()
        val r2 = rs * 1.34f
        for (i in 0 until ticks) {
            val major = i % 8 == 0
            val a = Math.toRadians((-90.0 + i * 3.75 + dial)).toFloat()
            val len = (if (major) 7f else 3.5f) * px
            val dir = Offset(cos(a), sin(a))
            drawLine(Heritage.Cream.copy(alpha = if (major) .42f else .16f), c + dir * r2, c + dir * (r2 + len), px, StrokeCap.Round)
        }

        // 5. Внешнее пунктирное кольцо и метки «+» по диагоналям, съезжающиеся на место.
        val outer = seg(p, .35f, .7f)
        val r3 = rs * 1.52f
        if (outer > 0f) {
            drawCircle(Heritage.Cream.copy(alpha = .12f * outer), r3, c,
                style = Stroke(px, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * px, 6 * px))))
            val marks = ease(seg(p, .45f, .75f))
            for (k in 0..3) {
                val a = (PI / 4 + k * PI / 2).toFloat()
                val at = c + Offset(cos(a), sin(a)) * (r3 + 16 * px * (1f - marks))
                val arm = 4.5f * px
                drawLine(Champagne.copy(alpha = .7f * marks), at - Offset(arm, 0f), at + Offset(arm, 0f), px)
                drawLine(Champagne.copy(alpha = .7f * marks), at - Offset(0f, arm), at + Offset(0f, arm), px)
            }
        }

        // 6. Наклонная орбита из иконки: дальняя половина уходит за знак, ближняя проходит перед ним.
        val orbitCenter = c + Offset(0f, 4f * unit)
        val rx = 30f * unit
        val ry = 10.5f * unit
        fun orbitPoint(t: Float) = orbitCenter + Offset(
            rx * cos(t) * cos(ORBIT_TILT) - ry * sin(t) * sin(ORBIT_TILT),
            rx * cos(t) * sin(ORBIT_TILT) + ry * sin(t) * cos(ORBIT_TILT),
        )
        val orbitDraw = ease(seg(p, .42f, .74f))
        val back = Path(); val front = Path()
        if (orbitDraw > 0f) {
            val steps = (120 * orbitDraw).toInt().coerceAtLeast(1)
            var last: Boolean? = null
            for (s in 0..steps) {
                val t = .6f + s * 2 * PI.toFloat() * orbitDraw / steps
                val pt = orbitPoint(t)
                val isFront = sin(t) > 0f
                val path = if (isFront) front else back
                if (last != isFront) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                last = isFront
            }
            drawPath(back, Heritage.Cream.copy(alpha = .4f), style = Stroke(1.4f * unit, cap = StrokeCap.Round))
        }

        // 7. Знак «A»: виден с первого кадра — там же, где системный сплэш; под ним — ирисовый ореол.
        val mark = Path().apply {
            moveTo(38f, 73f); lineTo(54f, 35f); lineTo(70f, 73f)
            moveTo(44.8f, 61f); lineTo(63.2f, 61f)
        }
        val pulse = 1f - .05f * sin(PI.toFloat() * seg(p, 0f, .3f))
        withTransform({
            translate(c.x - 54f * unit * pulse, c.y - 54f * unit * pulse)
            scale(unit * pulse, unit * pulse, Offset.Zero)
        }) {
            // Мягкий ореол несколькими тонкими слоями: один широкий штрих читался как размытая обводка.
            if (glowIn > 0f) for ((width, alpha) in listOf(7.5f to .05f, 5.5f to .07f, 4f to .1f)) {
                drawPath(mark, Champagne.copy(alpha = alpha * glowIn), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawPath(mark, Heritage.Cream, style = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Heritage.Cream, 2.4f, Offset(54f, 73f))
        }

        if (orbitDraw > 0f) drawPath(front, Heritage.Cream.copy(alpha = .95f), style = Stroke(1.4f * unit, cap = StrokeCap.Round))

        // 8. Спутник со шлейфом: стартует в конце вступления и дальше кружит по орбите.
        val sat = ease(seg(p, .72f, .9f))
        if (sat > 0f) {
            for (k in 14 downTo 0) {
                val t = orbit - k * .055f
                val pt = orbitPoint(t)
                val depth = if (sin(t) > 0f) 1f else .45f
                val fade = (1f - k / 15f)
                if (k == 0) {
                    drawCircle(Champagne.copy(alpha = .35f * sat * depth), 7f * unit, pt)
                    drawCircle(Heritage.Cream.copy(alpha = sat * depth), 2.6f * unit, pt)
                } else drawCircle(Champagne.copy(alpha = .5f * sat * depth * fade * fade), 1.3f * unit * fade + .4f * px, pt)
            }
        }
    }
}

/** Доля отрезка [a, b] шкалы вступления, пройденная к моменту [p]. */
private fun seg(p: Float, a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = FastOutSlowInEasing.transform(x)
