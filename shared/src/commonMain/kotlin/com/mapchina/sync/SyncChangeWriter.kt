package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Carving
import com.mapchina.domain.model.Journal
import com.mapchina.domain.model.JournalPhoto
import com.mapchina.domain.model.JournalTrackPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class SyncChangeWriter(private val database: MapChinaDatabase) {

    fun enqueueUpsert(entityType: SyncEntityType, entityId: String, payload: String, updatedAt: Long) {
        database.syncQueueQueries.insertPending(
            entityType.name,
            entityId,
            SyncOperation.UPSERT.name,
            payload,
            updatedAt
        )
    }

    fun enqueueDelete(entityType: SyncEntityType, entityId: String, updatedAt: Long) {
        enqueueDelete(entityType, entityId, "{}", updatedAt)
    }

    fun enqueueDelete(entityType: SyncEntityType, entityId: String, payload: String, updatedAt: Long) {
        database.syncQueueQueries.insertPending(
            entityType.name,
            entityId,
            SyncOperation.DELETE.name,
            payload,
            updatedAt
        )
    }

    fun enqueueFootprint(userId: String, regionId: String, level: String, timestamp: Long) {
        enqueueUpsert(
            SyncEntityType.FOOTPRINT,
            "$userId:$regionId",
            syncJson.encodeToString(FootprintSyncPayload(userId, regionId, level, timestamp)),
            timestamp
        )
    }

    fun enqueueFootprintDelete(userId: String, regionId: String, timestamp: Long) {
        enqueueDelete(
            SyncEntityType.FOOTPRINT,
            "$userId:$regionId",
            syncJson.encodeToString(FootprintSyncPayload(userId, regionId, "PASS_BY", timestamp)),
            timestamp
        )
    }

    fun enqueueAttractionVisit(userId: String, attractionId: String, level: String, timestamp: Long, note: String? = null) {
        enqueueUpsert(
            SyncEntityType.ATTRACTION_VISIT,
            "$userId:$attractionId",
            syncJson.encodeToString(AttractionVisitSyncPayload(userId, attractionId, level, timestamp, note)),
            timestamp
        )
    }

    fun enqueueAttractionVisitDelete(userId: String, attractionId: String, timestamp: Long) {
        enqueueDelete(
            SyncEntityType.ATTRACTION_VISIT,
            "$userId:$attractionId",
            syncJson.encodeToString(AttractionVisitSyncPayload(userId, attractionId, "PASS_BY", timestamp)),
            timestamp
        )
    }

    fun enqueueCarving(carving: Carving, updatedAt: Long = carving.createdAt) {
        enqueueUpsert(
            SyncEntityType.CARVING,
            carving.id,
            syncJson.encodeToString(
                CarvingSyncPayload(
                    id = carving.id,
                    userId = carving.userId,
                    regionId = carving.regionId,
                    regionName = carving.regionName,
                    imagePath = carving.imagePath,
                    strokeData = carving.strokeData,
                    createdAt = carving.createdAt,
                    attractionId = carving.attractionId,
                    attractionName = carving.attractionName,
                    previewAspectRatio = carving.previewAspectRatio?.toStableDouble()
                )
            ),
            updatedAt
        )
    }

    fun enqueueJournal(journal: Journal) {
        enqueueUpsert(
            SyncEntityType.JOURNAL,
            journal.id,
            syncJson.encodeToString(
                JournalSyncPayload(
                    id = journal.id,
                    userId = journal.userId,
                    title = journal.title,
                    description = journal.description,
                    regionId = journal.regionId,
                    attractionId = journal.attractionId,
                    startTime = journal.startTime,
                    endTime = journal.endTime,
                    createdAt = journal.createdAt,
                    updatedAt = journal.updatedAt
                )
            ),
            journal.updatedAt
        )
    }

    fun enqueueJournalDelete(journal: Journal?, id: String, updatedAt: Long = Clock.System.now().toEpochMilliseconds()) {
        val payload = if (journal != null) {
            syncJson.encodeToString(
                JournalSyncPayload(
                    id = journal.id,
                    userId = journal.userId,
                    title = journal.title,
                    description = journal.description,
                    regionId = journal.regionId,
                    attractionId = journal.attractionId,
                    startTime = journal.startTime,
                    endTime = journal.endTime,
                    createdAt = journal.createdAt,
                    updatedAt = updatedAt
                )
            )
        } else {
            "{}"
        }
        enqueueDelete(SyncEntityType.JOURNAL, id, payload, updatedAt)
    }

    fun enqueueJournalPhoto(photo: JournalPhoto, updatedAt: Long = photo.takenAt ?: Clock.System.now().toEpochMilliseconds()) {
        enqueueUpsert(
            SyncEntityType.JOURNAL_PHOTO,
            photo.id,
            syncJson.encodeToString(
                JournalPhotoSyncPayload(
                    id = photo.id,
                    journalId = photo.journalId,
                    localPath = photo.localPath,
                    latitude = photo.latitude,
                    longitude = photo.longitude,
                    takenAt = photo.takenAt,
                    sortOrder = photo.sortOrder
                )
            ),
            updatedAt
        )
    }

    fun enqueueJournalTrackPoint(point: JournalTrackPoint) {
        enqueueUpsert(
            SyncEntityType.JOURNAL_TRACK_POINT,
            point.id,
            syncJson.encodeToString(
                JournalTrackPointSyncPayload(
                    id = point.id,
                    journalId = point.journalId,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitude = point.altitude,
                    speed = point.speed,
                    timestamp = point.timestamp,
                    sortOrder = point.sortOrder
                )
            ),
            point.timestamp
        )
    }

    fun enqueueAppSetting(key: String, value: String, updatedAt: Long = Clock.System.now().toEpochMilliseconds()) {
        enqueueUpsert(
            SyncEntityType.APP_SETTING,
            key,
            syncJson.encodeToString(AppSettingSyncPayload(key, value)),
            updatedAt
        )
    }

    private companion object {
        val syncJson = Json { encodeDefaults = true }
    }
}

private fun Float.toStableDouble(): Double = toString().toDouble()
