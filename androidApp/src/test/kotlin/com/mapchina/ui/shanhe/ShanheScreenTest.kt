package com.mapchina.ui.shanhe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shanheDashboardKeepsContentVisibleAtLargeFontScale() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                Box(modifier = Modifier.width(320.dp).height(640.dp)) {
                    ShanheContent(
                        ui = ShanheUi(
                            levelNumber = 10,
                            levelTitle = "MapChina 宗师",
                            currentScore = 25000,
                            nextLevelTitle = "MapChina 宗师",
                            remainingScore = 0,
                            levelProgress = 1f,
                            targetTitle = "完成最后一块山河版图",
                            targetBody = "再点亮一个还没有走过的区县",
                            unlockedCount = 99,
                            totalAchievementCount = 100,
                            recentUnlocks = listOf("中华丈量师")
                        ),
                        onNavigate = {}
                    )
                }
            }
        }

        onNodeWithText("MapChina 宗师").assertIsDisplayed()
        onNodeWithText("25000 山河值").assertIsDisplayed()
        val levelBounds = onNodeWithText("MapChina 宗师").fetchSemanticsNode().boundsInRoot
        val scoreBounds = onNodeWithText("25000 山河值").fetchSemanticsNode().boundsInRoot
        assertFalse("level title and score must not overlap", levelBounds.overlaps(scoreBounds))
        onNode(hasScrollAction()).performScrollToNode(hasText("碑刻"))
        onNodeWithText("碑刻").assertIsDisplayed()
        onNodeWithText("石壁留名").assertIsDisplayed()
        val carvingTileBounds = onNode(
            hasClickAction() and hasAnyDescendant(hasText("碑刻")),
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        val carvingSubtitleBounds = onNodeWithText("石壁留名").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "growth tile must expand to contain its subtitle",
            carvingSubtitleBounds.bottom <= carvingTileBounds.bottom
        )
        onNode(hasScrollAction()).performScrollToNode(hasText("中华丈量师"))
        onNodeWithText("中华丈量师").assertIsDisplayed()
    }
}
