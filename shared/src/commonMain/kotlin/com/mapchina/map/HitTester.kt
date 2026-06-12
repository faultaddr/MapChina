package com.mapchina.map

import androidx.compose.ui.geometry.Rect

class HitTester(
    private val bounds: Map<String, Rect>,
    private val coords: Map<String, List<List<Pair<Double, Double>>>>
) {
    fun hitTest(screenX: Float, screenY: Float, projection: GeoProjection): String? {
        val (lng, lat) = projection.unproject(screenX, screenY)

        val candidates = bounds.filter { (_, rect) ->
            screenX >= rect.left && screenX <= rect.right &&
                screenY >= rect.top && screenY <= rect.bottom
        }
        for ((id, _) in candidates) {
            val rings = coords[id] ?: continue
            for (ring in rings) {
                if (pointInPolygon(lng, lat, ring)) {
                    return id
                }
            }
        }
        return null
    }

    private fun pointInPolygon(testLng: Double, testLat: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].first; val yi = polygon[i].second
            val xj = polygon[j].first; val yj = polygon[j].second
            if (((yi > testLat) != (yj > testLat)) &&
                (testLng < (xj - xi) * (testLat - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
