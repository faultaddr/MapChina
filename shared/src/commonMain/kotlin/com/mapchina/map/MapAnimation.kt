package com.mapchina.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun animatePulse(animatable: Animatable<Float, AnimationVector1D>) {
    animatable.animateTo(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
}

fun animateCameraMove(
    viewport: ViewportState,
    targetLng: Double,
    targetLat: Double,
    targetZoom: Float,
    scope: CoroutineScope
) {
    scope.launch {
        launch {
            val anim = Animatable(viewport.centerLng.toFloat())
            anim.animateTo(targetLng.toFloat(), tween(400)) { viewport.centerLng = value.toDouble() }
        }
        launch {
            val anim = Animatable(viewport.centerLat.toFloat())
            anim.animateTo(targetLat.toFloat(), tween(400)) { viewport.centerLat = value.toDouble() }
        }
        launch {
            val anim = Animatable(viewport.zoomLevel)
            anim.animateTo(targetZoom, tween(400)) { viewport.zoomLevel = value }
        }
    }
}
