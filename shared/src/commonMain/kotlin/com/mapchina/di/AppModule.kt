package com.mapchina.di

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.remote.AttractionDetailProvider
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.data.remote.DataSeeder
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.AuthService
import com.mapchina.domain.service.FootprintService
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AchievementSeeder
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.map.MapViewModel
import com.mapchina.ui.profile.ProfileViewModel
import com.mapchina.ui.stats.StatsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule = module {
    single { MapChinaDatabase(get()) }
    single { RegionRepository(get()) }
    single { AttractionRepository(get()) }
    single { FootprintRepository(get()) }
    single { FootprintService(get(), get(), get()) }
    single { AttractionService(get()) }
    single { AuthService() }

    single { AchievementRepository(get()) }
    single { UserScoreRepository(get()) }
    single { AchievementService(get(), get(), get(), get()) }

    factory { MapViewModel(get(), get(), get(), get(), getOrNull<com.mapchina.data.remote.BoundaryLoader>()) }
    factory { AttractionViewModel(get(), get(), get(), getOrNull<AttractionDetailProvider>()) }
    factory { StatsViewModel(get(), get(), get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { AchievementViewModel(get(), get(), get()) }
}

expect val platformModule: Module

fun seedDataAsync(regionRepo: RegionRepository, attractionRepo: AttractionRepository, boundaryLoader: BoundaryLoader? = null, achievementRepo: AchievementRepository? = null) {
    DataSeeder.seedRegions(regionRepo, boundaryLoader)
    DataSeeder.seedAttractions(attractionRepo, boundaryLoader)
    DataSeeder.seedBoundaries(regionRepo, boundaryLoader)
    if (achievementRepo != null) {
        AchievementSeeder.seedAchievements(achievementRepo)
    }
}
