package com.mapchina.di

import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> { DatabaseDriverFactory(get()).createDriver() }
}
