package com.mapchina.ui.map

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.RegionMatch
import com.mapchina.domain.service.FootprintSuggestionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MapScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test fun mapScreen_displaysInitializingMessage() = runComposeUiTest {
        setContent { MapScreen(onNavigate = {}, onBack = {}) }
        onNodeWithText("足迹地图（初始化中）").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    @Test fun mapScreen_showsLightweightHudAndSingleMapToolEntry() = runComposeUiTest {
        val fixture = createSuggestionFixture(offerSuggestion = false)
        fixture.viewModel.dismissOnboarding()

        setContent {
            MapScreen(
                onNavigate = {},
                onBack = {},
                viewModel = fixture.viewModel
            )
        }

        onNodeWithText("中国").assertIsDisplayed()
        onNodeWithText("省级地图 · 0%", substring = false).assertIsDisplayed()
        onNodeWithText("已点亮", substring = true).assertIsDisplayed()
        onAllNodesWithText("地图操作").assertCountEquals(0)
        onAllNodesWithText("随机出发").assertCountEquals(0)
        onAllNodesWithText("照片标记").assertCountEquals(0)
        onNodeWithContentDescription("地图工具").assertIsDisplayed().performClick()
        onNodeWithText("随机出发").assertIsDisplayed()
        onNodeWithText("照片标记").assertIsDisplayed()
        onNodeWithText("分享").assertIsDisplayed()
        onNodeWithText("当前定位").assertIsDisplayed()
        onAllNodesWithText("中国足迹").assertCountEquals(0)
        onAllNodesWithText("从第一处开始点亮").assertCountEquals(0)
        onAllNodesWithText("下一步").assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    @Test fun mapScreen_showsPendingSuggestionAndConfirmsIt() = runComposeUiTest {
        val fixture = createSuggestionFixture()

        setContent {
            MapScreen(
                onNavigate = {},
                onBack = {},
                viewModel = fixture.viewModel
            )
        }

        onNodeWithText("中国").assertIsDisplayed()
        onNodeWithText("省级地图 · 0%", substring = false).assertIsDisplayed()
        onNodeWithText("发现可能足迹").assertIsDisplayed()
        onNodeWithText("浙江省 / 杭州市 / 西湖区").assertIsDisplayed()
        onNodeWithText("当前位置 · 可信度高").assertIsDisplayed()
        onNodeWithText("确认后会同时将上级地区标记为途经").assertIsDisplayed()
        onAllNodesWithContentDescription("地图工具").assertCountEquals(0)

        onNodeWithText("小驻").performClick()

        onAllNodesWithText("发现可能足迹").assertCountEquals(0)
        org.junit.Assert.assertEquals(
            FootprintLevel.SHORT_VISIT,
            fixture.footprintRepo.getFootprint("u1", "330106")?.level
        )
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    @Test fun mapScreen_hidesPendingSuggestionWhenRegionPanelIsOpen() = runComposeUiTest {
        val fixture = createSuggestionFixture()
        fixture.viewModel.showRegionPanel("330106")

        setContent {
            MapScreen(
                onNavigate = {},
                onBack = {},
                viewModel = fixture.viewModel
            )
        }

        onAllNodesWithText("发现可能足迹").assertCountEquals(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createSuggestionFixture(offerSuggestion: Boolean = true): SuggestionFixture {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MapChinaDatabase.Schema.create(driver)
        val database = MapChinaDatabase(driver)
        val regionRepo = RegionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)
        val attractionService = AttractionService(AttractionRepository(database))

        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("330100", "杭州市", RegionLevel.CITY, "330000"))
        regionRepo.insertRegion(Region("330106", "西湖区", RegionLevel.DISTRICT, "330100"))
        if (offerSuggestion) {
            suggestionService.offerFromLocation(
                RegionMatch(
                    province = regionRepo.getRegion("330000"),
                    city = regionRepo.getRegion("330100"),
                    district = regionRepo.getRegion("330106")
                )
            )
        }

        val viewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher(),
            footprintSuggestionService = suggestionService
        )

        return SuggestionFixture(viewModel, footprintRepo)
    }

    private data class SuggestionFixture(
        val viewModel: MapViewModel,
        val footprintRepo: FootprintRepository
    )
}
