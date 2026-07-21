package com.mapchina.ui.carving

import androidx.compose.ui.geometry.Offset
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingPoint
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlin.math.sqrt

data class CarvingStrokeGeometry(
    val index: Int,
    val brush: CarvingBrushSpec,
    val widthPx: Float,
    val points: List<Offset>
)

data class CarvingDebris(
    val center: Offset,
    val radiusPx: Float,
    val rotationDegrees: Float
)

data class PlatformPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val elapsedTimeMillis: Long
)

fun buildCarvingGeometry(
    document: CarvingDocument,
    widthPx: Float,
    heightPx: Float
): List<CarvingStrokeGeometry> {
    if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f) {
        return emptyList()
    }

    val shortEdge = minOf(widthPx, heightPx)
    return document.strokes.mapIndexed { index, stroke ->
        CarvingStrokeGeometry(
            index = index,
            brush = stroke.brushSpec,
            widthPx = stroke.sizeFraction * shortEdge,
            points = stroke.points.map { point -> Offset(point.x * widthPx, point.y * heightPx) }
        )
    }
}

fun deterministicDebris(
    stroke: CarvingStroke,
    strokeIndex: Int,
    widthPx: Float,
    heightPx: Float
): List<CarvingDebris> {
    if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f) {
        return emptyList()
    }

    val shortEdge = minOf(widthPx, heightPx)
    val strokeWidth = stroke.sizeFraction * shortEdge
    if (strokeWidth <= 0f || stroke.points.isEmpty()) return emptyList()

    val debrisCount = (stroke.points.size - 1).coerceAtLeast(1).coerceAtMost(6)
    val random = StableRandom(stableStrokeSeed(stroke, strokeIndex))
    return List(debrisCount) { debrisIndex ->
        val t = (debrisIndex + 1f) / (debrisCount + 1f)
        val pointIndex = (t * (stroke.points.lastIndex)).toInt().coerceIn(0, stroke.points.lastIndex)
        val point = stroke.points[pointIndex]
        val perpendicularX = random.nextSignedFloat() * strokeWidth * 0.72f
        val perpendicularY = random.nextSignedFloat() * strokeWidth * 0.72f
        CarvingDebris(
            center = Offset(
                x = point.x * widthPx + perpendicularX,
                y = point.y * heightPx + perpendicularY
            ),
            radiusPx = strokeWidth * (0.05f + random.nextFloat() * 0.08f),
            rotationDegrees = random.nextFloat() * 360f
        )
    }
}

fun normalizePlatformPoints(
    points: List<PlatformPoint>,
    canvasWidthPx: Float,
    canvasHeightPx: Float
): List<CarvingPoint> {
    if (
        points.isEmpty() ||
        !canvasWidthPx.isFinite() ||
        !canvasHeightPx.isFinite() ||
        canvasWidthPx <= 0f ||
        canvasHeightPx <= 0f
    ) {
        return emptyList()
    }

    return points.map { point ->
        CarvingPoint(
            x = (point.x / canvasWidthPx).coerceIn(0f, 1f),
            y = (point.y / canvasHeightPx).coerceIn(0f, 1f),
            pressure = point.pressure.coerceIn(0f, 1f),
            elapsedTimeMillis = point.elapsedTimeMillis
        )
    }
}

fun coalescePlatformPoints(points: List<PlatformPoint>): List<PlatformPoint> {
    if (points.size < 2) return points

    val coalesced = ArrayList<PlatformPoint>(points.size)
    for (point in points) {
        val previous = coalesced.lastOrNull()
        if (previous != null && previous.elapsedTimeMillis == point.elapsedTimeMillis) {
            val distance = physicalDistance(previous, point)
            if (distance < COALESCE_DISTANCE_PX) continue
        }
        coalesced += point
    }
    return coalesced
}

private const val COALESCE_DISTANCE_PX = 0.5f

private fun physicalDistance(first: PlatformPoint, second: PlatformPoint): Float {
    val dx = second.x - first.x
    val dy = second.y - first.y
    return sqrt(dx * dx + dy * dy)
}

private fun stableStrokeSeed(stroke: CarvingStroke, index: Int): Long {
    var seed = 0x4D41504348494E41L xor index.toLong()
    seed = mixSeed(seed, stroke.brushType.ordinal.toLong())
    seed = mixSeed(seed, stroke.sizeFraction.toBits().toLong())
    seed = mixSeed(seed, stroke.colorArgb.toLong())
    for (point in stroke.points) {
        seed = mixSeed(seed, point.x.toBits().toLong())
        seed = mixSeed(seed, point.y.toBits().toLong())
        seed = mixSeed(seed, point.elapsedTimeMillis)
    }
    return if (seed == 0L) 1L else seed
}

private fun mixSeed(seed: Long, value: Long): Long =
    (seed xor value) * 0x100000001B3L

private class StableRandom(seed: Long) {
    private var state = seed

    fun nextFloat(): Float {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return ((state ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
    }

    fun nextSignedFloat(): Float = nextFloat() * 2f - 1f
}
