package com.mapchina.map

import kotlinx.coroutines.delay
import kotlin.time.TimeSource
import kotlin.math.PI
import kotlin.math.sin

internal fun smoothStep(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun mapLayerTransitionDurationMillis(
    reducedMotion: Boolean
): Int = if (reducedMotion) 0 else 220

internal fun focusOverlayOpacity(
    isSelected: Boolean,
    hasFocus: Boolean,
    progress: Float
): Float {
    if (!hasFocus || isSelected) return 1f
    return 1f - progress.coerceIn(0f, 1f) * 0.75f
}

internal class CameraAnimationRequestGate {
    private var sequence = 0L
    private var activeRequest = 0L

    fun begin(): Long {
        sequence += 1L
        activeRequest = sequence
        return activeRequest
    }

    fun isActive(requestId: Long): Boolean = requestId == activeRequest

    fun cancel() {
        activeRequest = 0L
    }
}

fun celebrationPulseAlpha(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val wave = sin(clamped * PI.toFloat() * 2f)
    return wave * wave * 0.62f
}

suspend fun animateCelebrationPulse(onAlpha: (Float) -> Unit) {
    val frameDelayMillis = 16L
    val durationMillis = 1200L
    val steps = (durationMillis / frameDelayMillis).toInt()
    for (step in 0..steps) {
        onAlpha(celebrationPulseAlpha(step.toFloat() / steps))
        if (step < steps) delay(frameDelayMillis)
    }
    onAlpha(0f)
}

suspend fun animatePulse(onAlpha: (Float) -> Unit) {
    val halfCycleMs = 600L
    while (true) {
        val start = TimeSource.Monotonic.markNow()
        while (true) {
            val elapsed = start.elapsedNow().inWholeMilliseconds
            val t = (elapsed.toFloat() / halfCycleMs).coerceIn(0f, 1f)
            val eased = t * t * (3f - 2f * t)
            onAlpha(eased * 0.5f)
            if (t >= 1f) break
            delay(16)
        }
        val start2 = TimeSource.Monotonic.markNow()
        while (true) {
            val elapsed = start2.elapsedNow().inWholeMilliseconds
            val t = (elapsed.toFloat() / halfCycleMs).coerceIn(0f, 1f)
            val eased = t * t * (3f - 2f * t)
            onAlpha((1f - eased) * 0.5f)
            if (t >= 1f) break
            delay(16)
        }
    }
}
