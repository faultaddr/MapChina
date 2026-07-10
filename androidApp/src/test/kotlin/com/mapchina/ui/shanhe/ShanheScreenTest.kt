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
    fun shanheScreenShowsGrowthDashboard() = runComposeUiTest {
        setContent {
            ShanheScreen(
                viewModel = ShanheViewModel(),
                onNavigate = {}
            )
        }

        onNodeWithText("山河").assertIsDisplayed()
        onNodeWithText("成长进度").assertIsDisplayed()
        onNodeWithText("Lv.1").assertIsDisplayed()
        onNodeWithText("下一步").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("成长图谱"))
        onNodeWithText("成长图谱").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("勋章"))
        onNodeWithText("勋章").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("图鉴"))
        onNodeWithText("图鉴").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("征版"))
        onNodeWithText("征版").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("碑刻"))
        onNodeWithText("碑刻").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("山河账本"))
        onNodeWithText("山河账本").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("最近解锁"))
        onNodeWithText("最近解锁").assertIsDisplayed()
    }
}
