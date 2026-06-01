package com.mapchina.integration

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.remote.DataSeeder
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.AchievementStatus
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AchievementSeeder
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AtlasSeeder
import com.mapchina.domain.service.AtlasService
import com.mapchina.domain.service.FootprintService
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.achievement.AtlasViewModel
import com.mapchina.ui.achievement.ProvinceConquestViewModel
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.map.MapViewModel
import com.mapchina.ui.profile.ProfileViewModel
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.domain.service.AttractionService
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
    private lateinit var achievementRepo: AchievementRepository
    private lateinit var atlasRepo: AtlasRepository
    private lateinit var userScoreRepo: UserScoreRepository
    private lateinit var footprintService: FootprintService
    private lateinit var achievementService: AchievementService
    private lateinit var atlasService: AtlasService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        regionRepo = RegionRepository(database)
        footprintRepo = FootprintRepository(database)
        attractionRepo = AttractionRepository(database)
        achievementRepo = AchievementRepository(database)
        atlasRepo = AtlasRepository(database)
        userScoreRepo = UserScoreRepository(database)
        footprintService = FootprintService(footprintRepo, regionRepo, null)
        achievementService = AchievementService(achievementRepo, footprintRepo, userScoreRepo, attractionRepo, regionRepo, atlasRepo)
        atlasService = AtlasService(atlasRepo, footprintRepo)

        DataSeeder.seedRegions(regionRepo)
        attractionRepo.insertAttraction(Attraction("a1", "故宫博物院", "110101", AttractionLevel.A5, 39.9163, 116.3972, "紫禁城"))
        attractionRepo.insertAttraction(Attraction("a2", "长城", "110229", AttractionLevel.A5, 40.3588, 116.0204, null))
        attractionRepo.insertAttraction(Attraction("a3", "武侯祠", "510107", AttractionLevel.A4, 30.6438, 104.0482, null))
    }

    @Test
    fun fullJourney_seedData_browseMarkViewStats() {
        val userId = "testUser"

        // 1. Browse provinces at national level
        val mapViewModel = MapViewModel(footprintService, regionRepo, footprintRepo, AttractionService(attractionRepo), null, userId)
        val provinces = mapViewModel.regions.value
        assertEquals(34, provinces.size)

        // 2. Drill into Sichuan
        mapViewModel.drillIntoRegion("510000")
        val sichuanCities = mapViewModel.regions.value
        assertTrue(sichuanCities.size >= 3)

        // 3. Mark Sichuan as deep visit
        mapViewModel.markFootprint("510000", FootprintLevel.DEEP)

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
        val attractionViewModel = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null)
        attractionViewModel.searchAttractions("故宫")
        val searchResults = attractionViewModel.attractions.value
        assertTrue(searchResults.isNotEmpty())
        assertEquals("故宫博物院", searchResults.first().name)

        // 7. Mark an attraction visit
        attractionViewModel.markVisit("a1", "110101", FootprintLevel.DEEP)
        val updatedAttraction = attractionViewModel.attractions.value.find { it.id == "a1" }
        assertEquals(FootprintLevel.DEEP, updatedAttraction?.visitLevel)

        // 8. Check coverage stats
        val statsViewModel = StatsViewModel(footprintService, attractionRepo, footprintRepo, userId)
        statsViewModel.refreshStats()
        val stats = statsViewModel.stats.value
        assertTrue(stats.visitedProvinces >= 2)
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
        // Seed region hierarchy so cascadeToParentRegions can walk up
        regionRepo.insertRegion(Region("510107", "武侯区", RegionLevel.DISTRICT, "510100"))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))

        footprintRepo.markAttractionVisit(userId, "a1", "510107", FootprintLevel.DEEP)

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

    // === Achievement path ===

    @Test
    fun achievementPath_markFootprint_unlocksAndScores() {
        val userId = "u1"
        AchievementSeeder.seedAchievements(achievementRepo)

        footprintRepo.markFootprint(userId, "510000", FootprintLevel.PASS_BY)
        footprintRepo.markFootprint(userId, "110000", FootprintLevel.DEEP)

        val result = achievementService.evaluateAndSettle(userId)
        assertTrue(result.newlyUnlocked.isNotEmpty())
        assertTrue(result.scoreAdded > 0)

        val vm = AchievementViewModel(achievementRepo, userScoreRepo, userId)
        vm.refresh()
        assertTrue(vm.ui.value.unlockedCount > 0)
    }

    @Test
    fun achievementPath_badgeWallShowsAllAchievements() {
        AchievementSeeder.seedAchievements(achievementRepo)
        achievementService.evaluateAndSettle("u1")

        val vm = AchievementViewModel(achievementRepo, userScoreRepo, "u1")
        vm.refresh()
        val ui = vm.ui.value
        assertTrue(ui.totalCount > 0)
        assertTrue(ui.allAchievements.size == ui.totalCount)
    }

    // === Atlas path ===

    @Test
    fun atlasPath_markVisit_showsProgress() {
        AtlasSeeder.seedAtlas(atlasRepo)

        // Use the real attraction ID that AtlasSeeder references
        val gugongId = "attr_B000A8UIN8"
        footprintRepo.markAttractionVisit("u1", gugongId, "110101", FootprintLevel.DEEP)

        val progress = atlasService.getAtlasProgress("u1")
        val heritage = progress.find { it.atlasId == "world_heritage" }
        assertNotNull(heritage)
        assertTrue(heritage.visitedItems > 0)

        val items = atlasService.getAtlasItemsWithVisitStatus("u1", "world_heritage")
        val guGong = items.find { it.attractionId == gugongId }
        assertTrue(guGong!!.isVisited)
    }

    @Test
    fun atlasPath_atlasViewModel_showsProgress() {
        AchievementSeeder.seedAchievements(achievementRepo)
        AtlasSeeder.seedAtlas(atlasRepo)

        val vm = AtlasViewModel(atlasService, achievementService, achievementRepo)
        vm.refresh()
        assertEquals(3, vm.ui.value.atlasProgress.size)

        vm.loadAtlasDetail("world_heritage")
        assertNotNull(vm.detailUi.value.progress)
        assertTrue(vm.detailUi.value.items.isNotEmpty())
    }

    // === Province Conquest path ===

    @Test
    fun provinceConquestPath_showsProvinces() {
        AchievementSeeder.seedAchievements(achievementRepo)

        val vm = ProvinceConquestViewModel(achievementService, achievementRepo)
        vm.refresh()
        assertTrue(vm.ui.value.provinces.isNotEmpty())
    }

    @Test
    fun provinceConquestPath_detailShowsAchievements() {
        AchievementSeeder.seedAchievements(achievementRepo)

        val vm = ProvinceConquestViewModel(achievementService, achievementRepo)
        vm.loadProvinceDetail("51")
        assertTrue(vm.detailUi.value.provinceAchievements.isNotEmpty())
    }

    // === Profile path ===

    @Test
    fun profilePath_loadAndLogout() {
        val authService = com.mapchina.domain.service.AuthService()
        val vm = ProfileViewModel(authService, userScoreRepo)
        vm.loadProfile()
        assertEquals("未登录", vm.profile.value.nickname)

        vm.logout()
        assertEquals("未登录", vm.profile.value.nickname)
        assertEquals(false, vm.isLoggedIn.value)
    }
}
