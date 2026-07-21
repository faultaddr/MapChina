package com.mapchina.map

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.log2
import kotlin.time.TimeSource

internal class ImmediateTapDispatcher(
    private val doubleTapMinTimeMillis: Long,
    private val doubleTapTimeoutMillis: Long,
    private val doubleTapTouchSlop: Float,
    private val onTap: (Offset) -> Boolean,
    private val onDoubleTap: ((Offset) -> Unit)?
) {
    private data class Tap(
        val offset: Offset,
        val uptimeMillis: Long,
        val consumed: Boolean
    )

    private var previousTap: Tap? = null

    fun dispatch(offset: Offset, uptimeMillis: Long) {
        val consumed = onTap(offset)
        val previous = previousTap
        val elapsed = previous?.let { uptimeMillis - it.uptimeMillis }
        val dx = previous?.let { offset.x - it.offset.x } ?: Float.MAX_VALUE
        val dy = previous?.let { offset.y - it.offset.y } ?: Float.MAX_VALUE
        val withinSlop = dx * dx + dy * dy <= doubleTapTouchSlop * doubleTapTouchSlop
        val isUnconsumedDoubleTap = previous != null &&
            elapsed != null &&
            elapsed in doubleTapMinTimeMillis..doubleTapTimeoutMillis &&
            withinSlop &&
            !previous.consumed &&
            !consumed

        if (isUnconsumedDoubleTap) {
            previousTap = null
            onDoubleTap?.invoke(offset)
        } else {
            previousTap = Tap(offset, uptimeMillis, consumed)
        }
    }
}

fun Modifier.mapGestures(
    viewport: ViewportState,
    onTap: (Offset) -> Boolean,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null
): Modifier = this
    .pointerInput(Unit) {
        val clockStart = TimeSource.Monotonic.markNow()
        val tapDispatcher = ImmediateTapDispatcher(
            doubleTapMinTimeMillis = viewConfiguration.doubleTapMinTimeMillis,
            doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
            doubleTapTouchSlop = viewConfiguration.touchSlop * 2f,
            onTap = onTap,
            onDoubleTap = onDoubleTap
        )
        detectTapGestures(
            onTap = { offset ->
                tapDispatcher.dispatch(
                    offset = offset,
                    uptimeMillis = clockStart.elapsedNow().inWholeMilliseconds
                )
            },
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
