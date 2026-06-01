package com.mapchina.android

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedDataAsync
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import kotlin.concurrent.thread

class MapChinaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        startKoin {
            androidContext(this@MapChinaApplication)
            modules(appModule, platformModule)
        }

        thread(name = "data-seed") {
            val koin = KoinPlatform.getKoin()
            val regionRepo = koin.get<RegionRepository>()
            val attractionRepo = koin.get<AttractionRepository>()
            val boundaryLoader = koin.get<BoundaryLoader>()
            val achievementRepo = koin.get<AchievementRepository>()
            val atlasRepo = koin.get<AtlasRepository>()
            seedDataAsync(regionRepo, attractionRepo, boundaryLoader, achievementRepo, atlasRepo)
        }
    }
}
