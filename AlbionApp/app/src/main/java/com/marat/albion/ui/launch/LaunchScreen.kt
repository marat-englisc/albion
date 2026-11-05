package com.marat.albion.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.marat.albion.R
import com.marat.albion.ui.components.engravedSurface
import com.marat.albion.ui.components.folioFrame
import com.marat.albion.ui.components.paperGrain
import com.marat.albion.ui.theme.AlbionFonts
import com.marat.albion.ui.theme.Heritage
import com.marat.albion.ui.theme.Hero
import com.marat.albion.ui.theme.LocalReducedMotion
import com.marat.albion.ui.theme.Typography

/**
 * A bookbinder's plate: green leather, a Literata initial, laurel tooling and ivory lettering.
 * The first frame uses the same vector, size and position as Android's system splash.
 * Decoration never delays readiness; reduced motion shows the complete, static composition.
 */
@Composable
fun LaunchScreen(
    ready: Boolean,
    status: String?,
    onFinished: () -> Unit,
) {
    if (ready) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }
    val reduced = LocalReducedMotion.current
    val intro = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) intro.snapTo(1f)
        else intro.animateTo(1f, tween(1_200, easing = FastOutSlowInEasing))
    }
    val barLoop = if (reduced) .5f else {
        val loop = rememberInfiniteTransition(label = "loading")
        val bar by loop.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing)), label = "progress")
        bar
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Hero.Gradient)
            .engravedSurface(Heritage.Champagne).paperGrain(Heritage.Champagne)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .semantics { contentDescription = "Albion загружается" },
    ) {
        val p = if (reduced) 1f else intro.value
        val sceneRadius = min(maxWidth, maxHeight * .8f) * .30f
        val lift = 52.dp * segment(p, .08f, .7f)

        // Drawing-only margins: the original sign, wordmark and status positions are retained.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)
            .folioFrame(Heritage.Champagne.copy(alpha = .65f), inset = 5.dp, embellished = true)
            .clearAndSetSemantics {})
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            drawBookplate(segment(p, .05f, .8f), sceneRadius.toPx(), lift.toPx())
        }
        // Android scales a 108-unit splash drawable to 288 dp when no icon background is used.
        Image(
            painterResource(R.drawable.splash_mark), null,
            Modifier.align(Alignment.Center).offset(y = -lift).size(288.dp),
        )

        Column(
            Modifier.align(Alignment.Center).offset(y = sceneRadius * 1.52f + 44.dp - lift)
                .graphicsLayer { alpha = segment(p, .18f, .85f) }
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("albion", style = TextStyle(
                fontFamily = AlbionFonts.Display, fontWeight = FontWeight.Medium,
                fontSize = 40.sp, color = Heritage.Cream, letterSpacing = (-.01).em,
            ))
            Spacer(Modifier.height(10.dp))
            Text("АНГЛИЙСКИЙ КАЖДЫЙ ДЕНЬ",
                style = Typography.labelMedium.copy(color = Heritage.Champagne, letterSpacing = .08.em))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(status ?: "ЗАГРУЗКА", style = Typography.labelMedium.copy(color = Heritage.Cream.copy(alpha = .75f)))
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.size(132.dp, 2.dp)) {
                val y = size.height / 2
                drawLine(Heritage.Champagne.copy(alpha = .2f), Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                val head = barLoop * 1.3f
                val start = (head - .3f).coerceIn(0f, 1f) * size.width
                val end = head.coerceIn(0f, 1f) * size.width
                if (end > start) drawLine(Heritage.Champagne, Offset(start, y), Offset(end, y), size.height, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawBookplate(alpha: Float, radius: Float, lift: Float) {
    if (alpha <= 0f) return
    val center = Offset(size.width / 2, size.height / 2 - lift)
    val foil = Heritage.Champagne
    drawCircle(Brush.radialGradient(listOf(foil.copy(alpha = .07f * alpha), Color.Transparent),
        center, radius * 1.65f), radius * 1.65f, center)
    for ((scale, opacity) in listOf(1.17f to .36f, 1.23f to .16f)) {
        drawCircle(foil.copy(alpha = opacity * alpha), radius * scale, center, style = Stroke(.65.dp.toPx()))
    }
    val finial = 2.dp.toPx()
    for (direction in listOf(Offset(0f, -1f), Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f))) {
        val at = center + direction * (radius * 1.2f)
        val diamond = Path().apply {
            moveTo(at.x, at.y - finial); lineTo(at.x + finial, at.y)
            lineTo(at.x, at.y + finial); lineTo(at.x - finial, at.y); close()
        }
        drawPath(diamond, foil.copy(alpha = .75f * alpha))
    }
}

private fun segment(progress: Float, start: Float, end: Float) =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)
