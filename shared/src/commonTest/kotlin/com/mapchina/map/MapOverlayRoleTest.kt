package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class MapOverlayRoleTest {
    private val square = "[[-1.0,-1.0],[1.0,-1.0],[1.0,1.0],[-1.0,1.0],[-1.0,-1.0]]"
    private val style = OverlayStyle(
        fillColor = 0xFFFFFFFF,
        strokeColor = 0xFF000000,
        strokeWidth = 1f,
        alpha = 1f
    )

    @Test
    fun orderedRegionOverlays_drawsContextBeforeActive() {
        val coords = listOf(listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0))
        val state = RenderState(
            overlays = linkedMapOf(
                "active" to OverlayData(coords, style, role = OverlayRole.ACTIVE),
                "context" to OverlayData(
                    coords,
                    style,
                    role = OverlayRole.CONTEXT,
                    opacityMultiplier = 0.25f
                )
            )
        )

        assertEquals(
            listOf("context", "active"),
            state.orderedRegionOverlays().map { it.key }
        )
    }

    @Test
    fun contextOverlay_isExcludedFromTapHitTesting() {
        val controller = MapController()
        controller.viewport.canvasWidth = 100f
        controller.viewport.canvasHeight = 100f
        controller.viewport.moveTo(0.0, 0.0, ViewportState.BASE_ZOOM)
        controller.addOverlay(
            regionId = "context",
            boundary = square,
            style = style,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )
        controller.updateHitTestBounds(mapOf("context" to Rect(35f, 35f, 65f, 65f)))
        var tapped: String? = null
        controller.setOnRegionTapListener { tapped = it }

        controller.handleTap(Offset(50f, 50f))

        assertNull(tapped)
    }

    @Test
    fun changingContextToActive_restoresTapHitTesting() {
        val controller = MapController()
        controller.viewport.canvasWidth = 100f
        controller.viewport.canvasHeight = 100f
        controller.viewport.moveTo(0.0, 0.0, ViewportState.BASE_ZOOM)
        controller.addOverlay(
            regionId = "region",
            boundary = square,
            style = style,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )
        controller.updateHitTestBounds(mapOf("region" to Rect(35f, 35f, 65f, 65f)))
        controller.updateOverlayRole("region", OverlayRole.ACTIVE, 1f)
        var tapped: String? = null
        controller.setOnRegionTapListener { tapped = it }

        controller.handleTap(Offset(50f, 50f))

        assertEquals("region", tapped)
    }

    @Test
    fun presentationUpdate_reusesParsedGeometry() {
        val controller = MapController()
        controller.addOverlay("region", square, style)
        val before = controller.renderState.value.overlays["region"]?.coords

        controller.updateOverlayPresentation(
            regionId = "region",
            style = style.copy(alpha = 0.5f),
            isVisited = true,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )

        val after = controller.renderState.value.overlays["region"]?.coords
        assertSame(before, after)
    }
}
