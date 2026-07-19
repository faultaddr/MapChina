package com.mapchina.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
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

class MapController {

    internal val viewport = ViewportState()
    private val _renderState = MutableStateFlow(RenderState())
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    internal val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _pulseAlpha by mutableFloatStateOf(0f)
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
    private val projectedOverlayBounds = mutableMapOf<String, androidx.compose.ui.geometry.Rect>()

    private var animJob: Job? = null
    private val cameraRequestGate = CameraAnimationRequestGate()

    // Share mode: saved styles to restore
    private val savedOverlayStyles = mutableMapOf<String, OverlayStyle>()

    // Guard against DisposableEffect re-execution overwriting camera state
    private var hasNotifiedReady = false
    var initialFitDone: Boolean = false
        internal set

    // ---- Overlay operations ----

    fun addOverlay(
        regionId: String,
        boundary: String,
        style: OverlayStyle,
        isVisited: Boolean = false,
        role: OverlayRole = OverlayRole.ACTIVE,
        opacityMultiplier: Float = 1f
    ) {
        val coords = BoundaryParser.parseFlatCoords(boundary)
        hitTestCoords[regionId] = coords
        _renderState.update {
            it.copy(
                overlays = it.overlays + (
                    regionId to OverlayData(
                        coords = coords,
                        style = style,
                        isVisited = isVisited,
                        role = role,
                        opacityMultiplier = opacityMultiplier
                    )
                )
            )
        }
        rebuildInteractiveBounds()
    }

    fun updateOverlayStyle(regionId: String, style: OverlayStyle, isVisited: Boolean? = null) {
        val existing = _renderState.value.overlays[regionId] ?: return
        val visited = isVisited ?: existing.isVisited
        _renderState.update { it.copy(overlays = it.overlays + (regionId to existing.copy(style = style, isVisited = visited))) }
    }

    fun updateOverlayRole(
        regionId: String,
        role: OverlayRole,
        opacityMultiplier: Float
    ) {
        val existing = _renderState.value.overlays[regionId] ?: return
        _renderState.update {
            it.copy(
                overlays = it.overlays + (
                    regionId to existing.copy(
                        role = role,
                        opacityMultiplier = opacityMultiplier.coerceIn(0f, 1f)
                    )
                )
            )
        }
        rebuildInteractiveBounds()
    }

    fun hasOverlay(regionId: String): Boolean =
        _renderState.value.overlays.containsKey(regionId)

    fun updateOverlayPresentation(
        regionId: String,
        style: OverlayStyle,
        isVisited: Boolean,
        role: OverlayRole,
        opacityMultiplier: Float
    ) {
        val existing = _renderState.value.overlays[regionId] ?: return
        _renderState.update {
            it.copy(
                overlays = it.overlays + (
                    regionId to existing.copy(
                        style = style,
                        isVisited = isVisited,
                        role = role,
                        opacityMultiplier = opacityMultiplier.coerceIn(0f, 1f)
                    )
                )
            )
        }
        rebuildInteractiveBounds()
    }

    fun removeOverlay(regionId: String) {
        hitTestCoords.remove(regionId)
        hitTestBounds.remove(regionId)
        projectedOverlayBounds.remove(regionId)
        _renderState.update { it.copy(overlays = it.overlays - regionId) }
    }

    fun clearOverlays() {
        hitTestCoords.clear()
        hitTestBounds.clear()
        projectedOverlayBounds.clear()
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
        projectedOverlayBounds.keys.retainAll(regionIds)
        _renderState.update {
            it.copy(overlays = it.overlays.filterKeys { key -> key in regionIds })
        }
    }

    private var pulseJob: Job? = null

    fun pulseOverlay(regionId: String) {
        _renderState.update { it.copy(pulseTarget = regionId) }
        pulseJob?.cancel()
        pulseJob = animationScope.launch { animatePulse { _pulseAlpha = it } }
    }

