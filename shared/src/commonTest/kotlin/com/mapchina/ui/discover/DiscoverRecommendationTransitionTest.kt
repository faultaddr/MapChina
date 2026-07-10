package com.mapchina.ui.discover

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoverRecommendationTransitionTest {
    private val start = Rect(left = 40f, top = 600f, right = 1040f, bottom = 1044f)

    @Test
    fun frameAtStart_matchesCard() {
        val frame = recommendationTransitionFrame(start, 1080f, 960f, 0f)

        assertEquals(start, frame.bounds)
        assertEquals(8f, frame.cornerRadiusDp)
        assertEquals(0f, frame.backgroundProgress)
    }

    @Test
    fun frameAtEnd_matchesDetailHero() {
        val frame = recommendationTransitionFrame(start, 1080f, 960f, 1f)

        assertEquals(Rect(0f, 0f, 1080f, 960f), frame.bounds)
        assertEquals(0f, frame.cornerRadiusDp)
        assertEquals(1f, frame.backgroundProgress)
    }

    @Test
    fun frameClampsProgress() {
        assertEquals(
            recommendationTransitionFrame(start, 1080f, 960f, 0f),
            recommendationTransitionFrame(start, 1080f, 960f, -1f)
        )
        assertEquals(
            recommendationTransitionFrame(start, 1080f, 960f, 1f),
            recommendationTransitionFrame(start, 1080f, 960f, 2f)
        )
    }

    @Test
    fun frameAtHalfway_interpolatesEveryEdge() {
        val frame = recommendationTransitionFrame(start, 1080f, 960f, 0.5f)

        assertEquals(Rect(20f, 300f, 1060f, 1002f), frame.bounds)
        assertEquals(4f, frame.cornerRadiusDp)
        assertEquals(0.5f, frame.backgroundProgress)
    }
}
