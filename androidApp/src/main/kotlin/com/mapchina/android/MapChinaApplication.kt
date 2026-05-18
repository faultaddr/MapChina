package com.mapchina.android

import android.app.Application
import com.amap.api.maps.MapsInitializer

class MapChinaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}
