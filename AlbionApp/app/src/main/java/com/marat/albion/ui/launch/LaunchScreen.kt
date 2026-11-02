package com.marat.albion.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.input.pointer.PointerEventType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Экран загрузки Albion. Системный сплэш Android 12+ показывает только знак «A» на графите
 * (drawable/splash_mark.xml); этот экран начинается с того же знака в той же точке и разворачивает
 * вокруг него мир Iris:
 *
 *  вступление (~2,8 с) — ирисовое свечение, сфера раскручивается и замедляется, кольца прорисовываются,
 *  шкала-«прибор» набирает деления, наклонная орбита из иконки обвивает знак, стартует спутник,
 *  по буквам появляется «albion» и подпись;
 *  петля — пока приложение готовится: сфера вращается, спутник кружит со шлейфом, свечение «дышит»,
 *  снизу статус импорта и бегущая полоса;
 *  выход — композиция укрупняется и растворяется, открывая приложение.
 *
 * «Меньше движения»: без вступления и петли — статичная композиция и короткое растворение.
 * Экран одинаков в обеих темах, как акцентная плита (Hero).
 */

private const val INTRO_MS = 2800
/** Повторный запуск, когда всё уже готово, — короткая версия вступления. */
private const val WARM_INTRO_MS = 1300
private const val EXIT_MS = 720

private val Ink = Color(0xFF0A0B0F)
private val Iris = Color(0xFF5B3FF5)
private val IrisDeep = Color(0xFF2C1C9C)
private val IrisLight = Color(0xFFB0A4FF)

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
    /** Процесс уже был запущен и всё готово — короткое вступление. */
    warm: Boolean,
    onFinished: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val intro = remember { Animatable(if (reduced) 1f else 0f) }
    val exit = remember { Animatable(0f) }
    val readyNow by rememberUpdatedState(ready)
    var skip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val play = if (!reduced) launch {
            intro.animateTo(1f, tween(if (warm) WARM_INTRO_MS else INTRO_MS, easing = LinearEasing))
        } else null
        if (reduced) delay(350)
        // Выходим, когда приложение готово и вступление доиграло (касание экрана его досрочно завершает).
        snapshotFlow { readyNow && (intro.value >= 1f || skip) }.first { it }
        play?.cancel()
        exit.animateTo(1f, tween(if (reduced) 220 else EXIT_MS, easing = FastOutSlowInEasing))
        onFinished()
    }

    // Петли идут с первого кадра: к концу вступления движение уже «разогнано» и не стартует рывком.
    val loop = rememberInfiniteTransition(label = "launch")
    val spinLoop by loop.animateFloat(0f, 2 * PI.toFloat(), infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "spin")
    val orbitLoop by loop.animateFloat(0f, 2 * PI.toFloat(), infiniteRepeatable(tween(3_600, easing = LinearEasing)), label = "orbit")
    val dialLoop by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "dial")
    val breatheLoop by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val barLoop by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing)), label = "bar")
    val spin = if (reduced) .38f else spinLoop
    val orbit = if (reduced) .6f else orbitLoop
    val dial = if (reduced) 0f else dialLoop
    val breathe = if (reduced) .5f else breatheLoop

    val statusAlpha by animateFloatAsState(if (intro.value >= 1f && !ready && exit.value == 0f) 1f else 0f, tween(400), label = "status")

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(Ink)
            // Экран перехватывает все касания, чтобы под ним ничего не нажималось; касание досрочно завершает вступление.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        if (event.type == PointerEventType.Release) skip = true
                    }
                }
            }
            .semantics { contentDescription = "Albion загружается" },
    ) {
        val p = intro.value
        val e = exit.value
        val sphere = min(maxWidth, maxHeight * .8f) * .30f
        val lift = 52.dp * ease(seg(p, .45f, .8f))

        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            drawScene(p, e, sphere.toPx(), lift.toPx(), spin, orbit, dial, breathe)
        }

        // Слово-знак и подпись под сферой; движутся вместе с группой.
        Column(
            Modifier.align(Alignment.Center).offset(y = sphere * 1.52f + 44.dp - lift)
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
                        style = TextStyle(fontFamily = AlbionFonts.Display, fontWeight = FontWeight.Medium, fontSize = 40.sp, color = Color.White, letterSpacing = (-.01).em),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val ta = ease(seg(p, .76f, .96f))
            Text(
                "АНГЛИЙСКИЙ КАЖДЫЙ ДЕНЬ",
                Modifier.graphicsLayer { alpha = ta },
                style = TextStyle(fontFamily = AlbionFonts.Tech, fontSize = 10.sp, color = IrisLight, letterSpacing = (.22f + .3f * (1f - ta)).em),
            )
        }

        // Статус загрузки — только в петле, когда вступление доиграло, а приложение ещё готовится.
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 40.dp).graphicsLayer { alpha = statusAlpha }.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(status ?: "ЗАГРУЗКА", style = TextStyle(fontFamily = AlbionFonts.Tech, fontSize = 10.sp, color = Color.White.copy(alpha = .62f), letterSpacing = .2.em))
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.size(132.dp, 2.dp)) {
                drawLine(Color.White.copy(alpha = .1f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                val head = barLoop * 1.3f
                val start = (head - .3f).coerceIn(0f, 1f) * size.width
                val end = head.coerceIn(0f, 1f) * size.width
                if (end > start) drawLine(IrisLight, Offset(start, size.height / 2), Offset(end, size.height / 2), size.height, StrokeCap.Round)
            }
        }
    }
}

