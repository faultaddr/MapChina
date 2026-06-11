package com.mapchina.map

import androidx.compose.ui.geometry.Rect

class HitTester(
    private val bounds: Map<String, Rect>,
    private val coords: Map<String, List<List<Pair<Double, Double>>>>
) {
    fun hitTest(x: Float, y: Float): String? {
        val candidates = bounds.filter { (_, rect) ->
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        }
        for ((id, _) in candidates) {
            val rings = coords[id] ?: continue
            for (ring in rings) {
                if (pointInPolygonD(x.toDouble(), y.toDouble(), ring)) {
                    return id
                }
            }
        }
        return null
    }

    private fun pointInPolygonD(x: Double, y: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var i = 0
        var j = polygon.size - 1
        while (i < polygon.size) {
            val xi = polygon[i].first; val yi = polygon[i].second
            val xj = polygon[j].first; val yj = polygon[j].second
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i++
        }
        return inside
    }
}
