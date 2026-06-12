package com.mapchina.ui.achievement

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class AtlasDetailScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun atlasDetailScreen_displaysTitle() = runComposeUiTest {
        setContent { AtlasDetailScreen() }
        onNodeWithText("图鉴详情").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun atlasDetailScreen_displaysLoadingWhenNoData() = runComposeUiTest {
        setContent { AtlasDetailScreen() }
        onNodeWithText("加载中...").assertIsDisplayed()
    }
}
