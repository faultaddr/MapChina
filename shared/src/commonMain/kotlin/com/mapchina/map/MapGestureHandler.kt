package com.mapchina.map

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.log2

fun Modifier.mapGestures(
    viewport: ViewportState,
    onTap: (Offset) -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = onDoubleTap,
            onTap = onTap,
            onLongPress = onLongPress
        )
    }
    .pointerInput(Unit) {
        detectTransformGestures(
            panZoomLock = false
        ) { centroid, pan, zoom, _ ->
            if (zoom != 1f) {
                val delta = log2(zoom.toDouble()).toFloat()
                viewport.zoomBy(delta, centroid)
            }
            if (pan != Offset.Zero) viewport.panBy(pan)
        }
    }
