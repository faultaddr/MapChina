package com.mapchina.map

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolygonOptions
import org.json.JSONArray

actual class MapController actual constructor() {

    private var aMap: AMap? = null
    private val overlays = mutableMapOf<String, com.amap.api.maps.model.Polygon>()
    private val markers = mutableMapOf<String, Marker>()
    private var regionLongPressListener: ((String) -> Unit)? = null
    private var regionDoubleTapListener: ((String) -> Unit)? = null
    private var markerTapListener: ((String) -> Unit)? = null
    private var cameraZoomChangeListener: ((Float) -> Unit)? = null

    private data class PendingOverlay(val regionId: String, val boundary: String, val style: OverlayStyle)
    private val pendingOverlays = mutableListOf<PendingOverlay>()
    private data class PendingMarker(val attractionId: String, val name: String, val lat: Double, val lng: Double, val visited: Boolean)
    private val pendingMarkers = mutableListOf<PendingMarker>()
    private var pendingCamera: Triple<Double, Double, Float>? = null

    private val handler = Handler(Looper.getMainLooper())

    // 长按检测
    private var touchDownTime = 0L
    private var touchDownPoint: android.graphics.Point? = null
    private var touchDownRegionId: String? = null
    private var longPressFired = false
    private val longPressTimeout = 500L

    // 双击检测
    private var lastTapTime = 0L
    private var lastTapRegionId: String? = null

    fun bindMap(amap: AMap) {
        this.aMap = amap

        amap.setOnMapTouchListener { motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    val map = aMap ?: return@setOnMapTouchListener
                    val screenPoint = android.graphics.Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                    val latLng = map.projection.fromScreenLocation(screenPoint)
                    val regionId = findRegionAt(latLng)
                    if (regionId != null) {
                        touchDownTime = System.currentTimeMillis()
                        touchDownPoint = screenPoint
                        touchDownRegionId = regionId
                        longPressFired = false
                        handler.postDelayed({
                            if (touchDownRegionId == regionId && !longPressFired) {
                                longPressFired = true
                                regionLongPressListener?.invoke(regionId)
                            }
                        }, longPressTimeout)
                    } else {
                        touchDownRegionId = null
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // 手指移动超过阈值则取消长按
                    if (touchDownPoint != null && touchDownRegionId != null) {
                        val dx = motionEvent.x.toInt() - touchDownPoint!!.x
                        val dy = motionEvent.y.toInt() - touchDownPoint!!.y
                        if (dx * dx + dy * dy > 25) {
                            handler.removeCallbacksAndMessages(null)
                            touchDownRegionId = null
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    if (longPressFired) {
                        touchDownRegionId = null
                        return@setOnMapTouchListener
                    }
                    val regionId = touchDownRegionId
                    if (regionId != null) {
                        val now = System.currentTimeMillis()
                        val isDoubleTap = (now - lastTapTime < 400) && (regionId == lastTapRegionId)
                        if (isDoubleTap) {
                            regionDoubleTapListener?.invoke(regionId)
                            lastTapTime = 0L
                            lastTapRegionId = null
                        } else {
                            lastTapTime = now
                            lastTapRegionId = regionId
                        }
                    }
                    touchDownRegionId = null
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    touchDownRegionId = null
                }
            }
        }

        amap.setOnMarkerClickListener { marker ->
            markers.entries.find { it.value == marker }?.key?.let { id ->
                markerTapListener?.invoke(id)
            }
            true
        }

        amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition) {}
            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition) {
                cameraZoomChangeListener?.invoke(position.zoom)
            }
        })

        // 回放缓存的 overlay
        val overlaysToReplay = pendingOverlays.toList()
        pendingOverlays.clear()
        for (p in overlaysToReplay) {
            addOverlayToMap(p.regionId, p.boundary, p.style)
        }

        // 回放缓存的 marker
        val markersToReplay = pendingMarkers.toList()
        pendingMarkers.clear()
        for (m in markersToReplay) {
            addMarkerToMap(m.attractionId, m.name, m.lat, m.lng, m.visited)
        }

        pendingCamera?.let { (lat, lng, zoom) ->
            amap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom))
        }
        pendingCamera = null

        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.86, 104.19), 4f))
    }

    fun unbindMap() {
        handler.removeCallbacksAndMessages(null)
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
        overlays.values.forEach { it.remove() }
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
        markers.values.forEach { it.remove() }
        markers.clear()
        pendingMarkers.clear()
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
        cameraZoomChangeListener = listener
    }

    actual fun dispose() {
        handler.removeCallbacksAndMessages(null)
        overlays.values.forEach { it.remove() }
        overlays.clear()
        markers.values.forEach { it.remove() }
        markers.clear()
        pendingOverlays.clear()
        pendingMarkers.clear()
        regionLongPressListener = null
        regionDoubleTapListener = null
        markerTapListener = null
        cameraZoomChangeListener = null
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

    private fun findRegionAt(latLng: LatLng): String? {
        for ((id, polygon) in overlays) {
            if (polygon.contains(latLng)) return id
        }
        return null
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
