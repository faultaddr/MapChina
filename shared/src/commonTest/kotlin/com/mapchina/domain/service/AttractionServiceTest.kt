package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AttractionServiceTest {

    private lateinit var repo: AttractionRepository
    private lateinit var service: AttractionService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        repo = AttractionRepository(database)
        service = AttractionService(repo)
    }

    @Test
    fun `search with blank query returns empty`() {
        assertEquals(0, service.searchAttractions("").size)
        assertEquals(0, service.searchAttractions("  ").size)
    }

    @Test
    fun `get attractions by region returns inserted`() {
        val attraction = Attraction("a1", "故宫", "110101", AttractionLevel.A5, 39.9163, 116.3972, "紫禁城")
        repo.insertAttraction(attraction)

        val results = service.getAttractionsByRegion("110101")
        assertEquals(1, results.size)
        assertEquals("故宫", results[0].name)
    }

    @Test
    fun `get attraction by id returns inserted`() {
        val attraction = Attraction("a1", "故宫", "110101", AttractionLevel.A5, 39.9163, 116.3972, null)
        repo.insertAttraction(attraction)

        val result = service.getAttraction("a1")
        assertEquals("故宫", result?.name)
    }
}