    fun celebrateOverlay(regionId: String) {
        pulseJob?.cancel()
        _renderState.update { it.copy(pulseTarget = regionId) }
        pulseJob = animationScope.launch {
            try {
                animateCelebrationPulse { _pulseAlpha = it }
            } finally {
                _pulseAlpha = 0f
                _renderState.update { state ->
                    if (state.pulseTarget == regionId) state.copy(pulseTarget = null) else state
                }
            }
        }
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
            applyImmediateCameraUpdate {
                viewport.moveTo(lng, lat, zoomLevel)
            }
        }
    }

    fun computeZoomForBounds(
        minLng: Double,
        maxLng: Double,
        minLat: Double,
        maxLat: Double
    ): Triple<Double, Double, Float> {
        val target = viewport.computeBoundsFitTarget(
            minLng = minLng,
            maxLng = maxLng,
            minLat = minLat,
            maxLat = maxLat
        )
        return Triple(target.centerLng, target.centerLat, target.zoomLevel)
    }

    fun zoomToBounds(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double, animated: Boolean) {
        val (targetLng, targetLat, targetZoom) = computeZoomForBounds(minLng, maxLng, minLat, maxLat)
        if (animated) animateCamera(targetLng, targetLat, targetZoom)
        else applyImmediateCameraUpdate {
            viewport.moveTo(targetLng, targetLat, targetZoom)
        }
    }

    fun fitChinaInView(animated: Boolean) {
        if (animated) {
            val target = viewport.computeChinaFitTarget()
            animateCamera(target.first, target.second, target.third)
        } else {
            applyImmediateCameraUpdate {
                viewport.fitChinaInView()
            }
        }
    }

    fun focusBounds(
        minLng: Double,
        maxLng: Double,
        minLat: Double,
        maxLat: Double,
        insets: ViewportInsets,
        durationMillis: Long = 650L,
        onComplete: (Long) -> Unit
    ): Long {
        val target = viewport.computeBoundsFitTarget(
            minLng,
            maxLng,
            minLat,
            maxLat,
            insets
        )
        return animateCamera(
            targetLng = target.centerLng,
            targetLat = target.centerLat,
            targetZoom = target.zoomLevel,
            durationMillis = durationMillis,
            onComplete = onComplete
        )
    }

    fun focusCamera(
        lat: Double,
        lng: Double,
        zoomLevel: Float,
        insets: ViewportInsets,
        durationMillis: Long = 650L,
        onComplete: (Long) -> Unit
    ): Long {
        val target = viewport.offsetCameraTarget(lng, lat, zoomLevel, insets)
        return animateCamera(
            targetLng = target.centerLng,
            targetLat = target.centerLat,
            targetZoom = target.zoomLevel,
            durationMillis = durationMillis,
            onComplete = onComplete
        )
    }

    fun cancelCameraAnimation() {
        animJob?.cancel()
        animJob = null
        cameraRequestGate.cancel()
    }

    private fun applyImmediateCameraUpdate(update: () -> Unit) {
        cancelCameraAnimation()
        update()
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

    private fun rebuildInteractiveBounds() {
        val activeIds = _renderState.value.overlays
            .filterValues { it.role == OverlayRole.ACTIVE }
            .keys
        hitTestBounds.clear()
        hitTestBounds.putAll(projectedOverlayBounds.filterKeys { it in activeIds })
    }

    internal fun updateHitTestBounds(
        bounds: Map<String, androidx.compose.ui.geometry.Rect>
    ) {
        projectedOverlayBounds.clear()
        projectedOverlayBounds.putAll(bounds)
        rebuildInteractiveBounds()
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

    private fun animateCamera(
        targetLng: Double,
        targetLat: Double,
        targetZoom: Float,
        durationMillis: Long = 400L,
        onComplete: ((Long) -> Unit)? = null
    ): Long {
        require(durationMillis > 0L) { "durationMillis must be positive" }
        animJob?.cancel()
        val requestId = cameraRequestGate.begin()
        val startLng = viewport.centerLng
        val startLat = viewport.centerLat
        val startZoom = viewport.zoomLevel

        if (startLng == targetLng && startLat == targetLat && startZoom == targetZoom) {
            if (cameraRequestGate.isActive(requestId)) {
                onComplete?.invoke(requestId)
                cameraAnimCompleteListener?.invoke()
            }
            return requestId
        }

        animJob = animationScope.launch {
            val startTime = TimeSource.Monotonic.markNow()
            while (cameraRequestGate.isActive(requestId)) {
                val elapsed = startTime.elapsedNow().inWholeMilliseconds
                val progress = (elapsed.toFloat() / durationMillis).coerceIn(0f, 1f)
                val eased = smoothStep(progress)
                viewport.updateCamera(
                    lng = startLng + (targetLng - startLng) * eased,
                    lat = startLat + (targetLat - startLat) * eased,
                    zoom = startZoom + (targetZoom - startZoom) * eased
                )
                if (progress >= 1f) break
                delay(16)
            }
            if (cameraRequestGate.isActive(requestId)) {
                onComplete?.invoke(requestId)
                cameraAnimCompleteListener?.invoke()
            }
        }
        return requestId
    }

    // ---- Lifecycle ----

    fun detachFromComposition() {
        cancelCameraAnimation()
        pulseJob?.cancel()
    }

    fun dispose() {
        animationScope.cancel()
    }
}
