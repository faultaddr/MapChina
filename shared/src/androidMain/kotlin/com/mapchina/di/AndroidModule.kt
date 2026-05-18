package com.mapchina.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.DatabaseDriverFactory
import com.mapchina.data.remote.AttractionDetailProvider
import com.mapchina.data.remote.BoundaryLoader
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> { DatabaseDriverFactory(get()).createDriver() }
    single { BoundaryLoader(get<Context>()) }
    single { AttractionDetailProvider(get<Context>()) }
}
