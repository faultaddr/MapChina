package com.mapchina.map

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.MotionEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolygonOptions
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.maps.model.BitmapDescriptorFactory
import org.json.JSONArray

actual class MapController actual constructor() {

    private var aMap: AMap? = null
    private val overlays = mutableMapOf<String, com.amap.api.maps.model.Polygon>()
    private val markers = mutableMapOf<String, Marker>()
    private val polylines = mutableMapOf<String, com.amap.api.maps.model.Polyline>()
    private val imageMarkers = mutableMapOf<String, Marker>()
    var appContext: android.content.Context? = null
    private var regionTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null
    private var cameraZoomChangeListener: ((Float) -> Unit)? = null
    private var cameraPositionListener: ((Double, Double, Float) -> Unit)? = null
    private var mapReadyListener: (() -> Unit)? = null

    private data class PendingOverlay(val regionId: String, val boundary: String, val style: OverlayStyle)
    private val pendingOverlays = mutableListOf<PendingOverlay>()
    private data class PendingMarker(val attractionId: String, val name: String, val lat: Double, val lng: Double, val visited: Boolean)
    private val pendingMarkers = mutableListOf<PendingMarker>()
    private data class PendingPolyline(val id: String, val points: List<Pair<Double, Double>>, val color: Long, val width: Float)
    private val pendingPolylines = mutableListOf<PendingPolyline>()
    private data class PendingImageMarker(val id: String, val lat: Double, val lng: Double, val imagePath: String, val count: Int)
    private val pendingImageMarkers = mutableListOf<PendingImageMarker>()
    private var pendingCamera: Triple<Double, Double, Float>? = null

    // Touch state
    private var touchDownPoint: android.graphics.Point? = null
    private var touchDownRegionId: String? = null
    private var touchDownTime: Long = 0
    private var longPressRunnable: Runnable? = null
    private var longPressMarkerId: String? = null
    private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val LONG_PRESS_TIMEOUT = 500L

    // Pulse animation
    private var pulseAnimator: ValueAnimator? = null
    private var pulseTargetId: String? = null
    private var pulseOriginalStyle: OverlayStyle? = null
    private var lastTapTime = 0L
    private val TAP_DEBOUNCE_MS = 300L

    fun bindMap(amap: AMap, context: android.content.Context? = null) {
        this.aMap = amap
        if (context != null) {
            appContext = context
        }

        // Clear stale references from previous AMap instance
        overlays.clear()
        markers.clear()
        polylines.clear()
        imageMarkers.clear()
        pulseTargetId = null
        pulseOriginalStyle = null
        pulseAnimator?.cancel()
        pulseAnimator = null

        // Hide AMap built-in zoom controls
        amap.uiSettings.isZoomControlsEnabled = false

        // Re-sync overlays after map is fully loaded (camera restored by mapReadyListener)
        amap.setOnMapLoadedListener {
            mapReadyListener?.invoke()
        }

        amap.setOnMapTouchListener { motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    val map = aMap ?: return@setOnMapTouchListener
                    val screenPoint = android.graphics.Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                    val latLng = map.projection.fromScreenLocation(screenPoint)
                    touchDownPoint = screenPoint
                    touchDownTime = System.currentTimeMillis()

                    // Check if touch is on a marker (long press candidate)
                    val markerId = findMarkerNear(screenPoint, map)
                    if (markerId != null) {
                        longPressMarkerId = markerId
                        val runnable = Runnable {
                            if (longPressMarkerId != null) {
                                markerTapListener?.invoke(longPressMarkerId!!)
                                longPressMarkerId = null
                                touchDownRegionId = null
                            }
                        }
                        longPressRunnable = runnable
                        longPressHandler.postDelayed(runnable, LONG_PRESS_TIMEOUT)
                    }

                    val regionId = findRegionAt(latLng)
                    touchDownRegionId = regionId
                }
                MotionEvent.ACTION_MOVE -> {
                    if (touchDownPoint != null) {
                        val dx = motionEvent.x.toInt() - touchDownPoint!!.x
                        val dy = motionEvent.y.toInt() - touchDownPoint!!.y
                        if (dx * dx + dy * dy > 25) {
                            touchDownRegionId = null
                            cancelLongPress()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime >= TAP_DEBOUNCE_MS) {
                        lastTapTime = now
                        touchDownRegionId?.let { regionTapListener?.invoke(it) }
                    }
                    cancelLongPress()
                    touchDownRegionId = null
                    touchDownPoint = null
                }
                MotionEvent.ACTION_CANCEL -> {
                    cancelLongPress()
                    touchDownRegionId = null
                    touchDownPoint = null
                }
            }
        }

        amap.setOnMarkerClickListener { _ ->
            // Consume event to prevent AMap default info window; marker taps handled via long press
            true
        }

        amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition) {}
            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition) {
                cameraZoomChangeListener?.invoke(position.zoom)
                cameraPositionListener?.invoke(position.target.latitude, position.target.longitude, position.zoom)
            }
        })

        // Replay cached overlays
        val overlaysToReplay = pendingOverlays.toList()
        pendingOverlays.clear()
        for (p in overlaysToReplay) {
            addOverlayToMap(p.regionId, p.boundary, p.style)
        }

        // Replay cached markers
        val markersToReplay = pendingMarkers.toList()
        pendingMarkers.clear()
        for (m in markersToReplay) {
            addMarkerToMap(m.attractionId, m.name, m.lat, m.lng, m.visited)
        }

        // Replay cached polylines
        val polylinesToReplay = pendingPolylines.toList()
        pendingPolylines.clear()
        for (p in polylinesToReplay) {
            addPolylineToMap(p.id, p.points, p.color, p.width)
        }

        // Replay cached image markers
        val imageMarkersToReplay = pendingImageMarkers.toList()
        pendingImageMarkers.clear()
        for (im in imageMarkersToReplay) {
            addImageMarkerToMap(im.id, im.lat, im.lng, im.imagePath, im.count)
        }

        pendingCamera?.let { (lat, lng, zoom) ->
            amap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom))
        }
        pendingCamera = null
    }

    fun unbindMap() {
        pulseAnimator?.cancel()
        cancelLongPress()
        aMap = null
    }

    actual fun addOverlay(regionId: String, boundary: String, style: OverlayStyle) {
        val map = aMap
        if (map != null) {
            addOverlayToMap(regionId, boundary, style)
        } else {
            pendingOverlays.removeAll { it.regionId == regionId }
            pendingOverlays.add(PendingOverlay(regionId, boundary, style))
        }
    }

    actual fun removeOverlay(regionId: String) {
        overlays.remove(regionId)?.remove()
        pendingOverlays.removeAll { it.regionId == regionId }
    }

    actual fun clearOverlays() {
        overlays.values.toList().forEach { it.remove() }
        overlays.clear()
        pendingOverlays.clear()
    }

    actual fun removeOverlaysExcept(regionIds: Set<String>) {
        val toRemove = overlays.keys.filter { it !in regionIds }
        for (id in toRemove) {
            overlays.remove(id)?.remove()
        }
        pendingOverlays.removeAll { it.regionId !in regionIds }
    }

    actual fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean) {
        val map = aMap
        if (map != null) {
            addMarkerToMap(attractionId, name, lat, lng, visited)
        } else {
            pendingMarkers.removeAll { it.attractionId == attractionId }
            pendingMarkers.add(PendingMarker(attractionId, name, lat, lng, visited))
        }
    }

    actual fun removeMarker(attractionId: String) {
        markers.remove(attractionId)?.remove()
        pendingMarkers.removeAll { it.attractionId == attractionId }
    }

    actual fun clearMarkers() {
        markers.values.toList().forEach { it.remove() }
        markers.clear()
        pendingMarkers.clear()
    }

    actual fun addPolyline(id: String, points: List<Pair<Double, Double>>, color: Long, width: Float) {
        val map = aMap
        if (map != null) {
            addPolylineToMap(id, points, color, width)
        } else {
            pendingPolylines.removeAll { it.id == id }
            pendingPolylines.add(PendingPolyline(id, points, color, width))
        }
    }

    actual fun removePolyline(id: String) {
        polylines.remove(id)?.remove()
        pendingPolylines.removeAll { it.id == id }
    }

    actual fun clearPolylines() {
        polylines.values.toList().forEach { it.remove() }
        polylines.clear()
        pendingPolylines.clear()
    }

    actual fun addImageMarker(id: String, lat: Double, lng: Double, imagePath: String, count: Int) {
        val map = aMap
        if (map != null) {
            addImageMarkerToMap(id, lat, lng, imagePath, count)
        } else {
            pendingImageMarkers.removeAll { it.id == id }
            pendingImageMarkers.add(PendingImageMarker(id, lat, lng, imagePath, count))
        }
    }

    actual fun removeImageMarker(id: String) {
        imageMarkers.remove(id)?.remove()
        pendingImageMarkers.removeAll { it.id == id }
    }

    actual fun clearImageMarkers() {
        imageMarkers.values.toList().forEach { it.remove() }
        imageMarkers.clear()
        pendingImageMarkers.clear()
    }

    actual fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        val map = aMap
        if (map != null) {
            val update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoomLevel)
            if (animated) map.animateCamera(update) else map.moveCamera(update)
        } else {
            pendingCamera = Triple(lat, lng, zoomLevel)
        }
    }

    actual fun toScreenLocation(lat: Double, lng: Double): Pair<Float, Float>? {
        val map = aMap ?: return null
        val point = map.projection.toScreenLocation(LatLng(lat, lng))
        return Pair(point.x.toFloat(), point.y.toFloat())
    }

    actual fun setOnRegionTapListener(listener: ((String) -> Unit)?) {
        regionTapListener = listener
    }

    actual fun setOnMarkerTapListener(listener: ((String) -> Unit)?) {
        markerTapListener = listener
    }

    actual fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) {
        cameraZoomChangeListener = listener
    }

    actual fun setOnCameraPositionListener(listener: ((Double, Double, Float) -> Unit)?) {
        cameraPositionListener = listener
    }

    actual fun setOnMapReadyListener(listener: (() -> Unit)?) {
        mapReadyListener = listener
    }

    actual fun pulseOverlay(regionId: String) {
        // Restore previously pulsed overlay to its original color
        pulseTargetId?.let { prevId ->
            if (prevId != regionId) {
                val prevStyle = pulseOriginalStyle
                if (prevStyle != null) {
                    pulseAnimator?.cancel()
                    val prevPolygon = overlays[prevId]
                    if (prevPolygon != null) {
                        prevPolygon.fillColor = applyAlpha(prevStyle.fillColor, prevStyle.alpha)
                    }
                }
            }
        }

        // Same region tapped again — just restart pulse
        if (pulseTargetId == regionId) {
            pulseAnimator?.cancel()
        }

        val polygon = overlays[regionId] ?: return
        pulseOriginalStyle = OverlayStyle(
            fillColor = polygon.fillColor.toLong() and 0xFF_FFFFFF,
            strokeColor = polygon.strokeColor.toLong(),
            strokeWidth = polygon.strokeWidth,
            alpha = (polygon.fillColor ushr 24) / 255f
        )

        pulseAnimator?.cancel()
        pulseTargetId = regionId

        val baseAlpha = (polygon.fillColor ushr 24) / 255f
        val peakAlpha = 0.95f

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.addUpdateListener { anim ->
            val fraction = anim.animatedValue as Float
            val alpha = if (fraction < 0.4f) {
                baseAlpha + (peakAlpha - baseAlpha) * (fraction / 0.4f)
            } else {
                peakAlpha - (peakAlpha - baseAlpha) * ((fraction - 0.4f) / 0.6f)
            }
            polygon.fillColor = applyAlpha(polygon.fillColor.toLong() and 0xFF_FFFFFF, alpha)
        }
        animator.start()
        pulseAnimator = animator
    }

    actual fun restorePulsedOverlay() {
        pulseAnimator?.cancel()
        val id = pulseTargetId ?: return
        val polygon = overlays[id] ?: return
        val style = pulseOriginalStyle ?: return
        polygon.fillColor = applyAlpha(style.fillColor, style.alpha)
        pulseTargetId = null
        pulseOriginalStyle = null
    }

    actual fun dispose() {
        pulseAnimator?.cancel()
        cancelLongPress()
        overlays.values.toList().forEach { it.remove() }
        overlays.clear()
        markers.values.toList().forEach { it.remove() }
        markers.clear()
        polylines.values.toList().forEach { it.remove() }
        polylines.clear()
        imageMarkers.values.toList().forEach { it.remove() }
        imageMarkers.clear()
        pendingOverlays.clear()
        pendingMarkers.clear()
        pendingPolylines.clear()
        pendingImageMarkers.clear()
        regionTapListener = null
        markerTapListener = null
        cameraZoomChangeListener = null
        cameraPositionListener = null
        mapReadyListener = null
        aMap = null
    }

    private fun addOverlayToMap(regionId: String, boundary: String, style: OverlayStyle) {
        val map = aMap ?: return
        val points = parseBoundary(boundary) ?: return
        if (points.size < 3) return

        removeOverlay(regionId)

        val fillColor = applyAlpha(style.fillColor, style.alpha)
        val strokeColor = style.strokeColor.toInt()

        val polygonOptions = PolygonOptions()
            .addAll(points)
            .fillColor(fillColor)
            .strokeColor(strokeColor)
            .strokeWidth(style.strokeWidth)
            .zIndex(1f)

        val polygon = map.addPolygon(polygonOptions)
        overlays[regionId] = polygon
    }

    private fun addMarkerToMap(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean) {
        val map = aMap ?: return
        removeMarker(attractionId)

        val marker = map.addMarker(MarkerOptions()
            .position(LatLng(lat, lng))
            .title(name)
            .snippet(if (visited) "已到访" else "未到访")
        )
        markers[attractionId] = marker
    }

    private fun addImageMarkerToMap(id: String, lat: Double, lng: Double, imagePath: String, count: Int) {
        val map = aMap ?: return
        val context = appContext ?: return

        val bitmap = PhotoMarkerRenderer.render(context, imagePath, count) ?: return
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (aMap == null) { bitmap.recycle(); return@post }
            removeImageMarker(id)
            val marker = map.addMarker(MarkerOptions()
                .position(LatLng(lat, lng))
                .icon(descriptor)
                .anchor(0.5f, 1f)
                .zIndex(3f)
            )
            marker.title = "photo_marker_$id"
            imageMarkers[id] = marker
            bitmap.recycle()
        }
    }

    private fun addPolylineToMap(id: String, points: List<Pair<Double, Double>>, color: Long, width: Float) {
        val map = aMap ?: return
        removePolyline(id)
        if (points.size < 2) return

        val latLngs = points.map { LatLng(it.first, it.second) }
        val polylineOptions = PolylineOptions()
            .addAll(latLngs)
            .color(color.toInt())
            .width(width)
            .zIndex(2f)

        val polyline = map.addPolyline(polylineOptions)
        polylines[id] = polyline
    }

    private fun findRegionAt(latLng: LatLng): String? {
        for ((id, polygon) in overlays) {
            if (polygon.contains(latLng)) return id
        }
        return null
    }

    private fun findMarkerNear(screenPoint: android.graphics.Point, map: AMap): String? {
        val density = appContext?.resources?.displayMetrics?.density ?: 2f
        val radiusPx = (24 * density).toInt() // 24dp touch slop for markers

        for ((id, marker) in markers) {
            val markerScreen = map.projection.toScreenLocation(marker.position)
            val dx = screenPoint.x - markerScreen.x
            val dy = screenPoint.y - markerScreen.y
            if (dx * dx + dy * dy <= radiusPx * radiusPx) return id
        }
        for ((id, marker) in imageMarkers) {
            val markerScreen = map.projection.toScreenLocation(marker.position)
            val dx = screenPoint.x - markerScreen.x
            val dy = screenPoint.y - markerScreen.y
            if (dx * dx + dy * dy <= radiusPx * radiusPx) return id
        }
        return null
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
        longPressMarkerId = null
    }

    private fun parseBoundary(boundary: String): List<LatLng>? {
        return try {
            val coords = JSONArray(boundary)
            val points = mutableListOf<LatLng>()
            for (i in 0 until coords.length()) {
                val coord = coords.getJSONArray(i)
                points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
            }
            points
        } catch (_: Exception) {
            null
        }
    }

    private fun applyAlpha(color: Long, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val r = (color shr 16 and 0xFF).toInt()
        val g = (color shr 8 and 0xFF).toInt()
        val b = (color and 0xFF).toInt()
        return Color.argb(a, r, g, b)
    }
}
