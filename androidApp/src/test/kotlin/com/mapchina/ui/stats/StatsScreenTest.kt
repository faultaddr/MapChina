package com.mapchina.ui.stats

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class StatsScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun statsScreen_displaysTitle() = runComposeUiTest {
        setContent { StatsScreen() }
        onNodeWithText("统计").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun statsScreen_displaysCoverageSections() = runComposeUiTest {
        setContent { StatsScreen() }
        onNodeWithText("省份").assertIsDisplayed()
        onNodeWithText("城市").assertIsDisplayed()
        onNodeWithText("区县").assertIsDisplayed()
    }
}
