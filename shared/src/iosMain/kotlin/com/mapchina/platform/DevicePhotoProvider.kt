package com.mapchina.platform

actual class DevicePhotoProvider actual constructor() {
    actual fun getPhotosWithLocation(): List<DevicePhoto> = emptyList()
    actual fun isAvailable(): Boolean = false
}
