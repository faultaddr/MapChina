package com.mapchina.map

import androidx.compose.ui.graphics.Color

data class RenderState(
    val overlays: Map<String, OverlayData> = emptyMap(),
    val markers: Map<String, MarkerData> = emptyMap(),
    val attractionMarkers: Map<String, AttractionMarkerData> = emptyMap(),
    val imageMarkers: Map<String, ImageMarkerData> = emptyMap(),
    val polylines: Map<String, PolylineData> = emptyMap(),
    val pulseTarget: String? = null,
    val oceanColor: Color = Color(0xFFE8F4F8)
)

data class OverlayData(
    val coords: List<List<Pair<Double, Double>>>,
    val style: OverlayStyle
)

data class MarkerData(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val visited: Boolean
)

data class AttractionMarkerData(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val imageUrl: String?,
    val visited: Boolean
)

data class ImageMarkerData(
    val id: String,
    val lat: Double,
    val lng: Double,
    val imagePath: String,
    val count: Int
)

data class PolylineData(
    val id: String,
    val points: List<Pair<Double, Double>>,
    val color: Long,
    val width: Float
)

fun OverlayStyle.toFillColor(): Color {
    val a = ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
    val rgb = (fillColor and 0xFFFFFF).toInt()
    return Color(a or rgb)
}

fun OverlayStyle.toStrokeColor(): Color {
    val a = 0xFF000000.toInt()
    val rgb = (strokeColor and 0xFFFFFF).toInt()
    return Color(a or rgb)
}
