package com.mapchina.ui.discover

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
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoverViewModelTest {
    @Test
    fun recommendationsPreferUnvisitedA5AttractionsAndExplainPointValue() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)

        regionRepo.insertRegion(Region("340000", "安徽省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("341000", "黄山市", RegionLevel.CITY, "340000"))
        regionRepo.insertRegion(Region("341002", "屯溪区", RegionLevel.DISTRICT, "341000"))
        attractionRepo.insertAttraction(Attraction("a1", "黄山风景区", "341002", AttractionLevel.A5, 30.13, 118.16, null))
        attractionRepo.insertAttraction(Attraction("a2", "普通景区", "341002", AttractionLevel.A4, 30.12, 118.15, null))

        val vm = DiscoverViewModel(
            attractionRepository = attractionRepo,
            footprintRepository = footprintRepo,
            regionRepository = regionRepo,
            suggestionService = suggestionService,
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher()
        )

        val first = vm.ui.value.recommendations.first()
        assertEquals("黄山风景区", first.title)
        assertEquals("可点亮 安徽省 / 黄山市 / 屯溪区", first.reason)
        assertEquals("5A", first.levelLabel)
    }

    @Test
    fun pendingSuggestionsComeFromSuggestionService() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)

        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        suggestionService.offerFromAttractionVisit("330000", "西湖风景名胜区", FootprintLevel.PASS_BY)

        val vm = DiscoverViewModel(attractionRepo, footprintRepo, regionRepo, suggestionService, "u1", UnconfinedTestDispatcher())

        assertEquals(1, vm.ui.value.pendingSuggestions.size)
    }

    @Test
    fun searchUpdatesQueryAndFiltersUnvisitedAttractions() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)

        regionRepo.insertRegion(Region("340000", "安徽省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("341000", "黄山市", RegionLevel.CITY, "340000"))
        attractionRepo.insertAttraction(Attraction("a1", "黄山风景区", "341000", AttractionLevel.A5, 30.13, 118.16, null))
        attractionRepo.insertAttraction(Attraction("a2", "黄山温泉", "341000", AttractionLevel.A4, 30.12, 118.15, null))
        footprintRepo.recordAttractionVisit("u1", "a2", FootprintLevel.SHORT_VISIT)

        val vm = DiscoverViewModel(attractionRepo, footprintRepo, regionRepo, suggestionService, "u1", UnconfinedTestDispatcher())

        vm.search("黄山")

        assertEquals("黄山", vm.ui.value.searchQuery)
        assertEquals(listOf("黄山风景区"), vm.ui.value.recommendations.map { it.title })
    }
}
