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
        const val BASE_SCALE = 2.0f
        const val MIN_ZOOM = 3f
        const val MAX_ZOOM = 15f
    }

    var centerLng by mutableStateOf(initialCenterLng)
    var centerLat by mutableStateOf(initialCenterLat)
    var zoomLevel by mutableFloatStateOf(initialZoomLevel)

    val derivedScale: Float get() = BASE_SCALE * 2f.pow(zoomLevel - BASE_ZOOM)

    internal var canvasWidth: Float = 400f
    internal var canvasHeight: Float = 800f

    fun panBy(delta: Offset) {
        val s = derivedScale
        centerLng -= delta.x / s
        val currentMerc = ln(tan(PI / 4 + Math.toRadians(centerLat) / 2))
        val newMerc = currentMerc + delta.y / s
        centerLat = Math.toDegrees(2 * atan(exp(newMerc)) - PI / 2).coerceIn(-85.0, 85.0)
    }

    fun zoomBy(delta: Float, pivot: Offset) {
        val oldZoom = zoomLevel
        zoomLevel = (zoomLevel + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (zoomLevel == oldZoom) return

        val oldScale = BASE_SCALE * 2f.pow(oldZoom - BASE_ZOOM)
        val newScale = derivedScale
        val pivotLng = centerLng + (pivot.x - canvasWidth / 2) / oldScale
        val pivotMerc = ln(tan(PI / 4 + Math.toRadians(centerLat) / 2)) -
            (pivot.y - canvasHeight / 2) / oldScale
        val pivotLat = Math.toDegrees(2 * atan(exp(pivotMerc)) - PI / 2)

        centerLng = pivotLng - (pivot.x - canvasWidth / 2) / newScale
        val newPivotMerc = ln(tan(PI / 4 + Math.toRadians(pivotLat) / 2))
        centerLat = Math.toDegrees(
            2 * atan(exp(newPivotMerc + (pivot.y - canvasHeight / 2) / newScale)) - PI / 2
        ).coerceIn(-85.0, 85.0)
    }

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean = false) {
        centerLng = lng
        centerLat = lat.coerceIn(-85.0, 85.0)
        zoomLevel = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
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
}
