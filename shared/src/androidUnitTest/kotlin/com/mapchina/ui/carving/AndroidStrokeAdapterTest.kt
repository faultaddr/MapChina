package com.mapchina.ui.carving

import androidx.compose.ui.unit.IntSize
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingBrushType
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidStrokeAdapterTest {

    private val brush = CarvingBrushSpec(
        type = CarvingBrushType.MONUMENTAL,
        sizeFraction = 0.09f,
        colorArgb = 0xFF1A1612.toInt()
    )

    @Test
    fun platformPoints_areNormalizedAgainstCanvas() {
        val points = normalizePlatformPoints(
            listOf(
                PlatformPoint(100f, 200f, 0.7f, 0L),
                PlatformPoint(300f, 600f, 0.8f, 12L)
            ),
            IntSize(400, 800)
        )

        assertEquals(0.25f, points.first().x)
        assertEquals(0.25f, points.first().y)
        assertEquals(0.75f, points.last().x)
    }

    @Test
    fun inkBrushSize_usesMinimumEpsilonBeforeSurfaceMeasurement() {
        assertEquals(0.1f, inkBrushSize(brush, IntSize.Zero))
        assertEquals(36f, inkBrushSize(brush, IntSize(400, 800)))
    }
}
