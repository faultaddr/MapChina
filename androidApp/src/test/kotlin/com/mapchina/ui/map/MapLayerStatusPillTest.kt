package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MapLayerStatusPillTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loading_showsLevelSpecificCopy() = runComposeUiTest {
        setContent {
            MapLayerStatusPill(
                state = MapLayerLoadState.Loading("510000", "正在展开市级地图"),
                onRetry = {},
                onDismiss = {}
            )
        }
        onNodeWithContentDescription("地图层级加载中").assertIsDisplayed()
        onNodeWithText("正在展开市级地图").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun error_exposesRetryAndDismiss() = runComposeUiTest {
        var retry = 0
        var dismiss = 0
        setContent {
            MapLayerStatusPill(
                state = MapLayerLoadState.Error("510000", "市级地图暂时无法展开"),
                onRetry = { retry += 1 },
                onDismiss = { dismiss += 1 }
            )
        }
        onNodeWithText("重试").performClick()
        onNodeWithText("关闭").performClick()
        assertEquals(1, retry)
        assertEquals(1, dismiss)
    }
}
