package com.mapchina.map

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlin.math.ln
import kotlin.math.tan
import kotlin.math.PI

class GeoPathCache {
    private var cachedPaths: Map<String, List<Path>> = emptyMap()
    private var cachedBounds: Map<String, Rect> = emptyMap()
    private var lastCenterLng: Double = Double.NaN
    private var lastCenterLat: Double = Double.NaN
    private var lastScale: Float = -1f
    private var lastMercScale: Float = -1f
    private var lastWidth: Float = -1f
    private var lastHeight: Float = -1f
    private var lastEpsilon: Double = -1.0
    private var lastOverlayKeys: Set<String>? = null

    private var precomputedRings: List<PrecomputedRing> = emptyList()

    val paths: Map<String, List<Path>> get() = cachedPaths
    val bounds: Map<String, Rect> get() = cachedBounds
    var boundsChanged: Boolean = false
        private set

    fun buildIfChanged(
        overlays: Map<String, OverlayData>,
        projection: GeoProjection,
        zoomLevel: Float
    ): GeoPathCache {
        val currentKeys = overlays.keys
        val epsilon = when {
            zoomLevel < 6 -> 0.05
            zoomLevel < 10 -> 0.01
            else -> 0.0
        }

        val scaleChanged = lastScale != projection.scale ||
            lastMercScale != projection.mercScale ||
            lastWidth != projection.canvasWidth ||
            lastHeight != projection.canvasHeight ||
            lastEpsilon != epsilon

        val keysChanged = currentKeys != lastOverlayKeys
        boundsChanged = false

        if (keysChanged || scaleChanged) {
            precomputeRings(overlays, epsilon)
        }

        val centerChanged = lastCenterLng != projection.viewCenterLng ||
            lastCenterLat != projection.viewCenterLat

        if (keysChanged || scaleChanged || centerChanged) {
            buildPaths(projection)
            boundsChanged = true
        }

        lastCenterLng = projection.viewCenterLng
        lastCenterLat = projection.viewCenterLat
        lastScale = projection.scale
        lastMercScale = projection.mercScale
        lastWidth = projection.canvasWidth
        lastHeight = projection.canvasHeight
        lastEpsilon = epsilon
        lastOverlayKeys = currentKeys

        return this
    }

    private fun precomputeRings(overlays: Map<String, OverlayData>, epsilon: Double) {
        precomputedRings = overlays.map { (id, data) ->
            val rings = data.coords.map { ring ->
                val simplified = if (epsilon > 0) DouglasPeucker.simplify(ring, epsilon) else ring
                val mercYList = simplified.map { (lng, lat) ->
                    PrecomputedPoint(lng, lat, ln(tan(PI / 4 + lat * PI / 360)))
                }
                PrecomputedRing(id, mercYList)
            }
            rings
        }.flatten()
    }

    private fun buildPaths(projection: GeoProjection) {
        val newPaths = mutableMapOf<String, MutableList<Path>>()
        val newBounds = mutableMapOf<String, Rect>()

        val cx = projection.viewCenterLng
        val cy = projection.viewCenterLat
        val s = projection.scale
        val ms = projection.mercScale
        val cw = projection.canvasWidth
        val ch = projection.canvasHeight
        val centerMercY = ln(tan(PI / 4 + cy * PI / 360))

        val halfW = cw / 2f
        val halfH = ch / 2f

        for (ring in precomputedRings) {
            val path = Path()
            path.fillType = PathFillType.EvenOdd
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for ((i, pt) in ring.points.withIndex()) {
                val x = ((pt.lng - cx) * s + halfW).toFloat()
                val y = (-(pt.mercY - centerMercY) * ms + halfH).toFloat()
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            newPaths.getOrPut(ring.regionId) { mutableListOf() }.add(path)
            if (minX < Float.MAX_VALUE) {
                newBounds[ring.regionId] = Rect(minX, minY, maxX, maxY)
            }
        }

        cachedPaths = newPaths
        cachedBounds = newBounds
    }

    fun getVisibleRegions(viewport: Rect): List<String> {
        return cachedBounds.filter { (_, rect) -> rect.overlaps(viewport) }.keys.toList()
    }
}

private data class PrecomputedPoint(val lng: Double, val lat: Double, val mercY: Double)

private data class PrecomputedRing(val regionId: String, val points: List<PrecomputedPoint>)
