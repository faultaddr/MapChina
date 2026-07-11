package com.mapchina.ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoverDetailHandoffTest {
    @Test
    fun discoverRecommendation_usesSeamlessNavigationHandoff() {
        assertTrue(usesSeamlessDiscoverHandoff(AttractionDetailScreen("a1", fromDiscover = true)))
    }

    @Test
    fun otherAttractionEntries_keepStandardNavigationTransition() {
        assertFalse(usesSeamlessDiscoverHandoff(AttractionDetailScreen("a1")))
        assertFalse(usesSeamlessDiscoverHandoff(MapScreen))
    }
}
