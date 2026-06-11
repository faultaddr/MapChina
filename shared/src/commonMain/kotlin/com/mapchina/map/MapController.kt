package com.mapchina.map

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapController {

    internal val viewport = ViewportState()
    private val _renderState = MutableStateFlow(RenderState())
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    internal val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _pulseAnimatable = Animatable(0f)
    val pulseAlpha: Float get() = _pulseAnimatable.value

    private var regionTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null
    private var cameraZoomChangeListener: ((Float) -> Unit)? = null
    private var cameraPositionListener: ((Double, Double, Float) -> Unit)? = null
    private var mapReadyListener: (() -> Unit)? = null

    private val hitTestBounds = mutableMapOf<String, androidx.compose.ui.geometry.Rect>()
    private val hitTestCoords = mutableMapOf<String, List<List<Pair<Double, Double>>>>()

    // ---- Overlay operations ----

    fun addOverlay(regionId: String, boundary: String, style: OverlayStyle) {
        val coords = BoundaryParser.parseFlatCoords(boundary)
        hitTestCoords[regionId] = coords
        _renderState.update { it.copy(overlays = it.overlays + (regionId to OverlayData(coords, style))) }
    }

    fun removeOverlay(regionId: String) {
        hitTestCoords.remove(regionId)
        hitTestBounds.remove(regionId)
        _renderState.update { it.copy(overlays = it.overlays - regionId) }
    }

    fun clearOverlays() {
        hitTestCoords.clear()
        hitTestBounds.clear()
        _renderState.update { it.copy(overlays = emptyMap()) }
    }

    fun removeOverlaysExcept(regionIds: Set<String>) {
        hitTestCoords.keys.retainAll(regionIds)
        hitTestBounds.keys.retainAll(regionIds)
        _renderState.update { it.copy(overlays = it.overlays.filterKeys { k -> k in regionIds }) }
    }

    fun pulseOverlay(regionId: String) {
        _renderState.update { it.copy(pulseTarget = regionId) }
        animationScope.launch { animatePulse(_pulseAnimatable) }
    }

    fun restorePulsedOverlay() {
        _renderState.update { it.copy(pulseTarget = null) }
        animationScope.launch { _pulseAnimatable.snapTo(0f) }
    }

    // ---- Marker operations ----

    fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean) {
        _renderState.update {
            it.copy(markers = it.markers + (attractionId to MarkerData(attractionId, name, lat, lng, visited)))
        }
    }

    fun addAttractionMarker(attractionId: String, name: String, lat: Double, lng: Double, imageUrl: String?, visited: Boolean) {
        _renderState.update {
            it.copy(attractionMarkers = it.attractionMarkers + (attractionId to AttractionMarkerData(attractionId, name, lat, lng, imageUrl, visited)))
        }
    }

    fun removeMarker(attractionId: String) {
        _renderState.update {
            it.copy(
                markers = it.markers - attractionId,
                attractionMarkers = it.attractionMarkers - attractionId
            )
        }
    }

    fun clearMarkers() {
        _renderState.update { it.copy(markers = emptyMap(), attractionMarkers = emptyMap()) }
    }

    // ---- Image marker operations ----

    fun addImageMarker(id: String, lat: Double, lng: Double, imagePath: String, count: Int) {
        _renderState.update {
            it.copy(imageMarkers = it.imageMarkers + (id to ImageMarkerData(id, lat, lng, imagePath, count)))
        }
    }

    fun removeImageMarker(id: String) {
        _renderState.update { it.copy(imageMarkers = it.imageMarkers - id) }
    }

    fun clearImageMarkers() {
        _renderState.update { it.copy(imageMarkers = emptyMap()) }
    }

    // ---- Polyline operations ----

    fun addPolyline(id: String, points: List<Pair<Double, Double>>, color: Long, width: Float) {
        _renderState.update {
            it.copy(polylines = it.polylines + (id to PolylineData(id, points, color, width)))
        }
    }

    fun removePolyline(id: String) {
        _renderState.update { it.copy(polylines = it.polylines - id) }
    }

    fun clearPolylines() {
        _renderState.update { it.copy(polylines = emptyMap()) }
    }

    // ---- Camera operations ----

    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        if (animated) {
            animateCameraMove(viewport, lng, lat, zoomLevel, animationScope)
        } else {
            viewport.moveTo(lng, lat, zoomLevel)
        }
    }

    fun toScreenLocation(lat: Double, lng: Double): Pair<Float, Float>? {
        val proj = viewport.toProjection(viewport.canvasWidth, viewport.canvasHeight)
        val offset = proj.project(lng, lat)
        return offset.x to offset.y
    }

    // ---- Callbacks ----

    fun setOnRegionTapListener(listener: ((String) -> Unit)?) { regionTapListener = listener }
    fun setOnMarkerTapListener(listener: ((String) -> Unit)?) { markerTapListener = listener }
    fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) { cameraZoomChangeListener = listener }
    fun setOnCameraPositionListener(listener: ((Double, Double, Float) -> Unit)?) { cameraPositionListener = listener }
    fun setOnMapReadyListener(listener: (() -> Unit)?) { mapReadyListener = listener }

    // ---- Internal event handling ----

    internal fun handleTap(offset: Offset) {
        val tester = HitTester(hitTestBounds, hitTestCoords)
        val regionId = tester.hitTest(offset.x, offset.y)
        if (regionId != null) {
            regionTapListener?.invoke(regionId)
        }

        val rs = _renderState.value
        val projection = viewport.toProjection(viewport.canvasWidth, viewport.canvasHeight)
        val tapThreshold = 20f
        for (marker in rs.markers.values) {
            val screenPos = projection.project(marker.lng, marker.lat)
            val dx = offset.x - screenPos.x
            val dy = offset.y - screenPos.y
            if (dx * dx + dy * dy < tapThreshold * tapThreshold) {
                markerTapListener?.invoke(marker.id)
                return
            }
        }
        for (marker in rs.attractionMarkers.values) {
            val screenPos = projection.project(marker.lng, marker.lat)
            val dx = offset.x - screenPos.x
            val dy = offset.y - screenPos.y
            if (dx * dx + dy * dy < tapThreshold * tapThreshold) {
                markerTapListener?.invoke(marker.id)
                return
            }
        }
    }

    internal fun handleLongPress(offset: Offset) {
        // Future: long-press on marker for context menu
    }

    internal fun updateHitTestBounds(bounds: Map<String, androidx.compose.ui.geometry.Rect>) {
        hitTestBounds.clear()
        hitTestBounds.putAll(bounds)
    }

    internal fun notifyMapReady() {
        mapReadyListener?.invoke()
    }

    // ---- Lifecycle ----

    fun dispose() {
        animationScope.cancel()
    }
}
