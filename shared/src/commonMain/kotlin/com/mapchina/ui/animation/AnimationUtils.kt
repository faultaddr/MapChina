package com.mapchina.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===== Button press feedback (visual only, no click handling) =====
// Pass interactionSource from parent (e.g. Button) to avoid double-clickable conflict.
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    animationScale: Float = 1f
): Modifier = composed {
    val scale = LocalAnimationScale.current * animationScale
    if (scale == 0f) return@composed this
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val anim = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            anim.animateTo(
                AnimationSpecs.Scale.buttonPress,
                animationSpec = tween((AnimationSpecs.Duration.buttonPress * scale).toInt())
            )
        } else {
            anim.animateTo(1f, animationSpec = AnimationSpecs.springGentle)
        }
    }

    this
        .graphicsLayer { scaleX = anim.value; scaleY = anim.value }
        .then(
            if (interactionSource == null) Modifier.clickable(interactionSource = source, indication = null) {}
            else Modifier
        )
}

// ===== Card press feedback =====
fun Modifier.cardPress(
    onClick: () -> Unit,
    animationScale: Float = 1f
): Modifier = composed {
    val scale = LocalAnimationScale.current * animationScale
    if (scale == 0f) return@composed this.clickable(onClick = onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val anim = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            anim.animateTo(
                AnimationSpecs.Scale.cardPress,
                animationSpec = tween((AnimationSpecs.Duration.buttonPress * scale).toInt())
            )
        } else {
            anim.animateTo(1f, animationSpec = AnimationSpecs.springGentle)
        }
    }

    this
        .graphicsLayer { scaleX = anim.value; scaleY = anim.value }
        .clickable(interactionSource = interactionSource, indication = null) { onClick() }
}

// ===== Elastic pop-in entrance =====
@Composable
fun rememberPopInAnimation(initialScale: Float = AnimationSpecs.Scale.popInFrom): Animatable<Float, AnimationVector1D> {
    val scale = LocalAnimationScale.current
    val anim = remember { Animatable(initialScale) }
    LaunchedEffect(Unit) {
        if (scale > 0f) anim.animateTo(1f, animationSpec = AnimationSpecs.springBouncy)
        else anim.snapTo(1f)
    }
    return anim
}

// ===== Staggered entrance =====
fun Modifier.staggeredEntrance(
    index: Int,
    delayPerItem: Int = AnimationSpecs.Stagger.listItem
): Modifier = composed {
    val scale = LocalAnimationScale.current
    if (scale == 0f) return@composed this
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        delay((index * delayPerItem * scale).toLong())
        launch {
            translationY.animateTo(0f, animationSpec = AnimationSpecs.springGentle)
        }
        alpha.animateTo(1f, animationSpec = AnimationSpecs.tweenSlowEase)
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}

// ===== Number counter animation =====
@Composable
fun animateCount(targetValue: Int, maxDuration: Int = 800): Float {
    val scale = LocalAnimationScale.current
    val animatedValue = remember { Animatable(0f) }
    val duration = if (targetValue <= 10) 300 else maxDuration.coerceAtMost(800)
    LaunchedEffect(targetValue) {
        if (scale > 0f) {
            animatedValue.animateTo(
                targetValue.toFloat(),
                animationSpec = tween(durationMillis = (duration * scale).toInt(), easing = LinearOutSlowInEasing)
            )
        } else {
            animatedValue.snapTo(targetValue.toFloat())
        }
    }
    return animatedValue.value
}
