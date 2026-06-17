package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.time.TimeSource
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

    private var _pulseAlpha = 0f
    val pulseAlpha: Float get() = _pulseAlpha

    private var regionTapListener: ((String) -> Unit)? = null
    private var regionDoubleTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null
    private var cameraZoomChangeListener: ((Float) -> Unit)? = null
    private var cameraPositionListener: ((Double, Double, Float) -> Unit)? = null
    private var mapReadyListener: (() -> Unit)? = null
    private var cameraAnimCompleteListener: (() -> Unit)? = null

    private val hitTestBounds = mutableMapOf<String, androidx.compose.ui.geometry.Rect>()
    private val hitTestCoords = mutableMapOf<String, List<List<Pair<Double, Double>>>>()

    private var animJob: Job? = null

    // Share mode: saved styles to restore
    private val savedOverlayStyles = mutableMapOf<String, OverlayStyle>()

    // Guard against DisposableEffect re-execution overwriting camera state
    private var hasNotifiedReady = false
    var initialFitDone: Boolean = false
        internal set

    // ---- Overlay operations ----

    fun addOverlay(regionId: String, boundary: String, style: OverlayStyle, isVisited: Boolean = false) {
        val coords = BoundaryParser.parseFlatCoords(boundary)
        hitTestCoords[regionId] = coords
        _renderState.update { it.copy(overlays = it.overlays + (regionId to OverlayData(coords, style, isVisited))) }
    }

    fun updateOverlayStyle(regionId: String, style: OverlayStyle, isVisited: Boolean? = null) {
        val existing = _renderState.value.overlays[regionId] ?: return
        val visited = isVisited ?: existing.isVisited
        _renderState.update { it.copy(overlays = it.overlays + (regionId to existing.copy(style = style, isVisited = visited))) }
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

    fun removeHitTestFor(regionIds: Set<String>) {
        for (id in regionIds) {
            hitTestCoords.remove(id)
            hitTestBounds.remove(id)
        }
    }

    fun removeOverlaysExcept(regionIds: Set<String>) {
        hitTestCoords.keys.retainAll(regionIds)
        hitTestBounds.keys.retainAll(regionIds)
        _renderState.update { it.copy(overlays = it.overlays.filterKeys { k -> k in regionIds }) }
    }

    private var pulseJob: Job? = null

    fun pulseOverlay(regionId: String) {
        _renderState.update { it.copy(pulseTarget = regionId) }
        pulseJob?.cancel()
        pulseJob = animationScope.launch { animatePulse { _pulseAlpha = it } }
    }

    fun restorePulsedOverlay() {
        pulseJob?.cancel()
        _pulseAlpha = 0f
        _renderState.update { it.copy(pulseTarget = null) }
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

    // ---- Label operations ----

    fun addLabel(id: String, name: String, lat: Double, lng: Double, minZoom: Float) {
        _renderState.update {
            it.copy(labels = it.labels + (id to LabelData(id, name, lat, lng, minZoom)))
        }
    }

    fun removeLabel(id: String) {
        _renderState.update { it.copy(labels = it.labels - id) }
    }

    fun setLabels(labels: Map<String, LabelData>) {
        _renderState.update { it.copy(labels = labels) }
    }

    fun clearLabels() {
        _renderState.update { it.copy(labels = emptyMap()) }
    }

    // ---- Neighbor outlines ----

    fun setNeighborOutlines(outlines: List<List<Pair<Double, Double>>>) {
        _renderState.update { it.copy(neighborOutlines = outlines) }
    }

    // ---- Ocean background ----

    fun setOceanColor(color: androidx.compose.ui.graphics.Color) {
        _renderState.update { it.copy(oceanColor = color) }
    }

    fun setBackgroundTheme(theme: MapTheme) {
        _renderState.update { it.copy(backgroundTheme = theme, oceanColor = theme.oceanColor) }
    }

    // ---- Share mode ----

    private val shareVisitedStyle = OverlayStyle(
        fillColor = 0xFFC8963EL,
        strokeColor = 0xFFA07830L,
        strokeWidth = 0.8f,
        alpha = 0.35f
    )

    private val shareUnvisitedStyle = OverlayStyle(
        fillColor = 0xFF4A9E94L,
        strokeColor = 0xFF3A887EL,
        strokeWidth = 0.5f,
        alpha = 0.10f
    )

    fun setShareMode(enabled: Boolean) {
        if (enabled) {
            savedOverlayStyles.clear()
            val current = _renderState.value.overlays
            val updated = current.mapValues { (id, data) ->
                savedOverlayStyles[id] = data.style
                data.copy(style = if (data.isVisited) shareVisitedStyle else shareUnvisitedStyle)
            }
            _renderState.update { it.copy(overlays = updated, shareMode = true) }
        } else {
            if (savedOverlayStyles.isNotEmpty()) {
                val current = _renderState.value.overlays
                val restored = current.mapValues { (id, data) ->
                    val saved = savedOverlayStyles[id]
                    if (saved != null) data.copy(style = saved) else data
                }
                _renderState.update { it.copy(overlays = restored, shareMode = false) }
            } else {
                _renderState.update { it.copy(shareMode = false) }
            }
            savedOverlayStyles.clear()
        }
    }

    // ---- Camera operations ----

    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        if (animated) {
            animateCamera(lng, lat, zoomLevel)
        } else {
            viewport.moveTo(lng, lat, zoomLevel)
        }
    }

    fun computeZoomForBounds(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double): Triple<Double, Double, Float> {
        val w = viewport.canvasWidth
        val h = viewport.canvasHeight
        val targetLng = (minLng + maxLng) / 2.0
        val targetLat = (minLat + maxLat) / 2.0

        val lngSpan = maxLng - minLng
        if (lngSpan <= 0.0 || w <= 0f || h <= 0f) {
            return Triple(targetLng, targetLat, (viewport.zoomLevel + 3f).coerceIn(ViewportState.MIN_ZOOM, ViewportState.MAX_ZOOM))
        }

        val mercMin = ln(tan(PI / 4 + minLat * PI / 360))
        val mercMax = ln(tan(PI / 4 + maxLat * PI / 360))
        val mercSpan = mercMax - mercMin

        val padding = 0.75f
        val scaleFromLng = (w * padding) / lngSpan.toFloat()
        val scaleFromLat = if (mercSpan > 0.0)
            (h * padding) / (mercSpan.toFloat() * (180.0 / PI).toFloat())
        else Float.MAX_VALUE

        val targetScale = minOf(scaleFromLng, scaleFromLat)
        val targetZoom = (ViewportState.BASE_ZOOM +
            log2((targetScale / ViewportState.BASE_SCALE).toDouble()).toFloat())
            .coerceIn(ViewportState.MIN_ZOOM, ViewportState.MAX_ZOOM)

        return Triple(targetLng, targetLat, targetZoom)
    }

    fun zoomToBounds(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double, animated: Boolean) {
        val (targetLng, targetLat, targetZoom) = computeZoomForBounds(minLng, maxLng, minLat, maxLat)
        if (animated) animateCamera(targetLng, targetLat, targetZoom)
        else viewport.moveTo(targetLng, targetLat, targetZoom)
    }

    fun fitChinaInView(animated: Boolean) {
        if (animated) {
            val target = viewport.computeChinaFitTarget()
            animateCamera(target.first, target.second, target.third)
        } else {
            viewport.fitChinaInView()
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
    fun setOnCameraAnimCompleteListener(listener: (() -> Unit)?) { cameraAnimCompleteListener = listener }

    // ---- Internal event handling ----

    internal fun handleTap(offset: Offset) {
        val projection = viewport.toProjection(viewport.canvasWidth, viewport.canvasHeight)
        val rs = _renderState.value
        val tapThreshold = 24f

        // Markers take priority over regions
        for (marker in rs.attractionMarkers.values) {
            val screenPos = projection.project(marker.lng, marker.lat)
            val dx = offset.x - screenPos.x
            val dy = offset.y - screenPos.y
            if (dx * dx + dy * dy < tapThreshold * tapThreshold) {
                markerTapListener?.invoke(marker.id)
                return
            }
        }
        for (marker in rs.markers.values) {
            val screenPos = projection.project(marker.lng, marker.lat)
            val dx = offset.x - screenPos.x
            val dy = offset.y - screenPos.y
            if (dx * dx + dy * dy < tapThreshold * tapThreshold) {
                markerTapListener?.invoke(marker.id)
                return
            }
        }

        val tester = HitTester(hitTestBounds, hitTestCoords)
        val regionId = tester.hitTest(offset.x, offset.y, projection)
        if (regionId != null) {
            regionTapListener?.invoke(regionId)
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
        if (hasNotifiedReady) return
        hasNotifiedReady = true
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

        zoomToBounds(minLng, maxLng, minLat, maxLat, animated = true)
    }

    // ---- Animation ----

    private fun animateCamera(targetLng: Double, targetLat: Double, targetZoom: Float) {
        animJob?.cancel()
        val startLng = viewport.centerLng
        val startLat = viewport.centerLat
        val startZoom = viewport.zoomLevel

        if (startLng == targetLng && startLat == targetLat && startZoom == targetZoom) {
            cameraAnimCompleteListener?.invoke()
            return
        }

        animJob = animationScope.launch {
            val durationMs = 400L
            val startTime = TimeSource.Monotonic.markNow()
            var lastT = -1f
            while (true) {
                val elapsed = startTime.elapsedNow().inWholeMilliseconds
                val t = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                if (t != lastT) {
                    val eased = t * t * (3f - 2f * t)
                    val lng = startLng + (targetLng - startLng) * eased
                    val lat = startLat + (targetLat - startLat) * eased
                    val zoom = startZoom + (targetZoom - startZoom) * eased
                    viewport.updateCamera(lng, lat, zoom)
                    lastT = t
                }
                if (t >= 1f) break
                delay(16)
            }
            cameraAnimCompleteListener?.invoke()
        }
    }

    // ---- Lifecycle ----

    fun detachFromComposition() {
        animJob?.cancel()
        pulseJob?.cancel()
    }

    fun dispose() {
        animationScope.cancel()
    }
}
