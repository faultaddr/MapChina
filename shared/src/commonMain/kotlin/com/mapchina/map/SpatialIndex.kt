package com.mapchina.map

import androidx.compose.ui.geometry.Offset

data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    fun contains(point: Offset): Boolean =
        point.x in minX..maxX && point.y in minY..maxY

    fun intersects(other: BoundingBox): Boolean =
        !(other.maxX < minX || other.minX > maxX || other.maxY < minY || other.minY > maxY)
}

data class IndexedRegion(
    val regionId: String,
    val bounds: BoundingBox,
    val polygon: List<Offset>
)

class SpatialIndex {
    private val regions = mutableListOf<IndexedRegion>()

    fun insert(region: IndexedRegion) {
        regions.add(region)
    }

    fun clear() {
        regions.clear()
    }

    fun query(point: Offset): List<IndexedRegion> {
        return regions.filter { region ->
            region.bounds.contains(point) && pointInPolygon(point.x, point.y, region.polygon)
        }
    }

    fun queryNearest(point: Offset, maxResults: Int = 1): List<IndexedRegion> {
        return regions
            .filter { it.bounds.contains(point) }
            .sortedBy { region ->
                val cx = (region.bounds.minX + region.bounds.maxX) / 2
                val cy = (region.bounds.minY + region.bounds.maxY) / 2
                val dx = point.x - cx
                val dy = point.y - cy
                dx * dx + dy * dy
            }
            .take(maxResults)
    }

    private fun pointInPolygon(x: Float, y: Float, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
