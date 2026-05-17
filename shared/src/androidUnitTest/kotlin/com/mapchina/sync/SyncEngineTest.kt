package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.FootprintLevel
import com.mapchina.domain.service.SyncService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncEngineTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var syncEngine: SyncEngine
    private lateinit var fakeApiClient: FakeRemoteSyncClient

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        fakeApiClient = FakeRemoteSyncClient()
        syncEngine = SyncEngine(fakeApiClient, database)
    }

    @Test
    fun pushChanges_sendsPendingItems() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "510000", "UPSERT", """{"level":"DEEP"}""", 1000L)
        fakeApiClient.pushResult = true

        kotlinx.coroutines.runBlocking {
            syncEngine.pushChanges()
        }

        assertEquals(0, database.syncQueueQueries.countPending().executeAsOne().toInt())
        assertEquals(1, fakeApiClient.pushCallCount)
    }

    @Test
    fun pushChanges_onFailure_incrementsRetry() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "510000", "UPSERT", "{}", 1000L)
        fakeApiClient.pushResult = false

        kotlinx.coroutines.runBlocking {
            syncEngine.pushChanges()
        }

        val pending = database.syncQueueQueries.selectPending(10).executeAsList()
        assertEquals(1, pending.size)
        assertEquals(1L, pending.first().retry_count)
    }

    @Test
    fun pushChanges_maxRetries_deletesItem() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "510000", "UPSERT", "{}", 1000L)
        // Increment retry to max
        repeat(5) { database.syncQueueQueries.incrementRetry(1) }
        fakeApiClient.pushResult = false

        kotlinx.coroutines.runBlocking {
            syncEngine.pushChanges()
        }

        assertEquals(0, database.syncQueueQueries.countPending().executeAsOne().toInt())
    }

    @Test
    fun pullChanges_mergesRemoteFootprints() {
        database.footprintQueries.upsertFootprint("u1", "510000", "PASS_BY", 1000L)
        fakeApiClient.delta = SyncDelta(
            footprints = listOf(
                FootprintDto("u1", "510000", FootprintLevel.DEEP, 2000L)
            ),
            timestamp = 2000L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        val merged = database.footprintQueries.selectByUserAndRegion("u1", "510000").executeAsOne()
        assertEquals("DEEP", merged.level)
    }

    @Test
    fun pullChanges_remoteHasNoLocal_createsNew() {
        fakeApiClient.delta = SyncDelta(
            footprints = listOf(
                FootprintDto("u1", "110000", FootprintLevel.SHORT_VISIT, 1500L)
            ),
            timestamp = 1500L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        val footprint = database.footprintQueries.selectByUserAndRegion("u1", "110000").executeAsOne()
        assertEquals("SHORT_VISIT", footprint.level)
    }

    @Test
    fun pullChanges_onNetworkError_setsOfflineStatus() {
        fakeApiClient.shouldThrow = true

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        assertEquals(SyncStatus.OFFLINE, syncEngine.status.value)
    }

    @Test
    fun enqueueChange_addsToSyncQueue() {
        syncEngine.enqueueChange("FOOTPRINT", "510000", "UPSERT", """{"level":"DEEP"}""")
        val count = database.syncQueueQueries.countPending().executeAsOne()
        assertEquals(1L, count)
    }
}

class FakeRemoteSyncClient : RemoteSyncClient {
    var pushResult: Boolean = true
    var pushCallCount: Int = 0
    var delta: SyncDelta = SyncDelta()
    var shouldThrow: Boolean = false

    override suspend fun pushChange(entityType: String, entityId: String, operation: String, payload: String): Boolean {
        pushCallCount++
        return pushResult
    }

    override suspend fun pullDelta(sinceTimestamp: Long): SyncDelta {
        if (shouldThrow) throw RuntimeException("Network error")
        return delta
    }
}
