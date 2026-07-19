package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewportStateTest {

    @Test
    fun default_center_is_china_center() {
        val vp = ViewportState()
        assertEquals(104.0, vp.centerLng)
        assertEquals(35.5, vp.centerLat)
        assertEquals(3.5f, vp.zoomLevel)
    }

    @Test
    fun panBy_updates_center() {
        val vp = ViewportState()
        vp.canvasWidth = 400f
        vp.canvasHeight = 800f
        vp.panBy(Offset(10f, 0f))
        assertTrue(vp.centerLng < 104.0, "Pan right should decrease centerLng (screen x inverted to geo)")
    }

    @Test
    fun zoomBy_increases_zoom_level() {
        val vp = ViewportState()
        vp.canvasWidth = 400f
        vp.canvasHeight = 800f
        vp.zoomBy(1f, Offset(200f, 400f))
        assertTrue(vp.zoomLevel > 3.5f)
    }

    @Test
    fun zoom_level_clamped_to_valid_range() {
        val vp = ViewportState()
        vp.canvasWidth = 400f
        vp.canvasHeight = 800f
        vp.zoomBy(-100f, Offset(200f, 400f))
        assertEquals(2f, vp.zoomLevel)
        vp.zoomBy(100f, Offset(200f, 400f))
        assertEquals(15f, vp.zoomLevel)
    }

    @Test
    fun toProjection_returns_valid_projection() {
        val vp = ViewportState()
        val proj = vp.toProjection(400f, 800f)
        assertEquals(104.0, proj.viewCenterLng)
        assertEquals(35.5, proj.viewCenterLat)
    }

    @Test
    fun moveTo_updates_state() {
        val vp = ViewportState()
        vp.moveTo(116.4, 39.9, 10f)
        assertEquals(116.4, vp.centerLng)
        assertEquals(39.9, vp.centerLat)
        assertEquals(10f, vp.zoomLevel)
    }

    @Test
    fun chinaFit_usesMainlandDisplayRangeWithoutChangingFullBounds() {
        val vp = ViewportState()
        vp.canvasWidth = 1280f
        vp.canvasHeight = 2600f

        val (_, centerLat, zoom) = vp.computeChinaFitTarget()

        assertEquals(34.5, centerLat, 0.01)
        assertTrue(zoom >= ViewportState.BASE_ZOOM)
        assertEquals(14.0, ViewportState.CHINA_MIN_LAT)
    }

    @Test
    fun boundsFit_withBottomCard_movesContentAbovePhysicalCenter() {
        val viewport = ViewportState().apply {
            canvasWidth = 1080f
            canvasHeight = 2400f
        }
        val target = viewport.computeBoundsFitTarget(
            minLng = 118.0,
            maxLng = 123.0,
            minLat = 28.0,
            maxLat = 35.0,
            insets = ViewportInsets(topPx = 180f, bottomPx = 760f)
        )
        val projection = viewport.toProjection(1080f, 2400f).copy(
            viewCenterLng = target.centerLng,
            viewCenterLat = target.centerLat,
            scale = ViewportState.BASE_SCALE *
                2f.pow(target.zoomLevel - ViewportState.BASE_ZOOM)
        )

        assertTrue(projection.project(120.5, 31.5).y < 1200f)
    }
}
