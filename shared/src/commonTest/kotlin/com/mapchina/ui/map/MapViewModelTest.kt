package com.mapchina.ui.map

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import com.mapchina.domain.service.RegionMatcher
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.ViewportInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var attractionService: AttractionService
    private lateinit var footprintService: FootprintService
    private lateinit var regionRepo: RegionRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var suggestionService: FootprintSuggestionService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        attractionService = AttractionService(attractionRepo)
        footprintService = FootprintService(footprintRepo, regionRepo, null)
        suggestionService = FootprintSuggestionService(regionRepo, footprintService)
        viewModel = MapViewModel(
            footprintService,
            regionRepo,
            footprintRepo,
            attractionService,
            null,
            null,
            null,
            null,
            null,
            null,
            "testUser",
            UnconfinedTestDispatcher(),
            suggestionService
        )
    }

    @Test
    fun initialLevel_isNational() {
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    }

    @Test
    fun initialPath_isEmpty() {
        assertEquals(0, viewModel.currentPath.value.size)
    }

    @Test
    fun focusRegion_withoutSpatialData_finishesAndSelectsRegion() {
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))

        viewModel.focusRegion(
            regionId = "330000",
            insets = ViewportInsets(),
            reducedMotion = false
        )

        assertEquals("330000", viewModel.selectedRegion.value?.regionId)
        assertEquals(
            RegionFocusState.Focused("330000"),
            viewModel.regionFocusState.value
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun focusRegion_withSpatialData_staysAnimatingUntilCameraCompletes() = runTest {
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        regionRepo.updateBoundary(
            "330000",
            "[[118.0,27.0],[123.0,27.0],[123.0,32.0],[118.0,32.0],[118.0,27.0]]"
        )
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val controller = MapController()

        try {
            viewModel.mapController = controller
            val request = assertNotNull(
                viewModel.focusRegion(
                    regionId = "330000",
                    insets = ViewportInsets(),
                    reducedMotion = true
                )
            )

            assertEquals(
                RegionFocusState.Animating("330000", request),
                viewModel.regionFocusState.value
            )
            assertEquals(BottomPanel.None, viewModel.bottomPanel.value)

            runCurrent()
            assertEquals(
                RegionFocusState.Animating("330000", request),
                viewModel.regionFocusState.value
            )

            withContext(Dispatchers.Default) {
                delay(160L)
            }
            advanceTimeBy(16L)
            runCurrent()

            assertEquals(
                RegionFocusState.Focused("330000"),
                viewModel.regionFocusState.value
            )
            assertEquals(BottomPanel.None, viewModel.bottomPanel.value)
        } finally {
            viewModel.mapController = null
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyRepository_startsFirstFootprintActivation() {
        assertTrue(viewModel.firstFootprintActivation.value)
    }

    @Test
    fun firstMark_emitsCelebrationAndEndsActivation() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))

        viewModel.markFootprint("510000", FootprintLevel.DEEP)

        assertFalse(viewModel.firstFootprintActivation.value)
        assertEquals("510000", viewModel.firstFootprintCelebration.value?.regionId)
        assertEquals("四川省", viewModel.firstFootprintCelebration.value?.regionName)
        assertEquals(FootprintLevel.DEEP, viewModel.firstFootprintCelebration.value?.level)

        viewModel.dismissFirstFootprintCelebration()
        assertNull(viewModel.firstFootprintCelebration.value)
    }

    @Test
    fun firstMark_updatesVisibleStateBeforeBackgroundPersistenceFinishes() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        val dispatcher = StandardTestDispatcher()
        val deferredViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            userId = "deferredUser",
            dispatcher = dispatcher
        )
        deferredViewModel.selectRegion("510000")

        deferredViewModel.markFootprint("510000", FootprintLevel.DEEP)

        assertFalse(deferredViewModel.firstFootprintActivation.value)
        assertEquals("四川省", deferredViewModel.firstFootprintCelebration.value?.regionName)
        assertEquals(FootprintLevel.DEEP, deferredViewModel.selectedRegion.value?.footprintLevel)
        assertNull(footprintRepo.getFootprint("deferredUser", "510000"))

        deferredViewModel.reloadData()
        assertFalse(deferredViewModel.firstFootprintActivation.value)

        dispatcher.scheduler.runCurrent()
        assertFalse(deferredViewModel.firstFootprintActivation.value)
        assertEquals(
            FootprintLevel.DEEP,
            footprintRepo.getFootprint("deferredUser", "510000")?.level
        )
    }

    @Test
    fun reloadData_detectsFootprintWrittenByAnotherScreen() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        footprintRepo.markFootprint("testUser", "510000", FootprintLevel.PASS_BY)

        viewModel.reloadData()

        assertFalse(viewModel.firstFootprintActivation.value)
        assertNull(viewModel.firstFootprintCelebration.value)
    }

    @Test
    fun drillIntoProvince_updatesCurrentLevel() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        viewModel.drillIntoRegion("510000")
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals(1, viewModel.currentPath.value.size)
        assertEquals("510000", viewModel.currentPath.value.first().id)
    }

    @Test
    fun drillIntoCity_thenNavigateUp() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        viewModel.drillIntoRegion("510000")
        viewModel.drillIntoRegion("510100")
        assertEquals(MapZoomLevel.CITY, viewModel.currentLevel.value)

        viewModel.navigateUp()
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals(1, viewModel.currentPath.value.size)
    }

    @Test
    fun navigateUp_fromNational_staysNational() {
        viewModel.navigateUp()
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    }

    @Test
    fun markFootprint_updatesRegionState() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        viewModel.markFootprint("510000", FootprintLevel.DEEP)
        val region = viewModel.regions.value.find { it.regionId == "510000" }
        assertEquals(FootprintLevel.DEEP, region?.footprintLevel)
    }

    @Test
    fun markFootprint_upgradeOnly() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        viewModel.markFootprint("510000", FootprintLevel.DEEP)
        viewModel.markFootprint("510000", FootprintLevel.PASS_BY)
        val region = viewModel.regions.value.find { it.regionId == "510000" }
        assertEquals(FootprintLevel.DEEP, region?.footprintLevel)
    }

    @Test
    fun selectRegion_andClearSelection() {
        assertNull(viewModel.selectedRegion.value)
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        viewModel.selectRegion("510000")
        assertNotNull(viewModel.selectedRegion.value)
        viewModel.clearSelection()
        assertNull(viewModel.selectedRegion.value)
    }

    @Test
    fun footprintSuggestions_exposesPendingSuggestions() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))

        suggestionService.offerFromAttractionVisit(
            regionId = "110101",
            attractionName = "故宫博物院",
            level = FootprintLevel.DEEP
        )

        assertEquals(1, viewModel.footprintSuggestions.value.size)
        assertEquals("110101", viewModel.footprintSuggestions.value.first().regionId)
    }

    @Test
    fun confirmSuggestion_writesFootprintAndClearsPendingSuggestion() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))

        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "110101",
            attractionName = "故宫博物院",
            level = FootprintLevel.SHORT_VISIT
        )

        viewModel.confirmSuggestion(suggestion!!.id, FootprintLevel.SHORT_VISIT)

        assertEquals(FootprintLevel.SHORT_VISIT, footprintRepo.getFootprint("testUser", "110101")?.level)
        assertEquals(0, viewModel.footprintSuggestions.value.size)
    }

    @Test
    fun dismissSuggestion_removesPendingSuggestion() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "110000",
            attractionName = "天安门",
            level = FootprintLevel.PASS_BY
        )

        viewModel.dismissSuggestion(suggestion!!.id)

        assertEquals(0, viewModel.footprintSuggestions.value.size)
    }

    @Test
    fun autoMarkFromGps_createsSuggestionMessageWithoutWritingFootprint() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))
        regionRepo.updateBoundariesInTransaction(
            listOf(
                "110000" to "[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]",
                "110100" to "[[116.2,39.7],[116.8,39.7],[116.8,40.0],[116.2,40.0],[116.2,39.7]]",
                "110101" to "[[116.4,39.9],[116.5,39.9],[116.5,40.0],[116.4,40.0],[116.4,39.9]]"
            )
        )
        val gpsViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            regionMatcher = RegionMatcher(regionRepo),
            userId = "testUser",
            dispatcher = UnconfinedTestDispatcher(),
            footprintSuggestionService = suggestionService,
            currentLocationProvider = FakeCurrentLocationProvider(39.95 to 116.45)
        )

        gpsViewModel.autoMarkFromGps()

        assertEquals(1, gpsViewModel.footprintSuggestions.value.size)
        assertEquals("110101", gpsViewModel.footprintSuggestions.value.first().regionId)
        assertEquals("发现可能足迹：北京市 / 北京市 / 东城区", gpsViewModel.autoMarkMessage.value)
        assertEquals(null, footprintRepo.getFootprint("testUser", "110101"))
    }

    @Test
    fun activateCurrentLocation_selectsMostSpecificMatchedRegion() {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))
        regionRepo.updateBoundariesInTransaction(
            listOf(
                "110000" to "[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]",
                "110100" to "[[116.2,39.7],[116.8,39.7],[116.8,40.0],[116.2,40.0],[116.2,39.7]]",
                "110101" to "[[116.4,39.9],[116.5,39.9],[116.5,40.0],[116.4,40.0],[116.4,39.9]]"
            )
        )
        val gpsViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            regionMatcher = RegionMatcher(regionRepo),
            userId = "testUser",
            dispatcher = UnconfinedTestDispatcher(),
            currentLocationProvider = FakeCurrentLocationProvider(39.95 to 116.45)
        )

        gpsViewModel.activateCurrentLocation()

        assertEquals("110101", gpsViewModel.selectedRegion.value?.regionId)
        assertEquals(BottomPanel.Region("110101"), gpsViewModel.bottomPanel.value)
    }

    @Test
    fun activateCurrentLocation_waitsForFirstAsyncLocationResult() = runTest {
        regionRepo.insertRegion(Region("110000", "北京市", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("110100", "北京市", RegionLevel.CITY, "110000"))
        regionRepo.insertRegion(Region("110101", "东城区", RegionLevel.DISTRICT, "110100"))
        regionRepo.updateBoundariesInTransaction(
            listOf(
                "110000" to "[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]",
                "110100" to "[[116.2,39.7],[116.8,39.7],[116.8,40.0],[116.2,40.0],[116.2,39.7]]",
                "110101" to "[[116.4,39.9],[116.5,39.9],[116.5,40.0],[116.4,40.0],[116.4,39.9]]"
            )
        )
        val gpsViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            regionMatcher = RegionMatcher(regionRepo),
            userId = "testUser",
            dispatcher = StandardTestDispatcher(testScheduler),
            currentLocationProvider = SequencedCurrentLocationProvider(
                listOf(null, 39.95 to 116.45)
            )
        )

        gpsViewModel.activateCurrentLocation()
        advanceUntilIdle()

        assertEquals("110101", gpsViewModel.selectedRegion.value?.regionId)
        assertEquals(BottomPanel.Region("110101"), gpsViewModel.bottomPanel.value)
    }

    @Test
    fun togglePhotoMarkers_showsPhaseMessage() {
        viewModel.togglePhotoMarkers()

        assertEquals("照片回溯将在后续版本开放", viewModel.autoMarkMessage.value)
    }
}

private class FakeCurrentLocationProvider(
    private val location: Pair<Double, Double>?
) : CurrentLocationProvider {
    override fun isAvailable(): Boolean = location != null

    override fun getCurrentLocation(): Pair<Double, Double>? = location
}

private class SequencedCurrentLocationProvider(
    locations: List<Pair<Double, Double>?>
) : CurrentLocationProvider {
    private val remaining = locations.toMutableList()

    override fun isAvailable(): Boolean = true

    override fun getCurrentLocation(): Pair<Double, Double>? =
        if (remaining.isEmpty()) null else remaining.removeAt(0)
}
