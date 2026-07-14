package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SouthChinaSeaTest {
    @Test
    fun geographicData_containsExactlyTenIndependentSegments() {
        assertEquals(10, SouthChinaSea.DASH_SEGMENTS.size)
        assertTrue(SouthChinaSea.DASH_SEGMENTS.all { it.size >= 2 })
    }

    @Test
    fun geographicData_matchesSouthChinaSeaReferenceExtents() {
        val points = SouthChinaSea.DASH_SEGMENTS.flatten()

        assertEquals(108.30727608084116, points.minOf { it.first }, absoluteTolerance = 0.0001)
        assertEquals(123.00481138309124, points.maxOf { it.first }, absoluteTolerance = 0.0001)
        assertEquals(3.553559321848772, points.minOf { it.second }, absoluteTolerance = 0.0001)
        assertEquals(24.74934291726869, points.maxOf { it.second }, absoluteTolerance = 0.0001)
    }

    @Test
    fun smoothCurve_passesThroughEveryProjectedPoint() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 16f), Offset(24f, 20f))
        val curve = requireNotNull(buildSouthChinaSeaCurve(points))

        assertEquals(points.first(), curve.start)
        assertEquals(points.drop(1), curve.commands.map { it.end })
    }

    @Test
    fun smoothCurve_keepsFiniteControlPoints() {
        val points = listOf(Offset(2f, 3f), Offset(8f, 14f), Offset(21f, 16f))
        val curve = requireNotNull(buildSouthChinaSeaCurve(points))

        assertTrue(curve.commands.all { command ->
            command.control1.x.isFinite() && command.control1.y.isFinite() &&
                command.control2.x.isFinite() && command.control2.y.isFinite() &&
                command.end.x.isFinite() && command.end.y.isFinite()
        })
    }

    @Test
    fun strokeStyle_keepsNationalViewSubordinateToProvinceBoundaries() {
        assertEquals(
            SouthChinaSeaStrokeStyle(widthDp = 1.05f, alpha = 0.50f),
            southChinaSeaStrokeStyle(zoomLevel = 5f),
        )
    }

    @Test
    fun strokeStyle_becomesLighterAndThinnerWhenZoomedIn() {
        assertEquals(
            SouthChinaSeaStrokeStyle(widthDp = 0.85f, alpha = 0.42f),
            southChinaSeaStrokeStyle(zoomLevel = 6f),
        )
    }
}
