package com.mapchina.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SyncEntityType {
    FOOTPRINT,
    ATTRACTION_VISIT,
    CARVING,
    CUSTOM_ATTRACTION,
    JOURNAL,
    JOURNAL_PHOTO,
    JOURNAL_TRACK_POINT,
    APP_SETTING;

    companion object {
        fun fromRaw(value: String): SyncEntityType? =
            entries.firstOrNull { it.name == value.trim().uppercase() }
    }
}

@Serializable
enum class SyncOperation {
    UPSERT,
    DELETE;

    companion object {
        fun fromRaw(value: String): SyncOperation? =
            entries.firstOrNull { it.name == value.trim().uppercase() }
    }
}

@Serializable
data class SyncQueueItem(
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val updatedAt: Long,
    val deleted: Boolean = false
)

@Serializable
data class SyncPushRequest(val items: List<SyncQueueItem>)

@Serializable
data class SyncPushResponse(
    val accepted: Int = 0,
    val serverTime: Long = 0L
)

@Serializable
data class SyncDelta(
    val items: List<SyncQueueItem> = emptyList(),
    @SerialName("serverTime") val timestamp: Long = 0L
)

@Serializable
data class FootprintSyncPayload(
    val userId: String,
    val regionId: String,
    val level: String,
    val timestamp: Long
)

@Serializable
data class AttractionVisitSyncPayload(
    val userId: String,
    val attractionId: String,
    val level: String,
    val timestamp: Long,
    val note: String? = null
)

@Serializable
data class CarvingSyncPayload(
    val id: String,
    val userId: String,
    val regionId: String,
    val regionName: String,
    val imagePath: String? = null,
    val strokeData: String? = null,
    val createdAt: Long,
    val attractionId: String? = null,
    val attractionName: String? = null,
    val previewAspectRatio: Double? = null
)

@Serializable
data class CustomAttractionSyncPayload(
    val id: String,
    val userId: String,
    val name: String,
    val regionId: String,
    val level: String = "CUSTOM",
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class JournalSyncPayload(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val regionId: String? = null,
    val attractionId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class JournalPhotoSyncPayload(
    val id: String,
    val journalId: String,
    val localPath: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val takenAt: Long? = null,
    val sortOrder: Long = 0L
)

@Serializable
data class JournalTrackPointSyncPayload(
    val id: String,
    val journalId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Double = 0.0,
    val timestamp: Long,
    val sortOrder: Long = 0L
)

@Serializable
data class AppSettingSyncPayload(
    val key: String,
    val value: String
)
