package com.mapchina.ui.carving

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class CarvingScreenTest {
    @Test
    fun monumentalBrushIsDefaultAndScalesLargerThanChisel() {
        assertEquals(CarvingBrushType.MONUMENTAL, defaultCarvingBrushType())
        assertTrue(
            adjustedCarvingBrushSize(CarvingBrushType.MONUMENTAL, 24f) >
                adjustedCarvingBrushSize(CarvingBrushType.IRON_CHISEL, 24f)
        )
        assertTrue(adjustedCarvingBrushSize(CarvingBrushType.MONUMENTAL, 24f) >= 64f)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fieldSiteHeaderShowsPlaceContextAndEmptySaveHint() = runComposeUiTest {
        setContent {
            CarvingFieldSiteHeader(
                titleName = "杭州市杭州西湖风景区",
                emptySaveHintVisible = true
            )
        }

        onNodeWithText("杭州市杭州西湖风景区").assertIsDisplayed()
        onNodeWithText("在这面山石上刻下今日足迹").assertIsDisplayed()
        onNodeWithText("先刻下一笔，再落成碑刻").assertIsDisplayed()
    }

    @Test
    fun emptyCarvingSaveIsBlockedOnlyWhenThereAreNoStrokes() {
        assertTrue(shouldBlockEmptyCarvingSave(finishedStrokeCount = 0, existingStrokeCount = 0))
        assertFalse(shouldBlockEmptyCarvingSave(finishedStrokeCount = 1, existingStrokeCount = 0))
        assertFalse(shouldBlockEmptyCarvingSave(finishedStrokeCount = 0, existingStrokeCount = 1))
    }
}
