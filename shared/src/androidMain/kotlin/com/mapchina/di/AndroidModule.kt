package com.mapchina.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.DatabaseDriverFactory
import com.mapchina.data.remote.AttractionDetailProvider
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.platform.PhotoPicker
import com.mapchina.platform.DevicePhotoProvider
import com.mapchina.platform.LocationProvider
import com.mapchina.platform.ExternalNavigator
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> { DatabaseDriverFactory(get()).createDriver() }
    single { BoundaryLoader(get<Context>()) }
    single { AttractionDetailProvider(get<Context>()) }
    single { PhotoPicker() }
    single { DevicePhotoProvider().apply { context = get<Context>() } }
    single { LocationProvider().apply { context = get<Context>() } }
    single { ExternalNavigator(get<Context>()) }
}
