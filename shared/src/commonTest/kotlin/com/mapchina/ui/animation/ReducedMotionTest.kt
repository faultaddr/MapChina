package com.mapchina.ui.animation

import kotlin.test.Test
import kotlin.test.assertNotNull

class ReducedMotionTest {

    @Test
    fun localAnimationScale_isCompositionLocal() {
        // Verify LocalAnimationScale is properly defined
        assertNotNull(LocalAnimationScale)
    }
}
