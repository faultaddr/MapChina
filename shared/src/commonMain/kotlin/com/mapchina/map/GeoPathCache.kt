package com.mapchina.map

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType

class GeoPathCache {
    private var cachedPaths: Map<String, List<Path>> = emptyMap()
    private var cachedBounds: Map<String, Rect> = emptyMap()
    private var lastProjection: GeoProjection? = null
    private var lastOverlayKeys: Set<String>? = null

    val paths: Map<String, List<Path>> get() = cachedPaths
    val bounds: Map<String, Rect> get() = cachedBounds

    fun buildIfChanged(
        overlays: Map<String, OverlayData>,
        projection: GeoProjection,
        zoomLevel: Float
    ): GeoPathCache {
        val currentKeys = overlays.keys
        if (projection == lastProjection && currentKeys == lastOverlayKeys) return this

        val epsilon = when {
            zoomLevel < 6 -> 0.05
            zoomLevel < 10 -> 0.01
            else -> 0.0
        }

        val newPaths = mutableMapOf<String, List<Path>>()
        val newBounds = mutableMapOf<String, Rect>()

        for ((id, data) in overlays) {
            val paths = mutableListOf<Path>()
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (ring in data.coords) {
                val simplified = if (epsilon > 0) DouglasPeucker.simplify(ring, epsilon) else ring
                val path = Path()
                path.fillType = PathFillType.EvenOdd
                for ((i, point) in simplified.withIndex()) {
                    val offset = projection.project(point.first, point.second)
                    if (offset.x < minX) minX = offset.x
                    if (offset.y < minY) minY = offset.y
                    if (offset.x > maxX) maxX = offset.x
                    if (offset.y > maxY) maxY = offset.y
                    if (i == 0) path.moveTo(offset.x, offset.y)
                    else path.lineTo(offset.x, offset.y)
                }
                path.close()
                paths.add(path)
            }

            newPaths[id] = paths
            if (minX < Float.MAX_VALUE) {
                newBounds[id] = Rect(minX, minY, maxX, maxY)
            }
        }

        cachedPaths = newPaths
        cachedBounds = newBounds
        lastProjection = projection
        lastOverlayKeys = currentKeys
        return this
    }

    fun getVisibleRegions(viewport: Rect): List<String> {
        return cachedBounds.filter { (_, rect) ->
            rect.overlaps(viewport)
        }.keys.toList()
    }
}
