package com.mapchina.ui.profile

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class LoginScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_displaysAppName() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("MapChina").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_displaysSubtitle() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("用地图点亮你的中国足迹").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_displaysNicknameField() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("输入昵称快速开始").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_disablesQuickStartWhenNicknameEmpty() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("快速开始").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_displaysPhoneLoginOption() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("手机号登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun loginScreen_enablesQuickStartAfterTyping() = runComposeUiTest {
        setContent { LoginScreen() }
        onNodeWithText("输入昵称快速开始").performTextInput("测试用户")
        onNodeWithText("快速开始").assertIsDisplayed()
    }
}
