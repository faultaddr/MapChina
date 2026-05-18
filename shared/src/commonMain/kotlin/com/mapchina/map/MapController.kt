package com.mapchina.map

expect class MapController() {
    fun addOverlay(regionId: String, boundary: String, style: OverlayStyle)
    fun removeOverlay(regionId: String)
    fun clearOverlays()
    fun removeOverlaysExcept(regionIds: Set<String>)
    fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean)
    fun removeMarker(attractionId: String)
    fun clearMarkers()
    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean)
    fun setOnRegionLongPressListener(listener: ((String) -> Unit)?)
    fun setOnRegionDoubleTapListener(listener: ((String) -> Unit)?)
    fun setOnMarkerTapListener(listener: ((String) -> Unit)?)
    fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?)
    fun dispose()
}
