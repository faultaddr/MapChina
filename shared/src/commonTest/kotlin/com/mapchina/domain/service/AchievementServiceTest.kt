package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.AchievementCategory
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.AchievementStatus
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementServiceTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var achievementRepo: AchievementRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var userScoreRepo: UserScoreRepository
    private lateinit var attractionRepo: AttractionRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var atlasRepo: AtlasRepository
    private lateinit var service: AchievementService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        achievementRepo = AchievementRepository(database)
        footprintRepo = FootprintRepository(database)
        userScoreRepo = UserScoreRepository(database)
        attractionRepo = AttractionRepository(database)
        regionRepo = RegionRepository(database)
        atlasRepo = AtlasRepository(database)
        service = AchievementService(achievementRepo, footprintRepo, userScoreRepo, attractionRepo, regionRepo, atlasRepo)

        seedAchievementDefinitions()
        seedRegions()
    }

    private fun seedAchievementDefinitions() {
        AchievementSeeder.seedAchievements(achievementRepo)
    }

    private fun seedRegions() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("110100", "北京市(市)", RegionLevel.CITY, "110000"))
    }

    @Test
    fun evaluateAndSettle_initializesUserAchievements() {
        assertFalse(achievementRepo.isInitialized("u1"))
        service.evaluateAndSettle("u1")
        assertTrue(achievementRepo.isInitialized("u1"))
    }

    @Test
    fun evaluateAndSettle_noFootprints_allLocked() {
        val result = service.evaluateAndSettle("u1")
        assertEquals(0, result.newlyUnlocked.size)
        val achievements = achievementRepo.getUserAchievements("u1")
        achievements.forEach { assertEquals(AchievementStatus.LOCKED, it.status) }
    }

    @Test
    fun evaluateAndSettle_unlocksDistrictAchievement() {
        // Mark a district-level footprint
        regionRepo.insertRegion(Region("510107", "武侯区", RegionLevel.DISTRICT, "510100"))
        footprintRepo.markFootprint("u1", "510107", FootprintLevel.PASS_BY)

        val result = service.evaluateAndSettle("u1")
        val districtAchievements = achievementRepo.getUserAchievements("u1")
            .filter { it.achievementId.startsWith("region_district_") }

        val a1 = districtAchievements.find { it.achievementId == "region_district_1" }
        assertEquals(AchievementStatus.UNLOCKED, a1?.status)
        assertTrue(result.newlyUnlocked.any { it.achievementId == "region_district_1" })
    }

    @Test
    fun evaluateAndSettle_unlocksProvinceAchievement() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)

        val result = service.evaluateAndSettle("u1")
        val a1 = achievementRepo.getUserAchievement("u1", "region_province_1")
        assertEquals(AchievementStatus.UNLOCKED, a1?.status)
        assertTrue(result.scoreAdded > 0)
    }

    @Test
    fun evaluateAndSettle_addsScoreOnUnlock() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)

        val result = service.evaluateAndSettle("u1")
        assertTrue(result.scoreAdded > 0)

        val score = userScoreRepo.getCurrentScore("u1")
        assertEquals(result.scoreAdded, score)
    }

    @Test
    fun evaluateAndSettle_detectsLevelChange() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)
        footprintRepo.markFootprint("u1", "110000", FootprintLevel.PASS_BY)

        val result = service.evaluateAndSettle("u1")
        // At least region_province_1 should unlock (1 province visited)
        assertTrue(result.newlyUnlocked.size >= 1)
        assertTrue(result.scoreAdded > 0)
    }

    @Test
    fun evaluateAndSettle_doesNotReUnlock() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)
        service.evaluateAndSettle("u1")

        val result2 = service.evaluateAndSettle("u1")
        assertEquals(0, result2.newlyUnlocked.size)
    }

    @Test
    fun addFootprintScore_passBy_adds10() {
        userScoreRepo.ensureUserScore("u1")
        service.addFootprintScore("u1", FootprintLevel.PASS_BY)
        assertEquals(10, userScoreRepo.getCurrentScore("u1"))
    }

    @Test
    fun addFootprintScore_shortVisit_adds20() {
        userScoreRepo.ensureUserScore("u1")
        service.addFootprintScore("u1", FootprintLevel.SHORT_VISIT)
        assertEquals(20, userScoreRepo.getCurrentScore("u1"))
    }

    @Test
    fun addFootprintScore_deep_adds50() {
        userScoreRepo.ensureUserScore("u1")
        service.addFootprintScore("u1", FootprintLevel.DEEP)
        assertEquals(50, userScoreRepo.getCurrentScore("u1"))
    }

    @Test
    fun getProvinceConquestInfo_returnsProvinces() {
        footprintRepo.markFootprint("u1", "510000", FootprintLevel.PASS_BY)
        achievementRepo.initUserAchievements("u1")

        val info = service.getProvinceConquestInfo("u1")
        assertTrue(info.isNotEmpty())
        val sichuan = info.find { it.provinceId == "510000" }
        assertTrue(sichuan!!.visitedCities >= 0)
    }

    @Test
    fun evaluateAndSettle_scenic5aAchievement() {
        attractionRepo.insertAttraction(Attraction("a1", "故宫", "110101", AttractionLevel.A5, 39.9, 116.4, null))
        footprintRepo.markAttractionVisit("u1", "a1", "110101", FootprintLevel.DEEP)

        service.evaluateAndSettle("u1")
        val a5a1 = achievementRepo.getUserAchievement("u1", "scenic_5a_1")
        assertEquals(AchievementStatus.UNLOCKED, a5a1?.status)
    }

    @Test
    fun evaluateAndSettle_atlasAchievement() {
        attractionRepo.insertAttraction(Attraction("attr001", "故宫博物院", "110101", AttractionLevel.A5, 39.9, 116.4, null))
        AtlasSeeder.seedAtlas(atlasRepo)
        footprintRepo.markAttractionVisit("u1", "attr001", "110101", FootprintLevel.DEEP)

        service.evaluateAndSettle("u1")
        val heritage1 = achievementRepo.getUserAchievement("u1", "atlas_heritage_1")
        assertEquals(AchievementStatus.UNLOCKED, heritage1?.status)
    }
}
