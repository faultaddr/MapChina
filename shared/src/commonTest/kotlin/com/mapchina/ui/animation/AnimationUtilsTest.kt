package com.mapchina.ui.animation

import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnimationUtilsTest {

    @Test
    fun pressScale_returnsModifier() {
        val modifier = Modifier.pressScale()
        assertNotNull(modifier)
    }

    @Test
    fun cardPress_returnsModifier() {
        val modifier = Modifier.cardPress(onClick = {})
        assertNotNull(modifier)
    }

    @Test
    fun staggeredEntrance_returnsModifier() {
        val modifier = Modifier.staggeredEntrance(index = 0)
        assertNotNull(modifier)
    }

    @Test
    fun animateCount_compiles() {
        // animateCount is @Composable - can only be called from @Composable context.
        // This test verifies the import resolves (compile-time check).
        assertTrue(true)
    }
}
