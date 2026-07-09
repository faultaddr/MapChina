package com.mapchina.ui.discover

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
class DiscoverScreenTest {
    @Test
    fun spotlightUsesImageOnlyWhenRecommendationHasNonBlankImageUrl() {
        assertEquals(false, shouldUseSpotlightImage(null))
        assertEquals(false, shouldUseSpotlightImage(""))
        assertEquals(false, shouldUseSpotlightImage("   "))
        assertEquals(true, shouldUseSpotlightImage("https://example.com/huangshan.jpg"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun discoverContentShowsPendingAndRecommendations() = runComposeUiTest {
        val ui = DiscoverUi(
            pendingSuggestions = listOf(
                FootprintSuggestion(
                    id = "LOCATION:330106",
                    source = FootprintSuggestionSource.LOCATION,
                    regionId = "330106",
                    regionName = "西湖区",
                    parentPath = "浙江省 / 杭州市 / 西湖区",
                    evidenceLabel = "当前位置",
                    suggestedLevel = FootprintLevel.PASS_BY,
                    confidenceLabel = "高"
                )
            ),
            recommendations = listOf(
                DiscoverRecommendation(
                    id = "a1",
                    title = "黄山风景区",
                    subtitle = "安徽省黄山市",
                    levelLabel = "5A",
                    reason = "可点亮 安徽省 / 黄山市",
                    imageUrl = null
                )
            )
        )

        var clickedRecommendation: String? = null
        var openedPendingSuggestions = false

        setContent {
            DiscoverContent(
                ui = ui,
                onSearch = {},
                onRecommendationClick = { clickedRecommendation = it },
                onPendingSuggestionsClick = { openedPendingSuggestions = true }
            )
        }

        onNodeWithText("发现下一站").assertIsDisplayed()
        onNodeWithText("今日推荐").assertIsDisplayed()
        onNodeWithText("优先点亮").assertIsDisplayed()
        onNodeWithText("可点亮 安徽省 / 黄山市").assertIsDisplayed()

        onNode(hasScrollAction()).performScrollToNode(hasText("有 1 条足迹待确认"))
        onNodeWithText("有 1 条足迹待确认").assertIsDisplayed()
        onNodeWithText("浙江省 / 杭州市 / 西湖区").assertIsDisplayed()
        onNodeWithText("去足迹确认").assertIsDisplayed()

        onNodeWithText("去足迹确认").performClick()
        assertEquals(true, openedPendingSuggestions)

        onNode(hasScrollAction()).performScrollToNode(hasText("推荐去点亮"))
        onNodeWithText("推荐去点亮").assertIsDisplayed()
        onNodeWithText("黄山风景区").assertIsDisplayed()
        onNodeWithText("安徽省黄山市").assertIsDisplayed()
        onNodeWithText("5A").assertIsDisplayed()

        onNodeWithText("黄山风景区").performClick()
        assertEquals("a1", clickedRecommendation)

        onNode(hasScrollAction()).performScrollToNode(hasText("附近可点亮"))
        onNodeWithText("附近可点亮").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("主题路线"))
        onNodeWithText("主题路线").assertIsDisplayed()
    }
}
