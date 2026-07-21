package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncChangeWriterTest {

    @Test
    fun enqueueUpsert_requestsUploadAfterQueueWrite() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val trigger = RecordingSyncUploadTrigger()
        val writer = SyncChangeWriter(database, trigger)

        writer.enqueueUpsert(SyncEntityType.APP_SETTING, "map_theme", """{"key":"map_theme","value":"paper"}""", 1_000L)

        assertEquals(1L, database.syncQueueQueries.countPending().executeAsOne())
        assertEquals(1, trigger.requestCount)
    }
}

private class RecordingSyncUploadTrigger : SyncUploadTrigger {
    var requestCount: Int = 0

    override fun requestUpload() {
        requestCount += 1
    }
}
