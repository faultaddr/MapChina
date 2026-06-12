package com.mapchina.ui.common

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
class ErrorViewTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun errorView_displaysMessage() = runComposeUiTest {
        setContent { ErrorView(message = "加载失败") }
        onNodeWithText("加载失败").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun errorView_displaysRetryButton() = runComposeUiTest {
        setContent { ErrorView(message = "加载失败", onRetry = {}) }
        onNodeWithText("重试").assertIsDisplayed()
    }
}
