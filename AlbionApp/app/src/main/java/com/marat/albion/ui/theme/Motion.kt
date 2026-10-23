package com.marat.albion.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Кривые Material «emphasized» — резкий старт, долгое мягкое торможение. */
object Easing {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * Каскадное появление элементов при открытии экрана. Элементы, которые
 * появились позже (при прокрутке), показываются сразу — без повторной анимации.
 */
@Stable
class RevealController internal constructor() {
    internal val start = System.currentTimeMillis()
    fun elapsed() = System.currentTimeMillis() - start
}

@Composable
fun rememberReveal(): RevealController = remember { RevealController() }

fun Modifier.reveal(controller: RevealController, index: Int, step: Long = 25, duration: Int = 260): Modifier = composed {
    val delayMs = (index * step).coerceAtMost(160)
    val skip = remember { controller.elapsed() > delayMs + 150 }
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    val distance = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!skip) {
            delay((delayMs - controller.elapsed()).coerceAtLeast(0))
            progress.animateTo(1f, tween(duration, easing = Easing.EmphasizedDecelerate))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distance
    }
}

/** Пружинное «вдавливание» при нажатии. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Число, которое «набегает» при появлении и плавно меняется дальше. */
@Composable
fun AnimatedNumber(value: Int, style: TextStyle, color: Color = Color.Unspecified, suffix: String = "") {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(360, easing = Easing.Emphasized), label = "number")
    Text("$shown$suffix", style = style, color = color)
}

/** Запуск корутины один раз за жизнь composable — для одноразовых анимаций. */
@Composable
fun rememberOnce(block: suspend () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    remember { scope.launch { block() } }
}
