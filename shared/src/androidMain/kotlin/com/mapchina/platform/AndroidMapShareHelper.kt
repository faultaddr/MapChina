package com.mapchina.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class AndroidMapShareHelper(
    private val context: Context
) : MapShareHelper {

    override fun captureAndShare() {
        val activity = context as? Activity ?: return
        val view = activity.window.decorView.rootView
        val bitmap = captureView(view) ?: return
        shareBitmap(bitmap, activity)
    }

    private fun captureView(view: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun shareBitmap(bitmap: Bitmap, activity: Activity) {
        val cachePath = File(activity.cacheDir, "shares")
        cachePath.mkdirs()
        val file = File(cachePath, "map_china_share.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(shareIntent, "分享地图"))
    }
}
