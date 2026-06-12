package com.mapchina.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun ChinaMapView(
    controller: MapController,
    modifier: Modifier = Modifier
) {
    val renderState by controller.renderState.collectAsState()
    val pathCache = remember { GeoPathCache() }

    Box(modifier = modifier
        .onSizeChanged { size ->
            controller.viewport.canvasWidth = size.width.toFloat()
            controller.viewport.canvasHeight = size.height.toFloat()
        }
        .mapGestures(
            viewport = controller.viewport,
            onTap = { offset -> controller.handleTap(offset) },
            onDoubleTap = { offset -> controller.handleDoubleTap(offset) },
            onLongPress = { offset -> controller.handleLongPress(offset) }
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val projection = controller.viewport.toProjection(size.width, size.height)

            // L0: Background (ocean)
            drawRect(renderState.oceanColor)

            // L1/L2: Region overlays
            pathCache.buildIfChanged(renderState.overlays, projection, controller.viewport.zoomLevel)

            // Update hit test bounds from cache
            controller.updateHitTestBounds(pathCache.bounds)

            for ((regionId, overlayPaths) in pathCache.paths) {
                val data = renderState.overlays[regionId] ?: continue
                val fillColor = data.style.toFillColor()
                val strokeColor = data.style.toStrokeColor()
                val strokeWidth = if (controller.viewport.zoomLevel < 6f) 1.5.dp.toPx() else 1.dp.toPx()

                for (path in overlayPaths) {
                    drawPath(path, color = fillColor)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
                }
            }

            // L4: Markers
            for (marker in renderState.markers.values) {
                val pos = projection.project(marker.lng, marker.lat)
                val color = if (marker.visited) MapChinaColors.Primary else MapChinaColors.AccentBlue
                drawCircle(color, radius = 6.dp.toPx(), center = pos)
            }
            for (marker in renderState.attractionMarkers.values) {
                val pos = projection.project(marker.lng, marker.lat)
                val color = if (marker.visited) MapChinaColors.Primary else MapChinaColors.AccentBlue
                drawCircle(color, radius = 8.dp.toPx(), center = pos)
            }

            // L5: Polylines
            for (polyline in renderState.polylines.values) {
                val path = Path()
                for ((i, point) in polyline.points.withIndex()) {
                    val offset = projection.project(point.first, point.second)
                    if (i == 0) path.moveTo(offset.x, offset.y)
                    else path.lineTo(offset.x, offset.y)
                }
                drawPath(
                    path,
                    color = Color(polyline.color),
                    style = Stroke(width = polyline.width, cap = StrokeCap.Round)
                )
            }

            // L6: Pulse overlay
            val pulseTarget = renderState.pulseTarget
            if (pulseTarget != null) {
                val alpha = controller.pulseAlpha
                val pulsePaths = pathCache.paths[pulseTarget]
                if (pulsePaths != null) {
                    for (path in pulsePaths) {
                        drawPath(path, color = MapChinaColors.AccentGold.copy(alpha = alpha * 0.5f))
                    }
                }
            }

            // South China Sea inset
            drawSouthChinaSeaInset(
                zoomLevel = controller.viewport.zoomLevel,
                strokeColor = MapChinaColors.TextTertiary,
                islandColor = MapChinaColors.AccentBlue
            )
        }
    }

    DisposableEffect(Unit) {
        controller.notifyMapReady()
        onDispose {
            controller.dispose()
        }
    }
}
