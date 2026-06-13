package com.mapchina.ui.achievement

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
class BadgeDetailScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun badgeDetailScreen_displaysTitle() = runComposeUiTest {
        setContent { BadgeDetailScreen(item = null) }
        onNodeWithText("徽章详情").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun badgeDetailScreen_displaysNotFoundWhenNull() = runComposeUiTest {
        setContent { BadgeDetailScreen(item = null) }
        onNodeWithText("未找到成就").assertIsDisplayed()
    }
}
