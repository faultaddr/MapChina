package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.remote.MapChinaApiClient
import com.mapchina.domain.service.AuthService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SyncUploadTrigger {
    fun requestUpload()
}

class SyncCoordinator(
    private val authService: AuthService,
    private val apiClient: MapChinaApiClient,
    private val syncEngine: SyncEngine,
    private val database: MapChinaDatabase,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val uploadRetryDelayMillis: Long = DEFAULT_UPLOAD_RETRY_DELAY_MS,
    private val maxUploadAttempts: Int = DEFAULT_MAX_UPLOAD_ATTEMPTS
) : SyncUploadTrigger {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val syncMutex = Mutex()

    override fun requestUpload() {
        scope.launch {
            syncAfterLocalChangeWithRetry()
        }
    }

    fun requestFullSync() {
        scope.launch {
            syncOnLogin()
        }
    }

    suspend fun syncAfterLocalChange(): Boolean = syncMutex.withLock {
        val token = authService.accessToken?.takeIf { it.isNotBlank() } ?: return@withLock false
        apiClient.accessToken = token
        syncEngine.pushChanges()
    }

    internal suspend fun syncAfterLocalChangeWithRetry(): Boolean {
        if (!hasAccessToken()) return false

        val attempts = maxUploadAttempts.coerceAtLeast(1)
        repeat(attempts) { attempt ->
            if (syncAfterLocalChange()) return true
            val hasAnotherAttempt = attempt < attempts - 1
            if (hasAnotherAttempt && hasAccessToken()) {
                delay(uploadRetryDelayMillis.coerceAtLeast(0L))
            }
        }
        return false
    }

    suspend fun syncOnLogin(): Boolean = syncMutex.withLock {
        val token = authService.accessToken?.takeIf { it.isNotBlank() } ?: return@withLock false
        apiClient.accessToken = token

        val pushed = syncEngine.pushChanges()
        if (!pushed) return@withLock false

        val since = database.appSettingQueries
            .selectByKey(LAST_PULL_TIMESTAMP_KEY)
            .executeAsOneOrNull()
            ?.value_
            ?.toLongOrNull()
            ?: 0L
        val serverTime = syncEngine.pullChanges(since) ?: return@withLock false
        database.appSettingQueries.upsert(LAST_PULL_TIMESTAMP_KEY, serverTime.toString())
        true
    }

    fun shutdown() {
        scope.cancel()
    }

    private fun hasAccessToken(): Boolean =
        !authService.accessToken.isNullOrBlank()

    companion object {
        const val LAST_PULL_TIMESTAMP_KEY = "sync.last_pull_timestamp"
        private const val DEFAULT_UPLOAD_RETRY_DELAY_MS = 15_000L
        private const val DEFAULT_MAX_UPLOAD_ATTEMPTS = 3
    }
}
