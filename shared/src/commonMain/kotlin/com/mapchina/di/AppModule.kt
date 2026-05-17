package com.mapchina.di

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.remote.DataSeeder
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.AuthService
import com.mapchina.domain.service.FootprintService
import com.mapchina.ui.attraction.AttractionViewModel
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
    single { FootprintService(get(), get()) }
    single { AttractionService(get()) }
    single { AuthService() }

    factory { MapViewModel(get(), get(), get()) }
    factory { AttractionViewModel(get(), get(), get()) }
    factory { StatsViewModel(get()) }
    factory { ProfileViewModel(get()) }
}

expect val platformModule: Module

fun seedData() {
    val koin = org.koin.core.context.GlobalContext.get()
    val regionRepo = koin.get<RegionRepository>()
    val attractionRepo = koin.get<AttractionRepository>()
    DataSeeder.seedRegions(regionRepo)
    DataSeeder.seedAttractions(attractionRepo)
}
