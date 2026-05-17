package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Gcj02Test {

    @Test
    fun `wgs84_to_gcj02_shifts_coordinates_inside_china`() {
        val beijingWgs = Pair(39.9042, 116.4074)
        val result = Gcj02.wgs84ToGcj02(beijingWgs.first, beijingWgs.second)
        assertTrue(result.first != beijingWgs.first || result.second != beijingWgs.second)
    }

    @Test
    fun `wgs84_to_gcj02_returns_same_for_outside_china`() {
        val newYork = Pair(40.7128, -74.0060)
        val result = Gcj02.wgs84ToGcj02(newYork.first, newYork.second)
        assertEquals(newYork.first, result.first)
        assertEquals(newYork.second, result.second)
    }

    @Test
    fun `gcj02_to_wgs84_roundtrip_inside_china`() {
        val originalLat = 31.2304
        val originalLng = 121.4737
        val gcj = Gcj02.wgs84ToGcj02(originalLat, originalLng)
        val wgs = Gcj02.gcj02ToWgs84(gcj.first, gcj.second)
        assertEquals(originalLat, wgs.first, 0.0001)
        assertEquals(originalLng, wgs.second, 0.0001)
    }

    @Test
    fun `wgs84_to_gcj02_boundary_check_west`() {
        val result = Gcj02.wgs84ToGcj02(35.0, 71.0)
        assertEquals(35.0, result.first)
        assertEquals(71.0, result.second)
    }

    @Test
    fun `wgs84_to_gcj02_boundary_check_east`() {
        val result = Gcj02.wgs84ToGcj02(35.0, 138.0)
        assertEquals(35.0, result.first)
        assertEquals(138.0, result.second)
    }
}
