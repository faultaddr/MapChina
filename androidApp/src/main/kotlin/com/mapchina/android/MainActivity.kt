package com.mapchina.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mapchina.platform.PhotoPicker
import com.mapchina.ui.MapChinaApp

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhotoPicker.setActivity(this)
        enableEdgeToEdge()
        requestPhotoPermission()
        setContent {
            MapChinaApp()
        }
    }

    private fun requestPhotoPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionLauncher.launch(permissions)
        }
    }

    @Deprecated("Override for photo picker result handling")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val paths = PhotoPicker.handleActivityResult(requestCode, resultCode, data, this)
        if (paths.isNotEmpty()) {
            PhotoPicker.deliverResult(paths)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PhotoPicker.setActivity(this)
    }
}
