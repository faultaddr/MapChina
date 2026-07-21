package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun pushChanges_sendsPendingItemsAsOneBatch() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "510000", "UPSERT", """{"level":"DEEP"}""", 1000L)
        database.syncQueueQueries.insertPending("CARVING", "carving-1", "UPSERT", """{"text":"山河"}""", 1100L)
        fakeApiClient.pushResult = true

        kotlinx.coroutines.runBlocking {
            syncEngine.pushChanges()
        }

        assertEquals(0, database.syncQueueQueries.countPending().executeAsOne().toInt())
        assertEquals(1, fakeApiClient.pushedBatches.size)
        assertEquals(listOf("510000", "carving-1"), fakeApiClient.pushedBatches.single().map { it.entityId })
    }

    @Test
    fun pushChanges_onBatchFailure_incrementsRetryForEveryItem() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "510000", "UPSERT", "{}", 1000L)
        database.syncQueueQueries.insertPending("CARVING", "carving-1", "UPSERT", "{}", 1100L)
        fakeApiClient.pushResult = false

        kotlinx.coroutines.runBlocking {
            syncEngine.pushChanges()
        }

        val pending = database.syncQueueQueries.selectPending(10).executeAsList()
        assertEquals(2, pending.size)
        assertEquals(listOf(1L, 1L), pending.map { it.retry_count })
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
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.FOOTPRINT,
                    entityId = "u1:510000",
                    operation = SyncOperation.UPSERT,
                    payload = """{"userId":"u1","regionId":"510000","level":"DEEP","timestamp":2000}""",
                    updatedAt = 2000L
                )
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
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.FOOTPRINT,
                    entityId = "u1:110000",
                    operation = SyncOperation.UPSERT,
                    payload = """{"userId":"u1","regionId":"110000","level":"SHORT_VISIT","timestamp":1500}""",
                    updatedAt = 1500L
                )
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
    fun pullChanges_mergesRemoteCarving() {
        fakeApiClient.delta = SyncDelta(
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.CARVING,
                    entityId = "carving-1",
                    operation = SyncOperation.UPSERT,
                    payload = """
                        {
                            "id":"carving-1",
                            "userId":"u1",
                            "regionId":"510000",
                            "regionName":"四川",
                            "imagePath":"/local/carving.png",
                            "strokeData":"[]",
                            "createdAt":2000,
                            "attractionId":"attr-1",
                            "attractionName":"青城山",
                            "previewAspectRatio":1.4
                        }
                    """.trimIndent(),
                    updatedAt = 2000L
                )
            ),
            timestamp = 2000L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        val carving = database.carvingQueries.selectById("carving-1").executeAsOne()
        assertEquals("四川", carving.region_name)
        assertEquals("/local/carving.png", carving.image_path)
        assertEquals(1.4, carving.preview_aspect_ratio)
    }

    @Test
    fun pullChanges_mergesRemoteCustomAttraction() {
        fakeApiClient.delta = SyncDelta(
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.CUSTOM_ATTRACTION,
                    entityId = "custom-1",
                    operation = SyncOperation.UPSERT,
                    payload = """
                        {
                            "id":"custom-1",
                            "userId":"u1",
                            "name":"巷口老茶馆",
                            "regionId":"510000",
                            "level":"CUSTOM",
                            "latitude":30.66,
                            "longitude":104.06,
                            "description":"自己补录的地方",
                            "imageUrl":"/local/tea.jpg"
                        }
                    """.trimIndent(),
                    updatedAt = 2_000L
                )
            ),
            timestamp = 2_000L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        val attraction = database.attractionQueries.selectById("custom-1").executeAsOne()
        assertEquals("巷口老茶馆", attraction.name)
        assertEquals("CUSTOM", attraction.level)
        assertEquals(1L, attraction.is_custom)
        assertEquals("u1", attraction.user_id)
    }

    @Test
    fun pullChanges_newerDeleteTombstoneRemovesLocalCarving() {
        database.carvingQueries.insertCarving(
            id = "carving-1",
            user_id = "u1",
            region_id = "510000",
            region_name = "四川",
            image_path = "/local/old.png",
            stroke_data = "[]",
            created_at = 1000L,
            attraction_id = null,
            attraction_name = null,
            preview_aspect_ratio = null
        )
        fakeApiClient.delta = SyncDelta(
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.CARVING,
                    entityId = "carving-1",
                    operation = SyncOperation.DELETE,
                    payload = "{}",
                    updatedAt = 2000L,
                    deleted = true
                )
            ),
            timestamp = 2000L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        assertNull(database.carvingQueries.selectById("carving-1").executeAsOneOrNull())
    }

    @Test
    fun pullChanges_journalDeleteTombstoneRemovesLocalChildren() {
        database.journalQueries.insertJournal(
            id = "journal-1",
            user_id = "u1",
            title = "旧游记",
            description = "",
            region_id = "510000",
            attraction_id = null,
            start_time = 1_000L,
            end_time = null,
            created_at = 1_000L,
            updated_at = 1_000L
        )
        database.journalPhotoQueries.insertPhoto(
            id = "photo-1",
            journal_id = "journal-1",
            local_path = "/local/photo.jpg",
            latitude = null,
            longitude = null,
            taken_at = null,
            sort_order = 0L
        )
        database.journalTrackPointQueries.insertTrackPoint(
            id = "point-1",
            journal_id = "journal-1",
            latitude = 30.0,
            longitude = 104.0,
            altitude = 0.0,
            speed = 0.0,
            timestamp = 1_000L,
            sort_order = 0L
        )
        fakeApiClient.delta = SyncDelta(
            items = listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.JOURNAL,
                    entityId = "journal-1",
                    operation = SyncOperation.DELETE,
                    payload = """
                        {
                            "id":"journal-1",
                            "userId":"u1",
                            "title":"旧游记",
                            "description":"",
                            "regionId":"510000",
                            "startTime":1000,
                            "createdAt":1000,
                            "updatedAt":2000
                        }
                    """.trimIndent(),
                    updatedAt = 2_000L,
                    deleted = true
                )
            ),
            timestamp = 2_000L
        )

        kotlinx.coroutines.runBlocking {
            syncEngine.pullChanges(0L)
        }

        assertNull(database.journalQueries.selectById("journal-1").executeAsOneOrNull())
        assertEquals(0, database.journalPhotoQueries.selectByJournalId("journal-1").executeAsList().size)
        assertEquals(0, database.journalTrackPointQueries.selectByJournalId("journal-1").executeAsList().size)
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
    val pushedBatches = mutableListOf<List<SyncQueueItem>>()
    var delta: SyncDelta = SyncDelta()
    var shouldThrow: Boolean = false

    override suspend fun pushChanges(items: List<SyncQueueItem>): Boolean {
        pushedBatches += items
        return pushResult
    }

    override suspend fun pullDelta(sinceTimestamp: Long): SyncDelta {
        if (shouldThrow) throw RuntimeException("Network error")
        return delta
    }
}
