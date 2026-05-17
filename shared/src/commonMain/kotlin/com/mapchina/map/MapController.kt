package com.mapchina.map

expect class MapController() {
    fun addOverlay(regionId: String, boundary: String, style: OverlayStyle)
    fun removeOverlay(regionId: String)
    fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean)
    fun removeMarker(attractionId: String)
    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean)
    fun setOnRegionTapListener(listener: ((String) -> Unit)?)
    fun setOnMarkerTapListener(listener: ((String) -> Unit)?)
    fun dispose()
}
