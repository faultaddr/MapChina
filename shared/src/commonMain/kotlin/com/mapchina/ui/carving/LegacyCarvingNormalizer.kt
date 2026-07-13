package com.mapchina.ui.carving.v2

import kotlinx.serialization.Serializable

@Serializable
internal data class LegacyStroke(
    val inputs: List<LegacyInput> = emptyList(),
    val brushSize: Float = 0f,
    val brushColorArgb: Int = 0,
    val brushType: String = ""
)

@Serializable
internal data class LegacyInput(
    val x: Float = Float.NaN,
    val y: Float = Float.NaN,
    val pressure: Float = 0.5f,
    val elapsedTimeMillis: Long = 0L
)

internal object LegacyCarvingNormalizer {
    private const val CONTENT_PADDING_FRACTION = 0.08
    private const val MIN_CONTENT_ASPECT_RATIO = 0.55f
    private const val MAX_CONTENT_ASPECT_RATIO = 1.0f
    private const val MIN_SIZE_FRACTION = 0.005f
    private const val MAX_SIZE_FRACTION = 0.30f

    fun normalize(
        strokes: List<LegacyStroke>,
        preferredAspectRatio: Float?
    ): CarvingDocument {
        val validStrokes = strokes.mapNotNull { it.validOrNull() }
        val contentPoints = validStrokes.flatMap { stroke ->
            stroke.points.map { point -> point to stroke.brushSize }
        }
        val contentBounds = contentPoints.boundsOrNull()
            ?: return CarvingDocument(canvasAspectRatio = 1f, strokes = emptyList())

        val paddedBounds = contentBounds.expandByFraction(CONTENT_PADDING_FRACTION)
        val aspectRatio = preferredAspectRatio
            ?.takeIf { it.isFinite() && it > 0f }
            ?: paddedBounds.aspectRatio.toFloat().coerceIn(
                MIN_CONTENT_ASPECT_RATIO,
                MAX_CONTENT_ASPECT_RATIO
            )
        val viewport = paddedBounds.fitInside(aspectRatio.toDouble())

        return CarvingDocument(
            canvasAspectRatio = aspectRatio,
            strokes = validStrokes.map { stroke ->
                CarvingStroke(
                    brushType = stroke.brushType,
                    sizeFraction = (stroke.brushSize / viewport.width)
                        .toFloat()
                        .coerceIn(MIN_SIZE_FRACTION, MAX_SIZE_FRACTION),
                    colorArgb = stroke.colorArgb,
                    points = stroke.points.mapIndexed { index, point ->
                        CarvingPoint(
                            x = ((point.x - viewport.left) / viewport.width)
                                .coerceIn(0.0, 1.0)
                                .toFloat(),
                            y = ((point.y - viewport.top) / viewport.height)
                                .coerceIn(0.0, 1.0)
                                .toFloat(),
                            pressure = point.pressure.coerceIn(0f, 1f),
                            elapsedTimeMillis = stroke.elapsedTimeMillis[index]
                        )
                    }
                )
            }
        )
    }

    private data class ValidLegacyStroke(
        val points: List<LegacyPoint>,
        val elapsedTimeMillis: List<Long>,
        val brushSize: Double,
        val colorArgb: Int,
        val brushType: CarvingBrushType
    )

    private data class LegacyPoint(
        val x: Double,
        val y: Double,
        val pressure: Float
    )

    private data class Bounds(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double
    ) {
        val width: Double
            get() = right - left
        val height: Double
            get() = bottom - top
        val aspectRatio: Double
            get() = width / height

        fun include(x: Double, y: Double, radius: Double): Bounds {
            return Bounds(
                left = minOf(left, x - radius),
                top = minOf(top, y - radius),
                right = maxOf(right, x + radius),
                bottom = maxOf(bottom, y + radius)
            )
        }

        fun expandByFraction(fraction: Double): Bounds {
            val horizontalPadding = width * fraction
            val verticalPadding = height * fraction
            return Bounds(
                left = left - horizontalPadding,
                top = top - verticalPadding,
                right = right + horizontalPadding,
                bottom = bottom + verticalPadding
            )
        }

        fun fitInside(aspectRatio: Double): Bounds {
            val targetWidth = maxOf(width, height * aspectRatio)
            val targetHeight = maxOf(height, width / aspectRatio)
            val horizontalInset = (targetWidth - width) / 2.0
            val verticalInset = (targetHeight - height) / 2.0
            return Bounds(
                left = left - horizontalInset,
                top = top - verticalInset,
                right = right + horizontalInset,
                bottom = bottom + verticalInset
            )
        }
    }

    private fun LegacyStroke.validOrNull(): ValidLegacyStroke? {
        if (!brushSize.isFinite() || brushSize <= 0f) return null

        val validInputs = inputs.filter { input ->
            input.x.isFinite() && input.y.isFinite() && input.pressure.isFinite()
        }
        if (validInputs.size < 2) return null

        return ValidLegacyStroke(
            points = validInputs.map { input ->
                LegacyPoint(input.x.toDouble(), input.y.toDouble(), input.pressure)
            },
            elapsedTimeMillis = validInputs.map { it.elapsedTimeMillis },
            brushSize = brushSize.toDouble(),
            colorArgb = brushColorArgb,
            brushType = CarvingBrushType.entries.firstOrNull { it.name == brushType }
                ?: CarvingBrushType.MONUMENTAL
        )
    }

    private fun List<Pair<LegacyPoint, Double>>.boundsOrNull(): Bounds? {
        if (isEmpty()) return null
        val (firstPoint, firstBrushSize) = first()
        var bounds = Bounds(
            left = firstPoint.x - firstBrushSize / 2.0,
            top = firstPoint.y - firstBrushSize / 2.0,
            right = firstPoint.x + firstBrushSize / 2.0,
            bottom = firstPoint.y + firstBrushSize / 2.0
        )
        for ((point, brushSize) in drop(1)) {
            bounds = bounds.include(point.x, point.y, brushSize / 2.0)
        }
        return bounds
    }
}
