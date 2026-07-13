package com.mapchina.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

private const val TracePrefix = "MAPCHINA_PERF"

@Composable
fun RecompositionProbe(name: String) {
    if (!performanceTracingEnabled()) return

    val policy = remember(name) { RecompositionTracePolicy(enabled = true) }
    SideEffect {
        policy.nextEvent(name)?.let(::performanceLog)
    }
}

fun formatRecompositionEvent(name: String, count: Int): String =
    "$TracePrefix|RECOMPOSE|$name|$count"

internal class RecompositionTracePolicy(
    private val enabled: Boolean,
) {
    var count: Int = 0
        private set

    fun nextEvent(name: String): String? {
        if (!enabled) return null

        count += 1
        return formatRecompositionEvent(name, count)
    }
}

expect fun performanceTracingEnabled(): Boolean

expect fun performanceLog(message: String)
