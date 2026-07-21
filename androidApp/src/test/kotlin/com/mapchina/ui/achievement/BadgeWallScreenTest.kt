package com.mapchina.ui.achievement

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertTrue
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class BadgeWallScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun badgeWallScreen_displaysTitle() = runComposeUiTest {
        setContent { BadgeWallScreen() }
        onNodeWithText("徽章墙").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun badgeWallScreen_backActionIsReachable() = runComposeUiTest {
        var wentBack = false
        setContent { BadgeWallScreen(onBack = { wentBack = true }) }

        onNodeWithContentDescription("返回").performClick()

        assertTrue(wentBack)
    }
}
