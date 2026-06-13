package com.mapchina.ui.common

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class EmptyStateTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun emptyState_displaysTitle() = runComposeUiTest {
        setContent { EmptyState(icon = Icons.Default.Explore, title = "暂无数据", subtitle = "开始探索吧") }
        onNodeWithText("暂无数据").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun emptyState_displaysSubtitle() = runComposeUiTest {
        setContent { EmptyState(icon = Icons.Default.Explore, title = "暂无数据", subtitle = "开始探索吧") }
        onNodeWithText("开始探索吧").assertIsDisplayed()
    }
}
