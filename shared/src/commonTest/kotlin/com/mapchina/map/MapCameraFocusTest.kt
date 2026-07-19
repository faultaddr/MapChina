package com.mapchina.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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

    @Test
    fun setCameraImmediately_cancelsInFlightFocus() = runTest {
        assertImmediateCameraUpdateCancelsFocus { controller ->
            controller.setCamera(
                lat = 20.0,
                lng = 80.0,
                zoomLevel = 5f,
                animated = false
            )
        }
    }

    @Test
    fun zoomToBoundsImmediately_cancelsInFlightFocus() = runTest {
        assertImmediateCameraUpdateCancelsFocus { controller ->
            controller.zoomToBounds(
                minLng = 110.0,
                maxLng = 112.0,
                minLat = 20.0,
                maxLat = 22.0,
                animated = false
            )
        }
    }

    @Test
    fun fitChinaImmediately_cancelsInFlightFocus() = runTest {
        assertImmediateCameraUpdateCancelsFocus { controller ->
            controller.fitChinaInView(animated = false)
        }
    }

    private suspend fun TestScope.assertImmediateCameraUpdateCancelsFocus(
        updateImmediately: (MapController) -> Unit
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val controller = MapController()
        var requestCompletions = 0
        var globalCompletions = 0
        controller.setOnCameraAnimCompleteListener { globalCompletions += 1 }

        try {
            controller.focusCamera(
                lat = 45.0,
                lng = 120.0,
                zoomLevel = 8f,
                insets = ViewportInsets()
            ) {
                requestCompletions += 1
            }
            runCurrent()

            updateImmediately(controller)
            val expectedCamera = controller.viewport.camera

            withContext(Dispatchers.Default) {
                delay(700L)
            }
            advanceUntilIdle()

            assertEquals(
                Triple(expectedCamera, 0, 0),
                Triple(
                    controller.viewport.camera,
                    requestCompletions,
                    globalCompletions
                )
            )
        } finally {
            controller.dispose()
            Dispatchers.resetMain()
        }
    }
}
