package com.mapchina.map

import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HitTesterTest {

    private val squareCoords = listOf(
        listOf(0.0 to 0.0, 10.0 to 0.0, 10.0 to 10.0, 0.0 to 10.0, 0.0 to 0.0)
    )
    private val squareBounds = Rect(0f, 0f, 10f, 10f)

    // Projection where (x,y) screen maps to roughly (x,y) geo
    private val testProj = GeoProjection(
        viewCenterLng = 5.0,
        viewCenterLat = 5.0,
        scale = 1f,
        canvasWidth = 10f,
        canvasHeight = 10f
    )

    @Test
    fun hit_inside_square_returns_region_id() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        assertEquals("region_a", tester.hitTest(5f, 5f, testProj))
    }

    @Test
    fun hit_outside_square_returns_null() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        assertNull(tester.hitTest(15f, 15f, testProj))
    }

    @Test
    fun hit_on_edge_does_not_crash() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        tester.hitTest(10f, 5f, testProj)
    }

    @Test
    fun multiple_regions_returns_a_hit() {
        val smallSquare = listOf(listOf(2.0 to 2.0, 8.0 to 2.0, 8.0 to 8.0, 2.0 to 8.0, 2.0 to 2.0))
        val tester = HitTester(
            bounds = mapOf("big" to squareBounds, "small" to Rect(2f, 2f, 8f, 8f)),
            coords = mapOf("big" to squareCoords, "small" to smallSquare)
        )
        val result = tester.hitTest(5f, 5f, testProj)
        assertTrue(result == "big" || result == "small")
    }

    @Test
    fun empty_bounds_returns_null() {
        val tester = HitTester(bounds = emptyMap(), coords = emptyMap())
        assertNull(tester.hitTest(5f, 5f, testProj))
    }
}
