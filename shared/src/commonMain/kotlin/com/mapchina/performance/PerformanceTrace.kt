package com.mapchina.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

private const val TracePrefix = "MAPCHINA_PERF"

@Composable
fun RecompositionProbe(name: String) {
    if (!performanceTracingEnabled()) return

    val count = remember(name) { mutableIntStateOf(0) }
    SideEffect {
        count.intValue += 1
        performanceLog(formatRecompositionEvent(name, count.intValue))
    }
}

fun formatRecompositionEvent(name: String, count: Int): String =
    "$TracePrefix|RECOMPOSE|$name|$count"

expect fun performanceTracingEnabled(): Boolean

expect fun performanceLog(message: String)
