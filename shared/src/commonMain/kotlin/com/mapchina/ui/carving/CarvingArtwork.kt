package com.mapchina.ui.carving

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import com.mapchina.performance.RecompositionProbe
import com.mapchina.ui.carving.v2.CarvingDocument
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.cliff_face
import org.jetbrains.compose.resources.imageResource

@Composable
fun CarvingArtwork(
    document: CarvingDocument,
    modifier: Modifier = Modifier,
    weatheredAlpha: Float = 1f,
    showBackground: Boolean = true
) {
    RecompositionProbe("CarvingArtwork")

    val cliffFace = imageResource(Res.drawable.cliff_face)
    val layerAlpha = weatheredAlpha.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier.drawWithCache {
            val geometry = buildCarvingGeometry(document, size.width, size.height)
            val paths = geometry.map(::buildCenterlinePath)
            val debris = geometry.flatMap { geometryItem ->
                deterministicDebris(
                    stroke = document.strokes[geometryItem.index],
                    strokeIndex = geometryItem.index,
                    widthPx = size.width,
                    heightPx = size.height
                )
            }

            onDrawBehind {
                if (showBackground) {
                    drawImage(
                        image = cliffFace,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
                drawCarvingLayers(geometry, paths, debris, layerAlpha)
            }
        }
    ) {}
}

private fun buildCenterlinePath(geometry: CarvingStrokeGeometry): Path = Path().apply {
    geometry.points.forEachIndexed { index, point ->
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
}

private fun DrawScope.drawCarvingLayers(
    geometry: List<CarvingStrokeGeometry>,
    paths: List<Path>,
    debris: List<CarvingDebris>,
    weatheredAlpha: Float
) {
    geometry.zip(paths).forEach { (stroke, path) ->
        if (stroke.points.isEmpty() || stroke.widthPx <= 0f) return@forEach

        val width = stroke.widthPx
        val cap = StrokeCap.Round
        val floor = Color(stroke.brush.colorArgb).copy(alpha = 0.78f * weatheredAlpha)

        withTransform({ translate(width * 0.16f, width * 0.20f) }) {
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.22f * weatheredAlpha),
                style = Stroke(width = width * 1.70f, cap = cap)
            )
        }
        withTransform({ translate(width * 0.09f, width * 0.12f) }) {
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.42f * weatheredAlpha),
                style = Stroke(width = width * 1.10f, cap = cap)
            )
        }
        drawPath(path = path, color = floor, style = Stroke(width = width, cap = cap))
        withTransform({ translate(-width * 0.07f, -width * 0.09f) }) {
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.32f * weatheredAlpha),
                style = Stroke(width = width * 0.26f, cap = cap)
            )
        }

        stroke.points.forEachIndexed { pointIndex, point ->
            if (pointIndex == 0 || pointIndex == stroke.points.lastIndex || pointIndex % 3 == 0) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.24f * weatheredAlpha),
                    radius = width * 0.23f,
                    center = point + Offset(width * 0.05f, width * 0.07f)
                )
                drawCircle(
                    color = Color(0xFF6B5844).copy(alpha = 0.50f * weatheredAlpha),
                    radius = width * 0.15f,
                    center = point
                )
            }
        }
    }

    debris.forEach { particle ->
        drawCircle(
            color = Color.Black.copy(alpha = 0.32f * weatheredAlpha),
            radius = particle.radiusPx,
            center = particle.center + Offset(particle.radiusPx * 0.20f, particle.radiusPx * 0.25f)
        )
        drawCircle(
            color = Color(0xFF8C765B).copy(alpha = 0.72f * weatheredAlpha),
            radius = particle.radiusPx * 0.76f,
            center = particle.center
        )
    }
}
