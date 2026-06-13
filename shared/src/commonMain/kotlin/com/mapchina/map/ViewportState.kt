package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.tan

data class CameraState(
    val centerLng: Double = 104.0,
    val centerLat: Double = 35.5,
    val zoomLevel: Float = 3.5f
)

class ViewportState(
    initialCamera: CameraState = CameraState()
) {
    companion object {
        const val BASE_ZOOM = 3.5f
        const val BASE_SCALE = 12.0f
        const val MIN_ZOOM = 2f
        const val MAX_ZOOM = 15f

        const val CHINA_MIN_LNG = 68.0
        const val CHINA_MAX_LNG = 140.0
        const val CHINA_MIN_LAT = 14.0
        const val CHINA_MAX_LAT = 57.0

        const val CHINA_FIT_MIN_LNG = 73.0
        const val CHINA_FIT_MAX_LNG = 136.0
        const val CHINA_FIT_MIN_LAT = 3.0
        const val CHINA_FIT_MAX_LAT = 54.0
    }

    var camera by mutableStateOf(initialCamera)
        private set

    val centerLng: Double get() = camera.centerLng
    val centerLat: Double get() = camera.centerLat
    val zoomLevel: Float get() = camera.zoomLevel
    val derivedScale: Float get() = BASE_SCALE * 2f.pow(zoomLevel - BASE_ZOOM)

    internal var canvasWidth: Float = 400f
    internal var canvasHeight: Float = 800f

    var panEnabled by mutableStateOf(true)

    var boundsMinLng: Double? = null
    var boundsMaxLng: Double? = null
    var boundsMinLat: Double? = null
    var boundsMaxLat: Double? = null

    fun panBy(delta: Offset) {
        if (!panEnabled) return
        val s = derivedScale
        val ms = s * (180.0 / PI).toFloat()
        val newLng = centerLng - delta.x / s
        val currentMerc = ln(tan(PI / 4 + centerLat * PI / 360))
        val newMerc = currentMerc + delta.y / ms
        val newLat = ((2 * atan(exp(newMerc)) - PI / 2) * 180 / PI).coerceIn(-85.0, 85.0)
        camera = CameraState(newLng, newLat, zoomLevel)
        clampToBounds()
    }

    fun zoomBy(delta: Float, pivot: Offset) {
        val oldZoom = zoomLevel
        val newZoom = (zoomLevel + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (newZoom == oldZoom) return

        val oldScale = BASE_SCALE * 2f.pow(oldZoom - BASE_ZOOM)
        val newScale = BASE_SCALE * 2f.pow(newZoom - BASE_ZOOM)
        val oldMercScale = oldScale * (180.0 / PI).toFloat()
        val newMercScale = newScale * (180.0 / PI).toFloat()
        val pivotLng = centerLng + (pivot.x - canvasWidth / 2) / oldScale
        val pivotMerc = ln(tan(PI / 4 + centerLat * PI / 360)) -
            (pivot.y - canvasHeight / 2) / oldMercScale
        val pivotLat = (2 * atan(exp(pivotMerc)) - PI / 2) * 180 / PI

        val resultLng = pivotLng - (pivot.x - canvasWidth / 2) / newScale
        val newPivotMerc = ln(tan(PI / 4 + pivotLat * PI / 360))
        val resultLat = ((2 * atan(exp(newPivotMerc + (pivot.y - canvasHeight / 2) / newMercScale)) - PI / 2) * 180 / PI).coerceIn(-85.0, 85.0)
        camera = CameraState(resultLng, resultLat, newZoom)
        clampToBounds()
    }

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean = false) {
        camera = CameraState(lng, lat.coerceIn(-85.0, 85.0), zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
    }

    fun updateCamera(lng: Double, lat: Double, zoom: Float) {
        camera = CameraState(lng, lat.coerceIn(-85.0, 85.0), zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
    }

    fun fitChinaInView(padding: Float = 0.96f) {
        val target = computeChinaFitTarget(padding)
        camera = CameraState(target.first, target.second, target.third)
    }

    fun computeChinaFitTarget(padding: Float = 0.96f): Triple<Double, Double, Float> {
        val w = canvasWidth
        val h = canvasHeight
        val targetLng = (CHINA_FIT_MIN_LNG + CHINA_FIT_MAX_LNG) / 2.0
        val targetLat = (CHINA_FIT_MIN_LAT + CHINA_FIT_MAX_LAT) / 2.0
        if (w <= 0f || h <= 0f) return Triple(targetLng, targetLat, 3.5f)

        val lngSpan = CHINA_FIT_MAX_LNG - CHINA_FIT_MIN_LNG
        val mercMin = ln(tan(PI / 4 + CHINA_FIT_MIN_LAT * PI / 360))
        val mercMax = ln(tan(PI / 4 + CHINA_FIT_MAX_LAT * PI / 360))
        val mercSpan = mercMax - mercMin

        val scaleFromLng = (w * padding) / lngSpan.toFloat()
        val scaleFromLat = if (mercSpan > 0.0)
            (h * padding) / (mercSpan.toFloat() * (180.0 / PI).toFloat())
        else Float.MAX_VALUE

        val targetScale = minOf(scaleFromLng, scaleFromLat)
        val targetZoom = (BASE_ZOOM +
            log2((targetScale / BASE_SCALE).toDouble()).toFloat())
            .coerceIn(MIN_ZOOM, MAX_ZOOM)

        return Triple(targetLng, targetLat, targetZoom)
    }

    fun setChinaBounds() {
        boundsMinLng = CHINA_MIN_LNG
        boundsMaxLng = CHINA_MAX_LNG
        boundsMinLat = CHINA_MIN_LAT
        boundsMaxLat = CHINA_MAX_LAT
    }

    fun clearBounds() {
        boundsMinLng = null
        boundsMaxLng = null
        boundsMinLat = null
        boundsMaxLat = null
    }

    fun toProjection(width: Float, height: Float): GeoProjection {
        canvasWidth = width
        canvasHeight = height
        return GeoProjection(
            viewCenterLng = centerLng,
            viewCenterLat = centerLat,
            scale = derivedScale,
            canvasWidth = width,
            canvasHeight = height
        )
    }

    private fun clampToBounds() {
        val minLng = boundsMinLng
        val maxLng = boundsMaxLng
        val minLat = boundsMinLat
        val maxLat = boundsMaxLat
        var lng = centerLng
        var lat = centerLat

        if (minLng != null && maxLng != null) {
            val halfSpanLng = (canvasWidth / 2) / derivedScale
            val minCenterLng = minLng + halfSpanLng
            val maxCenterLng = maxLng - halfSpanLng
            if (minCenterLng < maxCenterLng) {
                lng = lng.coerceIn(minCenterLng, maxCenterLng)
            }
        }
        if (minLat != null && maxLat != null) {
            val ms = derivedScale * (180.0 / PI).toFloat()
            val halfSpanMerc = (canvasHeight / 2) / ms
            val minMerc = ln(tan(PI / 4 + minLat * PI / 360))
            val maxMerc = ln(tan(PI / 4 + maxLat * PI / 360))
            val minCenterMerc = minMerc + halfSpanMerc
            val maxCenterMerc = maxMerc - halfSpanMerc
            if (minCenterMerc < maxCenterMerc) {
                val currentMerc = ln(tan(PI / 4 + lat * PI / 360))
                val clampedMerc = currentMerc.coerceIn(minCenterMerc, maxCenterMerc)
                lat = (2 * atan(exp(clampedMerc)) - PI / 2) * 180 / PI
            }
        }
        if (lng != centerLng || lat != centerLat) {
            camera = CameraState(lng, lat, zoomLevel)
        }
    }
}
