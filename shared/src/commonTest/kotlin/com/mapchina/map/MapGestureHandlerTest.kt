package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class MapGestureHandlerTest {
    @Test
    fun regionTap_dispatchesImmediatelyWithoutWaitingForDoubleTapWindow() {
        val events = mutableListOf<String>()
        val dispatcher = ImmediateTapDispatcher(
            doubleTapMinTimeMillis = 40L,
            doubleTapTimeoutMillis = 300L,
            doubleTapTouchSlop = 24f,
            onTap = { offset ->
                events += "tap:${offset.x}"
                true
            },
            onDoubleTap = { events += "double" }
        )

        dispatcher.dispatch(Offset(10f, 10f), uptimeMillis = 100L)

        assertEquals(listOf("tap:10.0"), events)
    }

    @Test
    fun rapidRegionTaps_eachDispatchImmediatelyAndNeverBecomeDoubleTapZoom() {
        val events = mutableListOf<String>()
        val dispatcher = ImmediateTapDispatcher(
            doubleTapMinTimeMillis = 40L,
            doubleTapTimeoutMillis = 300L,
            doubleTapTouchSlop = 24f,
            onTap = { offset ->
                events += "tap:${offset.x}"
                true
            },
            onDoubleTap = { events += "double" }
        )

        dispatcher.dispatch(Offset(10f, 10f), uptimeMillis = 100L)
        dispatcher.dispatch(Offset(10f, 10f), uptimeMillis = 220L)

        assertEquals(listOf("tap:10.0", "tap:10.0"), events)
    }

    @Test
    fun rapidBackgroundTaps_preserveDoubleTapZoomAfterBothImmediateHitTests() {
        val events = mutableListOf<String>()
        val dispatcher = ImmediateTapDispatcher(
            doubleTapMinTimeMillis = 40L,
            doubleTapTimeoutMillis = 300L,
            doubleTapTouchSlop = 24f,
            onTap = { offset ->
                events += "tap:${offset.x}"
                false
            },
            onDoubleTap = { events += "double" }
        )

        dispatcher.dispatch(Offset(10f, 10f), uptimeMillis = 100L)
        dispatcher.dispatch(Offset(12f, 11f), uptimeMillis = 220L)

        assertEquals(listOf("tap:10.0", "tap:12.0", "double"), events)
    }
}
