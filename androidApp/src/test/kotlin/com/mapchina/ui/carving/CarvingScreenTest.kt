package com.mapchina.ui.carving

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class CarvingScreenTest {

    @Test
    fun sharedEditorPolicy_isAvailableToAndroid() {
        assertEquals("西湖", carvingPlaceTitle("浙江省", "西湖"))
        assertEquals("cn_landscape", carvingContextTarget("我的碑刻", null, null).regionId)
    }
}
