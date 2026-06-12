package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.domain.model.FootprintLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AtlasServiceTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var atlasRepo: AtlasRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var service: AtlasService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        atlasRepo = AtlasRepository(database)
        footprintRepo = FootprintRepository(database)
        service = AtlasService(atlasRepo, footprintRepo)

        AtlasSeeder.seedAtlas(atlasRepo)
    }

    @Test
    fun getAtlasProgress_emptyUser_returnsZeroProgress() {
        val progress = service.getAtlasProgress("u1")
        assertEquals(3, progress.size) // heritage, museum, mountain
        progress.forEach { assertEquals(0, it.visitedItems) }
    }

    @Test
    fun getAtlasProgress_afterVisit_showsProgress() {
        footprintRepo.markAttractionVisit("u1", "attr_B000A8UIN8", "110101", FootprintLevel.DEEP)

        val progress = service.getAtlasProgress("u1")
        val heritage = progress.find { it.atlasId == "world_heritage" }
        assertNotNull(heritage)
        assertTrue(heritage.visitedItems > 0)
        assertTrue(heritage.completionPercent > 0)
    }

    @Test
    fun getAtlasDetail_returnsCorrectAtlas() {
        val detail = service.getAtlasDetail("u1", "world_heritage")
        assertNotNull(detail)
        assertEquals("世界遗产", detail.atlasName)
        assertTrue(detail.totalItems > 0)
    }

    @Test
    fun getAtlasDetail_unknownAtlas_returnsNull() {
        val detail = service.getAtlasDetail("u1", "nonexistent")
        assertEquals(null, detail)
    }

    @Test
    fun getAtlasItemsWithVisitStatus_unvisited_returnsAllFalse() {
        val items = service.getAtlasItemsWithVisitStatus("u1", "world_heritage")
        assertTrue(items.isNotEmpty())
        items.forEach { assertFalse(it.isVisited) }
    }

    @Test
    fun getAtlasItemsWithVisitStatus_afterVisit_showsVisited() {
        footprintRepo.markAttractionVisit("u1", "attr_B000A8UIN8", "110101", FootprintLevel.DEEP)

        val items = service.getAtlasItemsWithVisitStatus("u1", "world_heritage")
        val guGong = items.find { it.attractionId == "attr_B000A8UIN8" }
        assertNotNull(guGong)
        assertTrue(guGong.isVisited)
    }

    @Test
    fun getAtlasItemsForAttraction_returnsCorrectAtlases() {
        val result = service.getAtlasItemsForAttraction("attr_B000A8UIN8")
        // attr_B000A8UIN8 is in world_heritage and museum
        assertTrue(result.any { it.atlasId == "world_heritage" })
        assertTrue(result.any { it.atlasId == "museum" })
    }

    @Test
    fun getAtlasItemsForAttraction_unknownAttraction_returnsEmpty() {
        val result = service.getAtlasItemsForAttraction("nonexistent")
        assertEquals(0, result.size)
    }
}
