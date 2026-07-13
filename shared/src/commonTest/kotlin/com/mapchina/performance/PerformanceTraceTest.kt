package com.mapchina.performance

import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceTraceTest {
    @Test
    fun recompositionEvent_hasMachineReadableFormat() {
        assertEquals(
            "MAPCHINA_PERF|RECOMPOSE|MapScreen|42",
            formatRecompositionEvent("MapScreen", 42),
        )
    }
}
