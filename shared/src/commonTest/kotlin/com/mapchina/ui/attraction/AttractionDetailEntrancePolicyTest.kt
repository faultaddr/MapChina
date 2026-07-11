package com.mapchina.ui.attraction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttractionDetailEntrancePolicyTest {
    @Test
    fun discoverHandoff_startsWithStableHeroAndContent() {
        val policy = attractionDetailEntrancePolicy(animateHeroEntrance = false)

        assertEquals(1f, policy.heroAlpha)
        assertEquals(1f, policy.contentAlpha)
        assertEquals(1f, policy.backAlpha)
        assertFalse(policy.shouldAnimate)
    }

    @Test
    fun standardEntry_keepsDetailEntranceAnimation() {
        val policy = attractionDetailEntrancePolicy(animateHeroEntrance = true)

        assertEquals(0f, policy.heroAlpha)
        assertEquals(0f, policy.contentAlpha)
        assertEquals(0f, policy.backAlpha)
        assertTrue(policy.shouldAnimate)
    }
}
