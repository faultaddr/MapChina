package com.mapchina.ui.stats

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var footprintService: FootprintService
    private lateinit var attractionRepo: AttractionRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var regionRepo: RegionRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        regionRepo = RegionRepository(database)
        footprintRepo = FootprintRepository(database)
        attractionRepo = AttractionRepository(database)
        footprintService = FootprintService(footprintRepo, regionRepo, null)

        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
    }

    @Test
    fun initialStats_allZero() {
        val vm = StatsViewModel(footprintService, attractionRepo, footprintRepo, regionRepo, com.mapchina.domain.service.AuthService(), UnconfinedTestDispatcher())
        val stats = vm.stats.value
        assertEquals(0, stats.visitedProvinces)
        assertEquals(0, stats.visitedAttractions)
    }

    @Test
    fun refreshStats_showsCorrectCoverage() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)
        footprintRepo.markFootprint("u1", "110000", FootprintLevel.DEEP)

        val authService = com.mapchina.domain.service.AuthService()
        authService.onLogin(com.mapchina.data.model.UserDto("u1", "", "U1", null, 0L))
        val vm = StatsViewModel(footprintService, attractionRepo, footprintRepo, regionRepo, authService, UnconfinedTestDispatcher())
        vm.refreshStats()
        val stats = vm.stats.value
        assertTrue(stats.visitedProvinces >= 2)
    }

    @Test
    fun refreshStats_levelDistributionCorrect() {
        attractionRepo.insertAttraction(Attraction("a1", "故宫", "110101", AttractionLevel.A5, 39.9, 116.4, null))
        attractionRepo.insertAttraction(Attraction("a2", "景山", "110101", AttractionLevel.A4, 39.92, 116.4, null))
        footprintRepo.markAttractionVisit("u1", "a1", "110101", FootprintLevel.DEEP)
        footprintRepo.markAttractionVisit("u1", "a2", "110101", FootprintLevel.SHORT_VISIT)

        val authService = com.mapchina.domain.service.AuthService()
        authService.onLogin(com.mapchina.data.model.UserDto("u1", "", "U1", null, 0L))
        val vm = StatsViewModel(footprintService, attractionRepo, footprintRepo, regionRepo, authService, UnconfinedTestDispatcher())
        vm.refreshStats()
        val stats = vm.stats.value
        assertEquals(1, stats.levelDistribution.a5Visited)
        assertEquals(1, stats.levelDistribution.a4Visited)
    }

    @Test
    fun refreshStats_visitLevelCounts() {
        attractionRepo.insertAttraction(Attraction("a1", "故宫", "110101", AttractionLevel.A5, 39.9, 116.4, null))
        footprintRepo.markAttractionVisit("u1", "a1", "110101", FootprintLevel.DEEP)

        val authService = com.mapchina.domain.service.AuthService()
        authService.onLogin(com.mapchina.data.model.UserDto("u1", "", "U1", null, 0L))
        val vm = StatsViewModel(footprintService, attractionRepo, footprintRepo, regionRepo, authService, UnconfinedTestDispatcher())
        vm.refreshStats()
        val stats = vm.stats.value
        assertEquals(1, stats.visitLevelCounts[FootprintLevel.DEEP])
    }
}
