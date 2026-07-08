package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.model.FootprintSuggestionSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class FootprintSuggestionCardTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cardDisplaysRegionPathAndConfirmActions() = runComposeUiTest {
        var confirmed: FootprintLevel? = null
        var dismissed = false
        val suggestion = FootprintSuggestion(
            id = "LOCATION:330106",
            source = FootprintSuggestionSource.LOCATION,
            regionId = "330106",
            regionName = "西湖区",
            parentPath = "浙江省 / 杭州市 / 西湖区",
            evidenceLabel = "当前位置",
            suggestedLevel = FootprintLevel.PASS_BY,
            confidenceLabel = "高"
        )

        setContent {
            FootprintSuggestionCard(
                suggestion = suggestion,
                onConfirm = { confirmed = it },
                onDismiss = { dismissed = true }
            )
        }

        onNodeWithText("发现可能足迹").assertIsDisplayed()
        onNodeWithText("浙江省 / 杭州市 / 西湖区").assertIsDisplayed()
        onNodeWithText("当前位置 · 可信度高").assertIsDisplayed()
        onNodeWithText("确认后会同时将上级地区标记为途经").assertIsDisplayed()
        onNodeWithText("途经").performClick()
        assertEquals(FootprintLevel.PASS_BY, confirmed)
        onNodeWithText("小驻").performClick()
        assertEquals(FootprintLevel.SHORT_VISIT, confirmed)
        onNodeWithText("深游").performClick()
        assertEquals(FootprintLevel.DEEP, confirmed)
        onNodeWithText("忽略").performClick()
        assertEquals(true, dismissed)
    }
}
