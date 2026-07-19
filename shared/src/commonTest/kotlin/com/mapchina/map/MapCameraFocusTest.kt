package com.mapchina.map

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapCameraFocusTest {
    @Test
    fun boundsFit_centersRegionInsideSafeViewport() {
        val viewport = ViewportState().apply {
            canvasWidth = 1000f
            canvasHeight = 2000f
        }
        val insets = ViewportInsets(
            leftPx = 40f,
            topPx = 200f,
            rightPx = 40f,
            bottomPx = 600f
        )

        val target = viewport.computeBoundsFitTarget(
            minLng = 100.0,
            maxLng = 110.0,
            minLat = 30.0,
            maxLat = 40.0,
            insets = insets
        )
        val projection = GeoProjection(
            viewCenterLng = target.centerLng,
            viewCenterLat = target.centerLat,
            scale = ViewportState.BASE_SCALE *
                2f.pow(target.zoomLevel - ViewportState.BASE_ZOOM),
            canvasWidth = 1000f,
            canvasHeight = 2000f
        )
        val projectedCenterX = projection.project(105.0, 35.0).x
        val topY = projection.project(105.0, 40.0).y
        val bottomY = projection.project(105.0, 30.0).y

        assertEquals(500f, projectedCenterX, 1f)
        assertEquals(800f, (topY + bottomY) / 2f, 1f)
    }

    @Test
    fun cameraRequestGate_acceptsOnlyLatestRequest() {
        val gate = CameraAnimationRequestGate()
        val first = gate.begin()
        val second = gate.begin()

        assertFalse(gate.isActive(first))
        assertTrue(gate.isActive(second))

        gate.cancel()
        assertFalse(gate.isActive(second))
    }

    @Test
    fun smoothStep_reachesStableEndpoints() {
        assertEquals(0f, smoothStep(0f))
        assertEquals(0.5f, smoothStep(0.5f))
        assertEquals(1f, smoothStep(1f))
    }
}
