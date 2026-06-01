package com.mapchina.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedDataAsync
import com.mapchina.ui.MapChinaApp
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin {
        modules(appModule, platformModule)
    }
    val koin = KoinPlatform.getKoin()
    seedDataAsync(koin.get<RegionRepository>(), koin.get<AttractionRepository>(), achievementRepo = koin.get<AchievementRepository>(), atlasRepo = koin.get<AtlasRepository>())
    return ComposeUIViewController {
        MapChinaApp()
    }
}
