package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapAnimationTest {
    @Test
    fun celebrationPulse_hasTwoFinitePeaksAndReturnsToZero() {
        assertEquals(0f, celebrationPulseAlpha(0f))
        assertTrue(celebrationPulseAlpha(0.25f) > 0.55f)
        assertTrue(celebrationPulseAlpha(0.5f) < 0.001f)
        assertTrue(celebrationPulseAlpha(0.75f) > 0.55f)
        assertTrue(celebrationPulseAlpha(1f) < 0.001f)
    }

    @Test
    fun celebrationPulse_clampsProgress() {
        assertEquals(0f, celebrationPulseAlpha(-1f))
        assertTrue(celebrationPulseAlpha(2f) < 0.001f)
    }
}
