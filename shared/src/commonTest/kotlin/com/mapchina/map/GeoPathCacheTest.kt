package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoPathCacheTest {
    private val style = OverlayStyle(
        fillColor = 0xFF000000,
        strokeColor = 0xFF000000,
        strokeWidth = 1f,
        alpha = 1f
    )

    @Test
    fun zoomChangesWithinSameSimplificationBucket_doNotSimplifyAgain() {
        var simplifyCalls = 0
        val cache = countingCache { simplifyCalls++ }
        val overlays = overlays()

        cache.buildIfChanged(overlays, projection(scale = 12f), zoomLevel = 3.5f)
        val callsAfterFirstBuild = simplifyCalls
        cache.buildIfChanged(overlays, projection(scale = 20f), zoomLevel = 5.9f)

        assertEquals(callsAfterFirstBuild, simplifyCalls)
    }

    @Test
    fun crossingSimplificationBucket_recomputesRingsOnce() {
        var simplifyCalls = 0
        val cache = countingCache { simplifyCalls++ }
        val overlays = overlays()

        cache.buildIfChanged(overlays, projection(scale = 20f), zoomLevel = 5.9f)
        val callsBeforeBucketChange = simplifyCalls
        cache.buildIfChanged(overlays, projection(scale = 21f), zoomLevel = 6f)

        assertEquals(callsBeforeBucketChange + 1, simplifyCalls)
    }

    @Test
    fun replacingGeometryUnderSameRegionId_recomputesRings() {
        var simplifyCalls = 0
        val cache = countingCache { simplifyCalls++ }

        cache.buildIfChanged(overlays(), projection(scale = 20f), zoomLevel = 5f)
        val callsBeforeGeometryChange = simplifyCalls
        cache.buildIfChanged(
            overlays(lastPoint = 3.0 to 2.0),
            projection(scale = 20f),
            zoomLevel = 5f
        )

        assertEquals(callsBeforeGeometryChange + 1, simplifyCalls)
    }

    @Test
    fun styleOnlyOverlayCopy_doesNotRecomputeRings() {
        var simplifyCalls = 0
        val cache = countingCache { simplifyCalls++ }
        val overlays = overlays()

        cache.buildIfChanged(overlays, projection(scale = 20f), zoomLevel = 5f)
        val callsBeforeStyleChange = simplifyCalls
        val restyled = overlays.mapValues { (_, data) ->
            data.copy(style = data.style.copy(alpha = 0.5f))
        }
        cache.buildIfChanged(restyled, projection(scale = 20f), zoomLevel = 5f)

        assertEquals(callsBeforeStyleChange, simplifyCalls)
    }

    private fun countingCache(onSimplify: () -> Unit): GeoPathCache =
        GeoPathCache { points, _ ->
            onSimplify()
            points
        }

    private fun overlays(lastPoint: Pair<Double, Double> = 2.0 to 0.0) = mapOf(
        "region" to OverlayData(
            coords = listOf(listOf(0.0 to 0.0, 1.0 to 1.0, lastPoint)),
            style = style
        )
    )

    private fun projection(scale: Float) = GeoProjection(
        viewCenterLng = 104.0,
        viewCenterLat = 35.5,
        scale = scale,
        canvasWidth = 400f,
        canvasHeight = 800f
    )
}
