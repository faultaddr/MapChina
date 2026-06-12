package com.mapchina.ui.profile

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
class ProfileScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysTabs() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("我的").assertIsDisplayed()
        onNodeWithText("成就").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysNotLoggedInState() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("未登录").assertIsDisplayed()
        onNodeWithText("登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysFeatureCards() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("我的游记").assertIsDisplayed()
        onNodeWithText("徽章墙").assertIsDisplayed()
        onNodeWithText("省份征服").assertIsDisplayed()
        onNodeWithText("主题图鉴").assertIsDisplayed()
    }
}
