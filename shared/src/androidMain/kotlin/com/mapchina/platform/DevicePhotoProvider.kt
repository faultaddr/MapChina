package com.mapchina.platform

import android.content.Context
import android.provider.MediaStore
import android.location.Location
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

actual class DevicePhotoProvider {
    var context: Context? = null

    actual fun getPhotosWithLocation(): List<DevicePhoto> {
        val ctx = context ?: return emptyList()
        val photos = mutableListOf<DevicePhoto>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        ctx.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val filePath = cursor.getString(dataCol) ?: continue
                val dateTaken = cursor.getLong(dateCol)
                val location = getLocationFromExif(ctx, id, filePath) ?: continue
                photos.add(DevicePhoto(
                    id = "photo_$id",
                    filePath = filePath,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    dateTaken = dateTaken
                ))
            }
        }
        return photos
    }

    actual fun isAvailable(): Boolean = context != null

    private fun getLocationFromExif(ctx: Context, id: Long, filePath: String): Location? {
        return try {
            val exif = try {
                val contentUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                ctx.contentResolver.openInputStream(contentUri)?.use { ExifInterface(it) }
            } catch (_: Exception) {
                null
            } ?: ExifInterface(filePath)

            val latLon = exif.latLong ?: return null
            if (latLon[0] == 0.0 && latLon[1] == 0.0) return null
            Location("exif").apply {
                latitude = latLon[0]
                longitude = latLon[1]
            }
        } catch (_: Exception) {
            null
        }
    }
}
