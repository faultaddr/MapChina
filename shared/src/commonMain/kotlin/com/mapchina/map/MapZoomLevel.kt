package com.mapchina.map

enum class MapZoomLevel {
    NATIONAL,
    PROVINCIAL,
    CITY,
    DISTRICT;

    fun nextDrillDown(): MapZoomLevel? = when (this) {
        NATIONAL -> PROVINCIAL
        PROVINCIAL -> CITY
        CITY -> DISTRICT
        DISTRICT -> null
    }

    fun navigateUp(): MapZoomLevel? = when (this) {
        DISTRICT -> CITY
        CITY -> PROVINCIAL
        PROVINCIAL -> NATIONAL
        NATIONAL -> null
    }
}
