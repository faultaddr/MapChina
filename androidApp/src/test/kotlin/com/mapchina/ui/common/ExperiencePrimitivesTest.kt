package com.mapchina.ui.common

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.ui.theme.MapChinaColors
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ExperiencePrimitivesTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun actionTileDisplaysTextAndHandlesClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            ExperienceActionTile(
                title = "补地图推荐",
                subtitle = "优先点亮未到访省份",
                accent = MapChinaColors.AccentBlue,
                onClick = { clicks++ }
            )
        }

        onNodeWithText("补地图推荐").assertIsDisplayed()
        onNodeWithText("优先点亮未到访省份").assertIsDisplayed()
        onNodeWithText("补地图推荐").performClick()
        assertEquals(1, clicks)
    }
}
