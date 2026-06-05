package com.mapchina.platform

data class DevicePhoto(
    val id: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val dateTaken: Long
)

enum class PhotoResult {
    SUCCESS,
    NO_PERMISSION,
    NO_PHOTOS_WITH_LOCATION
}

expect class DevicePhotoProvider() {
    fun getPhotosWithLocation(): List<DevicePhoto>
    fun isAvailable(): Boolean
    fun checkPermission(): PhotoResult
}
