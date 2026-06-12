package com.mapchina.map

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.PI
import kotlin.math.tan

class MapController {

    internal val viewport = ViewportState()
    private val _renderState = MutableStateFlow(RenderState())
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    internal val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _pulseAnimatable = Animatable(0f)
    val pulseAlpha: Float get() = _pulseAnimatable.value

    private var regionTapListener: ((String) -> Unit)? = null
    private var regionDoubleTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null
    private var cameraZoomChangeListener: ((Float) -> Unit)? = null
    private var cameraPositionListener: ((Double, Double, Float) -> Unit)? = null
    private var mapReadyListener: (() -> Unit)? = null

    private val hitTestBounds = mutableMapOf<String, androidx.compose.ui.geometry.Rect>()
    private val hitTestCoords = mutableMapOf<String, List<List<Pair<Double, Double>>>>()

    private var animJob: Job? = null

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

    // ---- Ocean background ----

    fun setOceanColor(color: androidx.compose.ui.graphics.Color) {
        _renderState.update { it.copy(oceanColor = color) }
    }

    // ---- Camera operations ----

    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        if (animated) {
            animateCamera(lng, lat, zoomLevel)
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
    fun setOnRegionDoubleTapListener(listener: ((String) -> Unit)?) { regionDoubleTapListener = listener }
    fun setOnMarkerTapListener(listener: ((String) -> Unit)?) { markerTapListener = listener }
    fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) { cameraZoomChangeListener = listener }
    fun setOnCameraPositionListener(listener: ((Double, Double, Float) -> Unit)?) { cameraPositionListener = listener }
    fun setOnMapReadyListener(listener: (() -> Unit)?) { mapReadyListener = listener }

    // ---- Internal event handling ----

    internal fun handleTap(offset: Offset) {
        val projection = viewport.toProjection(viewport.canvasWidth, viewport.canvasHeight)
        val tester = HitTester(hitTestBounds, hitTestCoords)
        val regionId = tester.hitTest(offset.x, offset.y, projection)
        if (regionId != null) {
            regionTapListener?.invoke(regionId)
            return
        }

        val rs = _renderState.value
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

    internal fun handleDoubleTap(offset: Offset) {
        val projection = viewport.toProjection(viewport.canvasWidth, viewport.canvasHeight)
        val tester = HitTester(hitTestBounds, hitTestCoords)
        val regionId = tester.hitTest(offset.x, offset.y, projection)

        if (regionId != null) {
            regionDoubleTapListener?.invoke(regionId) ?: zoomToRegion(regionId)
        } else {
            viewport.zoomBy(1f, offset)
        }
    }

    internal fun handleLongPress(offset: Offset) {}

    internal fun updateHitTestBounds(bounds: Map<String, androidx.compose.ui.geometry.Rect>) {
        hitTestBounds.clear()
        hitTestBounds.putAll(bounds)
    }

    internal fun notifyMapReady() {
        mapReadyListener?.invoke()
    }

    // ---- Region zoom ----

    private fun zoomToRegion(regionId: String) {
        val allRings = hitTestCoords[regionId] ?: return
        if (allRings.isEmpty()) return

        var minLng = Double.MAX_VALUE; var maxLng = -Double.MAX_VALUE
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE

        for (ring in allRings) {
            for ((lng, lat) in ring) {
                if (lng < minLng) minLng = lng
                if (lng > maxLng) maxLng = lng
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
            }
        }

        val targetLng = (minLng + maxLng) / 2.0
        val targetLat = (minLat + maxLat) / 2.0

        val w = viewport.canvasWidth
        val h = viewport.canvasHeight
        if (w <= 0f || h <= 0f) return

        val lngSpan = maxLng - minLng
        if (lngSpan <= 0.0) {
            animateCamera(targetLng, targetLat, viewport.zoomLevel + 3f)
            return
        }

        val mercMin = ln(tan(PI / 4 + minLat * PI / 360))
        val mercMax = ln(tan(PI / 4 + maxLat * PI / 360))
        val mercSpan = mercMax - mercMin

        val padding = 0.7f
        val scaleFromLng = (w * padding) / lngSpan.toFloat()
        val scaleFromLat = if (mercSpan > 0.0)
            (h * padding) / (mercSpan.toFloat() * (180.0 / PI).toFloat())
        else
            Float.MAX_VALUE

        val targetScale = minOf(scaleFromLng, scaleFromLat)
        val targetZoom = (ViewportState.BASE_ZOOM +
            log2((targetScale / ViewportState.BASE_SCALE).toDouble()).toFloat())
            .coerceIn(ViewportState.MIN_ZOOM, ViewportState.MAX_ZOOM)

        animateCamera(targetLng, targetLat, targetZoom)
    }

    // ---- Animation ----

    private fun animateCamera(targetLng: Double, targetLat: Double, targetZoom: Float) {
        animJob?.cancel()
        val startLng = viewport.centerLng
        val startLat = viewport.centerLat
        val startZoom = viewport.zoomLevel

        if (startLng == targetLng && startLat == targetLat && startZoom == targetZoom) return

        animJob = animationScope.launch {
            val steps = 15
            val stepMs = 25L
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val eased = t * t * (3f - 2f * t)
                viewport.centerLng = startLng + (targetLng - startLng) * eased
                viewport.centerLat = startLat + (targetLat - startLat) * eased
                viewport.zoomLevel = startZoom + (targetZoom - startZoom) * eased
                delay(stepMs)
            }
        }
    }

    // ---- Lifecycle ----

    fun dispose() {
        animationScope.cancel()
    }
}
