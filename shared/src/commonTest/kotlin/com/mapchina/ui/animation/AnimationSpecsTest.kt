package com.mapchina.ui.animation

import androidx.compose.animation.core.Spring
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationSpecsTest {

    @Test
    fun springGentle_hasCorrectDamping() {
        val spec = AnimationSpecs.springGentle
        assertEquals(0.7f, spec.dampingRatio, 0.01f)
    }

    @Test
    fun durations_arePositive() {
        assertTrue(AnimationSpecs.Duration.buttonPress > 0)
        assertTrue(AnimationSpecs.Duration.pageTransition > 0)
        assertTrue(AnimationSpecs.Duration.unlockContent > 0)
    }

    @Test
    fun staggers_arePositive() {
        assertTrue(AnimationSpecs.Stagger.listItem > 0)
        assertTrue(AnimationSpecs.Stagger.gridItem > 0)
    }

    @Test
    fun scales_areValid() {
        assertTrue(AnimationSpecs.Scale.buttonPress < 1f)
        assertTrue(AnimationSpecs.Scale.drillDownZoom > 1f)
    }

    @Test
    fun tweenSlowEase_hasCorrectDuration() {
        val spec = AnimationSpecs.tweenSlowEase
        assertEquals(400, spec.durationMillis)
    }
}
