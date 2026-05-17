package com.mapchina.domain.service

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttractionServiceTest {

    private class FakeAttractionRepository : AttractionRepository(
        database = com.mapchina.data.local.MapChinaDatabase(
            com.mapchina.data.local.TestDatabaseDriverFactory().createDriver()
        )
    )

    @Test
    fun `search_with_blank_query_returns_empty`() {
        val repo = FakeAttractionRepository()
        val service = AttractionService(repo)
        assertEquals(0, service.searchAttractions("").size)
        assertEquals(0, service.searchAttractions("  ").size)
    }

    @Test
    fun `get_attractions_by_region_returns_inserted`() {
        val repo = FakeAttractionRepository()
        val service = AttractionService(repo)
        val attraction = Attraction("a1", "故宫", "110101", AttractionLevel.AAAAA, 39.9163, 116.3972, "紫禁城")
        repo.insertAttraction(attraction)

        val results = service.getAttractionsByRegion("110101")
        assertEquals(1, results.size)
        assertEquals("故宫", results[0].name)
    }

    @Test
    fun `get_attraction_by_id_returns_inserted`() {
        val repo = FakeAttractionRepository()
        val service = AttractionService(repo)
        val attraction = Attraction("a1", "故宫", "110101", AttractionLevel.AAAAA, 39.9163, 116.3972, null)
        repo.insertAttraction(attraction)

        val result = service.getAttraction("a1")
        assertEquals("故宫", result?.name)
    }
}
