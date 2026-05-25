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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===== Button press feedback (supports animationScale) =====
fun Modifier.pressScale(
    animationScale: Float = 1f
): Modifier = composed {
    if (animationScale == 0f) return@composed this
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(
                AnimationSpecs.Scale.buttonPress,
                animationSpec = tween((AnimationSpecs.Duration.buttonPress * animationScale).toInt())
            )
        } else {
            scale.animateTo(1f, animationSpec = AnimationSpecs.springGentle)
        }
    }

    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(interactionSource = interactionSource, indication = null) {}
}

// ===== Card press feedback (supports animationScale) =====
fun Modifier.cardPress(
    onClick: () -> Unit,
    animationScale: Float = 1f
): Modifier = composed {
    if (animationScale == 0f) return@composed this.clickable(onClick = onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(
                AnimationSpecs.Scale.cardPress,
                animationSpec = tween((AnimationSpecs.Duration.buttonPress * animationScale).toInt())
            )
        } else {
            scale.animateTo(1f, animationSpec = AnimationSpecs.springGentle)
        }
    }

    this
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(interactionSource = interactionSource, indication = null) { onClick() }
}

// ===== Elastic pop-in entrance =====
@Composable
fun rememberPopInAnimation(initialScale: Float = AnimationSpecs.Scale.popInFrom): Animatable<Float, AnimationVector1D> {
    val anim = remember { Animatable(initialScale) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = AnimationSpecs.springBouncy)
    }
    return anim
}

// ===== Staggered entrance =====
fun Modifier.staggeredEntrance(
    index: Int,
    delayPerItem: Int = AnimationSpecs.Stagger.listItem
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        delay((index * delayPerItem).toLong())
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
    val animatedValue = remember { Animatable(0f) }
    val duration = if (targetValue <= 10) 300 else maxDuration.coerceAtMost(800)
    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue.toFloat(),
            animationSpec = tween(durationMillis = duration, easing = LinearOutSlowInEasing)
        )
    }
    return animatedValue.value
}
