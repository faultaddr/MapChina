package com.mapchina.ui.profile

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.ui.theme.Copy
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ProfileScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysUserCard() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("未登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysNotLoggedInState() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText("未登录").assertIsDisplayed()
        onNodeWithText("登录").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun profileScreen_displaysFeatureCards() = runComposeUiTest {
        setContent { ProfileScreen() }
        onNodeWithText(Copy.FEATURE_JOURNAL_TITLE).assertIsDisplayed()
        onNodeWithText(Copy.FEATURE_BADGE_TITLE).assertIsDisplayed()
        onNodeWithText(Copy.FEATURE_PROVINCE_TITLE).assertIsDisplayed()
        onNodeWithText(Copy.FEATURE_ATLAS_TITLE).assertIsDisplayed()
    }
}
