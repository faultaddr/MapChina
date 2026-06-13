package com.mapchina.ui.map

import androidx.compose.ui.geometry.Offset
import com.mapchina.map.pointInPolygon
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorBlockViewTest {

    @Test
    fun pointInPolygon_pointInsideSquare_returnsTrue() {
        val polygon = listOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(1f, 1f), Offset(0f, 1f)
        )
        assertTrue(pointInPolygon(0.5f, 0.5f, polygon))
    }

    @Test
    fun pointInPolygon_pointOutsideSquare_returnsFalse() {
        val polygon = listOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(1f, 1f), Offset(0f, 1f)
        )
        assertFalse(pointInPolygon(2f, 2f, polygon))
    }

    @Test
    fun pointInPolygon_pointOnEdge_returnsTrue() {
        val polygon = listOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(1f, 1f), Offset(0f, 1f)
        )
        assertTrue(pointInPolygon(0.5f, 0f, polygon))
    }

    @Test
    fun pointInPolygon_triangleInside_returnsTrue() {
        val polygon = listOf(
            Offset(0f, 0f), Offset(2f, 0f), Offset(1f, 2f)
        )
        assertTrue(pointInPolygon(1f, 0.5f, polygon))
    }

    @Test
    fun pointInPolygon_triangleOutside_returnsFalse() {
        val polygon = listOf(
            Offset(0f, 0f), Offset(2f, 0f), Offset(1f, 2f)
        )
        assertFalse(pointInPolygon(0f, 1f, polygon))
    }

    @Test
    fun pointInPolygon_lessThanThreePoints_returnsFalse() {
        val polygon = listOf(Offset(0f, 0f), Offset(1f, 0f))
        assertFalse(pointInPolygon(0.5f, 0.5f, polygon))
    }

    @Test
    fun pointInPolygon_emptyList_returnsFalse() {
        assertFalse(pointInPolygon(0.5f, 0.5f, emptyList()))
    }
}
