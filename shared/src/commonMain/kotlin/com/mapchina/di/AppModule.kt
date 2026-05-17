package com.mapchina.di

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.AuthService
import com.mapchina.domain.service.FootprintService
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
}

expect val platformModule: Module
