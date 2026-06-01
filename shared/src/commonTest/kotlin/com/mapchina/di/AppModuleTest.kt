package com.mapchina.di

import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AtlasService
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.AuthService
import com.mapchina.domain.service.FootprintService
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.achievement.AtlasViewModel
import com.mapchina.ui.achievement.ProvinceConquestViewModel
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.map.MapViewModel
import com.mapchina.ui.profile.ProfileViewModel
import com.mapchina.ui.stats.StatsViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppModuleTest {

    private val testPlatformModule = module {
        single<SqlDriver> { TestDatabaseDriverFactory().createDriver() }
    }

    @BeforeTest
    fun setup() {
        stopKoin()
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun appModule_resolvesAllServices() {
        val koin = startKoin {
            modules(appModule, testPlatformModule)
        }.koin
        assertNotNull(koin.get<FootprintService>())
        assertNotNull(koin.get<AttractionService>())
        assertNotNull(koin.get<AuthService>())
        assertNotNull(koin.get<FootprintRepository>())
        assertNotNull(koin.get<RegionRepository>())
    }

    @Test
    fun appModule_resolvesAchievementServices() {
        val koin = startKoin {
            modules(appModule, testPlatformModule)
        }.koin
        assertNotNull(koin.get<AchievementRepository>())
        assertNotNull(koin.get<AtlasRepository>())
        assertNotNull(koin.get<UserScoreRepository>())
        assertNotNull(koin.get<AchievementService>())
        assertNotNull(koin.get<AtlasService>())
    }

    @Test
    fun appModule_resolvesAllViewModels() {
        val koin = startKoin {
            modules(appModule, testPlatformModule)
        }.koin
        assertNotNull(koin.get<MapViewModel>())
        assertNotNull(koin.get<AttractionViewModel>())
        assertNotNull(koin.get<StatsViewModel>())
        assertNotNull(koin.get<ProfileViewModel>())
        assertNotNull(koin.get<AchievementViewModel>())
        assertNotNull(koin.get<ProvinceConquestViewModel>())
        assertNotNull(koin.get<AtlasViewModel>())
    }
}
