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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.imageResource
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun ChinaMapView(
    controller: MapController,
    modifier: Modifier = Modifier
) {
    val renderState by controller.renderState.collectAsState()
    val pathCache = remember { GeoPathCache() }
    val textMeasurer = rememberTextMeasurer()

    val backgroundBitmap: ImageBitmap? = renderState.backgroundTheme.backgroundRes?.let {
        imageResource(it)
    }

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
        // Read zoom from ViewportState (Compose state) to drive Compose-level visibility
        val zoom by remember {
            derivedStateOf { controller.viewport.zoomLevel }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val projection = controller.viewport.toProjection(size.width, size.height)

            // L0: Background (ocean)
            drawRect(renderState.oceanColor)

            // L0.5: Theme background texture
            if (backgroundBitmap != null) {
                drawImage(
                    image = backgroundBitmap,
                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                    alpha = 0.35f
                )
            }

            // L0.7: Neighbor country outlines (浅灰描边，提供地理参照)
            val neighborStrokeWidth = if (zoom < 6f) 0.8.dp.toPx() else 0.5.dp.toPx()
            for (outline in renderState.neighborOutlines) {
                val path = Path()
                for ((i, point) in outline.withIndex()) {
                    val offset = projection.project(point.first, point.second)
                    if (i == 0) path.moveTo(offset.x, offset.y)
                    else path.lineTo(offset.x, offset.y)
                }
                drawPath(
                    path,
                    color = MapChinaColors.BorderMedium.copy(alpha = 0.5f),
                    style = Stroke(width = neighborStrokeWidth)
                )
            }

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

                // When a theme background is active, draw an opaque ocean-color base
                // under each overlay so the texture doesn't bleed through
                if (backgroundBitmap != null) {
                    for (path in overlayPaths) {
                        drawPath(path, color = renderState.oceanColor)
                    }
                }

                for (path in overlayPaths) {
                    drawPath(path, color = fillColor)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
                }
            }

            // L3.5: China boundary glow — 碧玉色光晕环绕中国，让留白变设计感
            // 对每个已访问省份画一层扩展的半透明光晕
            if (renderState.overlays.isNotEmpty()) {
                val glowColor = MapChinaColors.Primary.copy(alpha = 0.06f)
                for ((regionId, overlayPaths) in pathCache.paths) {
                    val data = renderState.overlays[regionId] ?: continue
                    if (!data.isVisited) continue
                    for (path in overlayPaths) {
                        drawPath(
                            path,
                            color = glowColor,
                            style = Stroke(width = 12.dp.toPx())
                        )
                    }
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

            // South China Sea nine-dash line (directly on map)
            drawSouthChinaSeaOnMap(
                projection = projection,
                zoomLevel = zoom,
                strokeColor = MapChinaColors.TextTertiary,
                islandColor = MapChinaColors.AccentBlue
            )

            // L7: Region labels with collision avoidance
            if (zoom >= 3.5f) {
                val fontSizePx = when {
                    zoom >= 10f -> 12.dp.toPx()
                    zoom >= 7f -> 10.dp.toPx()
                    else -> 9.dp.toPx()
                }
                val style = TextStyle(
                    color = MapChinaColors.TextPrimary.copy(alpha = if (zoom < 5f) 0.7f else 0.85f),
                    fontSize = with(density) { fontSizePx.toSp() },
                    textAlign = TextAlign.Center
                )
                val visibleLabels = renderState.labels.values.filter { zoom >= it.minZoom }
                val measuredLabels = visibleLabels.mapNotNull { label ->
                    val pos = projection.project(label.lng, label.lat)
                    val measured = textMeasurer.measure(label.name, style)
                    val rect = androidx.compose.ui.geometry.Rect(
                        left = pos.x - measured.size.width / 2f,
                        top = pos.y - measured.size.height / 2f,
                        right = pos.x + measured.size.width / 2f,
                        bottom = pos.y + measured.size.height / 2f
                    )
                    Triple(label, measured, rect)
                }
                val occupied = mutableListOf<androidx.compose.ui.geometry.Rect>()
                for ((_, measured, rect) in measuredLabels) {
                    val overlaps = occupied.any { existing ->
                        rect.overlaps(existing)
                    }
                    if (!overlaps) {
                        drawText(
                            textLayoutResult = measured,
                            topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top)
                        )
                        occupied.add(rect)
                    }
                }
            }
        }

        // Attraction markers with images (Compose overlay, city level+)
        if (zoom >= 7f) {
            AttractionMarkerOverlay(controller, renderState.attractionMarkers, zoom)
        }
    }

    DisposableEffect(Unit) {
        controller.notifyMapReady()
        onDispose {
            controller.detachFromComposition()
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

    // Derive screen positions from camera state — recomputes only when camera changes
    val markerPositions by remember(markers) {
        derivedStateOf {
            val vp = controller.viewport
            val proj = vp.toProjection(vp.canvasWidth, vp.canvasHeight)
            markers.mapValues { (_, marker) ->
                proj.project(marker.lng, marker.lat)
            }
        }
    }

    for (marker in markers.values) {
        val pos = markerPositions[marker.id] ?: continue
        val offsetX = with(density) { (pos.x - halfMarkerPx).toDp() }
        val offsetY = with(density) { (pos.y - halfMarkerPx).toDp() }

        key(marker.id) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(markerSizeDp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (marker.imageUrl != null) {
                    AsyncImage(
                        model = marker.imageUrl,
                        contentDescription = marker.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
