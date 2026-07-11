package com.mapchina.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationProviderTest {
    @Test
    fun lastKnownLocationMustBeRecentAndMonotonic() {
        val now = 300_000_000_000L

        assertTrue(isFreshLastKnownLocation(now - 60_000_000_000L, now))
        assertFalse(isFreshLastKnownLocation(now - 121_000_000_000L, now))
        assertFalse(isFreshLastKnownLocation(now + 1L, now))
        assertFalse(isFreshLastKnownLocation(0L, now))
    }
}
