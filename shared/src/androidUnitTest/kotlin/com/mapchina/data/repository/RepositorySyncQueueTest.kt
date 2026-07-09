package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.Carving
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Journal
import com.mapchina.sync.SyncChangeWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositorySyncQueueTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var syncChangeWriter: SyncChangeWriter

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        syncChangeWriter = SyncChangeWriter(database)
    }

    @Test
    fun markFootprint_enqueuesFootprintUpsert() {
        val repository = FootprintRepository(database, syncChangeWriter)

        repository.markFootprint("u1", "510000", FootprintLevel.DEEP)

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("FOOTPRINT", item.entity_type)
        assertEquals("u1:510000", item.entity_id)
        assertEquals("UPSERT", item.operation)
        assertTrue(item.payload.contains(""""regionId":"510000""""))
        assertTrue(item.payload.contains(""""level":"DEEP""""))
    }

    @Test
    fun insertCarving_enqueuesCarvingUpsert() {
        val repository = CarvingRepository(database, syncChangeWriter)

        repository.insertCarving(
            Carving(
                id = "carving-1",
                userId = "u1",
                regionId = "510000",
                regionName = "四川",
                imagePath = "/local/carving.png",
                strokeData = "[]",
                createdAt = 2_000L,
                attractionId = "attr-1",
                attractionName = "青城山",
                previewAspectRatio = 1.4f
            )
        )

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("CARVING", item.entity_type)
        assertEquals("carving-1", item.entity_id)
        assertEquals("UPSERT", item.operation)
        assertTrue(item.payload.contains(""""regionName":"四川""""))
        assertTrue(item.payload.contains(""""previewAspectRatio":1.4"""))
    }

    @Test
    fun insertJournal_enqueuesJournalUpsert() {
        val repository = JournalRepository(database, syncChangeWriter)

        repository.insertJournal(
            Journal(
                id = "journal-1",
                userId = "u1",
                title = "川西小记",
                description = "山路和云",
                regionId = "510000",
                attractionId = null,
                startTime = 1_000L,
                endTime = null,
                createdAt = 1_000L,
                updatedAt = 2_000L
            )
        )

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("JOURNAL", item.entity_type)
        assertEquals("journal-1", item.entity_id)
        assertEquals("UPSERT", item.operation)
        assertTrue(item.payload.contains(""""title":"川西小记""""))
        assertTrue(item.payload.contains(""""updatedAt":2000"""))
    }

    @Test
    fun insertAttraction_doesNotEnqueuePublicSeedAttraction() {
        val repository = AttractionRepository(database, syncChangeWriter)

        repository.insertAttraction(
            Attraction(
                id = "attr-public",
                name = "故宫博物院",
                regionId = "110101",
                level = AttractionLevel.A5,
                latitude = 39.9163,
                longitude = 116.3972,
                description = "紫禁城"
            )
        )

        assertEquals(0L, database.syncQueueQueries.countPending().executeAsOne())
    }

    @Test
    fun insertCustomAttraction_enqueuesCustomAttractionUpsert() {
        val repository = AttractionRepository(database, syncChangeWriter)

        repository.insertCustomAttraction(
            Attraction(
                id = "custom-1",
                name = "巷口老茶馆",
                regionId = "510000",
                level = AttractionLevel.CUSTOM,
                latitude = 30.66,
                longitude = 104.06,
                description = "自己补录的地方",
                imageUrl = "/local/tea.jpg",
                isCustom = true,
                userId = "u1"
            )
        )

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("CUSTOM_ATTRACTION", item.entity_type)
        assertEquals("custom-1", item.entity_id)
        assertEquals("UPSERT", item.operation)
        assertTrue(item.payload.contains(""""name":"巷口老茶馆""""))
        assertTrue(item.payload.contains(""""userId":"u1""""))
    }

    @Test
    fun setString_enqueuesAppSettingUpsert() {
        val repository = SettingsRepository(database, syncChangeWriter)

        repository.setString("map.style", "ink")

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("APP_SETTING", item.entity_type)
        assertEquals("map.style", item.entity_id)
        assertEquals("UPSERT", item.operation)
        assertTrue(item.payload.contains(""""key":"map.style""""))
        assertTrue(item.payload.contains(""""value":"ink""""))
    }

    @Test
    fun removeAttractionVisit_enqueuesDeleteWithIdentityPayload() {
        val repository = FootprintRepository(database, syncChangeWriter)

        repository.removeAttractionVisit("u1", "attr-1")

        val item = database.syncQueueQueries.selectPending(10).executeAsOne()
        assertEquals("ATTRACTION_VISIT", item.entity_type)
        assertEquals("u1:attr-1", item.entity_id)
        assertEquals("DELETE", item.operation)
        assertTrue(item.payload.contains(""""userId":"u1""""))
        assertTrue(item.payload.contains(""""attractionId":"attr-1""""))
    }
}
