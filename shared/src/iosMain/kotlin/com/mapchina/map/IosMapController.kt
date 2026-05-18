package com.mapchina.map

actual class MapController actual constructor() {

    private var regionLongPressListener: ((String) -> Unit)? = null
    private var regionDoubleTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null

    actual fun addOverlay(regionId: String, boundary: String, style: OverlayStyle) {
        // TODO: Integrate with AMap SDK
    }

    actual fun removeOverlay(regionId: String) {
        // TODO: Integrate with AMap SDK
    }

    actual fun clearOverlays() {
        // TODO: Integrate with AMap SDK
    }

    actual fun removeOverlaysExcept(regionIds: Set<String>) {
        // TODO: Integrate with AMap SDK
    }

    actual fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean) {
        // TODO: Integrate with AMap SDK
    }

    actual fun removeMarker(attractionId: String) {
        // TODO: Integrate with AMap SDK
    }

    actual fun clearMarkers() {
        // TODO: Integrate with AMap SDK
    }

    actual fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        // TODO: Integrate with AMap SDK
    }

    actual fun setOnRegionLongPressListener(listener: ((String) -> Unit)?) {
        regionLongPressListener = listener
    }

    actual fun setOnRegionDoubleTapListener(listener: ((String) -> Unit)?) {
        regionDoubleTapListener = listener
    }

    actual fun setOnMarkerTapListener(listener: ((String) -> Unit)?) {
        markerTapListener = listener
    }

    actual fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) {
        // TODO
    }

    actual fun dispose() {
        regionLongPressListener = null
        regionDoubleTapListener = null
        markerTapListener = null
    }
}
