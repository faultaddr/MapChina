package com.mapchina.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseTest {

    private lateinit var database: MapChinaDatabase

    @Test
    fun insertAndQueryRegion() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.regionQueries.insertRegion("110000", "北京市", "PROVINCE", null, null)
        val region = database.regionQueries.selectById("110000").executeAsOne()
        assertEquals("北京市", region.name)
        assertEquals("PROVINCE", region.level)
    }

    @Test
    fun queryRegionsByParentId() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.regionQueries.insertRegion("510000", "四川省", "PROVINCE", null, null)
        database.regionQueries.insertRegion("510100", "成都市", "CITY", "510000", null)
        database.regionQueries.insertRegion("510300", "自贡市", "CITY", "510000", null)
        val cities = database.regionQueries.selectByParentId("510000").executeAsList()
        assertEquals(2, cities.size)
    }

    @Test
    fun insertAndQueryFootprint() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.footprintQueries.upsertFootprint("u1", "110000", "PASS_BY", 1000L)
        val fp = database.footprintQueries.selectByUserAndRegion("u1", "110000").executeAsOne()
        assertEquals("PASS_BY", fp.level)
    }

    @Test
    fun upsertFootprint_replacesValue() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.footprintQueries.upsertFootprint("u1", "110000", "PASS_BY", 1000L)
        database.footprintQueries.upsertFootprint("u1", "110000", "DEEP", 2000L)
        val fp = database.footprintQueries.selectByUserAndRegion("u1", "110000").executeAsOne()
        assertEquals("DEEP", fp.level)
    }

    @Test
    fun syncQueue_insertAndRetrieve() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.syncQueueQueries.insertPending("FOOTPRINT", "110000", "UPSERT", "{}", 1000L)
        val pending = database.syncQueueQueries.selectPending(10).executeAsList()
        assertEquals(1, pending.size)
        assertEquals("FOOTPRINT", pending.first().entity_type)
    }

    @Test
    fun syncQueue_deleteById() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.syncQueueQueries.insertPending("FOOTPRINT", "110000", "UPSERT", "{}", 1000L)
        val pending = database.syncQueueQueries.selectPending(10).executeAsList()
        database.syncQueueQueries.deleteById(pending.first().id)
        val remaining = database.syncQueueQueries.countPending().executeAsOne()
        assertEquals(0L, remaining)
    }

    @Test
    fun attractionVisit_insertAndCount() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.attractionVisitQueries.upsertVisit("u1", "attr1", "DEEP", 1000L, null)
        database.attractionVisitQueries.upsertVisit("u1", "attr2", "PASS_BY", 2000L, null)
        val count = database.attractionVisitQueries.countByUser("u1").executeAsOne()
        assertEquals(2L, count)
    }
}
