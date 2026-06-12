package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

class ViewportState(
    initialCenterLng: Double = 104.0,
    initialCenterLat: Double = 35.5,
    initialZoomLevel: Float = 3.5f
) {
    companion object {
        const val BASE_ZOOM = 3.5f
        const val BASE_SCALE = 12.0f
        const val MIN_ZOOM = 3f
        const val MAX_ZOOM = 15f

        // China bounding box with padding for national-level constraint
        const val CHINA_MIN_LNG = 68.0
        const val CHINA_MAX_LNG = 140.0
        const val CHINA_MIN_LAT = 14.0
        const val CHINA_MAX_LAT = 57.0
    }

    var centerLng by mutableStateOf(initialCenterLng)
    var centerLat by mutableStateOf(initialCenterLat)
    var zoomLevel by mutableFloatStateOf(initialZoomLevel)

    val derivedScale: Float get() = BASE_SCALE * 2f.pow(zoomLevel - BASE_ZOOM)

    internal var canvasWidth: Float = 400f
    internal var canvasHeight: Float = 800f

    // Viewport constraint
    var panEnabled by mutableStateOf(true)

    // Bounding box constraint (null = unconstrained)
    var boundsMinLng: Double? = null
    var boundsMaxLng: Double? = null
    var boundsMinLat: Double? = null
    var boundsMaxLat: Double? = null

    fun panBy(delta: Offset) {
        if (!panEnabled) return
        val s = derivedScale
        val ms = s * (180.0 / PI).toFloat()
        centerLng -= delta.x / s
        val currentMerc = ln(tan(PI / 4 + centerLat * PI / 360))
        val newMerc = currentMerc + delta.y / ms
        centerLat = (2 * atan(exp(newMerc)) - PI / 2) * 180 / PI
        centerLat = centerLat.coerceIn(-85.0, 85.0)
        clampToBounds()
    }

    fun zoomBy(delta: Float, pivot: Offset) {
        val oldZoom = zoomLevel
        zoomLevel = (zoomLevel + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (zoomLevel == oldZoom) return

        val oldScale = BASE_SCALE * 2f.pow(oldZoom - BASE_ZOOM)
        val newScale = derivedScale
        val oldMercScale = oldScale * (180.0 / PI).toFloat()
        val newMercScale = newScale * (180.0 / PI).toFloat()
        val pivotLng = centerLng + (pivot.x - canvasWidth / 2) / oldScale
        val pivotMerc = ln(tan(PI / 4 + centerLat * PI / 360)) -
            (pivot.y - canvasHeight / 2) / oldMercScale
        val pivotLat = (2 * atan(exp(pivotMerc)) - PI / 2) * 180 / PI

        centerLng = pivotLng - (pivot.x - canvasWidth / 2) / newScale
        val newPivotMerc = ln(tan(PI / 4 + pivotLat * PI / 360))
        centerLat = (2 * atan(exp(newPivotMerc + (pivot.y - canvasHeight / 2) / newMercScale)) - PI / 2) * 180 / PI
        centerLat = centerLat.coerceIn(-85.0, 85.0)
        clampToBounds()
    }

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean = false) {
        centerLng = lng
        centerLat = lat.coerceIn(-85.0, 85.0)
        zoomLevel = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
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
        if (minLng != null && maxLng != null) {
            val halfSpanLng = (canvasWidth / 2) / derivedScale
            val minCenterLng = minLng + halfSpanLng
            val maxCenterLng = maxLng - halfSpanLng
            if (minCenterLng < maxCenterLng) {
                centerLng = centerLng.coerceIn(minCenterLng, maxCenterLng)
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
                val currentMerc = ln(tan(PI / 4 + centerLat * PI / 360))
                val clampedMerc = currentMerc.coerceIn(minCenterMerc, maxCenterMerc)
                centerLat = (2 * atan(exp(clampedMerc)) - PI / 2) * 180 / PI
            }
        }
    }
}
