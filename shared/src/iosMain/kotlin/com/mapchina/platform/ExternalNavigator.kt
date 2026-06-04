package com.mapchina.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class ExternalNavigator {
    actual fun navigateToAmap(latitude: Double, longitude: Double, name: String) {
        // Try Amap app first, fall back to web
        val amapUrl = "iosamap://navi?sourceApplication=MapChina&lat=$latitude&lon=$longitude&dev=0"
        val nsAmapUrl = NSURL.URLWithString(amapUrl)

        if (nsAmapUrl != null && UIApplication.sharedApplication.canOpenURL(nsAmapUrl)) {
            UIApplication.sharedApplication.openURL(nsAmapUrl)
        } else {
            val webUrl = NSURL.URLWithString(
                "https://uri.amap.com/navigation?to=$longitude,$latitude&mode=bus&src=MapChina"
            )
            webUrl?.let { UIApplication.sharedApplication.openURL(it) }
        }
    }
}
