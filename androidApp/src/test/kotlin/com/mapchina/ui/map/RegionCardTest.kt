package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.domain.model.FootprintLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class RegionCardTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun firstFootprintActivation_showsLevelsWithoutIntermediateAction() = runComposeUiTest {
        var markedLevel: FootprintLevel? = null

        setContent {
            RegionCard(
                region = RegionFootprintUi(
                    regionId = "330000",
                    name = "浙江省",
                    footprintLevel = null,
                    normalizedPath = emptyList(),
                    bounds = RegionBounds(0f, 0f, 0f, 0f)
                ),
                attractionCount = 12,
                canDrillDown = true,
                firstFootprintActivation = true,
                onMarkFootprint = { _, level -> markedLevel = level },
                onDrillDown = {},
                onShowAttractions = {}
            )
        }

        onNodeWithText("这次停留有多深？").assertIsDisplayed()
        onNodeWithText("途经").assertIsDisplayed()
        onNodeWithText("小驻").assertIsDisplayed()
        onNodeWithText("深游").assertIsDisplayed()
        onAllNodesWithText("标记足迹").assertCountEquals(0)
        onAllNodesWithText("查看下级").assertCountEquals(0)

        onNodeWithText("小驻").performClick()
        assertEquals(FootprintLevel.SHORT_VISIT, markedLevel)
    }
}
