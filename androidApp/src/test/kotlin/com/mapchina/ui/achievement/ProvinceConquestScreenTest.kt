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
class ProvinceConquestScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun provinceConquestScreen_displaysTitle() = runComposeUiTest {
        setContent { ProvinceConquestScreen() }
        onNodeWithText("省份征服").assertIsDisplayed()
    }
}
