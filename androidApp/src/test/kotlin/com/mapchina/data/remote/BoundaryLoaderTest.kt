package com.mapchina.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class BoundaryLoaderTest {

    private val loader = BoundaryLoader(RuntimeEnvironment.getApplication())

    @Test
    fun packagedChildRegionAvailability_distinguishesExpandableAndTerminalCities() {
        assertTrue(loader.hasChildRegions("510100"))
        assertFalse(loader.hasChildRegions("110101"))
    }
}
