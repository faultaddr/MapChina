package com.mapchina.platform

expect class PhotoPicker() {
    fun pickPhotos(onResult: (List<String>) -> Unit)
    fun isAvailable(): Boolean
}
