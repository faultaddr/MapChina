package com.mapchina.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            controller.viewport.canvasWidth = w
            controller.viewport.canvasHeight = h
            if (!controller.initialFitDone && w > 0f && h > 0f) {
                controller.initialFitDone = true
                controller.viewport.fitChinaInView()
            }
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
            val zoom = controller.viewport.zoomLevel

            // L0: Background (ocean)
            drawRect(renderState.oceanColor)

            // L1/L2: Region overlays
            pathCache.buildIfChanged(renderState.overlays, projection, zoom)

            // Update hit test bounds from cache (only when changed)
            if (pathCache.boundsChanged) {
                controller.updateHitTestBounds(pathCache.bounds)
            }

            for ((regionId, overlayPaths) in pathCache.paths) {
                val data = renderState.overlays[regionId] ?: continue
                val fillColor = data.style.toFillColor()
                val strokeColor = data.style.toStrokeColor()
                val strokeWidth = if (zoom < 6f) 1.5.dp.toPx() else 1.dp.toPx()

                for (path in overlayPaths) {
                    drawPath(path, color = fillColor)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
                }
            }

            // L4: Simple foot markers (non-attraction)
            if (zoom >= 7f) {
                val markerRadius = if (zoom >= 10f) 6.dp.toPx() else 4.dp.toPx()
                for (marker in renderState.markers.values) {
                    val pos = projection.project(marker.lng, marker.lat)
                    val color = if (marker.visited) MapChinaColors.Primary else MapChinaColors.AccentBlue
                    drawCircle(color, radius = markerRadius, center = pos)
                }
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
                zoomLevel = zoom,
                strokeColor = MapChinaColors.TextTertiary,
                islandColor = MapChinaColors.AccentBlue
            )
        }

        // Attraction markers with images (Compose overlay, city level+)
        val zoom = controller.viewport.zoomLevel
        if (zoom >= 7f) {
            AttractionMarkerOverlay(controller, renderState.attractionMarkers, zoom)
        }
    }

    DisposableEffect(Unit) {
        controller.notifyMapReady()
        onDispose {
            controller.dispose()
        }
    }
}

@Composable
private fun AttractionMarkerOverlay(
    controller: MapController,
    markers: Map<String, AttractionMarkerData>,
    zoom: Float
) {
    val density = LocalDensity.current
    val markerSizeDp = if (zoom >= 10f) 36.dp else 28.dp
    val markerSizePx = with(density) { markerSizeDp.toPx() }
    val halfMarkerPx = markerSizePx / 2f

    // Derived projection so all markers share one calculation per recomposition
    val projection by remember {
        derivedStateOf {
            controller.viewport.toProjection(
                controller.viewport.canvasWidth,
                controller.viewport.canvasHeight
            )
        }
    }

    for (marker in markers.values) {
        val pos = projection.project(marker.lng, marker.lat)
        val offsetX = with(density) { (pos.x - halfMarkerPx).toDp() }
        val offsetY = with(density) { (pos.y - halfMarkerPx).toDp() }

        key(marker.id) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(markerSizeDp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (marker.imageUrl != null) {
                    AsyncImage(
                        model = marker.imageUrl,
                        contentDescription = marker.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val tint = if (marker.visited) MapChinaColors.Primary else MapChinaColors.AccentBlue
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(tint, radius = size.minDimension / 2)
                    }
                }
            }
        }
    }
}
