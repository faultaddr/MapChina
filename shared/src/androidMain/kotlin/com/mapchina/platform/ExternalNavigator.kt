package com.mapchina.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

actual class ExternalNavigator(private val context: Context) {

    actual fun navigateToAmap(latitude: Double, longitude: Double, name: String) {
        val amapUri = Uri.parse(
            "androidamap://navi?sourceApplication=MapChina&poiname=${Uri.encode(name)}&lat=$latitude&lon=$longitude&dev=0"
        )
        val amapIntent = Intent(Intent.ACTION_VIEW, amapUri).apply {
            setPackage("com.autonavi.minimap")
        }

        if (amapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(amapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val webUri = Uri.parse(
                "https://uri.amap.com/navigation?to=$longitude,$latitude,${Uri.encode(name)}&mode=bus&src=MapChina"
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
