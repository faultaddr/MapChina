package com.mapchina.ui.attraction

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
class AttractionsScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun attractionsScreen_displaysFilterChips() = runComposeUiTest {
        setContent { AttractionsScreen(navController = rememberNavController()) }
        onNodeWithText("全部").assertIsDisplayed()
        onNodeWithText("5A").assertIsDisplayed()
        onNodeWithText("4A").assertIsDisplayed()
    }
}
