package com.mapchina.ui.map

import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import com.mapchina.map.MapController
import com.mapchina.map.MapTheme
import com.mapchina.map.ViewportInsets
import com.mapchina.platform.DevicePhoto
import com.mapchina.platform.PhotoResult
import com.mapchina.platform.DevicePhotoProvider
import com.mapchina.platform.LocationProvider
import com.mapchina.domain.service.RegionMatcher
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.OverlayRole
import com.mapchina.map.OverlayStyle
import com.mapchina.map.LabelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOCATION_WARMUP_DELAY_MS = 900L

@OptIn(kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi::class)
private class ProjectedStateFlow<S, T>(
    private val source: StateFlow<S>,
    private val transform: (S) -> T
) : StateFlow<T> {
    override val value: T
        get() = transform(source.value)

    override val replayCache: List<T>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<T>): Nothing =
        source.collect(
            object : FlowCollector<S> {
                private var emitted = false
                private var previous: T? = null

                override suspend fun emit(value: S) {
                    val projected = transform(value)
                    if (!emitted || previous != projected) {
                        emitted = true
                        previous = projected
                        collector.emit(projected)
                    }
                }
            }
        )
}

private fun <S, T> StateFlow<S>.project(
    transform: (S) -> T
): StateFlow<T> = ProjectedStateFlow(this, transform)

data class AttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val description: String?,
    val visitLevel: FootprintLevel?,
    val imageUrl: String? = null
)

data class RegionFootprintUi(
    val regionId: String,
    val name: String,
    val footprintLevel: FootprintLevel?,
    val normalizedPath: List<androidx.compose.ui.geometry.Offset>,
    val bounds: RegionBounds,
    val childCoverageRate: Float = 0f
)

data class RegionBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

