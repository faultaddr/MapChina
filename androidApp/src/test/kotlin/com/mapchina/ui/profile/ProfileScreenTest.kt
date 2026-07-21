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
    @Test fun profileScreen_isAccountAndDataControlCenter() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("我的").assertIsDisplayed()
        onNodeWithText("账号与同步").assertIsDisplayed()
        onNodeWithText("本地保存").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("足迹记录"))
        onNodeWithText("足迹记录").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("仅在本机读取照片中的位置信息"))
        onNodeWithText("仅在本机读取照片中的位置信息").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("只生成建议，由你确认后点亮"))
        onNodeWithText("只生成建议，由你确认后点亮").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("地图外观"))
        onNodeWithText("地图外观").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("关于"))
        onNodeWithText("关于").assertIsDisplayed()
        onAllNodesWithText("省份").assertCountEquals(0)
        onAllNodesWithText("城市").assertCountEquals(0)
        onAllNodesWithText("区县").assertCountEquals(0)
    }
}
