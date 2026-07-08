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
        onNodeWithText("勋章").assertIsDisplayed()
        onNodeWithText("图鉴").assertIsDisplayed()
        onNodeWithText("征版").assertIsDisplayed()
        onNodeWithText("碑刻").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("统计"))
        onNodeWithText("统计").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("目标"))
        onNodeWithText("目标").assertIsDisplayed()
    }
}
