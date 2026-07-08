package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestionSource
import com.mapchina.domain.model.FootprintSuggestionStatus
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FootprintSuggestionServiceTest {
    private lateinit var database: MapChinaDatabase
    private lateinit var regionRepository: RegionRepository
    private lateinit var footprintRepository: FootprintRepository
    private lateinit var userScoreRepository: UserScoreRepository
    private lateinit var footprintService: FootprintService
    private lateinit var suggestionService: FootprintSuggestionService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        regionRepository = RegionRepository(database)
        footprintRepository = FootprintRepository(database)
        userScoreRepository = UserScoreRepository(database)
        val achievementService = AchievementService(
            AchievementRepository(database),
            footprintRepository,
            userScoreRepository,
            AttractionRepository(database),
            regionRepository,
            AtlasRepository(database)
        )
        footprintService = FootprintService(footprintRepository, regionRepository, achievementService)
        suggestionService = FootprintSuggestionService(regionRepository, footprintService)

        regionRepository.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        regionRepository.insertRegion(Region("330100", "杭州市", RegionLevel.CITY, "330000"))
        regionRepository.insertRegion(Region("330106", "西湖区", RegionLevel.DISTRICT, "330100"))
    }

    @Test
    fun offerFromAttractionVisit_buildsPendingSuggestionWithPath() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        assertNotNull(suggestion)
        assertEquals(FootprintSuggestionSource.ATTRACTION_VISIT, suggestion.source)
        assertEquals(FootprintSuggestionStatus.PENDING, suggestion.status)
        assertEquals("330106", suggestion.regionId)
        assertEquals("浙江省 / 杭州市 / 西湖区", suggestion.parentPath)
        assertEquals("西湖风景名胜区", suggestion.evidenceLabel)
        assertEquals(FootprintLevel.DEEP, suggestion.suggestedLevel)
        assertEquals(1, suggestionService.suggestions.value.size)
    }

    @Test
    fun offerFromLocation_prefersDistrictThenCityThenProvince() {
        val match = RegionMatch(
            province = regionRepository.getRegion("330000"),
            city = regionRepository.getRegion("330100"),
            district = regionRepository.getRegion("330106")
        )

        val suggestion = suggestionService.offerFromLocation(match)

        assertNotNull(suggestion)
        assertEquals(FootprintSuggestionSource.LOCATION, suggestion.source)
        assertEquals("330106", suggestion.regionId)
        assertEquals("高", suggestion.confidenceLabel)
    }

    @Test
    fun offerFromLocation_fallsBackToCityWithMediumConfidence() {
        val match = RegionMatch(
            province = regionRepository.getRegion("330000"),
            city = regionRepository.getRegion("330100"),
            district = null
        )

        val suggestion = suggestionService.offerFromLocation(match)

        assertNotNull(suggestion)
        assertEquals("330100", suggestion.regionId)
        assertEquals("中", suggestion.confidenceLabel)
    }

    @Test
    fun offerFromLocation_fallsBackToProvinceWithLowConfidence() {
        val match = RegionMatch(
            province = regionRepository.getRegion("330000"),
            city = null,
            district = null
        )

        val suggestion = suggestionService.offerFromLocation(match)

        assertNotNull(suggestion)
        assertEquals("330000", suggestion.regionId)
        assertEquals("低", suggestion.confidenceLabel)
    }

    @Test
    fun offerFromAttractionVisit_recordsAttractionVisitBeforeCreatingSuggestion() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            userId = "u1",
            attractionId = "attraction-westlake",
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        assertNotNull(suggestion)
        assertEquals(
            FootprintLevel.DEEP,
            footprintRepository.getAttractionVisit("u1", "attraction-westlake")?.level
        )
    }

    @Test
    fun confirm_writesFootprintAndRemovesSuggestion() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.SHORT_VISIT
        )

        val result = suggestionService.confirm("u1", suggestion!!.id, FootprintLevel.SHORT_VISIT)

        assertEquals(true, result?.isSuccess)
        assertEquals(FootprintLevel.SHORT_VISIT, footprintRepository.getFootprint("u1", "330106")?.level)
        assertEquals(0, suggestionService.suggestions.value.size)
    }

    @Test
    fun confirm_marksParentRegionsAsPassBy() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        suggestionService.confirm("u1", suggestion!!.id, FootprintLevel.DEEP)

        assertEquals(FootprintLevel.DEEP, footprintRepository.getFootprint("u1", "330106")?.level)
        assertEquals(FootprintLevel.PASS_BY, footprintRepository.getFootprint("u1", "330100")?.level)
        assertEquals(FootprintLevel.PASS_BY, footprintRepository.getFootprint("u1", "330000")?.level)
    }

    @Test
    fun confirm_doesNotAwardParentCascadeScore() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        val result = suggestionService.confirm("u1", suggestion!!.id, FootprintLevel.DEEP)

        assertNotNull(result)
        assertEquals(50, userScoreRepository.getCurrentScore("u1"))
    }

    @Test
    fun dismiss_marksSuggestionDismissedAndHidesFromPendingList() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.PASS_BY
        )

        suggestionService.dismiss(suggestion!!.id)

        assertEquals(0, suggestionService.suggestions.value.size)
        assertNull(suggestionService.confirm("u1", suggestion.id, FootprintLevel.PASS_BY))
    }
}
