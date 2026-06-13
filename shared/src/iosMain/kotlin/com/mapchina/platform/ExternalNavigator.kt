package com.mapchina.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class ExternalNavigator {
    actual fun navigateToAmap(latitude: Double, longitude: Double, name: String) {
        val amapUrl = "iosamap://viewMap?sourceApplication=MapChina&poiname=$name&lat=$latitude&lon=$longitude&dev=0"
        val nsAmapUrl = NSURL.URLWithString(amapUrl)

        if (nsAmapUrl != null && UIApplication.sharedApplication.canOpenURL(nsAmapUrl)) {
            UIApplication.sharedApplication.openURL(nsAmapUrl)
        } else {
            val webUrl = NSURL.URLWithString(
                "https://uri.amap.com/marker?position=$longitude,$latitude&name=$name&src=MapChina"
            )
            webUrl?.let { UIApplication.sharedApplication.openURL(it) }
        }
    }

    actual fun openUrl(url: String) {
        NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
    }
}
