package com.mapchina.ui.carving

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformCarvingInputPolicyTest {

    @Test
    fun finalPointerUpPoint_isRetainedUnlessItDuplicatesTheLastSample() {
        val active = listOf(
            PlatformPoint(x = 10f, y = 10f, pressure = 0.5f, elapsedTimeMillis = 0L),
            PlatformPoint(x = 40f, y = 50f, pressure = 0.6f, elapsedTimeMillis = 16L)
        )
        val up = PlatformPoint(x = 70f, y = 90f, pressure = 0.7f, elapsedTimeMillis = 24L)

        val finalized = finalizePlatformPoints(active, up)

        assertEquals(up, finalized.last())
        assertEquals(3, finalized.size)
        assertEquals(
            active,
            finalizePlatformPoints(active, active.last())
        )
    }
}
