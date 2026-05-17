package com.mapchina.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.mapchina.di.initKoin
import com.mapchina.ui.MapChinaApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        MapChinaApp()
    }
}
