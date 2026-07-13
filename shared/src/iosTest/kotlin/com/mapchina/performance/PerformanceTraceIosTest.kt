package com.mapchina.performance

import kotlin.test.Test

class PerformanceTraceIosTest {
    @Test
    fun performanceLog_acceptsMachineReadableEventWithoutCrashing() {
        performanceLog("MAPCHINA_PERF|RECOMPOSE|MapScreen|1")
    }
}
