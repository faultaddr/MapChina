package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.domain.model.FootprintLevel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class FirstFootprintSuccessBarTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun successBar_showsRegionAndLevel() = runComposeUiTest {
        setContent {
            FirstFootprintSuccessBar(
                celebration = FirstFootprintCelebration(
                    regionId = "330000",
                    regionName = "浙江省",
                    level = FootprintLevel.SHORT_VISIT
                )
            )
        }

        onNodeWithText("第一处已点亮").assertIsDisplayed()
        onNodeWithText("浙江省 · 小驻").assertIsDisplayed()
    }
}
