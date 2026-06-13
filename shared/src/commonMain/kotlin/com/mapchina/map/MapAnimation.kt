package com.mapchina.map

import kotlinx.coroutines.delay
import kotlin.time.TimeSource

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
