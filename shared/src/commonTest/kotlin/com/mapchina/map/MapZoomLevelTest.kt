package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapZoomLevelTest {

    @Test
    fun `next_drill_down_returns_correct_level`() {
        assertEquals(MapZoomLevel.PROVINCIAL, MapZoomLevel.NATIONAL.nextDrillDown())
        assertEquals(MapZoomLevel.CITY, MapZoomLevel.PROVINCIAL.nextDrillDown())
        assertEquals(MapZoomLevel.DISTRICT, MapZoomLevel.CITY.nextDrillDown())
        assertNull(MapZoomLevel.DISTRICT.nextDrillDown())
    }

    @Test
    fun `navigate_up_returns_correct_level`() {
        assertNull(MapZoomLevel.NATIONAL.navigateUp())
        assertEquals(MapZoomLevel.NATIONAL, MapZoomLevel.PROVINCIAL.navigateUp())
        assertEquals(MapZoomLevel.PROVINCIAL, MapZoomLevel.CITY.navigateUp())
        assertEquals(MapZoomLevel.CITY, MapZoomLevel.DISTRICT.navigateUp())
    }

    @Test
    fun `drill_down_then_up_returns_original`() {
        val down = MapZoomLevel.NATIONAL.nextDrillDown()!!
        val up = down.navigateUp()
        assertEquals(MapZoomLevel.NATIONAL, up)
    }
}
