package com.mapchina.ui.achievement

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.AchievementCategory
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.service.AchievementSeeder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AchievementViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var achievementRepo: AchievementRepository
    private lateinit var userScoreRepo: UserScoreRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        achievementRepo = AchievementRepository(database)
        userScoreRepo = UserScoreRepository(database)
        AchievementSeeder.seedAchievements(achievementRepo)
    }

    @Test
    fun refresh_loadsAllAchievements() {
        val vm = AchievementViewModel(achievementRepo, userScoreRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertTrue(ui.allAchievements.isNotEmpty())
        assertTrue(ui.totalCount > 0)
    }

    @Test
    fun refresh_noUnlockedInitially() {
        val vm = AchievementViewModel(achievementRepo, userScoreRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertEquals(0, ui.unlockedCount)
    }

    @Test
    fun refresh_categorizesRegionAndScenic() {
        val vm = AchievementViewModel(achievementRepo, userScoreRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertTrue(ui.regionAchievements.isNotEmpty())
        assertTrue(ui.scenicAchievements.isNotEmpty())
    }

    @Test
    fun refresh_nextTargetIsClosestLocked() {
        val vm = AchievementViewModel(achievementRepo, userScoreRepo)
        vm.refresh()
        val ui = vm.ui.value
        assertNotNull(ui.nextTarget)
        assertEquals(AchievementCategory.REGION, ui.nextTarget!!.definition.category)
    }
}
