package com.mapchina.map

actual class MapController actual constructor() {

    private var regionTapListener: ((String) -> Unit)? = null
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

    actual fun setOnRegionTapListener(listener: ((String) -> Unit)?) {
        regionTapListener = listener
    }

    actual fun setOnMarkerTapListener(listener: ((String) -> Unit)?) {
        markerTapListener = listener
    }

    actual fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) {
        // TODO
    }

    actual fun pulseOverlay(regionId: String) {
        // TODO: Integrate with AMap SDK
    }

    actual fun dispose() {
        regionTapListener = null
        markerTapListener = null
    }
}
