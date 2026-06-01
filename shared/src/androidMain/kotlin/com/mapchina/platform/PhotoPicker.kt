package com.mapchina.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

actual class PhotoPicker actual constructor() {

    actual fun pickPhotos(onResult: (List<String>) -> Unit) {
        pendingCallback = onResult
        val activity = activityRef ?: return
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        @Suppress("DEPRECATION")
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    actual fun isAvailable(): Boolean = activityRef != null

    companion object {
        private const val REQUEST_CODE = 10042
        private var activityRef: Activity? = null
        private var pendingCallback: ((List<String>) -> Unit)? = null

        fun setActivity(activity: Activity) {
            activityRef = activity
        }

        fun deliverResult(paths: List<String>) {
            pendingCallback?.invoke(paths)
            pendingCallback = null
        }

        fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, context: Context): List<String> {
            if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK) return emptyList()
            val paths = mutableListOf<String>()
            val clip = data?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    copyToInternal(context, clip.getItemAt(i).uri)?.let { paths.add(it) }
                }
            } else {
                data?.data?.let { uri -> copyToInternal(context, uri)?.let { paths.add(it) } }
            }
            return paths
        }

        private fun copyToInternal(context: Context, uri: Uri): String? {
            return try {
                val dir = File(context.filesDir, "journal_photos")
                if (!dir.exists()) dir.mkdirs()
                val fileName = "photo_${System.currentTimeMillis()}_${uri.hashCode().toUInt()}.jpg"
                val outFile = File(dir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                "file://${outFile.absolutePath}"
            } catch (_: Exception) {
                null
            }
        }
    }
}