/** Вся графика одним проходом: свечение, сфера, кольца, шкала, орбита, знак и спутник. */
private fun DrawScope.drawScene(
    p: Float, e: Float, rs: Float, lift: Float,
    spin: Float, orbit: Float, dial: Float, breathe: Float,
) {
    val c = Offset(size.width / 2, size.height / 2 - lift)
    val unit = IconBox.toPx() / 108f
    val px = 1.dp.toPx()

    withTransform({ scale(1f + .22f * e, 1f + .22f * e, c) }) {
        // 1. Ирисовое свечение: разгорается во вступлении и «дышит» в петле.
        val glowIn = ease(seg(p, 0f, .5f))
        val glowR = rs * (1.1f + 1.25f * glowIn) * (.95f + .1f * breathe)
        if (glowIn > 0f) drawCircle(
            Brush.radialGradient(listOf(Iris.copy(alpha = .46f * glowIn), IrisDeep.copy(alpha = .18f * glowIn), Color.Transparent), c, glowR),
            glowR, c,
        )

        // 2. Сфера: раскручивается быстро и замедляется до спокойного вращения, меридианы проявляются по одному.
        val sphereIn = ease(seg(p, .12f, .55f))
        if (sphereIn > 0f) {
            val whoosh = (1f - easeOut(seg(p, .1f, .78f))) * -3.4f
            drawSphere(c, rs * (.72f + .28f * sphereIn), spin + whoosh, sphereIn, px)
        }

        // 3. Внутреннее кольцо прорисовывается дугой сверху по часовой.
        val ring = ease(seg(p, .06f, .42f))
        if (ring > 0f) drawArc(Color.White.copy(alpha = .26f), -90f, 360f * ring, false,
            topLeft = c - Offset(rs * 1.16f, rs * 1.16f), size = androidx.compose.ui.geometry.Size(rs * 2.32f, rs * 2.32f), style = Stroke(px))

        // 4. Шкала-«прибор»: деления набираются по кругу и потом медленно плывут.
        val ticks = (96 * seg(p, .22f, .62f)).toInt()
        val r2 = rs * 1.34f
        for (i in 0 until ticks) {
            val major = i % 8 == 0
            val a = Math.toRadians((-90.0 + i * 3.75 + dial)).toFloat()
            val len = (if (major) 7f else 3.5f) * px
            val dir = Offset(cos(a), sin(a))
            drawLine(Color.White.copy(alpha = if (major) .42f else .16f), c + dir * r2, c + dir * (r2 + len), px, StrokeCap.Round)
        }

        // 5. Внешнее пунктирное кольцо и метки «+» по диагоналям, съезжающиеся на место.
        val outer = seg(p, .35f, .7f)
        val r3 = rs * 1.52f
        if (outer > 0f) {
            drawCircle(Color.White.copy(alpha = .12f * outer), r3, c,
                style = Stroke(px, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * px, 6 * px))))
            val marks = ease(seg(p, .45f, .75f))
            for (k in 0..3) {
                val a = (PI / 4 + k * PI / 2).toFloat()
                val at = c + Offset(cos(a), sin(a)) * (r3 + 16 * px * (1f - marks))
                val arm = 4.5f * px
                drawLine(IrisLight.copy(alpha = .7f * marks), at - Offset(arm, 0f), at + Offset(arm, 0f), px)
                drawLine(IrisLight.copy(alpha = .7f * marks), at - Offset(0f, arm), at + Offset(0f, arm), px)
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
            drawPath(back, Color.White.copy(alpha = .4f), style = Stroke(1.4f * unit, cap = StrokeCap.Round))
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
                drawPath(mark, IrisLight.copy(alpha = alpha * glowIn), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawPath(mark, Color.White, style = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Color.White, 2.4f, Offset(54f, 73f))
        }

        if (orbitDraw > 0f) drawPath(front, Color.White.copy(alpha = .95f), style = Stroke(1.4f * unit, cap = StrokeCap.Round))

        // 8. Спутник со шлейфом: стартует в конце вступления и дальше кружит по орбите.
        val sat = ease(seg(p, .72f, .9f))
        if (sat > 0f) {
            for (k in 14 downTo 0) {
                val t = orbit - k * .055f
                val pt = orbitPoint(t)
                val depth = if (sin(t) > 0f) 1f else .45f
                val fade = (1f - k / 15f)
                if (k == 0) {
                    drawCircle(IrisLight.copy(alpha = .35f * sat * depth), 7f * unit, pt)
                    drawCircle(Color.White.copy(alpha = sat * depth), 2.6f * unit, pt)
                } else drawCircle(IrisLight.copy(alpha = .5f * sat * depth * fade * fade), 1.3f * unit * fade + .4f * px, pt)
            }
        }
    }
}

