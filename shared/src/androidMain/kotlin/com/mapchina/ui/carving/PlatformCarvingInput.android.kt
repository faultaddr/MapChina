@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.mapchina.ui.carving

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.strokes.Stroke
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingStroke

@Composable
actual fun PlatformCarvingInputSurface(
    brush: CarvingBrushSpec,
    enabled: Boolean,
    modifier: Modifier,
    onStrokeCommitted: (CarvingStroke) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val inkBrush = remember(brush, canvasSize) {
        Brush.Builder()
            .setFamily(BrushFamily())
            .setSize(inkBrushSize(brush, canvasSize))
            .setColorIntArgb(brush.colorArgb)
            .setEpsilon(0.1f)
            .build()
    }

    if (enabled) {
        key(brush) {
            Box(modifier = modifier.onSizeChanged { canvasSize = it }) {
                InProgressStrokes(
                    defaultBrush = inkBrush,
                    onStrokesFinished = { strokes ->
                        strokes.mapNotNull { inkStrokeToCarvingStroke(it, brush, canvasSize) }
                            .forEach { stroke ->
                                onStrokeCommitted(stroke)
                                haptic.perform(HapticType.HEAVY)
                            }
                    }
                )
            }
        }
    }
}

fun normalizePlatformPoints(points: List<PlatformPoint>, canvasSize: IntSize) =
    normalizePlatformPoints(points, canvasSize.width.toFloat(), canvasSize.height.toFloat())

fun inkBrushSize(brush: CarvingBrushSpec, canvasSize: IntSize): Float =
    (brush.sizeFraction * minOf(canvasSize.width, canvasSize.height)).coerceAtLeast(INK_MINIMUM_SIZE)

private const val INK_MINIMUM_SIZE = 0.1f

fun inkStrokeToCarvingStroke(
    stroke: Stroke,
    brushSpec: CarvingBrushSpec,
    canvasSize: IntSize
): CarvingStroke? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null

    val platformPoints = buildList {
        val inputs = stroke.inputs
        for (index in 0 until inputs.size) {
            val input = inputs.get(index)
            add(
                PlatformPoint(
                    x = input.x,
                    y = input.y,
                    pressure = input.pressure.takeIf { it > 0f } ?: 0.5f,
                    elapsedTimeMillis = input.elapsedTimeMillis
                )
            )
        }
    }
    val points = normalizePlatformPoints(coalescePlatformPoints(platformPoints), canvasSize)
    return points.takeIf { it.size >= 2 }?.let {
        CarvingStroke(
            brushType = brushSpec.type,
            sizeFraction = brushSpec.sizeFraction,
            colorArgb = brushSpec.colorArgb,
            points = it
        )
    }
}
