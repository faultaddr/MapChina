package com.mapchina.ui.carving

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingStroke

@Composable
actual fun PlatformCarvingInputSurface(
    brush: CarvingBrushSpec,
    enabled: Boolean,
    modifier: Modifier,
    onStrokeCommitted: (CarvingStroke) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val activePoints = remember { mutableStateListOf<PlatformPoint>() }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(brush, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startedAt = down.uptimeMillis
                    var completed = false
                    activePoints.clear()
                    activePoints += PlatformPoint(
                        x = down.position.x,
                        y = down.position.y,
                        pressure = down.pressure.takeIf { it > 0f } ?: 0.5f,
                        elapsedTimeMillis = 0L
                    )
                    try {
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            pressed = change.pressed
                            if (pressed) {
                                activePoints += PlatformPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    pressure = change.pressure.takeIf { it > 0f } ?: 0.5f,
                                    elapsedTimeMillis = change.uptimeMillis - startedAt
                                )
                            } else {
                                completed = true
                            }
                        }
                        if (completed) {
                            val points = normalizePlatformPoints(
                                coalescePlatformPoints(activePoints),
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat()
                            )
                            if (points.size >= 2) {
                                onStrokeCommitted(
                                    CarvingStroke(
                                        brushType = brush.type,
                                        sizeFraction = brush.sizeFraction,
                                        colorArgb = brush.colorArgb,
                                        points = points
                                    )
                                )
                            }
                        }
                    } finally {
                        activePoints.clear()
                    }
                }
            }
    ) {
        ActiveStrokeCanvas(points = activePoints, brush = brush)
    }
}

@Composable
private fun ActiveStrokeCanvas(points: List<PlatformPoint>, brush: CarvingBrushSpec) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = (brush.sizeFraction * size.minDimension).coerceAtLeast(1f)
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color(brush.colorArgb).copy(alpha = 0.82f),
                start = Offset(start.x, start.y),
                end = Offset(end.x, end.y),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}
