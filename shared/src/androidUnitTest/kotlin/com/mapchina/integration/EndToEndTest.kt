package com.mapchina.integration

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.remote.DataSeeder
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.service.FootprintService
import com.mapchina.ui.map.MapViewModel
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.stats.StatsViewModel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndToEndTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var regionRepo: RegionRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var attractionRepo: AttractionRepository
    private lateinit var footprintService: FootprintService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        regionRepo = RegionRepository(database)
        footprintRepo = FootprintRepository(database)
        attractionRepo = AttractionRepository(database)
        footprintService = FootprintService(footprintRepo, regionRepo)

        DataSeeder.seedRegions(regionRepo)
        DataSeeder.seedAttractions(attractionRepo)
    }

    @Test
    fun fullJourney_seedData_browseMarkViewStats() {
        val userId = "testUser"

        // 1. Browse provinces at national level
        val mapViewModel = MapViewModel(footprintService, regionRepo, footprintRepo, userId)
        val provinces = mapViewModel.regions.value
        assertEquals(34, provinces.size)

        // 2. Drill into Sichuan
        mapViewModel.drillIntoRegion("510000")
        val sichuanCities = mapViewModel.regions.value
        assertTrue(sichuanCities.size >= 3) // 成都、自贡、绵阳 at minimum

        // 3. Mark Sichuan as deep visit
        mapViewModel.markFootprint("510000", FootprintLevel.DEEP)
        val sichuan = mapViewModel.regions.value.find { it.regionId == "510000" }
        // Sichuan is in current path, not in child list

        // 4. Navigate back to national and verify
        mapViewModel.navigateUp()
        val nationalRegions = mapViewModel.regions.value
        val updatedSichuan = nationalRegions.find { it.regionId == "510000" }
        assertEquals(FootprintLevel.DEEP, updatedSichuan?.footprintLevel)

        // 5. Mark Beijing as pass-by
        mapViewModel.markFootprint("110000", FootprintLevel.PASS_BY)
        val beijing = mapViewModel.regions.value.find { it.regionId == "110000" }
        assertEquals(FootprintLevel.PASS_BY, beijing?.footprintLevel)

        // 6. Search attractions
        val attractionViewModel = AttractionViewModel(attractionRepo, footprintService, footprintRepo, userId)
        attractionViewModel.searchAttractions("故宫")
        val searchResults = attractionViewModel.attractions.value
        assertTrue(searchResults.isNotEmpty())
        assertEquals("故宫博物院", searchResults.first().name)

        // 7. Mark an attraction visit
        attractionViewModel.markVisit("attr001", "110101", FootprintLevel.DEEP)
        val updatedAttraction = attractionViewModel.attractions.value.find { it.id == "attr001" }
        assertEquals(FootprintLevel.DEEP, updatedAttraction?.visitLevel)

        // 8. Check coverage stats
        val statsViewModel = StatsViewModel(footprintService, userId)
        statsViewModel.refreshStats()
        val stats = statsViewModel.stats.value
        assertTrue(stats.visitedProvinces >= 2) // Beijing + Sichuan
        assertTrue(stats.totalProvinces == 34)
    }

    @Test
    fun footprintUpgradeNeverDowngrades() {
        val userId = "u1"
        footprintRepo.markFootprint(userId, "510000", FootprintLevel.PASS_BY)
        footprintRepo.markFootprint(userId, "510000", FootprintLevel.DEEP)
        footprintRepo.markFootprint(userId, "510000", FootprintLevel.SHORT_VISIT)

        val footprint = footprintRepo.getFootprint(userId, "510000")
        assertEquals(FootprintLevel.DEEP, footprint!!.level)
    }

    @Test
    fun attractionVisitCascadesToParentRegions() {
        val userId = "u1"
        footprintRepo.markAttractionVisit(userId, "attr001", "510107", FootprintLevel.DEEP)

        val district = footprintRepo.getFootprint(userId, "510107")
        assertNotNull(district)
        assertEquals(FootprintLevel.DEEP, district.level)

        val city = footprintRepo.getFootprint(userId, "510100")
        assertNotNull(city)
        assertTrue(city!!.level >= FootprintLevel.PASS_BY)
    }

    @Test
    fun dataSeederIsIdempotent() {
        DataSeeder.seedRegions(regionRepo)
        val provinces = regionRepo.getRegionsByLevel(com.mapchina.domain.model.RegionLevel.PROVINCE)
        assertEquals(34, provinces.size)
    }
}
