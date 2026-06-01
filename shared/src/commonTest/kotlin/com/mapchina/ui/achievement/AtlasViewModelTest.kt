package com.mapchina.ui.achievement

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.service.AchievementSeeder
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AtlasSeeder
import com.mapchina.domain.service.AtlasService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AtlasViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var atlasService: AtlasService
    private lateinit var achievementRepo: AchievementRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val atlasRepo = AtlasRepository(database)
        val footprintRepo = FootprintRepository(database)
        achievementRepo = AchievementRepository(database)
        atlasService = AtlasService(atlasRepo, footprintRepo)
        val achievementService = AchievementService(achievementRepo, footprintRepo, UserScoreRepository(database), AttractionRepository(database), RegionRepository(database), atlasRepo)

        AtlasSeeder.seedAtlas(atlasRepo)
        AchievementSeeder.seedAchievements(achievementRepo)
    }

    @Test
    fun refresh_loadsAtlasProgress() {
        val vm = AtlasViewModel(atlasService, achievementService(), achievementRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertEquals(3, ui.atlasProgress.size)
        assertEquals(0, ui.completedAtlas)
    }

    @Test
    fun loadAtlasDetail_showsItems() {
        val vm = AtlasViewModel(atlasService, achievementService(), achievementRepo)
        vm.loadAtlasDetail("world_heritage")
        val detail = vm.detailUi.value
        assertNotNull(detail.progress)
        assertTrue(detail.items.isNotEmpty())
    }

    @Test
    fun loadAtlasDetail_showsAchievements() {
        val vm = AtlasViewModel(atlasService, achievementService(), achievementRepo)
        vm.loadAtlasDetail("world_heritage")
        val detail = vm.detailUi.value
        assertTrue(detail.atlasAchievements.isNotEmpty())
    }

    private fun achievementService(): AchievementService {
        return AchievementService(
            achievementRepo, FootprintRepository(database),
            UserScoreRepository(database), AttractionRepository(database),
            RegionRepository(database), AtlasRepository(database)
        )
    }
}
