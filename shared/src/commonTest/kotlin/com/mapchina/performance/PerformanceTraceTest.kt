package com.mapchina.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PerformanceTraceTest {
    @Test
    fun recompositionEvent_hasMachineReadableFormat() {
        assertEquals(
            "MAPCHINA_PERF|RECOMPOSE|MapScreen|42",
            formatRecompositionEvent("MapScreen", 42),
        )
    }

    @Test
    fun disabledPolicy_emitsNothingAndDoesNotAdvanceCount() {
        val policy = RecompositionTracePolicy(enabled = false)

        assertNull(policy.nextEvent("MapScreen"))
        assertEquals(0, policy.count)
    }

    @Test
    fun enabledPolicy_advancesCountAndEmitsSequentialEvents() {
        val policy = RecompositionTracePolicy(enabled = true)

        assertEquals("MAPCHINA_PERF|RECOMPOSE|MapScreen|1", policy.nextEvent("MapScreen"))
        assertEquals("MAPCHINA_PERF|RECOMPOSE|MapScreen|2", policy.nextEvent("MapScreen"))
        assertEquals(2, policy.count)
    }
}
