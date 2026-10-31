package com.marat.albion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** Число, которое «набегает» при появлении и плавно меняется дальше. */
@Composable
fun AnimatedNumber(value: Int, style: TextStyle, color: Color = Color.Unspecified, suffix: String = "") {
    val reduced = LocalReducedMotion.current
    var target by remember { mutableStateOf(if (reduced) value else 0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(if (reduced) 0 else 360, easing = Easing.Standard), label = "number")
    Text("$shown$suffix", style = style, color = color)
}

/** Central motion policy also covers expanding panels and changing exercise answers. */
@Composable
fun QuietAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
    exit: androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
    label: String = "visibility",
    content: @Composable androidx.compose.animation.AnimatedVisibilityScope.() -> Unit,
) {
    val reduced = LocalReducedMotion.current
    androidx.compose.animation.AnimatedVisibility(visible, modifier,
        enter = if (reduced) androidx.compose.animation.EnterTransition.None else enter,
        exit = if (reduced) androidx.compose.animation.ExitTransition.None else exit,
        label = label, content = content)
}

@Composable
fun <S> QuietAnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    transitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<S>.() -> androidx.compose.animation.ContentTransform = {
        androidx.compose.animation.ContentTransform(androidx.compose.animation.fadeIn(), androidx.compose.animation.fadeOut())
    },
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.TopStart,
    label: String = "content",
    contentKey: (S) -> Any? = { it },
    content: @Composable androidx.compose.animation.AnimatedContentScope.(S) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    androidx.compose.animation.AnimatedContent(targetState, modifier, transitionSpec = {
        if (reduced) androidx.compose.animation.ContentTransform(androidx.compose.animation.EnterTransition.None,
            androidx.compose.animation.ExitTransition.None, sizeTransform = null)
        else transitionSpec()
    }, contentAlignment = contentAlignment, label = label, contentKey = contentKey, content = content)
}
