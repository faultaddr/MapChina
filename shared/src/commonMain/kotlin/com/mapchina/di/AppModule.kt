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
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.JournalRepository
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AchievementSeeder
import com.mapchina.domain.service.AtlasSeeder
import com.mapchina.domain.service.JournalService
import com.mapchina.domain.service.AtlasService
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.journal.JournalViewModel
import com.mapchina.platform.PhotoPicker
import com.mapchina.platform.DevicePhotoProvider
import com.mapchina.ui.achievement.AtlasViewModel
import com.mapchina.ui.achievement.ProvinceConquestViewModel
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
    single { AtlasRepository(get()) }
    single { UserScoreRepository(get()) }
    single { SettingsRepository(get()) }
    single { AchievementService(get(), get(), get(), get(), get(), get()) }
    single { AtlasService(get(), get()) }

    single { JournalRepository(get()) }
    single { JournalService(get(), get(), get()) }

    single { MapViewModel(get(), get(), get(), get(), getOrNull<com.mapchina.data.remote.BoundaryLoader>(), get(), getOrNull<DevicePhotoProvider>()) }
    single { AttractionViewModel(get(), get(), get(), getOrNull<AttractionDetailProvider>(), get()) }
    single { StatsViewModel(get(), get(), get(), get()) }
    single { ProfileViewModel(get(), get()) }
    single { AchievementViewModel(get(), get()) }
    single { ProvinceConquestViewModel(get(), get()) }
    single { AtlasViewModel(get(), get(), get()) }
    single { JournalViewModel(get(), get(), get(), getOrNull<PhotoPicker>()) }
}

expect val platformModule: Module

fun seedDataAsync(regionRepo: RegionRepository, attractionRepo: AttractionRepository, boundaryLoader: BoundaryLoader? = null, achievementRepo: AchievementRepository? = null, atlasRepo: AtlasRepository? = null) {
    DataSeeder.seedRegions(regionRepo, boundaryLoader)
    DataSeeder.seedAttractions(attractionRepo, boundaryLoader)
    DataSeeder.seedBoundaries(regionRepo, boundaryLoader)
    if (achievementRepo != null) {
        AchievementSeeder.seedAchievements(achievementRepo)
    }
    if (atlasRepo != null) {
        AtlasSeeder.seedAtlas(atlasRepo)
    }
}
