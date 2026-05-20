package com.mapchina.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedDataAsync
import com.mapchina.ui.MapChinaApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule, platformModule)
        }

        enableEdgeToEdge()
        setContent {
            MapChinaApp()
        }

        // 所有数据 seed 都在后台线程执行
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