/** Сфера из меридианов и параллелей с наклоном оси — та же, что OrbitalField на главной, но ирисовая. */
private fun DrawScope.drawSphere(c: Offset, radius: Float, phase: Float, reveal: Float, px: Float) {
    val tilt = -.35f
    fun project(x: Float, y: Float, z: Float): Offset {
        val rx = x * cos(phase) + z * sin(phase)
        return c + Offset((rx * cos(tilt) - y * sin(tilt)) * radius, (rx * sin(tilt) + y * cos(tilt)) * radius)
    }
    drawCircle(IrisLight.copy(alpha = .5f * reveal), radius, c, style = Stroke(.9f * px))
    val meridians = 12
    val shown = meridians * reveal
    for (m in 0 until meridians) {
        val visible = (shown - m).coerceIn(0f, 1f)
        if (visible <= 0f) break
        val longitude = m * PI.toFloat() / (meridians / 2)
        val path = Path()
        for (step in 0..72) {
            val latitude = -PI.toFloat() / 2 + step * PI.toFloat() / 72
            val pt = project(cos(latitude) * cos(longitude), sin(latitude), cos(latitude) * sin(longitude))
            if (step == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, IrisLight.copy(alpha = .3f * visible), style = Stroke(.7f * px))
    }
    for (parallel in -3..3) {
        val latitude = parallel * PI.toFloat() / 9
        val path = Path()
        for (step in 0..96) {
            val longitude = step * 2 * PI.toFloat() / 96
            val pt = project(cos(latitude) * cos(longitude), sin(latitude), cos(latitude) * sin(longitude))
            if (step == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        val equator = parallel == 0
        drawPath(path, IrisLight.copy(alpha = (if (equator) .55f else .22f) * reveal), style = Stroke((if (equator) 1f else .7f) * px))
    }
}

/** Доля отрезка [a, b] шкалы вступления, пройденная к моменту [p]. */
private fun seg(p: Float, a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = FastOutSlowInEasing.transform(x)
private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

