package com.mapchina.ui.achievement

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AchievementSeeder
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AtlasSeeder
import com.mapchina.domain.service.AtlasService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProvinceConquestViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var achievementService: AchievementService
    private lateinit var achievementRepo: AchievementRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val userScoreRepo = UserScoreRepository(database)
        val attractionRepo = AttractionRepository(database)
        val atlasRepo = AtlasRepository(database)
        achievementRepo = AchievementRepository(database)
        achievementService = AchievementService(achievementRepo, footprintRepo, userScoreRepo, attractionRepo, regionRepo, atlasRepo)

        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        AchievementSeeder.seedAchievements(achievementRepo)
    }

    @Test
    fun refresh_loadsProvinceConquestInfo() {
        val vm = ProvinceConquestViewModel(achievementService, achievementRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertTrue(ui.provinces.isNotEmpty())
    }

    @Test
    fun refresh_countsTotalProvinces() {
        val vm = ProvinceConquestViewModel(achievementService, achievementRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertTrue(ui.totalProvinceCount >= 2)
    }

    @Test
    fun loadProvinceDetail_showsAchievements() {
        val vm = ProvinceConquestViewModel(achievementService, achievementRepo)
        vm.loadProvinceDetail("51")
        val detail = vm.detailUi.value
        assertTrue(detail.provinceAchievements.isNotEmpty())
    }

    private fun footprintRepo(): FootprintRepository {
        return FootprintRepository(database)
    }
}
