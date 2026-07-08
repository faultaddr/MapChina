package com.mapchina.ui.map

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import com.mapchina.map.MapZoomLevel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var footprintService: FootprintService
    private lateinit var regionRepo: RegionRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var suggestionService: FootprintSuggestionService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val attractionService = AttractionService(attractionRepo)
        footprintService = FootprintService(footprintRepo, regionRepo, null)
        suggestionService = FootprintSuggestionService(regionRepo, footprintService)
        viewModel = MapViewModel(
            footprintService,
            regionRepo,
            footprintRepo,
            attractionService,
            null,
            null,
            null,
            null,
            null,
            null,
            "testUser",
            UnconfinedTestDispatcher(),
            suggestionService
        )
    }

    @Test
    fun initialLevel_isNational() {
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    }

    @Test
    fun initialPath_isEmpty() {
        assertEquals(0, viewModel.currentPath.value.size)
    }

    @Test
    fun drillIntoProvince_updatesCurrentLevel() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        viewModel.drillIntoRegion("510000")
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals(1, viewModel.currentPath.value.size)
        assertEquals("510000", viewModel.currentPath.value.first().id)
    }

    @Test
    fun drillIntoCity_thenNavigateUp() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        viewModel.drillIntoRegion("510000")
        viewModel.drillIntoRegion("510100")
        assertEquals(MapZoomLevel.CITY, viewModel.currentLevel.value)

        viewModel.navigateUp()
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals(1, viewModel.currentPath.value.size)
    }

    @Test
    fun navigateUp_fromNational_staysNational() {
        viewModel.navigateUp()
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    }

    @Test
    fun markFootprint_updatesRegionState() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        viewModel.markFootprint("510000", FootprintLevel.DEEP)
        val region = viewModel.regions.value.find { it.regionId == "510000" }
        assertEquals(FootprintLevel.DEEP, region?.footprintLevel)
    }

    @Test
    fun markFootprint_upgradeOnly() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        viewModel.markFootprint("510000", FootprintLevel.DEEP)
        viewModel.markFootprint("510000", FootprintLevel.PASS_BY)
        val region = viewModel.regions.value.find { it.regionId == "510000" }
        assertEquals(FootprintLevel.DEEP, region?.footprintLevel)
    }

    @Test
    fun selectRegion_andClearSelection() {
        assertNull(viewModel.selectedRegion.value)
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        viewModel.selectRegion("510000")
        assertNotNull(viewModel.selectedRegion.value)
        viewModel.clearSelection()
        assertNull(viewModel.selectedRegion.value)
    }

    @Test
    fun footprintSuggestions_exposesPendingSuggestions() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))

        suggestionService.offerFromAttractionVisit(
            regionId = "110101",
            attractionName = "故宫博物院",
            level = FootprintLevel.DEEP
        )

        assertEquals(1, viewModel.footprintSuggestions.value.size)
        assertEquals("110101", viewModel.footprintSuggestions.value.first().regionId)
    }

    @Test
    fun confirmSuggestion_writesFootprintAndClearsPendingSuggestion() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))

        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "110101",
            attractionName = "故宫博物院",
            level = FootprintLevel.SHORT_VISIT
        )

        viewModel.confirmSuggestion(suggestion!!.id, FootprintLevel.SHORT_VISIT)

        assertEquals(FootprintLevel.SHORT_VISIT, footprintRepo.getFootprint("testUser", "110101")?.level)
        assertEquals(0, viewModel.footprintSuggestions.value.size)
    }

    @Test
    fun dismissSuggestion_removesPendingSuggestion() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "110000",
            attractionName = "天安门",
            level = FootprintLevel.PASS_BY
        )

        viewModel.dismissSuggestion(suggestion!!.id)

        assertEquals(0, viewModel.footprintSuggestions.value.size)
    }

    @Test
    fun togglePhotoMarkers_showsPhaseMessage() {
        viewModel.togglePhotoMarkers()

        assertEquals("照片回溯将在后续版本开放", viewModel.autoMarkMessage.value)
    }
}
