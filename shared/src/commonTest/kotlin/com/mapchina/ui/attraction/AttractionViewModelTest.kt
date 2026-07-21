package com.mapchina.ui.attraction

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintSuggestionService
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val vm = AttractionViewModel(
            attractionRepo,
            footprintService,
            footprintRepo,
            null,
            attractionService = null,
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher()
        )
        vm.searchAttractions("故宫")
        vm.markVisit("a1", "110101", FootprintLevel.DEEP)

        val updated = vm.attractions.value.find { it.id == "a1" }
        assertEquals(FootprintLevel.DEEP, updated?.visitLevel)
    }

    @Test
    fun markVisit_recordsVisitAndCreatesSuggestionWithoutWritingFootprint() {
        val regionRepo = RegionRepository(database)
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)
        val vm = AttractionViewModel(
            attractionRepository = attractionRepo,
            footprintService = footprintService,
            footprintRepository = footprintRepo,
            detailProvider = null,
            attractionService = null,
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher(),
            footprintSuggestionService = suggestionService
        )

        vm.markVisit("a1", "110101", FootprintLevel.DEEP)

        assertEquals(FootprintLevel.DEEP, footprintRepo.getAttractionVisit("u1", "a1")?.level)
        assertEquals(null, footprintRepo.getFootprint("u1", "110101"))
        assertEquals(1, suggestionService.suggestions.value.size)
        assertEquals("110101", suggestionService.suggestions.value.first().regionId)
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
