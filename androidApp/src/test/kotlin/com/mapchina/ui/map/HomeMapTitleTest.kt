package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.map.MapTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class HomeMapTitleTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun title_displaysNationalProgressWithoutBackAction() = runComposeUiTest {
        setContent {
            HomeMapTitle(
                path = listOf(BreadcrumbItem("", "中国")),
                currentLevel = "省",
                visitedCount = 1,
                totalCount = 34,
                coveragePercent = 2,
                onNavigateUp = {},
                onNavigateToNational = {},
                mapTheme = MapTheme.DEFAULT
            )
        }

        onNodeWithContentDescription("地图铭牌").assertIsDisplayed()
        onNodeWithText("中国足迹").assertIsDisplayed()
        onNodeWithText("1/34 已点亮").assertIsDisplayed()
        onNodeWithText("省级地图 · 2%").assertIsDisplayed()
        onAllNodesWithContentDescription("返回上级").assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun title_showsBackActionForDrilledPath() = runComposeUiTest {
        var backCount = 0
        setContent {
            HomeMapTitle(
                path = listOf(
                    BreadcrumbItem("", "中国"),
                    BreadcrumbItem("330000", "浙江省")
                ),
                currentLevel = "市",
                visitedCount = 3,
                totalCount = 11,
                coveragePercent = 27,
                onNavigateUp = { backCount++ },
                onNavigateToNational = {},
                mapTheme = MapTheme.DEFAULT
            )
        }

        onNodeWithText("浙江省").assertIsDisplayed()
        onNodeWithContentDescription("返回上级").performClick()
        assertEquals(1, backCount)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun title_displaysFreshNationalHudAndNationalAction() = runComposeUiTest {
        var nationalCount = 0
        setContent {
            HomeMapTitle(
                path = listOf(BreadcrumbItem("", "中国")),
                currentLevel = "省",
                visitedCount = 8,
                totalCount = 34,
                coveragePercent = 23,
                onNavigateUp = {},
                onNavigateToNational = { nationalCount += 1 },
                mapTheme = MapTheme.DEFAULT
            )
        }

        onNodeWithText("中国足迹").assertIsDisplayed()
        onNodeWithText("8/34 已点亮").assertIsDisplayed()
        onNodeWithText("足迹").assertIsDisplayed()
        onNodeWithText("全国").assertIsDisplayed().performClick()
        assertEquals(1, nationalCount)
    }
}
