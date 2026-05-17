package com.mapchina.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mapchina.di.appModule
import com.mapchina.di.platformModule
import com.mapchina.di.seedData
import com.mapchina.ui.MapChinaApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule, platformModule)
        }
        seedData()
        enableEdgeToEdge()
        setContent {
            MapChinaApp()
        }
    }
}
