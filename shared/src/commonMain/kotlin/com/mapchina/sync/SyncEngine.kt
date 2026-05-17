package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.FootprintLevel
import com.mapchina.domain.service.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface RemoteSyncClient {
    suspend fun pushChange(entityType: String, entityId: String, operation: String, payload: String): Boolean
    suspend fun pullDelta(sinceTimestamp: Long): SyncDelta
}

data class SyncDelta(
    val footprints: List<FootprintDto> = emptyList(),
    val timestamp: Long = 0L
)

class SyncEngine(
    private val apiClient: RemoteSyncClient,
    private val database: MapChinaDatabase,
    private val maxRetries: Int = 5
) {
    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun pushChanges() {
        val pending = database.syncQueueQueries.selectPending(50).executeAsList()
        for (item in pending) {
            try {
                val success = apiClient.pushChange(
                    item.entity_type, item.entity_id, item.operation, item.payload
                )
                if (success) {
                    database.syncQueueQueries.deleteById(item.id)
                } else {
                    handleRetry(item.id, item.retry_count)
                }
            } catch (_: Exception) {
                handleRetry(item.id, item.retry_count)
            }
        }
    }

    suspend fun pullChanges(sinceTimestamp: Long) {
        _status.value = SyncStatus.SYNCING
        try {
            val delta = apiClient.pullDelta(sinceTimestamp)
            mergeFootprints(delta.footprints)
            _status.value = SyncStatus.SYNCED
        } catch (_: Exception) {
            _status.value = SyncStatus.OFFLINE
        }
    }

    fun enqueueChange(entityType: String, entityId: String, operation: String, payload: String) {
        database.syncQueueQueries.insertPending(
            entityType, entityId, operation, payload,
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
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

    private fun handleRetry(itemId: Long, currentRetryCount: Long) {
        if (currentRetryCount >= maxRetries) {
            database.syncQueueQueries.deleteById(itemId)
        } else {
            database.syncQueueQueries.incrementRetry(itemId)
        }
    }
}
