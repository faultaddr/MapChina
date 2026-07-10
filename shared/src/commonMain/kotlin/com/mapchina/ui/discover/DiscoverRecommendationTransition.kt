package com.mapchina.ui.discover

import androidx.compose.ui.geometry.Rect

data class RecommendationTransitionFrame(
    val bounds: Rect,
    val cornerRadiusDp: Float,
    val backgroundProgress: Float
)

fun recommendationTransitionFrame(
    start: Rect,
    viewportWidthPx: Float,
    heroHeightPx: Float,
    progress: Float
): RecommendationTransitionFrame {
    val fraction = progress.coerceIn(0f, 1f)
    return RecommendationTransitionFrame(
        bounds = Rect(
            left = transitionLerp(start.left, 0f, fraction),
            top = transitionLerp(start.top, 0f, fraction),
            right = transitionLerp(start.right, viewportWidthPx, fraction),
            bottom = transitionLerp(start.bottom, heroHeightPx, fraction)
        ),
        cornerRadiusDp = transitionLerp(8f, 0f, fraction),
        backgroundProgress = fraction
    )
}

private fun transitionLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction
