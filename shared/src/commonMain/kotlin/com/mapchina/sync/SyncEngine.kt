package com.mapchina.sync

import kotlin.time.Clock

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.Sync_queue
import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.FootprintLevel
import com.mapchina.domain.service.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

interface RemoteSyncClient {
    suspend fun pushChanges(items: List<SyncQueueItem>): Boolean
    suspend fun pullDelta(sinceTimestamp: Long): SyncDelta
}

class SyncEngine(
    private val apiClient: RemoteSyncClient,
    private val database: MapChinaDatabase,
    private val maxRetries: Int = 5
) {
    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun pushChanges() {
        val pending = database.syncQueueQueries.selectPending(50).executeAsList()
        if (pending.isEmpty()) return

        val validRows = pending.mapNotNull { item ->
            val syncItem = item.toSyncQueueItem()
            if (syncItem == null) {
                handleRetry(item.id, item.retry_count)
                null
            } else {
                item to syncItem
            }
        }
        val items = validRows.map { it.second }
        if (items.isEmpty()) return

        val success = try {
            apiClient.pushChanges(items)
        } catch (_: Exception) {
            false
        }

        validRows.forEach { (item, _) ->
            if (success) {
                database.syncQueueQueries.deleteById(item.id)
            } else {
                handleRetry(item.id, item.retry_count)
            }
        }
    }

    suspend fun pullChanges(sinceTimestamp: Long) {
        _status.value = SyncStatus.SYNCING
        try {
            val delta = apiClient.pullDelta(sinceTimestamp)
            mergeItems(delta.items)
            _status.value = SyncStatus.SYNCED
        } catch (_: Exception) {
            _status.value = SyncStatus.OFFLINE
        }
    }

    fun enqueueChange(entityType: String, entityId: String, operation: String, payload: String) {
        database.syncQueueQueries.insertPending(
            entityType, entityId, operation, payload,
            Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun mergeItems(items: List<SyncQueueItem>) {
        items.sortedBy { it.updatedAt }.forEach { item ->
            when (item.entityType) {
                SyncEntityType.FOOTPRINT -> mergeFootprintItem(item)
                SyncEntityType.ATTRACTION_VISIT -> mergeAttractionVisitItem(item)
                SyncEntityType.CARVING -> mergeCarvingItem(item)
                SyncEntityType.JOURNAL -> mergeJournalItem(item)
                SyncEntityType.JOURNAL_PHOTO -> mergeJournalPhotoItem(item)
                SyncEntityType.JOURNAL_TRACK_POINT -> mergeJournalTrackPointItem(item)
                SyncEntityType.APP_SETTING -> mergeAppSettingItem(item)
            }
        }
    }

    private fun mergeFootprintItem(item: SyncQueueItem) {
        val payload = item.decodePayload<FootprintSyncPayload>() ?: return
        if (item.isDelete()) {
            val local = database.footprintQueries
                .selectByUserAndRegion(payload.userId, payload.regionId)
                .executeAsOneOrNull()
            if (local == null || item.updatedAt >= local.timestamp) {
                database.footprintQueries.deleteByUserAndRegion(payload.userId, payload.regionId)
            }
            return
        }

        val level = runCatching { FootprintLevel.valueOf(payload.level) }.getOrNull() ?: return
        mergeFootprints(listOf(FootprintDto(payload.userId, payload.regionId, level, payload.timestamp)))
    }

    private fun mergeFootprints(remoteFootprints: List<FootprintDto>) {
        for (remote in remoteFootprints) {
            val local = database.footprintQueries
                .selectByUserAndRegion(remote.userId, remote.regionId)
                .executeAsOneOrNull()

            val effectiveLevel = if (local != null) {
                val localLevel = FootprintLevel.valueOf(local.level)
                val resolved = SyncService.resolveFootprintConflict(
                    FootprintDto(remote.userId, remote.regionId, localLevel, local.timestamp),
                    remote
                )
                resolved.level
            } else {
                remote.level
            }

            database.footprintQueries.upsertFootprint(
                remote.userId, remote.regionId,
                effectiveLevel.name, remote.timestamp
            )
        }
    }

    private fun mergeAttractionVisitItem(item: SyncQueueItem) {
        val payload = item.decodePayload<AttractionVisitSyncPayload>() ?: return
        if (item.isDelete()) {
            val local = database.attractionVisitQueries
                .selectByUserAndAttraction(payload.userId, payload.attractionId)
                .executeAsOneOrNull()
            if (local == null || item.updatedAt >= local.timestamp) {
                database.attractionVisitQueries.deleteByUserAndAttraction(payload.userId, payload.attractionId)
            }
            return
        }

        val remoteLevel = runCatching { FootprintLevel.valueOf(payload.level) }.getOrNull() ?: return
        val local = database.attractionVisitQueries
            .selectByUserAndAttraction(payload.userId, payload.attractionId)
            .executeAsOneOrNull()
        val effectiveLevel = if (local != null) {
            val localLevel = runCatching { FootprintLevel.valueOf(local.level) }.getOrNull()
            if (localLevel != null && localLevel.ordinal >= remoteLevel.ordinal) local.level else remoteLevel.name
        } else {
            remoteLevel.name
        }
        val effectiveTimestamp = maxOf(local?.timestamp ?: 0L, payload.timestamp)
        val effectiveNote = if (local != null && local.timestamp > payload.timestamp) local.note else payload.note
        database.attractionVisitQueries.upsertVisit(
            payload.userId,
            payload.attractionId,
            effectiveLevel,
            effectiveTimestamp,
            effectiveNote
        )
    }

    private fun mergeCarvingItem(item: SyncQueueItem) {
        if (item.isDelete()) {
            val local = database.carvingQueries.selectById(item.entityId).executeAsOneOrNull()
            if (local == null || item.updatedAt >= local.created_at) {
                database.carvingQueries.deleteById(item.entityId)
            }
            return
        }

        val payload = item.decodePayload<CarvingSyncPayload>() ?: return
        val local = database.carvingQueries.selectById(payload.id).executeAsOneOrNull()
        if (local == null || item.updatedAt >= local.created_at) {
            database.carvingQueries.insertCarving(
                id = payload.id,
                user_id = payload.userId,
                region_id = payload.regionId,
                region_name = payload.regionName,
                image_path = payload.imagePath,
                stroke_data = payload.strokeData,
                created_at = payload.createdAt,
                attraction_id = payload.attractionId,
                attraction_name = payload.attractionName,
                preview_aspect_ratio = payload.previewAspectRatio
            )
        }
    }

    private fun mergeJournalItem(item: SyncQueueItem) {
        val payload = item.decodePayload<JournalSyncPayload>() ?: return
        val local = database.journalQueries.selectById(payload.id).executeAsOneOrNull()
        if (item.isDelete()) {
            if (local == null || item.updatedAt >= local.updated_at) {
                database.journalPhotoQueries.deleteByJournalId(payload.id)
                database.journalTrackPointQueries.deleteByJournalId(payload.id)
                database.journalQueries.deleteById(payload.id)
            }
            return
        }

        if (local == null || payload.updatedAt >= local.updated_at) {
            database.journalQueries.insertJournal(
                id = payload.id,
                user_id = payload.userId,
                title = payload.title,
                description = payload.description,
                region_id = payload.regionId,
                attraction_id = payload.attractionId,
                start_time = payload.startTime,
                end_time = payload.endTime,
                created_at = payload.createdAt,
                updated_at = payload.updatedAt
            )
        }
    }

    private fun mergeJournalPhotoItem(item: SyncQueueItem) {
        if (item.isDelete()) {
            database.journalPhotoQueries.deleteById(item.entityId)
            return
        }
        val payload = item.decodePayload<JournalPhotoSyncPayload>() ?: return
        database.journalPhotoQueries.insertPhoto(
            id = payload.id,
            journal_id = payload.journalId,
            local_path = payload.localPath,
            latitude = payload.latitude,
            longitude = payload.longitude,
            taken_at = payload.takenAt,
            sort_order = payload.sortOrder
        )
    }

    private fun mergeJournalTrackPointItem(item: SyncQueueItem) {
        if (item.isDelete()) return
        val payload = item.decodePayload<JournalTrackPointSyncPayload>() ?: return
        database.journalTrackPointQueries.insertTrackPoint(
            id = payload.id,
            journal_id = payload.journalId,
            latitude = payload.latitude,
            longitude = payload.longitude,
            altitude = payload.altitude,
            speed = payload.speed,
            timestamp = payload.timestamp,
            sort_order = payload.sortOrder
        )
    }

    private fun mergeAppSettingItem(item: SyncQueueItem) {
        if (item.isDelete()) return
        val payload = item.decodePayload<AppSettingSyncPayload>() ?: return
        database.appSettingQueries.upsert(payload.key, payload.value)
    }

    private fun handleRetry(itemId: Long, currentRetryCount: Long) {
        if (currentRetryCount >= maxRetries) {
            database.syncQueueQueries.deleteById(itemId)
        } else {
            database.syncQueueQueries.incrementRetry(itemId)
        }
    }

    private fun Sync_queue.toSyncQueueItem(): SyncQueueItem? {
        val entityType = SyncEntityType.fromRaw(entity_type) ?: return null
        val operation = SyncOperation.fromRaw(operation) ?: return null
        return SyncQueueItem(
            entityType = entityType,
            entityId = entity_id,
            operation = operation,
            payload = payload,
            updatedAt = created_at,
            deleted = operation == SyncOperation.DELETE
        )
    }

    private fun SyncQueueItem.isDelete(): Boolean =
        deleted || operation == SyncOperation.DELETE

    private inline fun <reified T> SyncQueueItem.decodePayload(): T? =
        runCatching { syncJson.decodeFromString<T>(payload) }.getOrNull()

    private companion object {
        val syncJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
