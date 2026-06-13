package com.mapchina.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

actual class ExternalNavigator(private val context: Context) {

    actual fun navigateToAmap(latitude: Double, longitude: Double, name: String) {
        val amapUri = Uri.parse(
            "androidamap://viewMap?sourceApplication=MapChina&poiname=${Uri.encode(name)}&lat=$latitude&lon=$longitude&dev=0"
        )
        val amapIntent = Intent(Intent.ACTION_VIEW, amapUri).apply {
            setPackage("com.autonavi.minimap")
        }

        if (amapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(amapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val webUri = Uri.parse(
                "https://uri.amap.com/marker?position=$longitude,$latitude&name=${Uri.encode(name)}&src=MapChina"
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
