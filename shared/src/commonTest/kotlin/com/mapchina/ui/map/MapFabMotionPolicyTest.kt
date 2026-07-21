package com.mapchina.ui.map

import com.mapchina.map.focusOverlayOpacity
import com.mapchina.map.mapLayerTransitionDurationMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapFabMotionPolicyTest {
    @Test
    fun aurora_runsOnlyWhenCollapsedAndMotionAllowed() {
        assertTrue(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = false,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = true,
                reducedMotion = false,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = true,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = false,
                isScreenActive = false
            )
        )
    }

    @Test
    fun activeLayerFade_isRemovedForReducedMotion() {
        assertEquals(220, mapLayerTransitionDurationMillis(false))
        assertEquals(0, mapLayerTransitionDurationMillis(true))
    }

    @Test
    fun focusedMap_dimsOnlyNonSelectedActiveRegions() {
        assertEquals(
            1f,
            focusOverlayOpacity(isSelected = true, hasFocus = true, progress = 1f)
        )
        assertEquals(
            0.25f,
            focusOverlayOpacity(isSelected = false, hasFocus = true, progress = 1f)
        )
        assertEquals(
            1f,
            focusOverlayOpacity(isSelected = false, hasFocus = false, progress = 1f)
        )
    }
}
