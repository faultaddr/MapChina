package com.mapchina.ui.shanhe

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ShanheScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shanheScreenShowsGrowthEntrypoints() = runComposeUiTest {
        setContent {
            ShanheScreen(
                viewModel = ShanheViewModel(),
                onNavigate = {}
            )
        }

        onNodeWithText("山河").assertIsDisplayed()
        onNodeWithText("山河初识").assertIsDisplayed()
        onNodeWithText("0 山河值").assertIsDisplayed()
        onNodeWithText("今日目标").assertIsDisplayed()
        onNodeWithText("确认 1 条可能足迹，点亮下一块版图").assertIsDisplayed()
        onNodeWithText("成长入口").assertIsDisplayed()
        onNodeWithText("勋章").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("图鉴"))
        onNodeWithText("图鉴").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("征版"))
        onNodeWithText("征版").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("碑刻"))
        onNodeWithText("碑刻").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("统计"))
        onNodeWithText("统计").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("目标"))
        onNodeWithText("目标").assertIsDisplayed()
    }
}
