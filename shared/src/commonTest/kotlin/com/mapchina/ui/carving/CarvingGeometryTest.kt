package com.mapchina.ui.carving

import com.mapchina.ui.carving.v2.CarvingBrushType
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingPoint
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CarvingGeometryTest {

    @Test
    fun normalizedPoints_scaleToEveryTargetCanvas() {
        val geometry = buildCarvingGeometry(oneStrokeDocument, 620f, 1000f).single()

        assertEquals(124f, geometry.points.first().x, 0.01f)
        assertEquals(300f, geometry.points.first().y, 0.01f)
        assertEquals(55.8f, geometry.widthPx, 0.01f)
    }

    @Test
    fun strokeWidth_usesTargetShortEdgeAndIgnoresPointPressure() {
        val document = oneStrokeDocument.copy(
            strokes = listOf(
                oneStrokeDocument.strokes.single().copy(
                    points = listOf(
                        CarvingPoint(0.1f, 0.1f, pressure = 0f, elapsedTimeMillis = 0L),
                        CarvingPoint(0.8f, 0.8f, pressure = 1f, elapsedTimeMillis = 16L)
                    )
                )
            )
        )

        val geometry = buildCarvingGeometry(document, 1000f, 620f).single()

        assertEquals(55.8f, geometry.widthPx, 0.01f)
    }

    @Test
    fun debris_isStableForSameStrokeAndCanvas() {
        val stroke = oneStrokeDocument.strokes.single()

        val first = deterministicDebris(stroke, 0, 620f, 1000f)
        val second = deterministicDebris(stroke, 0, 620f, 1000f)

        assertEquals(first, second)
        assertTrue(first.isNotEmpty())
    }

    @Test
    fun normalizePlatformPoints_clampsCoordinatesAndPressure() {
        val normalized = normalizePlatformPoints(
            points = listOf(
                PlatformPoint(x = -20f, y = 500f, pressure = -1f, elapsedTimeMillis = 3L),
                PlatformPoint(x = 150f, y = 50f, pressure = 2f, elapsedTimeMillis = 7L)
            ),
            canvasWidthPx = 100f,
            canvasHeightPx = 200f
        )

        assertEquals(
            listOf(
                CarvingPoint(x = 0f, y = 1f, pressure = 0f, elapsedTimeMillis = 3L),
                CarvingPoint(x = 1f, y = 0.25f, pressure = 1f, elapsedTimeMillis = 7L)
            ),
            normalized
        )
    }

    @Test
    fun normalizePlatformPoints_returnsEmptyForEmptyOrZeroCanvas() {
        val point = PlatformPoint(x = 10f, y = 20f, pressure = 0.5f, elapsedTimeMillis = 1L)

        assertTrue(normalizePlatformPoints(emptyList(), 100f, 200f).isEmpty())
        assertTrue(normalizePlatformPoints(listOf(point), 0f, 200f).isEmpty())
        assertTrue(normalizePlatformPoints(listOf(point), 100f, 0f).isEmpty())
    }

    @Test
    fun coalescePlatformPoints_removesOnlySameTimestampNearDuplicates() {
        val first = PlatformPoint(x = 10f, y = 20f, pressure = 0.4f, elapsedTimeMillis = 1L)
        val nearDuplicate = PlatformPoint(x = 10.49f, y = 20f, pressure = 0.6f, elapsedTimeMillis = 1L)
        val thresholdPoint = PlatformPoint(x = 10.5f, y = 20f, pressure = 0.8f, elapsedTimeMillis = 1L)
        val differentTime = PlatformPoint(x = 10.51f, y = 20f, pressure = 1f, elapsedTimeMillis = 2L)

        assertEquals(
            listOf(first, thresholdPoint, differentTime),
            coalescePlatformPoints(listOf(first, nearDuplicate, thresholdPoint, differentTime))
        )
    }

    private val oneStrokeDocument = CarvingDocument(
        canvasAspectRatio = 0.62f,
        strokes = listOf(
            CarvingStroke(
                brushType = CarvingBrushType.MONUMENTAL,
                sizeFraction = 0.09f,
                colorArgb = 0xFF1A1612.toInt(),
                points = listOf(
                    CarvingPoint(0.2f, 0.3f, pressure = 0.2f, elapsedTimeMillis = 0L),
                    CarvingPoint(0.8f, 0.7f, pressure = 0.9f, elapsedTimeMillis = 16L)
                )
            )
        )
    )
}
