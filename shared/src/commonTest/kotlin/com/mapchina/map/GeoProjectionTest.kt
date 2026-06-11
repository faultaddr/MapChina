package com.mapchina.map

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoProjectionTest {

    @Test
    fun project_center_returns_canvas_center() {
        val proj = GeoProjection(
            viewCenterLng = 104.0,
            viewCenterLat = 35.5,
            scale = 10f,
            canvasWidth = 400f,
            canvasHeight = 800f
        )
        val result = proj.project(104.0, 35.5)
        assertEquals(200f, result.x, 0.1f)
        assertEquals(400f, result.y, 0.1f)
    }

    @Test
    fun project_offset_east_moves_right() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val result = proj.project(105.0, 35.5)
        assertTrue(result.x > 200f, "East of center should be right of canvas center")
    }

    @Test
    fun project_offset_north_moves_up() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val result = proj.project(104.0, 36.5)
        assertTrue(result.y < 400f, "North of center should be above canvas center")
    }

    @Test
    fun unproject_roundtrip_at_center() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val (lng, lat) = proj.unproject(200f, 400f)
        assertEquals(104.0, lng, 0.001)
        assertEquals(35.5, lat, 0.001)
    }

    @Test
    fun unproject_roundtrip_at_offset() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val (lng, lat) = proj.unproject(300f, 300f)
        val (x, y) = proj.project(lng, lat)
        assertEquals(300f, x, 0.5f)
        assertEquals(300f, y, 0.5f)
    }

    @Test
    fun project_beijing_and_shanghai_have_reasonable_distance() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val beijing = proj.project(116.4, 39.9)
        val shanghai = proj.project(121.5, 31.2)
        val dx = shanghai.x - beijing.x
        val dy = shanghai.y - beijing.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        assertTrue(dist > 50f, "Beijing and Shanghai should be clearly separated on map")
    }

    @Test
    fun project_symmetric_about_center() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val east = proj.project(105.0, 35.5)
        val west = proj.project(103.0, 35.5)
        val centerX = 200f
        assertEquals(centerX - west.x, east.x - centerX, 0.1f, "Should be symmetric about center")
    }
}
