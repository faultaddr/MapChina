package com.mapchina.ui.map

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.compose.rememberNavController
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MapScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun mapScreen_displaysInitializingMessage() = runComposeUiTest {
        setContent { MapScreen(navController = rememberNavController()) }
        onNodeWithText("足迹地图（初始化中）").assertIsDisplayed()
    }
}
