package com.mapchina.ui.achievement

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class AchievementScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun achievementScreen_displaysTitle() = runComposeUiTest {
        setContent { AchievementScreen() }
        onAllNodesWithText("成就").assertCountEquals(2)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun achievementScreen_displaysStatsTab() = runComposeUiTest {
        setContent { AchievementScreen() }
        onNodeWithText("统计").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun achievementScreen_displaysLevelCard() = runComposeUiTest {
        setContent { AchievementScreen() }
        onNodeWithText("初行者").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun achievementScreen_displaysProvinceConquest() = runComposeUiTest {
        setContent { AchievementScreen() }
        onNodeWithText("省份征服").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun achievementScreen_displaysAtlasLink() = runComposeUiTest {
        setContent { AchievementScreen() }
        onNodeWithText("主题图鉴").assertIsDisplayed()
    }
}
