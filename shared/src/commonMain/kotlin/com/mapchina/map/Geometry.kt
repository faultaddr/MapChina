package com.mapchina.map

import androidx.compose.ui.geometry.Offset

fun pointInPolygon(x: Float, y: Float, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var i = 0
    var j = polygon.size - 1
    while (i < polygon.size) {
        val xi = polygon[i].x; val yi = polygon[i].y
        val xj = polygon[j].x; val yj = polygon[j].y
        if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
            inside = !inside
        }
        j = i++
    }
    return inside
}
