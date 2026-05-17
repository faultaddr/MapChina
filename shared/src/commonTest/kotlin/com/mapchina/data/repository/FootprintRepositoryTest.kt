package com.mapchina.data.repository

import com.mapchina.data.local.InMemoryDatabaseDriverFactory
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.FootprintLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FootprintRepositoryTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var repository: FootprintRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        repository = FootprintRepository(database)
    }

    @Test
    fun markFootprint_newRegion_createsRecord() {
        repository.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)
        val fp = repository.getFootprint("u1", "110000")
        assertNotNull(fp)
        assertEquals(FootprintLevel.SHORT_VISIT, fp.level)
    }

    @Test
    fun markFootprint_upgradeLevel_succeeds() {
        repository.markFootprint("u1", "110000", FootprintLevel.PASS_BY)
        repository.markFootprint("u1", "110000", FootprintLevel.DEEP)
        val fp = repository.getFootprint("u1", "110000")
        assertEquals(FootprintLevel.DEEP, fp!!.level)
    }

    @Test
    fun markFootprint_downgradeLevel_ignored() {
        repository.markFootprint("u1", "110000", FootprintLevel.DEEP)
        repository.markFootprint("u1", "110000", FootprintLevel.PASS_BY)
        val fp = repository.getFootprint("u1", "110000")
        assertEquals(FootprintLevel.DEEP, fp!!.level)
    }

    @Test
    fun markAttractionVisit_cascadesToRegionFootprint() {
        insertRegionHierarchy()

        repository.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.DEEP)

        val district = repository.getFootprint("u1", "510107")
        assertEquals(FootprintLevel.DEEP, district!!.level)

        val city = repository.getFootprint("u1", "510100")
        assertNotNull(city)
        assertTrue(city!!.level >= FootprintLevel.PASS_BY)

        val province = repository.getFootprint("u1", "510000")
        assertNotNull(province)
    }

    @Test
    fun markAttractionVisit_cascadeDoesNotDowngradeParent() {
        insertRegionHierarchy()

        // 先将省级标记为 DEEP
        repository.markFootprint("u1", "510000", FootprintLevel.DEEP)
        // 通过景点访问级联 PASS_BY 到省级，不应降级
        repository.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.PASS_BY)

        val province = repository.getFootprint("u1", "510000")
        assertEquals(FootprintLevel.DEEP, province!!.level) // 不降级
    }

    @Test
    fun getFootprintsByUser_returnsAllForUser() {
        repository.markFootprint("u1", "510000", FootprintLevel.DEEP)
        repository.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)
        repository.markFootprint("u2", "330000", FootprintLevel.PASS_BY)

        val u1Footprints = repository.getFootprintsByUser("u1")
        assertEquals(2, u1Footprints.size)
    }

    @Test
    fun getFootprintCountsByLevel_returnsCorrectCounts() {
        repository.markFootprint("u1", "510000", FootprintLevel.DEEP)
        repository.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)
        repository.markFootprint("u1", "330000", FootprintLevel.PASS_BY)

        val counts = repository.getFootprintCountsByLevel("u1")
        assertEquals(1, counts[FootprintLevel.DEEP])
        assertEquals(1, counts[FootprintLevel.SHORT_VISIT])
        assertEquals(1, counts[FootprintLevel.PASS_BY])
    }

    private fun insertRegionHierarchy() {
        database.regionQueries.insertRegion("510000", "四川省", "PROVINCE", null, null)
        database.regionQueries.insertRegion("510100", "成都市", "CITY", "510000", null)
        database.regionQueries.insertRegion("510107", "武侯区", "DISTRICT", "510100", null)
    }
}
