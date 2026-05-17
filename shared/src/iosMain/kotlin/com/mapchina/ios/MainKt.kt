package com.mapchina.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedData
import com.mapchina.ui.MapChinaApp
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin {
        modules(appModule, platformModule)
    }
    seedData()
    return ComposeUIViewController {
        MapChinaApp()
    }
}
