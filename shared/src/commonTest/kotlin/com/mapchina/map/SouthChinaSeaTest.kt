package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SouthChinaSeaTest {
    @Test
    fun geographicData_containsExactlyNineIndependentSegments() {
        assertEquals(9, SouthChinaSea.DASH_SEGMENTS.size)
        assertTrue(SouthChinaSea.DASH_SEGMENTS.all { it.size >= 3 })
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
}
