package com.mapchina.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

data class RegionFootprintUi(
    val regionId: String,
    val name: String,
    val footprintLevel: FootprintLevel?,
    val normalizedPath: List<Offset>,
    val bounds: RegionBounds
)

data class RegionBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

@Composable
fun ColorBlockView(
    regions: List<RegionFootprintUi>,
    onRegionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(regions) {
                detectTapGestures { offset ->
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    val hit = findRegionAt(offset, regions, canvasSize)
                    hit?.let { onRegionTap(it.regionId) }
                }
            }
    ) {
        regions.forEach { item ->
            val color = when (item.footprintLevel) {
                FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
                FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
                FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
                null -> MapChinaColors.FootprintUnvisited
            }
            val path = buildPath(item.normalizedPath, size)
            drawPath(path, color, alpha = 0.85f)
            drawPath(path, Color.White, style = Stroke(width = 1f), alpha = 0.3f)
        }
    }
}

private fun buildPath(normalizedPoints: List<Offset>, size: Size): Path {
    val path = Path()
    if (normalizedPoints.isEmpty()) return path
    val first = normalizedPoints.first()
    path.moveTo(first.x * size.width, first.y * size.height)
    for (i in 1 until normalizedPoints.size) {
        val pt = normalizedPoints[i]
        path.lineTo(pt.x * size.width, pt.y * size.height)
    }
    path.close()
    return path
}

fun findRegionAt(
    offset: Offset,
    regions: List<RegionFootprintUi>,
    size: Size
): RegionFootprintUi? {
    val normalizedX = offset.x / size.width
    val normalizedY = offset.y / size.height
    return regions.lastOrNull { region ->
        pointInPolygon(normalizedX, normalizedY, region.normalizedPath)
    }
}

fun pointInPolygon(x: Float, y: Float, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y
        val intersect = ((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
        if (intersect) inside = !inside
        j = i
    }
    return inside
}
