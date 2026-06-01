package com.mapchina.platform

actual class PhotoPicker actual constructor() {
    actual fun pickPhotos(onResult: (List<String>) -> Unit) {
        onResult(emptyList())
    }

    actual fun isAvailable(): Boolean = false
}