data class CityDot(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

data class FirstFootprintCelebration(
    val regionId: String,
    val regionName: String,
    val level: FootprintLevel
)

interface CurrentLocationProvider {
    fun getCurrentLocation(): Pair<Double, Double>?
    fun isAvailable(): Boolean
}

private class PlatformCurrentLocationProvider(
    private val delegate: LocationProvider
) : CurrentLocationProvider {
    override fun getCurrentLocation(): Pair<Double, Double>? = delegate.getCurrentLocation()

    override fun isAvailable(): Boolean = delegate.isAvailable()
}

class MapViewModel(
    private val footprintService: FootprintService,
    private val regionRepository: RegionRepository,
    private val footprintRepository: FootprintRepository,
    private val attractionService: AttractionService,
    private val boundaryLoader: BoundaryLoader? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val devicePhotoProvider: DevicePhotoProvider? = null,
    private val locationProvider: LocationProvider? = null,
    private val regionMatcher: RegionMatcher? = null,
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = "",
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val footprintSuggestionService: FootprintSuggestionService? = null,
    private val currentLocationProvider: CurrentLocationProvider? = locationProvider?.let(::PlatformCurrentLocationProvider),
    controllerDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val childLayerAvailabilityOverride: ((String) -> Boolean)? = null
) {
    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)

    val persistentMapController = MapController()

    fun onCleared() {
        invalidateLayerLoad()
        invalidateCameraIntent()
        regionFocusCoordinator.cancel()
        _mapController?.let(::enqueueControllerRetirement)
        _mapController = null
        controllerEffects.close()
        vmScope.cancel()
    }

    private fun applyMapTheme(controller: MapController) {
        val themeName = settingsRepository?.getString("map_theme")
        val theme = MapTheme.fromName(themeName)
        controller.setBackgroundTheme(theme)
    }

    fun refreshMapTheme() {
        enqueueControllerEffect(ControllerEffectScope.CONTROLLER_SCOPED) { controller, _ ->
            applyMapTheme(controller)
        }
    }

    fun enterShareMode() {
        _shareMode.value = true
        com.mapchina.ui.ShareModeState.value = true
        enqueueControllerEffect(ControllerEffectScope.CONTROLLER_SCOPED) { controller, _ ->
            controller.setShareMode(true)
        }
    }

    fun exitShareMode() {
        _shareMode.value = false
        com.mapchina.ui.ShareModeState.value = false
        enqueueControllerEffect(ControllerEffectScope.CONTROLLER_SCOPED) { controller, _ ->
            controller.setShareMode(false)
        }
    }

    private data class PreparedChildLayer(
        val parent: Region,
        val children: List<Region>
    )

    private data class LayerLoadRequest(
        val generation: Long,
        val regionId: String,
        val sourcePath: List<String>,
        val sourceLevel: MapZoomLevel
    )

    private data class NavigationState(
        val navigationVersion: Long = 0L,
        val currentLevel: MapZoomLevel = MapZoomLevel.NATIONAL,
        val currentPath: List<Region> = emptyList(),
        val currentRegions: List<RegionFootprintUi> = emptyList(),
        val currentAttractions: List<AttractionUi> = emptyList(),
        val attractionsRegionId: String? = null,
        val selectedRegion: RegionFootprintUi? = null,
        val selectedRegionAttractions: List<AttractionUi> = emptyList(),
        val bottomPanel: BottomPanel = BottomPanel.None,
        val mapLayerLoadState: MapLayerLoadState = MapLayerLoadState.Idle,
        val layerLoadGeneration: Long = 0L,
        val activeLayerLoadRequest: LayerLoadRequest? = null,
        val failedLayerLoadRequest: LayerLoadRequest? = null,
        val committedLayerGeneration: Long? = null
    )

    private enum class ControllerEffectScope {
        NAVIGATION_SCOPED,
        CONTROLLER_SCOPED,
        CAMERA_INTENT_SCOPED,
        PHOTO_INTENT_SCOPED
    }

    private sealed interface ControllerCommand {
        val controller: MapController

        data class Effect(
            val scope: ControllerEffectScope,
            val navigationVersion: Long,
            val cameraIntentGeneration: Long,
            val photoIntentGeneration: Long,
            override val controller: MapController,
            val apply: (MapController, NavigationState) -> Unit
        ) : ControllerCommand

        data class InstallPresentation(
            val owner: Long,
            val navigationVersion: Long,
            override val controller: MapController,
            val apply: (MapController, NavigationState) -> Unit
        ) : ControllerCommand

        data class CleanupPresentation(
            val owner: Long,
            override val controller: MapController
        ) : ControllerCommand

        data class CleanupCurrentPresentation(
            override val controller: MapController
        ) : ControllerCommand

        data class ForceCleanupPresentation(
            override val controller: MapController
        ) : ControllerCommand

        data class RetireController(
            override val controller: MapController
        ) : ControllerCommand
    }

    private data class ControllerEffectSnapshot(
        val navigationVersion: Long,
        val cameraIntentGeneration: Long,
        val photoIntentGeneration: Long,
        val controller: MapController
    )

    private data class NavigationContextSnapshot(
        val navigationVersion: Long,
        val sourcePath: List<String>,
        val sourceLevel: MapZoomLevel
    )

    private data class PhotoMarkerIntent(
        val generation: Long = 0L,
        val visible: Boolean = false
    )

    private val navigationState = MutableStateFlow(NavigationState())
    private val cameraIntentGeneration = MutableStateFlow(0L)
    private val presentationOwnerSequence = MutableStateFlow(0L)
    private val photoMarkerIntent = MutableStateFlow(PhotoMarkerIntent())

    internal val navigationVersion: StateFlow<Long> =
        navigationState.project { it.navigationVersion }

    private val _currentLevel = navigationState.project { it.currentLevel }
    val currentLevel: StateFlow<MapZoomLevel> = _currentLevel

    private val _currentPath = navigationState.project { it.currentPath }
    val currentPath: StateFlow<List<Region>> = _currentPath

    private val _mapLayerLoadState = navigationState.project { it.mapLayerLoadState }
    val mapLayerLoadState: StateFlow<MapLayerLoadState> = _mapLayerLoadState

    internal var afterLayerLoadValidation: (suspend () -> Unit)? = null
    internal var afterChildLayerEffectSnapshot: (suspend () -> Unit)? = null
    internal var afterAttractionsLoad: (suspend (String) -> Unit)? = null
    internal var afterOrdinaryLayerPrepared: (suspend (String) -> Unit)? = null
    internal var afterPhotoLoad: (suspend () -> Unit)? = null
    internal var photoLoadOverride:
        (() -> Pair<PhotoResult, List<DevicePhoto>>)? = null
    internal var afterCurrentLocationRead: (suspend () -> Unit)? = null

    private val _achievementUnlock = MutableStateFlow<AchievementUnlockResult?>(null)
    val achievementUnlock: StateFlow<AchievementUnlockResult?> = _achievementUnlock.asStateFlow()

    private val _drillDownHint = MutableStateFlow<String?>(null)
    val drillDownHint: StateFlow<String?> = _drillDownHint.asStateFlow()

    private val _regions = navigationState.project { it.currentRegions }
    val regions: StateFlow<List<RegionFootprintUi>> = _regions

    private val _selectedRegion = navigationState.project { it.selectedRegion }
    val selectedRegion: StateFlow<RegionFootprintUi?> = _selectedRegion

    private val _attractions = navigationState.project { it.currentAttractions }
    val attractions: StateFlow<List<AttractionUi>> = _attractions

    private val _selectedRegionAttractions =
        navigationState.project { it.selectedRegionAttractions }
    val selectedRegionAttractions: StateFlow<List<AttractionUi>> =
        _selectedRegionAttractions

    private val _photoClusters = MutableStateFlow<List<PhotoCluster>>(emptyList())
    val photoClusters: StateFlow<List<PhotoCluster>> = _photoClusters.asStateFlow()

    val photoMarkersVisible: StateFlow<Boolean> =
        photoMarkerIntent.project { it.visible }

    private val _autoMarkMessage = MutableStateFlow<String?>(null)
    val autoMarkMessage: StateFlow<String?> = _autoMarkMessage.asStateFlow()

    val footprintSuggestions: StateFlow<List<com.mapchina.domain.model.FootprintSuggestion>> =
        footprintSuggestionService?.suggestions ?: MutableStateFlow(emptyList())

    private val _bottomPanel = navigationState.project { it.bottomPanel }
    val bottomPanel: StateFlow<BottomPanel> = _bottomPanel

    private val _previewAttraction = MutableStateFlow<AttractionUi?>(null)
    val previewAttraction: StateFlow<AttractionUi?> = _previewAttraction.asStateFlow()

    private val _shareMode = MutableStateFlow(false)
    val shareMode: StateFlow<Boolean> = _shareMode.asStateFlow()

    private val _firstFootprintActivation = MutableStateFlow(!hasAnyFootprint())
    val firstFootprintActivation: StateFlow<Boolean> = _firstFootprintActivation.asStateFlow()

    private val _firstFootprintCelebration = MutableStateFlow<FirstFootprintCelebration?>(null)
    val firstFootprintCelebration: StateFlow<FirstFootprintCelebration?> =
        _firstFootprintCelebration.asStateFlow()
    private val firstFootprintPersistenceInFlight = MutableStateFlow(false)

    private var footprintCache: Map<String, FootprintLevel>? = null

    private var childrenIndex: Map<String, List<String>> = emptyMap()
    private var childrenIndexReady = false
    private val childLayerAvailabilityCache = mutableMapOf<String, Boolean>()

    private var provinceBoundaryCache: Map<String, String> = emptyMap()
    private var provinceCenterCache: Map<String, Pair<Double, Double>> = emptyMap()
    private var provinceNameCache: Map<String, String> = emptyMap()
    private val regionFocusCoordinator = RegionFocusCoordinator()
    val regionFocusState: StateFlow<RegionFocusState> =
        regionFocusCoordinator.state

    private fun invalidateCaches() {
        footprintCache = null
        attractionVisitsCache = null
        attractionCountCache.clear()
    }

    private fun hasAnyFootprint(): Boolean =
        footprintRepository.getFootprintsByUser(userId).isNotEmpty() ||
            footprintRepository.getAttractionVisitsByUser(userId).isNotEmpty()

    private fun refreshFirstFootprintActivation() {
        if (firstFootprintPersistenceInFlight.value) return
        _firstFootprintActivation.value = !hasAnyFootprint()
    }

    private fun completeFirstFootprintIfNeeded(regionId: String, level: FootprintLevel) {
        if (!_firstFootprintActivation.value) return
        val regionName = regionRepository.getRegion(regionId)?.name ?: return
        _firstFootprintActivation.value = false
        _firstFootprintCelebration.value = FirstFootprintCelebration(
            regionId = regionId,
            regionName = regionName,
            level = level
        )
    }

    private var _mapController: MapController? = null
    private var _programmaticCamera = false
    private var _programmaticCameraJob: kotlinx.coroutines.Job? = null
    private var lastBoundaries: Map<String, String>? = null
    private var lastSyncedRegionIds: Set<String> = emptySet()

    // Persisted camera state (survives MapController recreation)
    private var savedCameraLat: Double = 35.5
    private var savedCameraLng: Double = 104.0
    private var savedCameraZoom: Float = 3.5f

    private val controllerEffectSupervisor = SupervisorJob()
    private val controllerEffectScope =
        CoroutineScope(controllerEffectSupervisor + controllerDispatcher)
    private val controllerEffects =
        Channel<ControllerCommand>(Channel.UNLIMITED)
    private val controllerEffectConsumer = controllerEffectScope.launch {
        val presentationOwners = mutableMapOf<MapController, Long>()
        try {
            for (command in controllerEffects) {
                val latest = navigationState.value
                when (command) {
                    is ControllerCommand.Effect -> {
                        if (isControllerEffectCurrent(command, latest)) {
                            command.apply(command.controller, latest)
                        }
                    }
                    is ControllerCommand.InstallPresentation -> {
                        if (
                            _mapController === command.controller &&
                            latest.navigationVersion == command.navigationVersion
                        ) {
                            presentationOwners[command.controller] = command.owner
                            command.apply(command.controller, latest)
                        }
                    }
                    is ControllerCommand.CleanupPresentation -> {
                        if (presentationOwners[command.controller] == command.owner) {
                            presentationOwners.remove(command.controller)
                            cleanupControllerPresentation(command.controller)
                        }
                    }
                    is ControllerCommand.CleanupCurrentPresentation -> {
                        if (presentationOwners.remove(command.controller) != null) {
                            cleanupControllerPresentation(command.controller)
                        }
                    }
                    is ControllerCommand.ForceCleanupPresentation -> {
                        presentationOwners.remove(command.controller)
                        cleanupControllerPresentation(command.controller)
                    }
                    is ControllerCommand.RetireController -> {
                        presentationOwners.remove(command.controller)
                        cleanupControllerPresentation(command.controller)
                        command.controller.setOnCameraZoomChangeListener(null)
                        command.controller.setOnCameraPositionListener(null)
                    }
                }
            }
        } finally {
            controllerEffectSupervisor.complete()
        }
    }

    private fun isControllerEffectCurrent(
        effect: ControllerCommand.Effect,
        latest: NavigationState
    ): Boolean {
        if (_mapController !== effect.controller) return false
        return when (effect.scope) {
            ControllerEffectScope.NAVIGATION_SCOPED ->
                latest.navigationVersion == effect.navigationVersion
            ControllerEffectScope.CONTROLLER_SCOPED -> true
            ControllerEffectScope.CAMERA_INTENT_SCOPED ->
                cameraIntentGeneration.value == effect.cameraIntentGeneration
            ControllerEffectScope.PHOTO_INTENT_SCOPED -> {
                val intent = photoMarkerIntent.value
                intent.visible &&
                    intent.generation == effect.photoIntentGeneration
            }
        }
    }

    private fun cleanupControllerPresentation(controller: MapController) {
        controller.restorePulsedOverlay()
        controller.setOnCameraAnimCompleteListener(null)
        controller.cancelCameraAnimation()
    }

    private fun captureControllerEffect(): ControllerEffectSnapshot? {
        val controller = _mapController ?: return null
        return ControllerEffectSnapshot(
            navigationVersion = navigationState.value.navigationVersion,
            cameraIntentGeneration = cameraIntentGeneration.value,
            photoIntentGeneration = photoMarkerIntent.value.generation,
            controller = controller
        )
    }

    private fun enqueueControllerEffect(
        scope: ControllerEffectScope,
        snapshot: ControllerEffectSnapshot? = captureControllerEffect(),
        apply: (MapController, NavigationState) -> Unit
    ) {
        snapshot ?: return
        controllerEffects.trySend(
            ControllerCommand.Effect(
                scope = scope,
                navigationVersion = snapshot.navigationVersion,
                cameraIntentGeneration = snapshot.cameraIntentGeneration,
                photoIntentGeneration = snapshot.photoIntentGeneration,
                controller = snapshot.controller,
                apply = apply
            )
        )
    }

    private fun nextPresentationOwner(): Long =
        presentationOwnerSequence.updateAndGet { it + 1L }

    private fun enqueuePresentationInstall(
        owner: Long,
        snapshot: ControllerEffectSnapshot? = captureControllerEffect(),
        apply: (MapController, NavigationState) -> Unit
    ) {
        snapshot ?: return
        controllerEffects.trySend(
            ControllerCommand.InstallPresentation(
                owner = owner,
                navigationVersion = snapshot.navigationVersion,
                controller = snapshot.controller,
                apply = apply
            )
        )
    }

    private fun enqueuePresentationCleanup(
        owner: Long,
        controller: MapController
    ) {
        controllerEffects.trySend(
            ControllerCommand.CleanupPresentation(owner, controller)
        )
    }

    private fun enqueueCurrentPresentationCleanup(
        controller: MapController? = _mapController
    ) {
        controller ?: return
        controllerEffects.trySend(
            ControllerCommand.CleanupCurrentPresentation(controller)
        )
    }

    private fun enqueueForcePresentationCleanup(
        controller: MapController? = _mapController
    ) {
        controller ?: return
        controllerEffects.trySend(
            ControllerCommand.ForceCleanupPresentation(controller)
        )
    }

    private fun enqueueControllerRetirement(controller: MapController) {
        controllerEffects.trySend(ControllerCommand.RetireController(controller))
    }

    internal suspend fun awaitControllerEffectsDrained() {
        controllerEffectConsumer.join()
    }

    var mapController: MapController?
        get() = _mapController
        set(value) {
            if (_mapController === value) return
            val previous = _mapController
            invalidateLayerLoad()
            invalidateCameraIntent()
            regionFocusCoordinator.cancel()
            if (previous != null) {
                enqueueControllerRetirement(previous)
            }
            _mapController = value
            if (value != null) {
                lastSyncedRegionIds = emptySet()
                enqueueControllerEffect(ControllerEffectScope.CONTROLLER_SCOPED) { controller, _ ->
                    applyMapTheme(controller)
                    controller.setOnCameraZoomChangeListener { zoom ->
                        savedCameraZoom = zoom
                        onCameraZoomChanged(zoom)
                    }
                    controller.setOnCameraPositionListener { lat, lng, zoom ->
                        savedCameraLat = lat
                        savedCameraLng = lng
                        savedCameraZoom = zoom
                    }
                }
                enqueueOverlayReconcile()
            }
        }

    private val attractionCountCache = mutableMapOf<String, Int>()
    private var attractionVisitsCache: Map<String, FootprintLevel>? = null

    init {
        val initialContext = captureNavigationContext()
        vmScope.launch {
            loadTopLevelRegions(initialContext)
            rebuildChildrenIndex()
            childrenIndexReady = true
            refreshCoverage()
        }
    }

    fun reloadData() {
        invalidateLayerLoad()
        val expectedContext = captureNavigationContext()
        cancelRegionFocus()
        invalidateCaches()
        refreshFirstFootprintActivation()
        _programmaticCamera = true
        vmScope.launch {
            refreshRegions(expectedContext)
            if (!childrenIndexReady) {
                rebuildChildrenIndex()
                childrenIndexReady = true
            }
            refreshCoverage()
        }
    }

    private fun getFootprintCache(): Map<String, FootprintLevel> {
        if (footprintCache == null) {
            footprintCache = footprintRepository.getFootprintsByUser(userId)
                .associate { it.regionId to it.level }
        }
        return footprintCache!!
    }

    private fun getAttractionVisitsCache(): Map<String, FootprintLevel> {
        if (attractionVisitsCache == null) {
            attractionVisitsCache = footprintRepository.getAttractionVisitsByUser(userId)
                .associate { it.attractionId to it.level }
        }
        return attractionVisitsCache!!
    }

    private fun rebuildChildrenIndex() {
        val index = mutableMapOf<String, MutableList<String>>()
        val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        for (city in allCities) {
            val parentId = city.parentId ?: continue
            index.getOrPut(parentId) { mutableListOf() }.add(city.id)
        }
        val allDistricts = regionRepository.getRegionsByLevel(RegionLevel.DISTRICT)
        for (district in allDistricts) {
            val parentId = district.parentId ?: continue
            index.getOrPut(parentId) { mutableListOf() }.add(district.id)
        }
        childrenIndex = index
    }

    fun drillIntoRegion(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        val hasChildLayerAvailability = boundaryLoader != null || childLayerAvailabilityOverride != null
        if (hasChildLayerAvailability && !canDrillIntoRegion(regionId)) return
        val label = when (region.level) {
            RegionLevel.PROVINCE -> "正在展开市级地图"
            RegionLevel.CITY -> "正在展开区级地图"
            RegionLevel.DISTRICT -> return
        }
        val request = beginLayerLoad(regionId, label) ?: return
        invalidateCameraIntent()
        launchLayerLoad(request, region)
    }

    private fun launchLayerLoad(request: LayerLoadRequest, region: Region) {
        vmScope.launch {
            val prepared = runCatching { prepareChildLayer(region) }.getOrNull()
            val preparedRegions = prepared?.let { buildRegionUi(it.children) }
            afterLayerLoadValidation?.invoke()
            if (prepared == null) {
                reduceLayerLoadError(request, region)
                return@launch
            }
            commitChildLayer(request, prepared, preparedRegions.orEmpty())
        }
    }

    private fun beginLayerLoad(
        regionId: String,
        label: String,
        retryRequest: LayerLoadRequest? = null
    ): LayerLoadRequest? {
        val updated = navigationState.updateAndGet { state ->
            if (retryRequest != null && !canRetryLayerLoad(state, retryRequest)) {
                return@updateAndGet state.withInvalidatedLayerLoad()
            }
            if (state.currentPath.any { it.id == regionId }) {
                return@updateAndGet state
            }
            val generation = state.layerLoadGeneration + 1L
            val request = LayerLoadRequest(
                generation = generation,
                regionId = regionId,
                sourcePath = state.currentPath.map { it.id },
                sourceLevel = state.currentLevel
            )
            state.copy(
                mapLayerLoadState = MapLayerLoadState.Loading(regionId, label),
                layerLoadGeneration = generation,
                activeLayerLoadRequest = request,
                failedLayerLoadRequest = null,
                committedLayerGeneration = null
            )
        }
        if (updated.currentPath.any { it.id == regionId }) return null
        return updated.activeLayerLoadRequest?.takeIf { request ->
            request.regionId == regionId &&
                request.generation == updated.layerLoadGeneration
        }
    }

    private fun isCurrentLayerLoad(
        state: NavigationState,
        request: LayerLoadRequest
    ): Boolean =
        state.activeLayerLoadRequest == request &&
            sourceContextMatches(state, request)

    private fun sourceContextMatches(
        state: NavigationState,
        request: LayerLoadRequest
    ): Boolean =
        state.currentLevel == request.sourceLevel &&
            state.currentPath.map { it.id } == request.sourcePath

    private fun canRetryLayerLoad(
        state: NavigationState,
        request: LayerLoadRequest
    ): Boolean {
        val error = state.mapLayerLoadState as? MapLayerLoadState.Error
            ?: return false
        return state.failedLayerLoadRequest == request &&
            error.regionId == request.regionId &&
            sourceContextMatches(state, request)
    }

    private fun NavigationState.withInvalidatedLayerLoad(): NavigationState =
        copy(
            mapLayerLoadState = MapLayerLoadState.Idle,
            layerLoadGeneration = layerLoadGeneration + 1L,
            activeLayerLoadRequest = null,
            failedLayerLoadRequest = null,
            committedLayerGeneration = null
        )

    private fun NavigationState.withInvalidatedNavigation(): NavigationState =
        withInvalidatedLayerLoad().copy(
            navigationVersion = navigationVersion + 1L
        )

    private fun beginCameraIntent(): Long =
        cameraIntentGeneration.updateAndGet { it + 1L }

    private fun invalidateCameraIntent() {
        beginCameraIntent()
    }

    private fun isCurrentCameraIntent(generation: Long): Boolean =
        cameraIntentGeneration.value == generation

    private fun invalidateLayerLoad() {
        navigationState.update { it.withInvalidatedNavigation() }
    }

    private fun prepareChildLayer(parent: Region): PreparedChildLayer? {
        var children = regionRepository.getChildRegions(parent.id)
        var boundaries = regionRepository.getBoundariesByParentId(parent.id)
        if (!isChildLayerReady(children, boundaries) && boundaryLoader != null) {
            val loaded = boundaryLoader.loadChildRegions(parent.id).orEmpty()
            if (loaded.isNotEmpty()) {
                val level = when (parent.level) {
                    RegionLevel.PROVINCE -> RegionLevel.CITY
                    RegionLevel.CITY -> RegionLevel.DISTRICT
                    RegionLevel.DISTRICT -> return null
                }
                regionRepository.insertRegionsInTransaction(
                    loaded.map { Region(it.id, it.name, level, parent.id) }
                )
                regionRepository.updateBoundariesInTransaction(
                    loaded.map { it.id to it.boundary }
                )
                children = regionRepository.getChildRegions(parent.id)
                boundaries = regionRepository.getBoundariesByParentId(parent.id)
                rebuildChildrenIndex()
            }
        }
        if (!isChildLayerReady(children, boundaries)) return null
        return PreparedChildLayer(parent, children)
    }

    private fun isChildLayerReady(
        children: List<Region>,
        boundaries: Map<String, String>
    ): Boolean =
        children.isNotEmpty() &&
            children.all { child -> !boundaries[child.id].isNullOrBlank() }

    private suspend fun commitChildLayer(
        request: LayerLoadRequest,
        prepared: PreparedChildLayer,
        preparedRegions: List<RegionFootprintUi>
    ) {
        val committed = navigationState.updateAndGet { state ->
            if (!isCurrentLayerLoad(state, request)) return@updateAndGet state
            state.copy(
                navigationVersion = state.navigationVersion + 1L,
                currentPath = state.currentPath + prepared.parent,
                currentLevel = when (prepared.parent.level) {
                    RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
                    RegionLevel.CITY -> MapZoomLevel.CITY
                    RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
                },
                currentRegions = preparedRegions,
                currentAttractions = emptyList(),
                attractionsRegionId = null,
                selectedRegion = null,
                selectedRegionAttractions = emptyList(),
                bottomPanel = BottomPanel.None,
                mapLayerLoadState = MapLayerLoadState.Idle,
                activeLayerLoadRequest = null,
                failedLayerLoadRequest = null,
                committedLayerGeneration = request.generation
            )
        }
        if (committed.committedLayerGeneration != request.generation) return

        cancelRegionFocus()
        val latest = navigationState.value
        val effectSnapshot = captureControllerEffect()
        afterChildLayerEffectSnapshot?.invoke()
        enqueueOverlayReconcile(effectSnapshot)
        if (latest.committedLayerGeneration == request.generation) {
            vmScope.launch {
                loadAttractionsForRegion(
                    regionId = prepared.parent.id,
                    navigationVersion = latest.navigationVersion
                )
            }
        }
    }

    private fun buildRegionUi(children: List<Region>): List<RegionFootprintUi> {
        val footprints = getFootprintCache()
        val coverage = if (childrenIndexReady) {
            computeCoverageBatch(children.map { it.id }, footprints)
        } else {
            emptyMap()
        }
        return children.map { region ->
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = coverage[region.id] ?: 0f
            )
        }
    }

    private fun reduceLayerLoadError(
        request: LayerLoadRequest,
        region: Region
    ) {
        val targetLabel =
            if (region.level == RegionLevel.PROVINCE) "市级" else "区级"
        navigationState.update { state ->
            if (!isCurrentLayerLoad(state, request)) return@update state
            state.copy(
                mapLayerLoadState = MapLayerLoadState.Error(
                    regionId = request.regionId,
                    message = "${targetLabel}地图暂时无法展开"
                ),
                activeLayerLoadRequest = null,
                failedLayerLoadRequest = request,
                committedLayerGeneration = null
            )
        }
    }

    private fun levelAfterDrill(parent: Region): MapZoomLevel =
        when (parent.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }

    fun retryLayerLoad() {
        val failedRequest = navigationState.value.failedLayerLoadRequest ?: return
        val region = regionRepository.getRegion(failedRequest.regionId) ?: return
        val label = when (region.level) {
            RegionLevel.PROVINCE -> "正在展开市级地图"
            RegionLevel.CITY -> "正在展开区级地图"
            RegionLevel.DISTRICT -> return
        }
        val request = beginLayerLoad(
            regionId = failedRequest.regionId,
            label = label,
            retryRequest = failedRequest
        ) ?: return
        invalidateCameraIntent()
        launchLayerLoad(request, region)
    }

    fun dismissLayerLoadError() {
        navigationState.update { state ->
            state.copy(
                failedLayerLoadRequest = null,
                mapLayerLoadState = MapLayerLoadState.Idle
            )
        }
    }

    private fun setProgrammaticCamera() {
        _programmaticCamera = true
        _programmaticCameraJob?.cancel()
        _programmaticCameraJob = vmScope.launch {
            kotlinx.coroutines.delay(1500L)
            _programmaticCamera = false
        }
    }

    private fun clearNavigationPresentationState() {
        regionFocusCoordinator.cancel()
        navigationState.update {
            it.copy(
                selectedRegion = null,
                selectedRegionAttractions = emptyList(),
                bottomPanel = BottomPanel.None
            )
        }
        _previewAttraction.value = null
    }

    private fun cleanupNoOpNavigationPresentation() {
        clearNavigationPresentationState()
        enqueueCurrentPresentationCleanup()
    }

    private fun cleanupChangedNavigationPresentation() {
        clearNavigationPresentationState()
        enqueueForcePresentationCleanup()
    }

    fun navigateToNational() {
        invalidateCameraIntent()
        val beforeNavigation = navigationState.value
        if (
            beforeNavigation.currentLevel == MapZoomLevel.NATIONAL &&
            beforeNavigation.currentPath.isEmpty() &&
            beforeNavigation.mapLayerLoadState == MapLayerLoadState.Idle &&
            beforeNavigation.activeLayerLoadRequest == null &&
            beforeNavigation.failedLayerLoadRequest == null
        ) {
            cleanupNoOpNavigationPresentation()
            return
        }
        val updated = navigationState.updateAndGet { state ->
            state.withInvalidatedNavigation().copy(
                currentLevel = MapZoomLevel.NATIONAL,
                currentPath = emptyList(),
                currentAttractions = emptyList(),
                attractionsRegionId = null
            )
        }
        val targetContext = updated.toNavigationContextSnapshot()
        cleanupChangedNavigationPresentation()
        enqueueControllerEffect(ControllerEffectScope.NAVIGATION_SCOPED) { controller, _ ->
            val target = controller.viewport.computeChinaFitTarget()
            savedCameraLat = target.first
            savedCameraLng = target.second
            savedCameraZoom = target.third
            setProgrammaticCamera()
            controller.fitChinaInView(true)
            controller.clearMarkers()
        }
        vmScope.launch {
            loadTopLevelRegions(targetContext)
        }
    }

    fun moveToCurrentLocation() {
        val provider = locationProvider ?: return
        val cameraIntent = beginCameraIntent()
        vmScope.launch {
            val location = provider.getCurrentLocation() ?: return@launch
            afterCurrentLocationRead?.invoke()
            if (!isCurrentCameraIntent(cameraIntent)) return@launch
            savedCameraLat = location.first
            savedCameraLng = location.second
            savedCameraZoom = 10f
            setProgrammaticCamera()
            captureControllerEffect()?.takeIf {
                it.cameraIntentGeneration == cameraIntent
            }?.let { effectSnapshot ->
                enqueueControllerEffect(
                    ControllerEffectScope.CAMERA_INTENT_SCOPED,
                    effectSnapshot
                ) { controller, _ ->
                    controller.setCamera(location.first, location.second, 10f, true)
                }
            }
        }
    }

    fun activateCurrentLocation() {
        val provider = currentLocationProvider
        val matcher = regionMatcher
        if (provider == null || matcher == null || !provider.isAvailable()) {
            showAutoMarkMessage("暂时无法获取当前位置")
            return
        }
        val locationIntent = beginCameraIntent()
        vmScope.launch {
            val location = provider.getCurrentLocation()
                ?: run {
                    delay(LOCATION_WARMUP_DELAY_MS)
                    provider.getCurrentLocation()
                }
            if (!isCurrentCameraIntent(locationIntent)) return@launch
            if (location == null) {
                showAutoMarkMessage("暂时无法获取当前位置")
                return@launch
            }
            afterCurrentLocationRead?.invoke()
            if (!isCurrentCameraIntent(locationIntent)) return@launch
            val match = matcher.match(location.first, location.second)
            val target = match.district ?: match.city ?: match.province
            if (target == null) {
                showAutoMarkMessage("当前位置暂未匹配到地区")
                return@launch
            }

            val parentId = target.parentId
            if (parentId != null) {
                navigateTo(parentId)
            } else {
                navigateToNational()
            }
            val cameraEffectSnapshot = captureControllerEffect()
            savedCameraLat = location.first
            savedCameraLng = location.second
            val targetZoom = when (target.level) {
                RegionLevel.PROVINCE -> 5.5f
                RegionLevel.CITY -> 8f
                RegionLevel.DISTRICT -> 10f
            }
            savedCameraZoom = targetZoom
            setProgrammaticCamera()
            if (cameraEffectSnapshot != null) {
                enqueueControllerEffect(
                    ControllerEffectScope.CAMERA_INTENT_SCOPED,
                    cameraEffectSnapshot
                ) { controller, _ ->
                    controller.setCamera(
                        location.first,
                        location.second,
                        targetZoom,
                        true
                    )
                }
            }
            selectRegion(target.id)
            showRegionPanel(target.id)
        }
    }

    fun navigateUp() {
        invalidateCameraIntent()
        val pathBeforeNavigation = navigationState.value.currentPath
        val beforeNavigation = navigationState.value
        if (
            pathBeforeNavigation.isEmpty() &&
            beforeNavigation.currentLevel == MapZoomLevel.NATIONAL &&
            beforeNavigation.mapLayerLoadState == MapLayerLoadState.Idle &&
            beforeNavigation.activeLayerLoadRequest == null &&
            beforeNavigation.failedLayerLoadRequest == null
        ) {
            cleanupNoOpNavigationPresentation()
            return
        }
        val updated = navigationState.updateAndGet { state ->
            val invalidated = state.withInvalidatedNavigation()
            when {
                state.currentPath.size > 1 -> {
                    val path = state.currentPath.dropLast(1)
                    invalidated.copy(
                        currentPath = path,
                        currentLevel = levelAfterDrill(path.last()),
                        currentAttractions = emptyList(),
                        attractionsRegionId = null
                    )
                }
                state.currentPath.size == 1 -> invalidated.copy(
                    currentLevel = MapZoomLevel.NATIONAL,
                    currentPath = emptyList(),
                    currentAttractions = emptyList(),
                    attractionsRegionId = null
                )
                else -> invalidated
            }
        }
        val targetContext = updated.toNavigationContextSnapshot()
        cleanupChangedNavigationPresentation()
        val parent = updated.currentPath.lastOrNull()
        if (parent != null) {
            moveCameraToRegion(parent)

            vmScope.launch {
                loadChildRegions(parent.id, targetContext)
                loadAttractionsForRegion(parent.id, updated.navigationVersion)
            }
        } else if (pathBeforeNavigation.isNotEmpty()) {
            enqueueControllerEffect(ControllerEffectScope.NAVIGATION_SCOPED) { controller, _ ->
                val target = controller.viewport.computeChinaFitTarget()
                savedCameraLat = target.first
                savedCameraLng = target.second
                savedCameraZoom = target.third
                setProgrammaticCamera()
                controller.fitChinaInView(true)
                controller.clearMarkers()
            }

            vmScope.launch {
                loadTopLevelRegions(targetContext)
            }
        }
    }

    fun navigateTo(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        val path = buildPathTo(regionId)
        val level = levelAfterDrill(region)
        invalidateCameraIntent()
        val beforeNavigation = navigationState.value
        if (
            beforeNavigation.currentPath.map { it.id } == path.map { it.id } &&
            beforeNavigation.currentLevel == level &&
            beforeNavigation.mapLayerLoadState == MapLayerLoadState.Idle &&
            beforeNavigation.activeLayerLoadRequest == null &&
            beforeNavigation.failedLayerLoadRequest == null
        ) {
            cleanupNoOpNavigationPresentation()
            return
        }
        val updated = navigationState.updateAndGet { state ->
            state.withInvalidatedNavigation().copy(
                currentPath = path,
                currentLevel = level,
                currentAttractions = emptyList(),
                attractionsRegionId = null,
                selectedRegion = null
            )
        }

        val targetContext = updated.toNavigationContextSnapshot()
        cleanupChangedNavigationPresentation()
        val presentationOwner = nextPresentationOwner()
        enqueuePresentationInstall(presentationOwner) { controller, state ->
            controller.pulseOverlay(regionId)
            controller.setOnCameraAnimCompleteListener {
                val completionSnapshot = ControllerEffectSnapshot(
                    navigationVersion = state.navigationVersion,
                    cameraIntentGeneration = cameraIntentGeneration.value,
                    photoIntentGeneration = photoMarkerIntent.value.generation,
                    controller = controller
                )
                enqueuePresentationCleanup(presentationOwner, controller)
                enqueueControllerEffect(
                    ControllerEffectScope.NAVIGATION_SCOPED,
                    completionSnapshot
                ) { _, _ ->
                    vmScope.launch {
                        loadChildRegions(regionId, targetContext)
                        loadAttractionsForRegion(regionId, updated.navigationVersion)
                    }
                }
            }
        }

        moveCameraToRegion(region)
    }

    fun selectRegion(regionId: String) {
        val fromList = _regions.value.find { it.regionId == regionId }
        val selected = if (fromList != null) {
            fromList
        } else {
            val region = regionRepository.getRegion(regionId) ?: return
            val footprints = getFootprintCache()
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f)
            )
        }
        val updated = navigationState.updateAndGet {
            it.copy(
                selectedRegion = selected,
                selectedRegionAttractions = emptyList()
            )
        }
        vmScope.launch {
            loadAttractionsForSelectedRegion(regionId, updated.navigationVersion)
        }
    }

    fun focusRegion(
        regionId: String,
        insets: ViewportInsets,
        reducedMotion: Boolean
    ): Long? {
        val region = regionRepository.getRegion(regionId) ?: return null
        beginCameraIntent()
        selectRegion(regionId)
        clearBottomPanel()
        val focusRequest = regionFocusCoordinator.begin(regionId)
        if (_mapController == null) {
            regionFocusCoordinator.complete(focusRequest)
            return focusRequest
        }
        val duration = regionFocusDurationMillis(reducedMotion)
        val bounds = regionRepository.getRegionBounds(regionId)
        val complete: (Long) -> Unit = {
            regionFocusCoordinator.complete(focusRequest)
        }
        val presentationOwner = nextPresentationOwner()
        enqueuePresentationInstall(presentationOwner) { controller, _ ->
            controller.pulseOverlay(regionId)
            if (bounds != null) {
                controller.focusBounds(
                    minLng = bounds.minLng,
                    maxLng = bounds.maxLng,
                    minLat = bounds.minLat,
                    maxLat = bounds.maxLat,
                    insets = insets,
                    durationMillis = duration,
                    onComplete = complete
                )
            } else {
                val center = regionRepository.getRegionCenter(regionId)
                if (center == null) {
                    regionFocusCoordinator.complete(focusRequest)
                } else {
                    val zoom = when (region.level) {
                        RegionLevel.PROVINCE -> 7f
                        RegionLevel.CITY -> 9f
                        RegionLevel.DISTRICT -> 11f
                    }
                    controller.focusCamera(
                        lat = center.first,
                        lng = center.second,
                        zoomLevel = zoom,
                        insets = insets,
                        durationMillis = duration,
                        onComplete = complete
                    )
                }
            }
        }
        return focusRequest
    }

    fun cancelRegionFocus() {
        regionFocusCoordinator.cancel()
        enqueueForcePresentationCleanup()
    }

    fun clearSelection() {
        cancelRegionFocus()
        navigationState.update {
            it.copy(
                selectedRegion = null,
                selectedRegionAttractions = emptyList()
            )
        }
    }

    fun dismissAchievementUnlock() {
        _achievementUnlock.value = null
    }

    fun dismissFirstFootprintCelebration() {
        _firstFootprintCelebration.value = null
    }

    fun getAchievementName(id: String): String =
        achievementRepository?.getDefinitionById(id)?.name ?: id

    fun getAchievementDescription(id: String): String =
        achievementRepository?.getDefinitionById(id)?.description ?: ""

    fun getAchievementRarity(id: String): String =
        achievementRepository?.getDefinitionById(id)?.rarity?.name ?: "COMMON"

    fun getSavedCameraState(): Triple<Double, Double, Float> =
        Triple(savedCameraLat, savedCameraLng, savedCameraZoom)

    fun dismissDrillDownHint() {
        _drillDownHint.value = null
    }

    fun canDrillIntoRegion(regionId: String): Boolean {
        val hasChildren = !childrenIndex[regionId].isNullOrEmpty()
        if (hasChildren) return true
        val region = regionRepository.getRegion(regionId) ?: return false
        if (region.level == RegionLevel.DISTRICT) return false
        return childLayerAvailabilityCache.getOrPut(regionId) {
            childLayerAvailabilityOverride?.invoke(regionId)
                ?: (boundaryLoader?.hasChildRegions(regionId) == true)
        }
    }

    fun getAttractionCountForRegion(regionId: String): Int {
        return attractionCountCache.getOrPut(regionId) {
            attractionService.getAttractionsByParentRegion(regionId).size
        }
    }

    fun togglePhotoMarkers() {
        val intent = photoMarkerIntent.updateAndGet { current ->
            PhotoMarkerIntent(
                generation = current.generation + 1L,
                visible = !current.visible
            )
        }
        if (intent.visible) {
            syncPhotoMarkersToMap(intent)
            autoMarkFromPhotos()
        } else {
            enqueueControllerEffect(ControllerEffectScope.CONTROLLER_SCOPED) { controller, _ ->
                controller.clearImageMarkers()
            }
            _photoClusters.value = emptyList()
        }
    }

    fun showAttractionPreview(attractionId: String) {
        val fromList = _attractions.value.find { it.id == attractionId }
            ?: _selectedRegionAttractions.value.find { it.id == attractionId }
        if (fromList != null) {
            _previewAttraction.value = fromList
        } else {
            val attraction = attractionService.getAttraction(attractionId) ?: return
            val visits = getAttractionVisitsCache()
            _previewAttraction.value = AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visits[attraction.id],
                imageUrl = attraction.imageUrl
            )
        }
        navigationState.update {
            it.copy(bottomPanel = BottomPanel.AttractionPreview(attractionId))
        }
    }

    fun showRegionPanel(regionId: String) {
        navigationState.update { it.copy(bottomPanel = BottomPanel.Region(regionId)) }
        _previewAttraction.value = null
    }

    fun clearBottomPanel() {
        navigationState.update { it.copy(bottomPanel = BottomPanel.None) }
        _previewAttraction.value = null
    }

    private val lastAutoMarkedRegionIds = mutableListOf<String>()

    fun autoMarkFromGps() {
        val provider = currentLocationProvider ?: return
        val matcher = regionMatcher ?: return
        val suggestionService = footprintSuggestionService ?: return
        if (!provider.isAvailable()) return
        vmScope.launch {
            val location = provider.getCurrentLocation() ?: return@launch
            val match = matcher.match(location.first, location.second)
            val suggestion = suggestionService.offerFromLocation(match)
            if (suggestion != null) {
                showAutoMarkMessage("发现可能足迹：${suggestion.parentPath}")
            }
        }
    }

    private fun autoMarkFromPhotos() {
        showAutoMarkMessage("照片回溯将在后续版本开放")
    }

    fun dismissAutoMarkMessage() {
        _autoMarkMessage.value = null
    }

    fun undoLastAutoMark() {
        if (lastAutoMarkedRegionIds.isEmpty()) return
        vmScope.launch {
            for (regionId in lastAutoMarkedRegionIds) {
                footprintService.removeFootprint(userId, regionId)
            }
            lastAutoMarkedRegionIds.clear()
            invalidateCaches()
            refreshRegions(captureNavigationContext())
        }
    }

    fun confirmSuggestion(suggestionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintSuggestionService?.confirm(userId, suggestionId, level) ?: return@launch
            result.footprint?.let { completeFirstFootprintIfNeeded(it.regionId, level) }
            invalidateCaches()
            refreshRegions(captureNavigationContext())
            if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun dismissSuggestion(suggestionId: String) {
        footprintSuggestionService?.dismiss(suggestionId)
    }

    private var autoMarkJob: kotlinx.coroutines.Job? = null

    private fun showAutoMarkMessage(msg: String) {
        autoMarkJob?.cancel()
        _autoMarkMessage.value = msg
        autoMarkJob = vmScope.launch {
            kotlinx.coroutines.delay(3000)
            _autoMarkMessage.value = null
        }
    }

    private var cachedCityDots: List<CityDot>? = null

    fun getCityDots(): List<CityDot> {
        cachedCityDots?.let { return it }
        val cities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        val dots = cities.mapNotNull { city ->
            val center = regionRepository.getRegionCenter(city.id) ?: return@mapNotNull null
            CityDot(city.id, city.name, center.first, center.second)
        }
        cachedCityDots = dots
        return dots
    }

    fun getCurrentRegionCityDots(): List<CityDot> {
        val currentParentId = _currentPath.value.lastOrNull()?.id
        return if (currentParentId != null) {
            regionRepository.getChildRegions(currentParentId).mapNotNull { city ->
                val center = regionRepository.getRegionCenter(city.id) ?: return@mapNotNull null
                CityDot(city.id, city.name, center.first, center.second)
            }
        } else {
            getCityDots()
        }
    }

    fun getRandomCityWithAttractions(): CityDot? {
        val currentParentId = _currentPath.value.lastOrNull()?.id
        val dots = if (currentParentId != null) {
            regionRepository.getChildRegions(currentParentId).mapNotNull { city ->
                val center = regionRepository.getRegionCenter(city.id) ?: return@mapNotNull null
                CityDot(city.id, city.name, center.first, center.second)
            }
        } else {
            getCityDots()
        }
        if (dots.isEmpty()) return null
        val withAttractions = dots.filter { dot ->
            attractionService.getAttractionsByParentRegion(dot.id).isNotEmpty()
        }
        if (withAttractions.isEmpty()) return dots.random()
        return withAttractions.random()
    }

    fun syncPhotoMarkersToMap() {
        syncPhotoMarkersToMap(photoMarkerIntent.value)
    }

    private fun syncPhotoMarkersToMap(intent: PhotoMarkerIntent) {
        if (!intent.visible) return
        val effectSnapshot = captureControllerEffect() ?: return
        val override = photoLoadOverride
        val provider = devicePhotoProvider
        if (override == null && (provider == null || !provider.isAvailable())) return
        vmScope.launch {
            val overrideResult = override?.invoke()
            val permResult = overrideResult?.first ?: provider!!.checkPermission()
            val photos = when {
                overrideResult != null -> overrideResult.second
                permResult == PhotoResult.SUCCESS -> provider!!.getPhotosWithLocation()
                else -> emptyList()
            }
            afterPhotoLoad?.invoke()
            if (!isCurrentPhotoIntent(intent, effectSnapshot.controller)) {
                return@launch
            }
            if (permResult == PhotoResult.NO_PERMISSION) {
                enqueueControllerEffect(
                    ControllerEffectScope.PHOTO_INTENT_SCOPED,
                    effectSnapshot
                ) { controller, _ ->
                    controller.clearImageMarkers()
                }
                _photoClusters.value = emptyList()
                showAutoMarkMessage("请授予相册权限以读取照片")
                return@launch
            }
            if (photos.isEmpty()) {
                enqueueControllerEffect(
                    ControllerEffectScope.PHOTO_INTENT_SCOPED,
                    effectSnapshot
                ) { controller, _ ->
                    controller.clearImageMarkers()
                }
                _photoClusters.value = emptyList()
                showAutoMarkMessage("未找到带位置信息的照片")
                return@launch
            }
            val clusters = PhotoClusterer.cluster(photos)
            _photoClusters.value = clusters
            enqueueControllerEffect(
                ControllerEffectScope.PHOTO_INTENT_SCOPED,
                effectSnapshot
            ) { controller, _ ->
                controller.clearImageMarkers()
                for (cluster in clusters) {
                    controller.addImageMarker(
                        cluster.id,
                        cluster.latitude,
                        cluster.longitude,
                        cluster.coverPath,
                        cluster.count
                    )
                }
            }
        }
    }

    private fun isCurrentPhotoIntent(
        intent: PhotoMarkerIntent,
        controller: MapController
    ): Boolean {
        val current = photoMarkerIntent.value
        return current.visible &&
            current.generation == intent.generation &&
            _mapController === controller
    }

    fun markFootprint(regionId: String, level: FootprintLevel) {
        val isFirstFootprint = _firstFootprintActivation.value
        val previousNavigationState = navigationState.value
        if (isFirstFootprint) {
            firstFootprintPersistenceInFlight.value = true
            navigationState.update { state ->
                state.copy(
                    currentRegions = state.currentRegions.map { region ->
                        if (region.regionId == regionId) {
                            region.copy(footprintLevel = level)
                        } else {
                            region
                        }
                    },
                    selectedRegion = state.selectedRegion?.let { region ->
                        if (region.regionId == regionId) {
                            region.copy(footprintLevel = level)
                        } else {
                            region
                        }
                    }
                )
            }
            completeFirstFootprintIfNeeded(regionId, level)
            updateOverlayColor(regionId, level)
        }

        vmScope.launch {
            val result = runCatching {
                footprintService.markFootprint(userId, regionId, level)
            }.getOrNull()
            val persisted = footprintRepository.getFootprint(userId, regionId) != null
            val succeeded = result?.isSuccess == true || persisted

            if (succeeded) {
                if (!isFirstFootprint) completeFirstFootprintIfNeeded(regionId, level)
            } else if (isFirstFootprint) {
                _firstFootprintCelebration.value = null
                navigationState.update { state ->
                    state.copy(
                        currentRegions = previousNavigationState.currentRegions,
                        selectedRegion = previousNavigationState.selectedRegion
                    )
                }
                showAutoMarkMessage("标记失败，请重试")
            }
            if (isFirstFootprint) firstFootprintPersistenceInFlight.value = false
            refreshFirstFootprintActivation()
            invalidateCaches()
            refreshRegions(captureNavigationContext())
            if (succeeded) updateOverlayColor(regionId, level)
            if (result?.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun removeFootprint(regionId: String) {
        vmScope.launch {
            footprintService.removeFootprint(userId, regionId)
            invalidateCaches()
            refreshFirstFootprintActivation()
            refreshRegions(captureNavigationContext())
        }
    }

    fun markAttractionVisit(attractionId: String, regionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintService.markAttractionVisit(userId, attractionId, regionId, level)
            if (result.isSuccess) completeFirstFootprintIfNeeded(regionId, level)
            invalidateCaches()
            refreshAttractions()
            refreshRegions(captureNavigationContext())
            if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun removeAttractionVisit(attractionId: String) {
        vmScope.launch {
            footprintService.removeAttractionVisit(userId, attractionId)
            invalidateCaches()
            refreshFirstFootprintActivation()
            refreshAttractions()
            refreshRegions(captureNavigationContext())
        }
    }

    private fun refreshAttractions() {
        val snapshot = navigationState.value
        val currentParentId = snapshot.currentPath.lastOrNull()?.id
        if (currentParentId != null) {
            vmScope.launch {
                loadAttractionsForRegion(
                    currentParentId,
                    snapshot.navigationVersion
                )
            }
        }
        val selectedId = snapshot.selectedRegion?.regionId
        if (selectedId != null) {
            vmScope.launch {
                loadAttractionsForSelectedRegion(
                    selectedId,
                    snapshot.navigationVersion
                )
            }
        }
    }

    private suspend fun loadAttractionsForRegion(
        regionId: String,
        navigationVersion: Long
    ) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        val visits = getAttractionVisitsCache()
        afterAttractionsLoad?.invoke(regionId)
        val attractions = list.map { attraction ->
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visits[attraction.id],
                imageUrl = attraction.imageUrl
            )
        }
        val updated = navigationState.updateAndGet { state ->
            if (
                state.navigationVersion != navigationVersion ||
                state.currentPath.lastOrNull()?.id != regionId
            ) {
                state
            } else {
                state.copy(
                    currentAttractions = attractions,
                    attractionsRegionId = regionId
                )
            }
        }
        if (
            updated.navigationVersion == navigationVersion &&
            updated.attractionsRegionId == regionId
        ) {
            enqueueMarkerReconcile(
                captureControllerEffect()?.takeIf {
                    it.navigationVersion == navigationVersion
                }
            )
        }
    }

    private suspend fun loadAttractionsForSelectedRegion(
        regionId: String,
        navigationVersion: Long
    ) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        val visits = getAttractionVisitsCache()
        val attractions = list.map { attraction ->
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visits[attraction.id],
                imageUrl = attraction.imageUrl
            )
        }
        navigationState.update { state ->
            if (
                state.navigationVersion != navigationVersion ||
                state.selectedRegion?.regionId != regionId
            ) {
                state
            } else {
                state.copy(selectedRegionAttractions = attractions)
            }
        }
    }

    private fun NavigationState.toNavigationContextSnapshot(): NavigationContextSnapshot =
        NavigationContextSnapshot(
            navigationVersion = navigationVersion,
            sourcePath = currentPath.map { it.id },
            sourceLevel = currentLevel
        )

    private fun captureNavigationContext(): NavigationContextSnapshot =
        navigationState.value.toNavigationContextSnapshot()

    private fun NavigationState.matches(
        expected: NavigationContextSnapshot
    ): Boolean =
        navigationVersion == expected.navigationVersion &&
            currentPath.map { it.id } == expected.sourcePath &&
            currentLevel == expected.sourceLevel

    private fun NavigationContextSnapshot.isNationalTarget(): Boolean =
        sourcePath.isEmpty() && sourceLevel == MapZoomLevel.NATIONAL

    private fun NavigationContextSnapshot.bindsChildLayer(parentId: String): Boolean =
        sourcePath.lastOrNull() == parentId &&
            sourceLevel != MapZoomLevel.NATIONAL

    private suspend fun loadTopLevelRegions(
        expected: NavigationContextSnapshot
    ) {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        if (provinces.isEmpty()) return
        val footprints = getFootprintCache()
        val boundaries = regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)
        val centers = provinces.associate {
            it.id to (regionRepository.getRegionCenter(it.id) ?: (0.0 to 0.0))
        }
        val names = provinces.associate { it.id to it.name }

        val regions = provinces.map { region ->
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = if (childrenIndexReady) {
                    computeCoverageBatch(listOf(region.id), footprints)[region.id] ?: 0f
                } else 0f
            )
        }
        afterOrdinaryLayerPrepared?.invoke("national")
        val updated = navigationState.updateAndGet { state ->
            if (!expected.isNationalTarget() || !state.matches(expected)) state
            else state.copy(currentRegions = regions)
        }
        if (updated.currentRegions !== regions) return
        lastBoundaries = boundaries
        provinceBoundaryCache = boundaries
        provinceCenterCache = centers
        provinceNameCache = names
        syncOverlaysToMap(boundaries)
    }

    private suspend fun loadChildRegions(
        parentId: String,
        expected: NavigationContextSnapshot
    ) {
        var children = regionRepository.getChildRegions(parentId)

        if (children.isEmpty() && boundaryLoader != null) {
            val childBoundaries = boundaryLoader.loadChildRegions(parentId)
            if (childBoundaries != null) {
                val childRegions = childBoundaries.map { child ->
                    Region(child.id, child.name, RegionLevel.DISTRICT, parentId)
                }
                regionRepository.insertRegionsInTransaction(childRegions)
                val boundaryUpdates = childBoundaries.map { it.id to it.boundary }
                regionRepository.updateBoundariesInTransaction(boundaryUpdates)
                children = regionRepository.getChildRegions(parentId)
                rebuildChildrenIndex()
            }
        }

        val footprints = getFootprintCache()
        val boundaries = regionRepository.getBoundariesByParentId(parentId)

        val regions = children.map { region ->
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = if (childrenIndexReady) {
                    computeCoverageBatch(listOf(region.id), footprints)[region.id] ?: 0f
                } else 0f
            )
        }
        afterOrdinaryLayerPrepared?.invoke(parentId)
        val updated = navigationState.updateAndGet { state ->
            if (!expected.bindsChildLayer(parentId) || !state.matches(expected)) state
            else state.copy(currentRegions = regions)
        }
        if (updated.currentRegions !== regions) return
        lastBoundaries = boundaries
        syncOverlaysToMap(boundaries)
    }

    private fun refreshCoverage() {
        val footprints = getFootprintCache()
        val currentRegions = _regions.value
        if (currentRegions.isEmpty()) return

        val coverageMap = computeCoverageBatch(currentRegions.map { it.regionId }, footprints)
        navigationState.update { state ->
            state.copy(
                currentRegions = state.currentRegions.map { region ->
                    region.copy(
                        childCoverageRate = coverageMap[region.regionId] ?: 0f
                    )
                }
            )
        }
        val parentId = _currentPath.value.lastOrNull()?.id
        val boundaries = if (parentId != null) {
            regionRepository.getBoundariesByParentId(parentId)
        } else {
            regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)
        }
        syncOverlaysToMap(boundaries)
    }

    private suspend fun refreshRegions(expected: NavigationContextSnapshot) {
        val parentId = expected.sourcePath.lastOrNull()
        if (parentId != null) {
            loadChildRegions(parentId, expected)
        } else {
            loadTopLevelRegions(expected)
        }
    }

    private fun computeCoverageBatch(
        parentIds: List<String>,
        footprints: Map<String, FootprintLevel>
    ): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        val visitedSet = footprints.keys

        for (parentId in parentIds) {
            val childIds = childrenIndex[parentId]
            if (childIds.isNullOrEmpty()) {
                result[parentId] = if (visitedSet.contains(parentId)) 1f else 0f
                continue
            }
            val visitedCount = childIds.count { childId ->
                visitedSet.contains(childId) || hasDescendantVisited(childId, visitedSet, mutableSetOf())
            }
            result[parentId] = visitedCount.toFloat() / childIds.size
        }
        return result
    }

    private fun hasDescendantVisited(
        regionId: String,
        visitedSet: Set<String>,
        seen: MutableSet<String>
    ): Boolean {
        if (regionId in seen) return false
        seen.add(regionId)
        val childIds = childrenIndex[regionId] ?: return false
        return childIds.any { childId ->
            visitedSet.contains(childId) || hasDescendantVisited(childId, visitedSet, seen)
        }
    }

    private fun zoomOutToParent() {
        val pathBeforeNavigation = navigationState.value.currentPath
        if (pathBeforeNavigation.isEmpty()) return
        invalidateCameraIntent()
        val updated = navigationState.updateAndGet { state ->
            val invalidated = state.withInvalidatedNavigation()
            when {
                state.currentPath.size > 1 -> {
                    val path = state.currentPath.dropLast(1)
                    invalidated.copy(
                        currentPath = path,
                        currentLevel = levelAfterDrill(path.last()),
                        currentAttractions = emptyList(),
                        attractionsRegionId = null
                    )
                }
                state.currentPath.size == 1 -> invalidated.copy(
                    currentLevel = MapZoomLevel.NATIONAL,
                    currentPath = emptyList(),
                    currentAttractions = emptyList(),
                    attractionsRegionId = null
                )
                else -> invalidated
            }
        }
        val targetContext = updated.toNavigationContextSnapshot()
        cleanupChangedNavigationPresentation()
        val parent = updated.currentPath.lastOrNull()
        if (parent != null) {
            vmScope.launch {
                loadChildRegions(parent.id, targetContext)
                loadAttractionsForRegion(parent.id, updated.navigationVersion)
            }
        } else if (pathBeforeNavigation.isNotEmpty()) {
            enqueueControllerEffect(ControllerEffectScope.NAVIGATION_SCOPED) { controller, _ ->
                val target = controller.viewport.computeChinaFitTarget()
                savedCameraLat = target.first
                savedCameraLng = target.second
                savedCameraZoom = target.third
                setProgrammaticCamera()
                controller.fitChinaInView(true)
                controller.clearMarkers()
            }
            vmScope.launch {
                loadTopLevelRegions(targetContext)
            }
        }
    }

    private var neighborOutlinesController: MapController? = null

    private fun enqueueOverlayReconcile(
        snapshot: ControllerEffectSnapshot? = captureControllerEffect()
    ) {
        enqueueControllerEffect(
            ControllerEffectScope.NAVIGATION_SCOPED,
            snapshot
        ) { controller, state ->
            reconcileOverlaysForNavigationState(controller, state)
        }
    }

    private fun reconcileOverlaysForNavigationState(
        controller: MapController,
        state: NavigationState
    ) {
        val parentId = state.currentPath.lastOrNull()?.id
        val boundaries = if (parentId != null) {
            regionRepository.getBoundariesByParentId(parentId)
        } else {
            regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)
        }
        lastBoundaries = boundaries
        reconcileOverlaysToMap(controller, boundaries, state)
    }

    private fun syncOverlaysToMap(
        @Suppress("UNUSED_PARAMETER")
        boundaries: Map<String, String>? = null
    ) {
        enqueueOverlayReconcile()
    }

    private fun reconcileOverlaysToMap(
        controller: MapController,
        boundaries: Map<String, String>?,
        state: NavigationState
    ) {
        // Load neighbor outlines once
        if (neighborOutlinesController !== controller) {
            controller.setNeighborOutlines(com.mapchina.map.NeighborOutlines.all)
            neighborOutlinesController = controller
        }

        val regionIds = mutableSetOf<String>()
        val labels = mutableMapOf<String, LabelData>()

        fun syncOverlay(
            regionId: String,
            boundary: String,
            style: OverlayStyle,
            isVisited: Boolean,
            role: OverlayRole,
            opacityMultiplier: Float
        ) {
            if (controller.hasOverlay(regionId)) {
                controller.updateOverlayPresentation(
                    regionId = regionId,
                    style = style,
                    isVisited = isVisited,
                    role = role,
                    opacityMultiplier = opacityMultiplier
                )
            } else {
                controller.addOverlay(
                    regionId = regionId,
                    boundary = boundary,
                    style = style,
                    isVisited = isVisited,
                    role = role,
                    opacityMultiplier = opacityMultiplier
                )
            }
        }

        // Keep province overlays visible as national context when drilled down.
        if (state.currentLevel != MapZoomLevel.NATIONAL && provinceBoundaryCache.isNotEmpty()) {
            val footprints = getFootprintCache()
            val coverageMap = if (childrenIndexReady) {
                computeCoverageBatch(provinceBoundaryCache.keys.toList(), footprints)
            } else emptyMap()

            for ((id, boundary) in provinceBoundaryCache) {
                regionIds.add(id)
                val coverage = coverageMap[id] ?: 0f
                val fp = footprints[id]
                val style = footprintOverlayStyle(fp, coverage)
                syncOverlay(
                    regionId = id,
                    boundary = boundary,
                    style = style,
                    isVisited = fp != null || coverage > 0f,
                    role = OverlayRole.CONTEXT,
                    opacityMultiplier = 0.25f
                )
            }
        }

        for (region in state.currentRegions) {
            regionIds.add(region.regionId)
            val style = footprintOverlayStyle(region.footprintLevel, region.childCoverageRate)
            val boundary = boundaries?.get(region.regionId)
                ?: regionRepository.getRegionBoundary(region.regionId)
            if (boundary != null) {
                syncOverlay(
                    regionId = region.regionId,
                    boundary = boundary,
                    style = style,
                    isVisited = region.footprintLevel != null ||
                        region.childCoverageRate > 0f,
                    role = OverlayRole.ACTIVE,
                    opacityMultiplier = 1f
                )
            }
            // Add label if we have center coords
            val center = regionRepository.getRegionCenter(region.regionId)
            if (center != null) {
                val minZoom = when (state.currentLevel) {
                    MapZoomLevel.NATIONAL -> 3.5f
                    MapZoomLevel.PROVINCIAL -> 6f
                    else -> 7f
                }
                labels[region.regionId] = LabelData(
                    id = region.regionId,
                    name = mapLabelName(region.name, state.currentLevel),
                    lat = center.first,
                    lng = center.second,
                    minZoom = minZoom
                )
            }
        }
        controller.removeOverlaysExcept(regionIds)
        controller.setLabels(labels)
        lastSyncedRegionIds = regionIds
    }

    private fun mapLabelName(name: String, level: MapZoomLevel): String {
        return when (level) {
            MapZoomLevel.NATIONAL -> name.stripAdministrativeSuffix(
                "特别行政区",
                "壮族自治区",
                "回族自治区",
                "维吾尔自治区",
                "自治区",
                "省",
                "市"
            )
            MapZoomLevel.PROVINCIAL -> name.stripAdministrativeSuffix("市")
            MapZoomLevel.CITY,
            MapZoomLevel.DISTRICT -> name
        }
    }

    private fun String.stripAdministrativeSuffix(vararg suffixes: String): String {
        for (suffix in suffixes) {
            if (endsWith(suffix)) return removeSuffix(suffix)
        }
        return this
    }

    private fun enqueueMarkerReconcile(
        snapshot: ControllerEffectSnapshot? = captureControllerEffect()
    ) {
        enqueueControllerEffect(
            ControllerEffectScope.NAVIGATION_SCOPED,
            snapshot
        ) { controller, state ->
            controller.clearMarkers()
            if (
                state.currentLevel != MapZoomLevel.CITY &&
                state.currentLevel != MapZoomLevel.DISTRICT
            ) {
                return@enqueueControllerEffect
            }
            for (attraction in state.currentAttractions) {
                val fullAttraction =
                    attractionService.getAttraction(attraction.id) ?: continue
                controller.addAttractionMarker(
                    attraction.id,
                    attraction.name,
                    fullAttraction.latitude,
                    fullAttraction.longitude,
                    fullAttraction.imageUrl,
                    attraction.visitLevel != null
                )
            }
        }
    }

    private fun updateOverlayColor(regionId: String, level: FootprintLevel) {
        val region = _regions.value.find { it.regionId == regionId }
        val coverage = region?.childCoverageRate ?: 0f
        val style = footprintOverlayStyle(level, coverage)
        val boundary = regionRepository.getRegionBoundary(regionId)
        if (boundary != null) {
            enqueueControllerEffect(ControllerEffectScope.NAVIGATION_SCOPED) { controller, _ ->
                controller.removeOverlay(regionId)
                controller.addOverlay(regionId, boundary, style, true)
            }
        }
    }

    private fun onCameraZoomChanged(zoom: Float) {
        if (_programmaticCamera) return

        if (_currentPath.value.isEmpty()) return

        val currentZoomLevel = _currentLevel.value

        val targetLevel = when {
            zoom < 5f -> MapZoomLevel.NATIONAL
            zoom < 7f -> MapZoomLevel.PROVINCIAL
            zoom < 9f -> MapZoomLevel.CITY
            else -> MapZoomLevel.DISTRICT
        }

        if (targetLevel == currentZoomLevel) return

        if (targetLevel.ordinal < currentZoomLevel.ordinal) {
            zoomOutToParent()
        }
    }

    private fun moveCameraToRegion(region: Region) {
        val bounds = regionRepository.getRegionBounds(region.id)
        enqueueControllerEffect(ControllerEffectScope.NAVIGATION_SCOPED) { controller, _ ->
            if (bounds != null) {
                val target = controller.computeZoomForBounds(
                    bounds.minLng,
                    bounds.maxLng,
                    bounds.minLat,
                    bounds.maxLat
                )
                savedCameraLat = target.second
                savedCameraLng = target.first
                savedCameraZoom = target.third
                setProgrammaticCamera()
                controller.zoomToBounds(
                    bounds.minLng,
                    bounds.maxLng,
                    bounds.minLat,
                    bounds.maxLat,
                    true
                )
            } else {
                val zoom = when (region.level) {
                    RegionLevel.PROVINCE -> 7f
                    RegionLevel.CITY -> 9f
                    RegionLevel.DISTRICT -> 11f
                }
                val center = regionRepository.getRegionCenter(region.id)
                if (center != null) {
                    savedCameraLat = center.first
                    savedCameraLng = center.second
                    savedCameraZoom = zoom
                    setProgrammaticCamera()
                    controller.setCamera(center.first, center.second, zoom, true)
                }
            }
        }
    }

    private fun footprintOverlayStyle(level: FootprintLevel?, childCoverageRate: Float = 0f): OverlayStyle {
        if (level == null) {
            val rate = childCoverageRate.coerceIn(0f, 1f)
            if (rate == 0f) {
                return OverlayStyle(
                    fillColor = 0xFFF7FCFAL,
                    strokeColor = 0xFF8ACCC6L,
                    strokeWidth = 0.5f,
                    alpha = 0.88f
                )
            }
            val alpha = 0.42f + rate * 0.18f
            return OverlayStyle(
                fillColor = 0xFFAEDCD7L,
                strokeColor = 0xFF69BDB5L,
                strokeWidth = 0.6f + rate * 0.5f,
                alpha = alpha
            )
        }
        return when (level) {
            FootprintLevel.DEEP -> OverlayStyle(
                fillColor = 0xFF1A7A70L,
                strokeColor = 0xFF0D5E5AL,
                strokeWidth = 1.2f,
                alpha = 0.62f
            )
            FootprintLevel.SHORT_VISIT -> OverlayStyle(
                fillColor = 0xFF3EA396L,
                strokeColor = 0xFF1A7A70L,
                strokeWidth = 1.0f,
                alpha = 0.48f
            )
            FootprintLevel.PASS_BY -> OverlayStyle(
                fillColor = 0xFF80C4BAL,
                strokeColor = 0xFF4A9E94L,
                strokeWidth = 0.8f,
                alpha = 0.35f
            )
        }
    }

    private fun buildPathTo(regionId: String): List<Region> {
        val path = mutableListOf<Region>()
        var current = regionRepository.getRegion(regionId)
        while (current != null) {
            path.add(0, current)
            current = current.parentId?.let { regionRepository.getRegion(it) }
        }
        return path
    }
}
