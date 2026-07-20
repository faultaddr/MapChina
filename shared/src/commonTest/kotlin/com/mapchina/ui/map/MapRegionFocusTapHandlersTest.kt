package com.mapchina.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.mapchina.map.MapController
import com.mapchina.map.OverlayStyle
import com.mapchina.map.ViewportState
import kotlin.test.Test
import kotlin.test.assertEquals

class MapRegionFocusTapHandlersTest {
    @Test
    fun doubleTapCollapsedFromRapidTaps_usesTheRegionFocusHandler() {
        val controller = MapController()
        controller.viewport.canvasWidth = 100f
        controller.viewport.canvasHeight = 100f
        controller.viewport.moveTo(0.0, 0.0, ViewportState.BASE_ZOOM)
        controller.addOverlay(
            regionId = "latest",
            boundary = "[[-1.0,-1.0],[1.0,-1.0],[1.0,1.0],[-1.0,1.0],[-1.0,-1.0]]",
            style = OverlayStyle(
                fillColor = 0xFFFFFFFF,
                strokeColor = 0xFF000000,
                strokeWidth = 1f,
                alpha = 1f
            )
        )
        controller.updateHitTestBounds(
            mapOf("latest" to Rect(35f, 35f, 65f, 65f))
        )
        val focusedRegions = mutableListOf<String>()

        controller.installRegionFocusTapHandlers { regionId ->
            focusedRegions += regionId
        }
        controller.handleDoubleTap(Offset(50f, 50f))

        assertEquals(listOf("latest"), focusedRegions)
    }
}
