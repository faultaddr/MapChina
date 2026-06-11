package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

data class GeoProjection(
    val viewCenterLng: Double,
    val viewCenterLat: Double,
    val scale: Float,
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    private val centerMercY: Double = ln(tan(PI / 4 + Math.toRadians(viewCenterLat) / 2))

    fun project(lng: Double, lat: Double): Offset {
        val x = (lng - viewCenterLng).toFloat() * scale + canvasWidth / 2
        val mercY = ln(tan(PI / 4 + Math.toRadians(lat) / 2))
        val y = -(mercY - centerMercY).toFloat() * scale + canvasHeight / 2
        return Offset(x, y)
    }

    fun unproject(x: Float, y: Float): Pair<Double, Double> {
        val lng = (x - canvasWidth / 2) / scale.toDouble() + viewCenterLng
        val mercY = -((y - canvasHeight / 2) / scale).toDouble() + centerMercY
        val lat = Math.toDegrees(2 * atan(exp(mercY)) - PI / 2)
        return lng to lat
    }
}
