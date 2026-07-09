package com.mapchina.ui.carving

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.CarvingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class CarvingListScreenTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun allCarvingsCreateShowsPlacePickerInsteadOfBlankCanvas() = runComposeUiTest {
        val viewModel = createCarvingViewModel()
        var selected: CarvingPlaceTarget? = null

        setContent {
            CarvingListScreen(
                viewModel = viewModel,
                title = "我的碑刻",
                showAll = true,
                onCreateClick = { selected = it },
                onBack = {}
            )
        }

        onNodeWithContentDescription("新碑刻").performClick()
        onNodeWithText("选择留刻地点").assertIsDisplayed()
        onNodeWithText("杭州市杭州西湖风景区").performClick()

        assertEquals("330100", selected?.regionId)
        assertEquals("杭州市", selected?.regionName)
        assertEquals("mct_1033", selected?.attractionId)
        assertEquals("杭州市杭州西湖风景区", selected?.attractionName)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createCarvingViewModel(): CarvingViewModel {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MapChinaDatabase.Schema.create(driver)
        return CarvingViewModel(
            carvingRepository = CarvingRepository(MapChinaDatabase(driver)),
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher()
        )
    }
}
