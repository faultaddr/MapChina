package com.mapchina.domain.model

data class Journal(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val regionId: String?,
    val attractionId: String?,
    val startTime: Long,
    val endTime: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class JournalPhoto(
    val id: String,
    val journalId: String,
    val localPath: String,
    val latitude: Double?,
    val longitude: Double?,
    val takenAt: Long?,
    val sortOrder: Long
)

data class JournalTrackPoint(
    val id: String,
    val journalId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Double,
    val timestamp: Long,
    val sortOrder: Long
)

data class JournalDetail(
    val journal: Journal,
    val photos: List<JournalPhoto>,
    val trackPoints: List<JournalTrackPoint>,
    val regionName: String?,
    val attractionName: String?
)
