package com.mapchina.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionFocusCoordinatorTest {
    @Test
    fun focus_staysAnimatingUntilMatchingRequestCompletes() {
        val coordinator = RegionFocusCoordinator()
        val request = coordinator.begin("330000")

        assertEquals(
            RegionFocusState.Animating("330000", request),
            coordinator.state.value
        )
        assertTrue(coordinator.complete(request))
        assertEquals(
            RegionFocusState.Focused("330000"),
            coordinator.state.value
        )
    }

    @Test
    fun staleCompletion_cannotReplaceLatestFocus() {
        val coordinator = RegionFocusCoordinator()
        val first = coordinator.begin("330000")
        val second = coordinator.begin("610000")

        assertFalse(coordinator.complete(first))
        assertEquals(
            RegionFocusState.Animating("610000", second),
            coordinator.state.value
        )
        assertTrue(coordinator.complete(second))
        assertEquals(
            RegionFocusState.Focused("610000"),
            coordinator.state.value
        )
    }

    @Test
    fun focusDuration_usesExactDefaultAndReducedMotionValues() {
        assertEquals(650L, regionFocusDurationMillis(reducedMotion = false))
        assertEquals(120L, regionFocusDurationMillis(reducedMotion = true))
    }
}
