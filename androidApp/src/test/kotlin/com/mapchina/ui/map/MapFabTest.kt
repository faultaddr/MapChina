package com.mapchina.ui.map

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MapFabTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun regularMode_exposesDirectLocationAndGroupedTools() = runComposeUiTest {
        var locationCount = 0
        setContent {
            MapFab(
                coveragePercent = 17,
                photoMarkersVisible = false,
                isExpanded = false,
                onExpandedChange = {},
                onTogglePhotos = {},
                onMyLocation = { locationCount++ }
            )
        }

        onNodeWithContentDescription("当前定位").assertIsDisplayed().performClick()
        assertEquals(1, locationCount)
        onNodeWithContentDescription("地图工具").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun expandedRegularMode_doesNotRepeatCurrentLocation() = runComposeUiTest {
        setContent {
            MapFab(
                coveragePercent = 17,
                photoMarkersVisible = false,
                isExpanded = true,
                onExpandedChange = {},
                onTogglePhotos = {},
                onDepart = {},
                onShare = {},
                onMyLocation = {}
            )
        }

        onAllNodesWithText("当前定位").assertCountEquals(0)
        onNodeWithText("随机出发").assertIsDisplayed()
        onNodeWithText("照片回溯 · 实验").assertIsDisplayed()
        onNodeWithText("分享").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun firstFootprintMode_usesOneCombinedAction() = runComposeUiTest {
        setContent {
            MapFab(
                coveragePercent = 0,
                photoMarkersVisible = false,
                isExpanded = false,
                onExpandedChange = {},
                onTogglePhotos = {},
                firstFootprintActivation = true,
                onChooseMap = {},
                onSearchAttraction = {},
                onUseCurrentLocation = {}
            )
        }

        onNode(
            hasText("添加第一处足迹") and
                hasContentDescription("添加第一处足迹")
        ).assertIsDisplayed().assertHasClickAction()
        onNodeWithText("添加第一处足迹").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun groupedMenu_keepsLongActionsReadableAtLargeFontScale() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                MapFab(
                    coveragePercent = 17,
                    photoMarkersVisible = false,
                    isExpanded = true,
                    onExpandedChange = {},
                    onTogglePhotos = {},
                    onDepart = {},
                    onShare = {}
                )
            }
        }

        onNodeWithContentDescription("地图工具菜单")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(220.dp)
        onNodeWithText("照片回溯 · 实验").assertIsDisplayed()
    }
}
