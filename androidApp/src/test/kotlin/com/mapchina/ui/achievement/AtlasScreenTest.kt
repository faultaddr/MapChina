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
class AtlasScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun atlasScreen_displaysTitle() = runComposeUiTest {
        setContent { AtlasScreen() }
        onNodeWithText("主题图鉴").assertIsDisplayed()
    }
}
