package com.mapchina.domain.service

import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FootprintServiceTest {

    private lateinit var footprintRepo: FootprintRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var service: FootprintService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        service = FootprintService(footprintRepo, regionRepo)
    }

    @Test
    fun markRegionFootprint_newRecord() {
        val result = service.markFootprint("u1", "510000", FootprintLevel.SHORT_VISIT)
        assertTrue(result.isSuccess)
        assertEquals(FootprintLevel.SHORT_VISIT, result.footprint!!.level)
    }

    @Test
    fun markAttractionVisit_cascadesCorrectly() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("510107", "武侯区", RegionLevel.DISTRICT, "510100"))

        val result = service.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.DEEP)
        assertTrue(result.isSuccess)

        assertEquals(FootprintLevel.DEEP, footprintRepo.getFootprint("u1", "510107")!!.level)
        assertTrue(footprintRepo.getFootprint("u1", "510100")!!.level >= FootprintLevel.PASS_BY)
        assertTrue(footprintRepo.getFootprint("u1", "510000")!!.level >= FootprintLevel.PASS_BY)
    }

    @Test
    fun getCoverageStats_calculatesCorrectly() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))

        service.markFootprint("u1", "510000", FootprintLevel.DEEP)
        service.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)

        val stats = service.getCoverageStats("u1")
        assertEquals(2, stats.visitedProvinces)
        assertEquals(3, stats.totalProvinces)
    }
}
