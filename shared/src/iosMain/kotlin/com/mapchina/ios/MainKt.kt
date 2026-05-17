package com.mapchina.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.mapchina.ui.MapChinaApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        MapChinaApp()
    }
}
