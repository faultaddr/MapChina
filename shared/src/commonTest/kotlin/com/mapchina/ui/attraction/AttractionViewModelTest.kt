package com.mapchina.ui.attraction

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttractionViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var attractionRepo: AttractionRepository
    private lateinit var footprintService: FootprintService
    private lateinit var footprintRepo: FootprintRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        attractionRepo = AttractionRepository(database)
        footprintRepo = FootprintRepository(database)
        val regionRepo = RegionRepository(database)
        footprintService = FootprintService(footprintRepo, regionRepo, null)

        attractionRepo.insertAttraction(Attraction("a1", "故宫博物院", "110101", AttractionLevel.A5, 39.9163, 116.3972, "紫禁城"))
        attractionRepo.insertAttraction(Attraction("a2", "长城", "110229", AttractionLevel.A5, 40.3588, 116.0204, null))
        attractionRepo.insertAttraction(Attraction("a3", "武侯祠", "510107", AttractionLevel.A4, 30.6438, 104.0482, null))
    }

    @Test
    fun searchAttractions_blankQuery_returnsAll() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        vm.searchAttractions("")
        assertEquals(3, vm.attractions.value.size)

        vm.searchAttractions("  ")
        assertEquals(3, vm.attractions.value.size)
    }

    @Test
    fun searchAttractions_matchingQuery_returnsResults() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        vm.searchAttractions("故宫")
        val results = vm.attractions.value
        assertEquals(1, results.size)
        assertEquals("故宫博物院", results[0].name)
    }

    @Test
    fun searchAttractions_noMatch_returnsEmpty() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        vm.searchAttractions("不存在的景点")
        assertEquals(0, vm.attractions.value.size)
    }

    @Test
    fun loadAttractionsByRegion_returnsCorrectAttractions() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        vm.loadAttractionsByRegion("110101")
        val results = vm.attractions.value
        assertEquals(1, results.size)
        assertEquals("故宫博物院", results[0].name)
    }

    @Test
    fun markVisit_updatesAttractionState() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, null, "u1", UnconfinedTestDispatcher())
        vm.searchAttractions("故宫")
        vm.markVisit("a1", "110101", FootprintLevel.DEEP)

        val updated = vm.attractions.value.find { it.id == "a1" }
        assertEquals(FootprintLevel.DEEP, updated?.visitLevel)
    }

    @Test
    fun getAttractionById_returnsAttraction() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        val attraction = vm.getAttractionById("a1")
        assertEquals("故宫博物院", attraction?.name)
    }

    @Test
    fun getAttractionById_unknown_returnsNull() {
        val vm = AttractionViewModel(attractionRepo, footprintService, footprintRepo, null, dispatcher = UnconfinedTestDispatcher())
        val attraction = vm.getAttractionById("nonexistent")
        assertEquals(null, attraction)
    }
}
