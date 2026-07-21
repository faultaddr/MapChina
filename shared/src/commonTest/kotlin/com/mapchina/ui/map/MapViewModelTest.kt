package com.mapchina.ui.map

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
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import com.mapchina.domain.service.RegionMatcher
import com.mapchina.map.MapController
import com.mapchina.map.MapTheme
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.OverlayRole
import com.mapchina.map.ViewportInsets
import com.mapchina.platform.DevicePhoto
import com.mapchina.platform.PhotoResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
    private lateinit var attractionRepo: AttractionRepository
    private lateinit var attractionService: AttractionService
    private lateinit var footprintService: FootprintService
    private lateinit var regionRepo: RegionRepository
    private lateinit var footprintRepo: FootprintRepository
    private lateinit var suggestionService: FootprintSuggestionService

    private val provinceBoundary =
        "[[100.0,30.0],[110.0,30.0],[110.0,40.0],[100.0,40.0],[100.0,30.0]]"
    private val cityBoundary =
        "[[103.0,32.0],[107.0,32.0],[107.0,36.0],[103.0,36.0],[103.0,32.0]]"
    private val districtBoundary =
        "[[104.0,33.0],[106.0,33.0],[106.0,35.0],[104.0,35.0],[104.0,33.0]]"

    @BeforeTest
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        attractionRepo = AttractionRepository(database)
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
            dispatcher,
            suggestionService,
            controllerDispatcher = dispatcher
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
            dispatcher = dispatcher,
            controllerDispatcher = dispatcher
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
    fun controllerEffects_runOnlyOnDedicatedControllerDispatcher() {
        val workDispatcher = StandardTestDispatcher()
        val controllerDispatcher = StandardTestDispatcher()
        val isolatedViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            userId = "controllerDispatcherUser",
            dispatcher = workDispatcher,
            controllerDispatcher = controllerDispatcher
        )
        val controller = MapController()

        try {
            isolatedViewModel.mapController = controller
            isolatedViewModel.enterShareMode()

            assertFalse(controller.renderState.value.shareMode)

            controllerDispatcher.scheduler.advanceUntilIdle()

            assertTrue(controller.renderState.value.shareMode)
        } finally {
            isolatedViewModel.mapController = null
            isolatedViewModel.onCleared()
            controllerDispatcher.scheduler.advanceUntilIdle()
            controller.dispose()
        }
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
        seedProvinceCityAndDistrict()
        viewModel.drillIntoRegion("510000")
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals("510000", viewModel.currentPath.value.single().id)
    }

    @Test
    fun drillIntoCity_thenNavigateUp() {
        seedProvinceCityAndDistrict()
        viewModel.drillIntoRegion("510000")
        viewModel.drillIntoRegion("510100")
        assertEquals(MapZoomLevel.CITY, viewModel.currentLevel.value)

        viewModel.navigateUp()
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals("510000", viewModel.currentPath.value.single().id)
    }

    @Test
    fun readyDrillAndNavigation_keepSynchronousStateFlowValueSemantics() {
        seedProvinceCityAndDistrict()

        viewModel.drillIntoRegion("510000")
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals("510000", viewModel.currentPath.value.single().id)
        assertEquals(listOf("510100"), viewModel.regions.value.map { it.regionId })

        viewModel.drillIntoRegion("510100")
        viewModel.navigateUp()
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals("510000", viewModel.currentPath.value.single().id)

        viewModel.navigateToNational()
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        assertTrue(viewModel.currentPath.value.isEmpty())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun projectedStateFlow_collectsOnlyDistinctFieldChanges() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "projectedStateFlowUser"
        )
        val levels = mutableListOf<MapZoomLevel>()

        try {
            runCurrent()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                delayedViewModel.currentLevel.take(2).toList(levels)
            }
            delayedViewModel.selectRegion("510000")
            delayedViewModel.showRegionPanel("510000")
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()

            assertEquals(
                listOf(MapZoomLevel.NATIONAL, MapZoomLevel.PROVINCIAL),
                levels
            )
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @Test
    fun drillWithoutChildBoundaries_keepsCurrentLevelAndExposesRetry() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.updateBoundary("510000", provinceBoundary)

        viewModel.drillIntoRegion("510000")

        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        assertTrue(viewModel.currentPath.value.isEmpty())
        assertEquals(
            MapLayerLoadState.Error(
                regionId = "510000",
                message = "市级地图暂时无法展开"
            ),
            viewModel.mapLayerLoadState.value
        )
    }

    @Test
    fun drillWithMissingBoundaryForOneChild_keepsCurrentMapUnchanged() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("510300", "自贡市", RegionLevel.CITY, "510000"))
        regionRepo.updateBoundary("510000", provinceBoundary)
        regionRepo.updateBoundary("510100", cityBoundary)

        viewModel.drillIntoRegion("510000")

        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        assertTrue(viewModel.currentPath.value.isEmpty())
        assertEquals(
            MapLayerLoadState.Error("510000", "市级地图暂时无法展开"),
            viewModel.mapLayerLoadState.value
        )
    }

    @Test
    fun drillError_canBeDismissedBackToIdle() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))

        viewModel.drillIntoRegion("510000")
        viewModel.dismissLayerLoadError()

        assertEquals(MapLayerLoadState.Idle, viewModel.mapLayerLoadState.value)
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        assertTrue(viewModel.currentPath.value.isEmpty())
    }

    @Test
    fun drillError_retryCommitsAfterChildrenBecomeReady() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        viewModel.drillIntoRegion("510000")
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.updateBoundary("510000", provinceBoundary)
        regionRepo.updateBoundary("510100", cityBoundary)

        viewModel.retryLayerLoad()

        assertEquals(MapLayerLoadState.Idle, viewModel.mapLayerLoadState.value)
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
        assertEquals("510000", viewModel.currentPath.value.single().id)
    }

    @Test
    fun drillWithReadyChildren_commitsPathAndKeepsProvinceAsContext() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.updateBoundary("510000", provinceBoundary)
        regionRepo.updateBoundary("510100", cityBoundary)
        val controller = MapController()
        viewModel.mapController = controller
        viewModel.reloadData()

        try {
            viewModel.drillIntoRegion("510000")

            assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
            assertEquals("510000", viewModel.currentPath.value.single().id)
            assertEquals(
                OverlayRole.CONTEXT,
                controller.renderState.value.overlays["510000"]?.role
            )
            assertEquals(
                0.25f,
                controller.renderState.value.overlays["510000"]?.opacityMultiplier
            )
            assertEquals(
                OverlayRole.ACTIVE,
                controller.renderState.value.overlays["510100"]?.role
            )
            assertEquals(MapLayerLoadState.Idle, viewModel.mapLayerLoadState.value)
        } finally {
            viewModel.mapController = null
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun delayedChildLayerCompletingAfterNavigateUp_cannotRestoreStaleDrill() = runTest {
        seedProvinceCityAndDistrict()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val delayedViewModel = createDelayedViewModel(dispatcher)

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.selectRegion("510100")
            delayedViewModel.showRegionPanel("510100")
            runCurrent()

            delayedViewModel.drillIntoRegion("510100")
            assertEquals(
                MapLayerLoadState.Loading("510100", "正在展开区级地图"),
                delayedViewModel.mapLayerLoadState.value
            )

            delayedViewModel.navigateUp()
            assertEquals(MapZoomLevel.NATIONAL, delayedViewModel.currentLevel.value)
            assertTrue(delayedViewModel.currentPath.value.isEmpty())

            runCurrent()

            assertEquals(MapZoomLevel.NATIONAL, delayedViewModel.currentLevel.value)
            assertTrue(delayedViewModel.currentPath.value.isEmpty())
            assertEquals(
                listOf("510000"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertNull(delayedViewModel.selectedRegion.value)
            assertEquals(BottomPanel.None, delayedViewModel.bottomPanel.value)
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun validatedChildLayerCompletion_cannotCommitAfterNavigationInvalidatesIt() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "validatedThenInvalidatedUser"
        )
        val validated = CompletableDeferred<Unit>()
        val releaseCompletion = CompletableDeferred<Unit>()
        val callOrder = mutableListOf<String>()

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.afterLayerLoadValidation = {
                callOrder += "old completion passed validation"
                validated.complete(Unit)
                releaseCompletion.await()
                callOrder += "old completion released before commit"
            }

            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertTrue(validated.isCompleted)

            callOrder += "navigateToNational invalidated old request"
            delayedViewModel.navigateToNational()
            assertEquals(MapZoomLevel.NATIONAL, delayedViewModel.currentLevel.value)
            assertTrue(delayedViewModel.currentPath.value.isEmpty())

            callOrder += "release old completion"
            releaseCompletion.complete(Unit)
            runCurrent()
            callOrder += "observed ${delayedViewModel.currentLevel.value}"
            println("layer-load TOCTOU: ${callOrder.joinToString(" -> ")}")

            assertEquals(MapZoomLevel.NATIONAL, delayedViewModel.currentLevel.value)
            assertTrue(delayedViewModel.currentPath.value.isEmpty())
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun validatedChildLayerError_cannotPublishAfterNavigationInvalidatesIt() = runTest {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "validatedErrorThenInvalidatedUser"
        )
        val validated = CompletableDeferred<Unit>()
        val releaseCompletion = CompletableDeferred<Unit>()

        try {
            runCurrent()
            delayedViewModel.afterLayerLoadValidation = {
                validated.complete(Unit)
                releaseCompletion.await()
            }
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            assertTrue(validated.isCompleted)

            delayedViewModel.navigateToNational()
            assertEquals(
                MapLayerLoadState.Idle,
                delayedViewModel.mapLayerLoadState.value
            )

            releaseCompletion.complete(Unit)
            runCurrent()

            assertEquals(MapZoomLevel.NATIONAL, delayedViewModel.currentLevel.value)
            assertTrue(delayedViewModel.currentPath.value.isEmpty())
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun staleChildEffectAfterNationalRender_cannotOverrideControllerOrAttractions() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        attractionRepo.insertAttraction(
            Attraction(
                id = "stale-city-attraction",
                name = "旧市级景点",
                regionId = "510104",
                level = AttractionLevel.A4,
                latitude = 30.0,
                longitude = 104.0
            )
        )
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "staleEffectSnapshotUser"
        )
        val controller = MapController()
        val childSnapshotRead = CompletableDeferred<Unit>()
        val releaseChildEffect = CompletableDeferred<Unit>()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.afterChildLayerEffectSnapshot = {
                childSnapshotRead.complete(Unit)
                releaseChildEffect.await()
            }

            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertTrue(childSnapshotRead.isCompleted)

            delayedViewModel.navigateToNational()
            runCurrent()
            assertNationalControllerRender(controller)
            assertEquals(
                emptyList(),
                delayedViewModel.attractions.value.map { it.id }
            )

            releaseChildEffect.complete(Unit)
            runCurrent()

            assertNationalControllerRender(controller)
            assertEquals(
                emptyList(),
                delayedViewModel.attractions.value.map { it.id }
            )
        } finally {
            releaseChildEffect.complete(Unit)
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun queuedChildEffect_cannotWriteReplacementController() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "replacementEffectUser"
        )
        val oldController = MapController()
        val replacementController = MapController()
        val childSnapshotRead = CompletableDeferred<Unit>()
        val releaseChildEffect = CompletableDeferred<Unit>()

        try {
            delayedViewModel.mapController = oldController
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.afterChildLayerEffectSnapshot = {
                childSnapshotRead.complete(Unit)
                releaseChildEffect.await()
            }
            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertTrue(childSnapshotRead.isCompleted)

            delayedViewModel.navigateToNational()
            runCurrent()
            delayedViewModel.mapController = replacementController
            runCurrent()
            assertNationalControllerRender(replacementController)

            releaseChildEffect.complete(Unit)
            runCurrent()

            assertNationalControllerRender(replacementController)
        } finally {
            releaseChildEffect.complete(Unit)
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            oldController.dispose()
            replacementController.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun oldAttractionResult_cannotPublishAfterNavigationVersionChanges() = runTest {
        seedProvinceCityAndDistrict()
        attractionRepo.insertAttraction(
            Attraction(
                id = "stale-attraction-result",
                name = "旧景点结果",
                regionId = "510104",
                level = AttractionLevel.A4,
                latitude = 30.0,
                longitude = 104.0
            )
        )
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "staleAttractionResultUser"
        )
        val attractionLoaded = CompletableDeferred<Unit>()
        val releaseAttractionResult = CompletableDeferred<Unit>()

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.afterAttractionsLoad = { regionId ->
                if (regionId == "510100") {
                    attractionLoaded.complete(Unit)
                    releaseAttractionResult.await()
                }
            }

            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertTrue(attractionLoaded.isCompleted)

            delayedViewModel.navigateToNational()
            runCurrent()
            assertTrue(delayedViewModel.attractions.value.isEmpty())

            releaseAttractionResult.complete(Unit)
            runCurrent()

            assertEquals(
                emptyList(),
                delayedViewModel.attractions.value.map { it.id }
            )
        } finally {
            releaseAttractionResult.complete(Unit)
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun ordinaryChildLayerResult_cannotPublishAfterNavigationChanges() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "ordinaryLayerIntentUser"
        )
        val controller = MapController()
        val childPrepared = CompletableDeferred<Unit>()
        val releaseChild = CompletableDeferred<Unit>()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.afterOrdinaryLayerPrepared = { source ->
                if (source == "510000") {
                    childPrepared.complete(Unit)
                    releaseChild.await()
                }
            }
            delayedViewModel.navigateTo("510000")
            runCurrent()
            assertNotNull(controller.captureCameraAnimCompleteListener()).invoke()
            runCurrent()
            assertTrue(childPrepared.isCompleted)

            delayedViewModel.navigateToNational()
            runCurrent()
            assertEquals(
                listOf("510000"),
                delayedViewModel.regions.value.map { it.regionId }
            )

            releaseChild.complete(Unit)
            runCurrent()

            assertEquals(
                listOf("510000"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertNationalControllerRender(controller)
        } finally {
            releaseChild.complete(Unit)
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun queuedNationalLoad_cannotCaptureNewerProvincialContextOnWorkerStart() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "queuedNationalSnapshotUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertEquals(
                listOf("510104"),
                delayedViewModel.regions.value.map { it.regionId }
            )

            delayedViewModel.navigateToNational()
            delayedViewModel.navigateTo("510000")
            assertEquals(MapZoomLevel.PROVINCIAL, delayedViewModel.currentLevel.value)
            assertEquals(
                listOf("510000"),
                delayedViewModel.currentPath.value.map { it.id }
            )

            runCurrent()

            assertEquals(
                listOf("510104"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertFalse(
                controller.renderState.value.overlays["510000"]?.role ==
                    OverlayRole.ACTIVE
            )
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun queuedChildLoad_cannotCaptureNewerCityContextOnWorkerStart() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "queuedChildSnapshotUser"
        )

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.drillIntoRegion("510100")
            runCurrent()
            assertEquals(
                listOf("510104"),
                delayedViewModel.regions.value.map { it.regionId }
            )

            delayedViewModel.navigateUp()
            delayedViewModel.navigateTo("510100")
            assertEquals(MapZoomLevel.CITY, delayedViewModel.currentLevel.value)
            assertEquals(
                listOf("510000", "510100"),
                delayedViewModel.currentPath.value.map { it.id }
            )

            runCurrent()

            assertEquals(
                listOf("510104"),
                delayedViewModel.regions.value.map { it.regionId }
            )
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun closedPhotoIntent_rejectsLateClustersAndMarkers() = runTest {
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "photoIntentUser"
        )
        val controller = MapController()
        val photoLoaded = CompletableDeferred<Unit>()
        val releasePhoto = CompletableDeferred<Unit>()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.photoLoadOverride = {
                PhotoResult.SUCCESS to listOf(
                    DevicePhoto(
                        id = "late-photo",
                        filePath = "/tmp/late-photo.jpg",
                        latitude = 30.0,
                        longitude = 104.0,
                        dateTaken = 1L
                    )
                )
            }
            delayedViewModel.afterPhotoLoad = {
                photoLoaded.complete(Unit)
                releasePhoto.await()
            }

            delayedViewModel.togglePhotoMarkers()
            runCurrent()
            assertTrue(photoLoaded.isCompleted)

            delayedViewModel.togglePhotoMarkers()
            runCurrent()
            assertFalse(delayedViewModel.photoMarkersVisible.value)
            assertTrue(controller.renderState.value.imageMarkers.isEmpty())

            releasePhoto.complete(Unit)
            runCurrent()

            assertFalse(delayedViewModel.photoMarkersVisible.value)
            assertTrue(delayedViewModel.photoClusters.value.isEmpty())
            assertTrue(controller.renderState.value.imageMarkers.isEmpty())
        } finally {
            releasePhoto.complete(Unit)
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun staleCurrentLocationIntent_cannotOverrideLaterNavigationCamera() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
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
        val locationRead = CompletableDeferred<Unit>()
        val releaseLocation = CompletableDeferred<Unit>()
        val gpsViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            regionMatcher = RegionMatcher(regionRepo),
            userId = "locationIntentUser",
            dispatcher = StandardTestDispatcher(testScheduler),
            currentLocationProvider = FakeCurrentLocationProvider(39.95 to 116.45),
            controllerDispatcher = StandardTestDispatcher(testScheduler)
        )
        val controller = MapController()

        try {
            gpsViewModel.mapController = controller
            runCurrent()
            gpsViewModel.afterCurrentLocationRead = {
                locationRead.complete(Unit)
                releaseLocation.await()
            }
            gpsViewModel.activateCurrentLocation()
            runCurrent()
            assertTrue(locationRead.isCompleted)

            gpsViewModel.navigateTo("510000")
            runCurrent()
            val expectedCamera = gpsViewModel.getSavedCameraState()

            releaseLocation.complete(Unit)
            runCurrent()

            assertEquals(
                listOf("510000"),
                gpsViewModel.currentPath.value.map { it.id }
            )
            assertEquals(expectedCamera, gpsViewModel.getSavedCameraState())
        } finally {
            releaseLocation.complete(Unit)
            gpsViewModel.mapController = null
            gpsViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun controllerScopedShareMode_survivesNationalNavigateUpNoOp() = runTest {
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "shareScopeUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            val versionBefore = delayedViewModel.navigationVersion.value

            delayedViewModel.enterShareMode()
            delayedViewModel.navigateUp()
            runCurrent()

            assertEquals(
                versionBefore to true,
                delayedViewModel.navigationVersion.value to
                    controller.renderState.value.shareMode
            )
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun controllerScopedThemeRefresh_survivesReloadVersionChange() = runTest {
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "themeScopeUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            controller.setBackgroundTheme(MapTheme.INK_WASH)

            delayedViewModel.refreshMapTheme()
            delayedViewModel.reloadData()
            runCurrent()

            assertEquals(
                MapTheme.DEFAULT,
                controller.renderState.value.backgroundTheme
            )
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun controllerScopedPhotoMarkerClear_survivesReloadVersionChange() = runTest {
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "photoScopeUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            controller.addImageMarker(
                id = "queued-photo",
                lat = 30.0,
                lng = 104.0,
                imagePath = "/tmp/queued-photo.jpg",
                count = 1
            )
            delayedViewModel.togglePhotoMarkers()

            delayedViewModel.togglePhotoMarkers()
            delayedViewModel.reloadData()
            runCurrent()

            assertTrue(controller.renderState.value.imageMarkers.isEmpty())
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun staleCameraCompletionAfterNationalNavigation_cleansPresentationOnly() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        attractionRepo.insertAttraction(
            Attraction(
                id = "stale-camera-attraction",
                name = "旧相机上下文景点",
                regionId = "510100",
                level = AttractionLevel.A4,
                latitude = 30.0,
                longitude = 104.0
            )
        )
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "cameraCleanupUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.navigateTo("510000")
            runCurrent()
            val staleCompletion =
                assertNotNull(controller.captureCameraAnimCompleteListener())
            assertEquals("510000", controller.renderState.value.pulseTarget)

            delayedViewModel.navigateToNational()
            staleCompletion.invoke()
            runCurrent()

            assertEquals(
                listOf<Any?>(
                    null,
                    false,
                    MapZoomLevel.NATIONAL,
                    emptyList<String>(),
                    listOf("510000"),
                    emptyList<String>()
                ),
                listOf(
                    controller.renderState.value.pulseTarget,
                    controller.hasCameraAnimCompleteListener(),
                    delayedViewModel.currentLevel.value,
                    delayedViewModel.currentPath.value.map { it.id },
                    delayedViewModel.regions.value.map { it.regionId },
                    delayedViewModel.attractions.value.map { it.id }
                )
            )
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun lateCameraCompletion_cannotCleanNewPresentationOwner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "cameraOwnerUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.navigateTo("510000")
            runCurrent()
            val staleCompletion =
                assertNotNull(controller.captureCameraAnimCompleteListener())

            delayedViewModel.navigateTo("330000")
            runCurrent()
            val currentCompletion =
                assertNotNull(controller.captureCameraAnimCompleteListener())
            assertTrue(staleCompletion !== currentCompletion)
            assertEquals("330000", controller.renderState.value.pulseTarget)

            staleCompletion.invoke()
            runCurrent()

            assertEquals("330000", controller.renderState.value.pulseTarget)
            assertTrue(controller.captureCameraAnimCompleteListener() === currentCompletion)
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun controllerReplacement_drainsOldCleanupWithoutCleaningNewOwner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "controllerRetirementUser"
        )
        val oldController = MapController()
        val replacementController = MapController()

        try {
            delayedViewModel.mapController = oldController
            runCurrent()
            delayedViewModel.navigateTo("510000")
            runCurrent()
            assertEquals("510000", oldController.renderState.value.pulseTarget)
            assertTrue(oldController.hasCameraAnimCompleteListener())

            delayedViewModel.mapController = replacementController
            delayedViewModel.navigateTo("330000")
            runCurrent()

            assertNull(oldController.renderState.value.pulseTarget)
            assertFalse(oldController.hasCameraAnimCompleteListener())
            assertEquals("330000", replacementController.renderState.value.pulseTarget)
            assertTrue(replacementController.hasCameraAnimCompleteListener())
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            oldController.dispose()
            replacementController.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelClear_drainsControllerLifecycleCleanup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "controllerDrainUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.navigateTo("510000")
            runCurrent()
            assertEquals("510000", controller.renderState.value.pulseTarget)
            assertTrue(controller.hasCameraAnimCompleteListener())

            delayedViewModel.onCleared()
            delayedViewModel.awaitControllerEffectsDrained()

            assertNull(controller.renderState.value.pulseTarget)
            assertFalse(controller.hasCameraAnimCompleteListener())
        } finally {
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun navigateToNational_whenAlreadyNational_isTrueNoOp() {
        val versionBefore = viewModel.navigationVersion.value

        viewModel.navigateToNational()

        assertEquals(versionBefore, viewModel.navigationVersion.value)
    }

    @Test
    fun navigateToCurrentRegion_whenContextAlreadyMatches_isTrueNoOp() {
        seedProvinceCityAndDistrict()
        viewModel.navigateTo("510000")
        val versionBefore = viewModel.navigationVersion.value

        viewModel.navigateTo("510000")

        assertEquals(versionBefore, viewModel.navigationVersion.value)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun nationalNavigateToNational_cleansFocusWithoutVersionChange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "nationalFocusCleanupUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.focusRegion("510000", ViewportInsets(), reducedMotion = false)
            delayedViewModel.showRegionPanel("510000")
            runCurrent()
            val versionBefore = delayedViewModel.navigationVersion.value
            assertTrue(delayedViewModel.regionFocusState.value is RegionFocusState.Animating)

            delayedViewModel.navigateToNational()
            runCurrent()

            assertEquals(versionBefore, delayedViewModel.navigationVersion.value)
            assertEquals(RegionFocusState.Idle, delayedViewModel.regionFocusState.value)
            assertNull(delayedViewModel.selectedRegion.value)
            assertEquals(BottomPanel.None, delayedViewModel.bottomPanel.value)
            assertNull(controller.renderState.value.pulseTarget)
            assertFalse(controller.hasCameraAnimCompleteListener())
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun nationalNavigateUp_cleansFocusWithoutVersionChange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "nationalUpFocusCleanupUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.focusRegion("510000", ViewportInsets(), reducedMotion = false)
            delayedViewModel.showRegionPanel("510000")
            runCurrent()
            val versionBefore = delayedViewModel.navigationVersion.value

            delayedViewModel.navigateUp()
            runCurrent()

            assertEquals(versionBefore, delayedViewModel.navigationVersion.value)
            assertEquals(RegionFocusState.Idle, delayedViewModel.regionFocusState.value)
            assertNull(delayedViewModel.selectedRegion.value)
            assertEquals(BottomPanel.None, delayedViewModel.bottomPanel.value)
            assertNull(controller.renderState.value.pulseTarget)
            assertFalse(controller.hasCameraAnimCompleteListener())
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun sameRegionNavigateTo_cleansPresentationWithoutVersionChange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "sameRegionCleanupUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.navigateTo("510000")
            delayedViewModel.selectRegion("510000")
            delayedViewModel.showRegionPanel("510000")
            runCurrent()
            val versionBefore = delayedViewModel.navigationVersion.value
            assertEquals("510000", controller.renderState.value.pulseTarget)

            delayedViewModel.navigateTo("510000")
            runCurrent()

            assertEquals(versionBefore, delayedViewModel.navigationVersion.value)
            assertNull(delayedViewModel.selectedRegion.value)
            assertEquals(BottomPanel.None, delayedViewModel.bottomPanel.value)
            assertNull(controller.renderState.value.pulseTarget)
            assertFalse(controller.hasCameraAnimCompleteListener())
        } finally {
            delayedViewModel.mapController = null
            delayedViewModel.onCleared()
            controller.dispose()
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun delayedChildLayerCompletingAfterReload_cannotCommitStaleDrill() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "reloadDrillUser"
        )

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.selectRegion("510100")
            delayedViewModel.showRegionPanel("510100")
            runCurrent()

            val selectionBeforeRequest = delayedViewModel.selectedRegion.value
            val panelBeforeRequest = delayedViewModel.bottomPanel.value
            delayedViewModel.drillIntoRegion("510100")
            delayedViewModel.reloadData()
            runCurrent()

            assertEquals(MapZoomLevel.PROVINCIAL, delayedViewModel.currentLevel.value)
            assertEquals("510000", delayedViewModel.currentPath.value.single().id)
            assertEquals(
                listOf("510100"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertEquals(selectionBeforeRequest, delayedViewModel.selectedRegion.value)
            assertEquals(panelBeforeRequest, delayedViewModel.bottomPanel.value)
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun delayedChildLayerCompletingAfterControllerTeardown_cannotCommit() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "controllerDrillUser"
        )
        val controller = MapController()

        try {
            delayedViewModel.mapController = controller
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            runCurrent()
            delayedViewModel.selectRegion("510100")
            delayedViewModel.showRegionPanel("510100")
            runCurrent()

            val selectionBeforeRequest = delayedViewModel.selectedRegion.value
            val panelBeforeRequest = delayedViewModel.bottomPanel.value
            delayedViewModel.drillIntoRegion("510100")
            delayedViewModel.mapController = null
            runCurrent()

            assertEquals(MapZoomLevel.PROVINCIAL, delayedViewModel.currentLevel.value)
            assertEquals("510000", delayedViewModel.currentPath.value.single().id)
            assertEquals(
                listOf("510100"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertEquals(selectionBeforeRequest, delayedViewModel.selectedRegion.value)
            assertEquals(panelBeforeRequest, delayedViewModel.bottomPanel.value)
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
            controller.dispose()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelTeardown_cancelsQueuedChildLayerAndReturnsIdle() = runTest {
        seedProvinceCityAndDistrict()
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "clearedDrillUser"
        )

        runCurrent()
        delayedViewModel.drillIntoRegion("510000")
        runCurrent()
        delayedViewModel.drillIntoRegion("510100")

        delayedViewModel.onCleared()
        runCurrent()

        assertEquals(MapZoomLevel.PROVINCIAL, delayedViewModel.currentLevel.value)
        assertEquals("510000", delayedViewModel.currentPath.value.single().id)
        assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun newerDrillGeneration_supersedesQueuedRequest() = runTest {
        seedProvinceCityAndDistrict()
        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("330100", "杭州市", RegionLevel.CITY, "330000"))
        regionRepo.updateBoundary("330000", provinceBoundary)
        regionRepo.updateBoundary("330100", cityBoundary)
        val delayedViewModel = createDelayedViewModel(
            StandardTestDispatcher(testScheduler),
            userId = "newerDrillUser"
        )

        try {
            runCurrent()
            delayedViewModel.drillIntoRegion("510000")
            delayedViewModel.drillIntoRegion("330000")
            runCurrent()

            assertEquals(MapZoomLevel.PROVINCIAL, delayedViewModel.currentLevel.value)
            assertEquals("330000", delayedViewModel.currentPath.value.single().id)
            assertEquals(
                listOf("330100"),
                delayedViewModel.regions.value.map { it.regionId }
            )
            assertEquals(MapLayerLoadState.Idle, delayedViewModel.mapLayerLoadState.value)
        } finally {
            delayedViewModel.onCleared()
        }
    }

    @Test
    fun retryAfterNavigationContextChanged_doesNotRestartFailedDrill() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.updateBoundary("510000", provinceBoundary)
        viewModel.drillIntoRegion("510000")
        assertTrue(viewModel.mapLayerLoadState.value is MapLayerLoadState.Error)

        viewModel.navigateToNational()
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.updateBoundary("510100", cityBoundary)
        viewModel.retryLayerLoad()

        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        assertTrue(viewModel.currentPath.value.isEmpty())
        assertEquals(MapLayerLoadState.Idle, viewModel.mapLayerLoadState.value)
    }

    @Test
    fun navigateUp_fromNational_staysNational() {
        viewModel.navigateUp()
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    }

    private fun seedProvinceCityAndDistrict() {
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("510104", "锦江区", RegionLevel.DISTRICT, "510100"))
        regionRepo.updateBoundary("510000", provinceBoundary)
        regionRepo.updateBoundary("510100", cityBoundary)
        regionRepo.updateBoundary("510104", districtBoundary)
    }

    private fun assertNationalControllerRender(controller: MapController) {
        assertEquals(
            mapOf("510000" to OverlayRole.ACTIVE),
            controller.renderState.value.overlays.mapValues { (_, overlay) -> overlay.role }
        )
    }

    private fun createDelayedViewModel(
        dispatcher: CoroutineDispatcher,
        userId: String = "delayedDrillUser"
    ): MapViewModel = MapViewModel(
        footprintService = footprintService,
        regionRepository = regionRepo,
        footprintRepository = footprintRepo,
        attractionService = attractionService,
        userId = userId,
        dispatcher = dispatcher,
        controllerDispatcher = dispatcher
    )

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
            currentLocationProvider = FakeCurrentLocationProvider(39.95 to 116.45),
            controllerDispatcher = UnconfinedTestDispatcher()
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
            currentLocationProvider = FakeCurrentLocationProvider(39.95 to 116.45),
            controllerDispatcher = UnconfinedTestDispatcher()
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
            ),
            controllerDispatcher = StandardTestDispatcher(testScheduler)
        )

        gpsViewModel.activateCurrentLocation()
        advanceUntilIdle()

        assertEquals("110101", gpsViewModel.selectedRegion.value?.regionId)
        assertEquals(BottomPanel.Region("110101"), gpsViewModel.bottomPanel.value)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun staleCurrentLocationNullRetry_doesNotShowUnavailableMessage() = runTest {
        seedProvinceCityAndDistrict()
        val gpsViewModel = MapViewModel(
            footprintService = footprintService,
            regionRepository = regionRepo,
            footprintRepository = footprintRepo,
            attractionService = attractionService,
            regionMatcher = RegionMatcher(regionRepo),
            userId = "staleNullLocationUser",
            dispatcher = StandardTestDispatcher(testScheduler),
            currentLocationProvider = SequencedCurrentLocationProvider(
                listOf(null, null)
            ),
            controllerDispatcher = StandardTestDispatcher(testScheduler)
        )

        try {
            gpsViewModel.activateCurrentLocation()
            runCurrent()

            gpsViewModel.navigateTo("510000")
            advanceTimeBy(900L)
            runCurrent()

            assertNull(gpsViewModel.autoMarkMessage.value)
        } finally {
            gpsViewModel.onCleared()
        }
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
