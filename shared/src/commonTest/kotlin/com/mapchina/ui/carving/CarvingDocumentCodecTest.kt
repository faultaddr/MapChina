package com.mapchina.ui.carving

import com.mapchina.ui.carving.v2.CarvingBrushType
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingDocumentCodec
import com.mapchina.ui.carving.v2.CarvingDecodeResult
import com.mapchina.ui.carving.v2.CarvingPoint
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CarvingDocumentCodecTest {
    private val document = CarvingDocument(
        canvasAspectRatio = 0.62f,
        strokes = listOf(
            CarvingStroke(
                brushType = CarvingBrushType.MONUMENTAL,
                sizeFraction = 0.09f,
                colorArgb = 0xFF1A1612.toInt(),
                points = listOf(
                    CarvingPoint(0.2f, 0.3f, 0.5f, 0L),
                    CarvingPoint(0.4f, 0.6f, 0.7f, 14L)
                )
            )
        )
    )

    @Test
    fun v2RoundTripPreservesDocument() {
        val encoded = CarvingDocumentCodec.encode(document)

        assertTrue(encoded.contains("\"brushType\""))
        assertTrue(encoded.contains("\"sizeFraction\""))
        assertTrue(encoded.contains("\"colorArgb\""))
        assertTrue(encoded.contains("\"points\""))
        assertFalse(encoded.contains("\"brush\""))

        val decoded = assertIs<CarvingDecodeResult.Success>(
            CarvingDocumentCodec.decode(encoded)
        )
        assertEquals(2, decoded.sourceVersion)
        assertEquals(document, decoded.document)
    }

    @Test
    fun invalidJsonIsNotTreatedAsEmpty() {
        assertIs<CarvingDecodeResult.Invalid>(CarvingDocumentCodec.decode("not-json"))
    }

    @Test
    fun unknownVersionIsRejectedWithoutFallbackToV1() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode("""{"version":9,"canvasAspectRatio":1,"strokes":[]}""")
        )
    }

    @Test
    fun missingVersionIsInvalid() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode("""{"canvasAspectRatio":1,"strokes":[]}""")
        )
    }

    @Test
    fun blankDataIsEmpty() {
        assertIs<CarvingDecodeResult.Empty>(CarvingDocumentCodec.decode("  \n"))
    }

    @Test
    fun emptyV1ArrayHasNoValidStrokes() {
        val result = assertIs<CarvingDecodeResult.Invalid>(CarvingDocumentCodec.decode("[]"))

        assertEquals("legacy_document_has_no_valid_strokes", result.reason)
    }

    @Test
    fun v1Array_normalizesGeometryAndKeepsBrushMetadata() {
        val legacy = """[{"inputs":[{"x":100,"y":200,"pressure":0.4,"elapsedTimeMillis":0},{"x":300,"y":600,"pressure":0.8,"elapsedTimeMillis":20}],"brushSize":48,"brushColorArgb":-15000000,"brushType":"MONUMENTAL"}]"""

        val result = assertIs<CarvingDecodeResult.Success>(
            CarvingDocumentCodec.decode(legacy, previewAspectRatio = 0.62f)
        )

        assertEquals(1, result.sourceVersion)
        assertTrue(result.document.strokes.single().points.all { it.x in 0f..1f && it.y in 0f..1f })
        assertEquals(CarvingBrushType.MONUMENTAL, result.document.strokes.single().brushType)
        assertEquals(0.62f, result.document.canvasAspectRatio)
        assertEquals(-15000000, result.document.strokes.single().colorArgb)
        assertTrue(result.document.strokes.single().sizeFraction > 0f)
    }

    @Test
    fun v1Conversion_isStableAcrossRepeatedReads() {
        val legacy = """[{"inputs":[{"x":10,"y":20,"pressure":0.5,"elapsedTimeMillis":0},{"x":30,"y":60,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":12,"brushColorArgb":-15000000,"brushType":"MONUMENTAL"}]"""

        val first = CarvingDocumentCodec.decode(legacy, null)
        val second = CarvingDocumentCodec.decode(legacy, null)

        assertEquals(first, second)
    }

    @Test
    fun v1UnknownBrushFallsBackToMonumental() {
        val legacy = """[{"inputs":[{"x":10,"y":20,"pressure":0.5,"elapsedTimeMillis":0},{"x":30,"y":60,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":12,"brushColorArgb":7,"brushType":"FUTURE_BRUSH"}]"""

        val result = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(legacy))

        assertEquals(CarvingBrushType.MONUMENTAL, result.document.strokes.single().brushType)
    }

    @Test
    fun v1InvalidStrokesAreSkippedAndNoValidStrokeIsInvalid() {
        val legacy = """[{"inputs":[{"x":10,"y":20,"pressure":0.5,"elapsedTimeMillis":0}],"brushSize":12,"brushColorArgb":7,"brushType":"MONUMENTAL"}]"""

        val result = assertIs<CarvingDecodeResult.Invalid>(CarvingDocumentCodec.decode(legacy))

        assertEquals("legacy_document_has_no_valid_strokes", result.reason)
    }

    @Test
    fun v1WithoutPreferredAspectClampsContentAspectRatio() {
        val legacy = """[{"inputs":[{"x":0,"y":0,"pressure":0.5,"elapsedTimeMillis":0},{"x":10,"y":100,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":2,"brushColorArgb":7,"brushType":"MONUMENTAL"}]"""

        val result = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(legacy, null))

        assertEquals(0.55f, result.document.canvasAspectRatio)
    }

    @Test
    fun v1LandscapePreferredAspectUsesShorterViewportEdgeForBrushSize() {
        val legacy = """[{"inputs":[{"x":0,"y":0,"pressure":0.5,"elapsedTimeMillis":0},{"x":100,"y":50,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":20,"brushColorArgb":7,"brushType":"MONUMENTAL"}]"""

        val result = assertIs<CarvingDecodeResult.Success>(
            CarvingDocumentCodec.decode(legacy, previewAspectRatio = 2f)
        )

        assertEquals(2f, result.document.canvasAspectRatio)
        assertEquals(20f / (70f * 1.16f), result.document.strokes.single().sizeFraction, 0.0001f)
    }

    @Test
    fun v1ElapsedTimeIsNonDecreasingWithinEachStroke() {
        val legacy = """[{"inputs":[{"x":0,"y":0,"pressure":0.5,"elapsedTimeMillis":20},{"x":100,"y":50,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":20,"brushColorArgb":7,"brushType":"MONUMENTAL"}]"""

        val result = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(legacy))

        assertEquals(listOf(20L, 20L), result.document.strokes.single().points.map { it.elapsedTimeMillis })
    }

    @Test
    fun decodeNormalizesValuesAndDropsStrokesWithoutTwoValidPoints() {
        val result = assertIs<CarvingDecodeResult.Success>(
            CarvingDocumentCodec.decode(
                """
                {
                  "version": 2,
                  "canvasAspectRatio": 0.8,
                  "strokes": [
                    {
                      "brushType": "IRON_CHISEL",
                      "sizeFraction": 2.0,
                      "colorArgb": -1,
                      "points": [
                        {"x": -0.2, "y": 1.4, "pressure": -1.0, "elapsedTimeMillis": 20},
                        {"x": 0.2, "y": 0.3, "pressure": 2.0, "elapsedTimeMillis": 10}
                      ]
                    },
                    {
                      "brushType": "WEATHERED",
                      "sizeFraction": 0.02,
                      "colorArgb": 7,
                      "points": [
                        {"x": 0.1, "y": 0.2, "pressure": 0.5, "elapsedTimeMillis": 1}
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val stroke = result.document.strokes.single()
        assertEquals(0.30f, stroke.sizeFraction)
        assertEquals(0f, stroke.points[0].x)
        assertEquals(1f, stroke.points[0].y)
        assertEquals(0f, stroke.points[0].pressure)
        assertEquals(1f, stroke.points[1].pressure)
        assertEquals(20L, stroke.points[0].elapsedTimeMillis)
        assertEquals(20L, stroke.points[1].elapsedTimeMillis)
    }

    @Test
    fun nonFiniteAspectRatioIsInvalid() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode(
                """{"version":2,"canvasAspectRatio":"NaN","strokes":[]}"""
            )
        )
    }

    @Test
    fun zeroAspectRatioIsRejectedByEncode() {
        assertFailsWith<IllegalArgumentException> {
            CarvingDocumentCodec.encode(document.copy(canvasAspectRatio = 0f))
        }
    }

    @Test
    fun negativeAspectRatioIsRejectedByEncode() {
        assertFailsWith<IllegalArgumentException> {
            CarvingDocumentCodec.encode(document.copy(canvasAspectRatio = -1f))
        }
    }

    @Test
    fun zeroAspectRatioIsInvalidOnDecode() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode(
                """{"version":2,"canvasAspectRatio":0,"strokes":[]}"""
            )
        )
    }

    @Test
    fun negativeAspectRatioIsInvalidOnDecode() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode(
                """{"version":2,"canvasAspectRatio":-1,"strokes":[]}"""
            )
        )
    }
}
