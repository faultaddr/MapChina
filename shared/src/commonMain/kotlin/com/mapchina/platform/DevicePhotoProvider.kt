package com.mapchina.platform

data class DevicePhoto(
    val id: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val dateTaken: Long
)

expect class DevicePhotoProvider() {
    fun getPhotosWithLocation(): List<DevicePhoto>
    fun isAvailable(): Boolean
}
