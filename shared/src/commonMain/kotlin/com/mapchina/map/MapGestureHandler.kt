package com.mapchina.map

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.mapGestures(
    viewport: ViewportState,
    onTap: (Offset) -> Unit,
    onLongPress: ((Offset) -> Unit)? = null
): Modifier = this
    .pointerInput(viewport) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            if (pan != Offset.Zero) viewport.panBy(pan)
            if (zoom != 1f) viewport.zoomBy(zoom - 1f, centroid)
        }
    }
    .pointerInput(viewport) {
        detectTapGestures(
            onDoubleTap = { offset -> viewport.zoomBy(1f, offset) },
            onTap = onTap,
            onLongPress = onLongPress
        )
    }
