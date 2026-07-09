package com.mapchina.ui.profile

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ProfileScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysUserCard() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("未登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysNotLoggedInState() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("未登录").assertIsDisplayed()
        onNodeWithText("登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_keepsSettingsAndRemovesGrowthEntries() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("账号与设置").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("足迹记录设置"))
        onNodeWithText("足迹记录设置").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("地图显示"))
        onNodeWithText("地图显示").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("数据与同步"))
        onNodeWithText("数据与同步").assertIsDisplayed()
        onAllNodesWithText("勋章").assertCountEquals(0)
        onAllNodesWithText("图鉴").assertCountEquals(0)
    }
}
