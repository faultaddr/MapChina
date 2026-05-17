package com.mapchina.ui.map

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintService
import com.mapchina.map.MapZoomLevel
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

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        footprintService = FootprintService(footprintRepo, regionRepo)
        viewModel = MapViewModel(footprintService, regionRepo, footprintRepo, "testUser")
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
    fun initialViewMode_isMap() {
        assertEquals(ViewMode.MAP, viewModel.viewMode.value)
    }

    @Test
    fun toggleViewMode_switchesBetweenMapAndBlock() {
        viewModel.toggleViewMode()
        assertEquals(ViewMode.BLOCK, viewModel.viewMode.value)
        viewModel.toggleViewMode()
        assertEquals(ViewMode.MAP, viewModel.viewMode.value)
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
        // At national level, provinces are shown
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
}
