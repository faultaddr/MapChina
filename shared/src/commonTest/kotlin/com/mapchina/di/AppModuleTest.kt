package com.mapchina.di

import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.InMemoryDatabaseDriverFactory
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.AuthService
import com.mapchina.domain.service.FootprintService
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppModuleTest {

    private val testPlatformModule = module {
        single<SqlDriver> { InMemoryDatabaseDriverFactory().createDriver() }
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
}
