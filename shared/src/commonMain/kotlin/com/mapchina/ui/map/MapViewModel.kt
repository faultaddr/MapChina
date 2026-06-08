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
import com.mapchina.map.MapController
import com.mapchina.platform.PhotoResult
import com.mapchina.platform.DevicePhotoProvider
import com.mapchina.platform.LocationProvider
import com.mapchina.domain.service.RegionMatcher
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.OverlayStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class AttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val description: String?,
    val visitLevel: FootprintLevel?
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
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onCleared() {
        vmScope.cancel()
    }

    private val _currentLevel = MutableStateFlow(MapZoomLevel.NATIONAL)
    val currentLevel: StateFlow<MapZoomLevel> = _currentLevel.asStateFlow()

    private val _currentPath = MutableStateFlow<List<Region>>(emptyList())
    val currentPath: StateFlow<List<Region>> = _currentPath.asStateFlow()

    private val _achievementUnlock = MutableStateFlow<AchievementUnlockResult?>(null)
    val achievementUnlock: StateFlow<AchievementUnlockResult?> = _achievementUnlock.asStateFlow()

    private val _drillDownHint = MutableStateFlow<String?>(null)
    val drillDownHint: StateFlow<String?> = _drillDownHint.asStateFlow()

    private val _regions = MutableStateFlow<List<RegionFootprintUi>>(emptyList())
    val regions: StateFlow<List<RegionFootprintUi>> = _regions.asStateFlow()

    private val _selectedRegion = MutableStateFlow<RegionFootprintUi?>(null)
    val selectedRegion: StateFlow<RegionFootprintUi?> = _selectedRegion.asStateFlow()

    private val _attractions = MutableStateFlow<List<AttractionUi>>(emptyList())
    val attractions: StateFlow<List<AttractionUi>> = _attractions.asStateFlow()

    private val _selectedRegionAttractions = MutableStateFlow<List<AttractionUi>>(emptyList())
    val selectedRegionAttractions: StateFlow<List<AttractionUi>> = _selectedRegionAttractions.asStateFlow()

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    private val _photoClusters = MutableStateFlow<List<PhotoCluster>>(emptyList())
    val photoClusters: StateFlow<List<PhotoCluster>> = _photoClusters.asStateFlow()

    private val _photoMarkersVisible = MutableStateFlow(false)
    val photoMarkersVisible: StateFlow<Boolean> = _photoMarkersVisible.asStateFlow()

    private val _autoMarkMessage = MutableStateFlow<String?>(null)
    val autoMarkMessage: StateFlow<String?> = _autoMarkMessage.asStateFlow()

    private var footprintCache: Map<String, FootprintLevel>? = null

    private var childrenIndex: Map<String, List<String>> = emptyMap()
    private var childrenIndexReady = false

    private fun invalidateCaches() {
        footprintCache = null
        attractionVisitsCache = null
        attractionCountCache.clear()
    }

    private var _mapController: MapController? = null
    private var _programmaticCamera = false
    private var lastBoundaries: Map<String, String>? = null
    private var lastSyncedRegionIds: Set<String> = emptySet()

    // Persisted camera state (survives MapController recreation)
    private var savedCameraLat: Double = 34.5
    private var savedCameraLng: Double = 106.0
    private var savedCameraZoom: Float = 3.8f
    var mapController: MapController?
        get() = _mapController
        set(value) {
            if (_mapController === value) return
            _mapController = value
            if (value != null) {
                lastSyncedRegionIds = emptySet()
                if (_regions.value.isNotEmpty()) {
                    syncOverlaysToMap(lastBoundaries)
                }
                value.setOnCameraZoomChangeListener { zoom ->
                    savedCameraZoom = zoom
                    onCameraZoomChanged(zoom)
                }
                value.setOnCameraPositionListener { lat, lng, zoom ->
                    savedCameraLat = lat
                    savedCameraLng = lng
                    savedCameraZoom = zoom
                }
            }
        }

    private val attractionCountCache = mutableMapOf<String, Int>()
    private var attractionVisitsCache: Map<String, FootprintLevel>? = null

    companion object {
        private const val KEY_ONBOARDING_COUNT = "onboarding_shown_count"
        private const val MAX_ONBOARDING_SHOWS = 2
    }

    init {
        val shownCount = settingsRepository?.getInt(KEY_ONBOARDING_COUNT) ?: 0
        _showOnboarding.value = shownCount < MAX_ONBOARDING_SHOWS
        if (_showOnboarding.value) {
            settingsRepository?.setInt(KEY_ONBOARDING_COUNT, shownCount + 1)
        }

        vmScope.launch {
            loadTopLevelRegions()
            rebuildChildrenIndex()
            childrenIndexReady = true
            refreshCoverage()
        }
    }

    fun reloadData() {
        invalidateCaches()
        _programmaticCamera = true
        vmScope.launch {
            if (_currentPath.value.isEmpty()) {
                loadTopLevelRegions()
            } else {
                refreshRegions()
            }
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
        _currentPath.value = _currentPath.value + region
        _currentLevel.value = when (region.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }
        _selectedRegion.value = null
        moveCameraToRegion(region)

        vmScope.launch {
            loadChildRegions(regionId)
            loadAttractionsForRegion(regionId)
        }
    }

    fun navigateToNational() {
        _currentLevel.value = MapZoomLevel.NATIONAL
        _currentPath.value = emptyList()
        savedCameraLat = 34.5
        savedCameraLng = 106.0
        savedCameraZoom = 3.8f
        _programmaticCamera = true
        mapController?.setCamera(34.5, 106.0, 3.8f, true)
        vmScope.launch {
            loadTopLevelRegions()
            _attractions.value = emptyList()
            mapController?.clearMarkers()
        }
    }

    fun moveToCurrentLocation() {
        val provider = locationProvider ?: return
        vmScope.launch {
            val location = provider.getCurrentLocation() ?: return@launch
            savedCameraLat = location.first
            savedCameraLng = location.second
            savedCameraZoom = 10f
            _programmaticCamera = true
            mapController?.setCamera(location.first, location.second, 10f, true)
        }
    }

    fun navigateUp() {
        val path = _currentPath.value
        if (path.size > 1) {
            _currentPath.value = path.dropLast(1)
            val parent = _currentPath.value.last()
            _currentLevel.value = when (parent.level) {
                RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
                RegionLevel.CITY -> MapZoomLevel.CITY
                RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
            }
            moveCameraToRegion(parent)

            vmScope.launch {
                loadChildRegions(parent.id)
                loadAttractionsForRegion(parent.id)
            }
        } else if (path.size == 1) {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            savedCameraLat = 34.5
            savedCameraLng = 106.0
            savedCameraZoom = 3.8f
            _programmaticCamera = true
            mapController?.setCamera(34.5, 106.0, 3.8f, true)

            vmScope.launch {
                loadTopLevelRegions()
                _attractions.value = emptyList()
                mapController?.clearMarkers()
            }
        }
    }

    fun navigateTo(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        val path = buildPathTo(regionId)
        _currentPath.value = path
        _currentLevel.value = when (region.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }
        moveCameraToRegion(region)

        vmScope.launch {
            loadChildRegions(regionId)
            loadAttractionsForRegion(regionId)
        }
    }

    fun selectRegion(regionId: String) {
        val fromList = _regions.value.find { it.regionId == regionId }
        if (fromList != null) {
            _selectedRegion.value = fromList
        } else {
            val region = regionRepository.getRegion(regionId) ?: return
            val footprints = getFootprintCache()
            _selectedRegion.value = RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f)
            )
        }
        vmScope.launch { loadAttractionsForSelectedRegion(regionId) }
    }

    fun clearSelection() {
        _selectedRegion.value = null
        _selectedRegionAttractions.value = emptyList()
    }

    fun dismissAchievementUnlock() {
        _achievementUnlock.value = null
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

    fun dismissOnboarding() {
        _showOnboarding.value = false
    }

    fun canDrillIntoRegion(regionId: String): Boolean {
        val hasChildren = !childrenIndex[regionId].isNullOrEmpty()
        if (hasChildren) return true
        val region = regionRepository.getRegion(regionId) ?: return false
        return region.level != RegionLevel.DISTRICT
    }

    fun getAttractionCountForRegion(regionId: String): Int {
        return attractionCountCache.getOrPut(regionId) {
            attractionService.getAttractionsByParentRegion(regionId).size
        }
    }

    fun togglePhotoMarkers() {
        val newValue = !_photoMarkersVisible.value
        _photoMarkersVisible.value = newValue
        if (newValue) {
            syncPhotoMarkersToMap()
            autoMarkFromPhotos()
        } else {
            _mapController?.clearImageMarkers()
            _photoClusters.value = emptyList()
        }
    }

    private val lastAutoMarkedRegionIds = mutableListOf<String>()

    fun autoMarkFromGps() {
        val provider = locationProvider ?: return
        val matcher = regionMatcher ?: return
        if (!provider.isAvailable()) return
        vmScope.launch {
            val location = provider.getCurrentLocation() ?: return@launch
            val match = matcher.match(location.first, location.second)
            val newRegions = mutableListOf<String>()
            val footprints = getFootprintCache()

            lastAutoMarkedRegionIds.clear()
            for (region in listOfNotNull(match.province, match.city, match.district)) {
                if (footprints[region.id] == null) {
                    footprintService.markFootprint(userId, region.id, FootprintLevel.PASS_BY)
                    newRegions.add(region.name)
                    lastAutoMarkedRegionIds.add(region.id)
                }
            }

            if (newRegions.isNotEmpty()) {
                invalidateCaches()
                refreshRegions()
                showAutoMarkMessage("从你的位置发现了 ${newRegions.size} 个新足迹")
            }
        }
    }

    private fun autoMarkFromPhotos() {
        val provider = devicePhotoProvider ?: return
        val matcher = regionMatcher ?: return
        if (!provider.isAvailable()) return
        vmScope.launch {
            if (provider.checkPermission() != PhotoResult.SUCCESS) return@launch
            val photos = provider.getPhotosWithLocation()
            if (photos.isEmpty()) return@launch

            val footprints = getFootprintCache()
            val newRegionIds = mutableSetOf<String>()

            lastAutoMarkedRegionIds.clear()
            for (photo in photos) {
                val match = matcher.match(photo.latitude, photo.longitude)
                for (region in listOfNotNull(match.province, match.city, match.district)) {
                    if (footprints[region.id] == null && region.id !in newRegionIds) {
                        footprintService.markFootprint(userId, region.id, FootprintLevel.PASS_BY)
                        newRegionIds.add(region.id)
                        lastAutoMarkedRegionIds.add(region.id)
                    }
                }
            }

            if (newRegionIds.isNotEmpty()) {
                invalidateCaches()
                refreshRegions()
                showAutoMarkMessage("从相册发现了 ${newRegionIds.size} 个新足迹")
            }
        }
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
            refreshRegions()
        }
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

    fun getRandomCityWithAttractions(): CityDot? {
        val dots = getCityDots()
        if (dots.isEmpty()) return null
        val withAttractions = dots.filter { dot ->
            attractionService.getAttractionsByParentRegion(dot.id).isNotEmpty()
        }
        if (withAttractions.isEmpty()) return dots.random()
        return withAttractions.random()
    }

    fun syncPhotoMarkersToMap() {
        val controller = _mapController ?: return
        val provider = devicePhotoProvider ?: return
        if (!provider.isAvailable()) return
        vmScope.launch {
            val permResult = provider.checkPermission()
            if (permResult == PhotoResult.NO_PERMISSION) {
                controller.clearImageMarkers()
                _photoClusters.value = emptyList()
                showAutoMarkMessage("请授予相册权限以读取照片")
                return@launch
            }
            val photos = provider.getPhotosWithLocation()
            if (photos.isEmpty()) {
                controller.clearImageMarkers()
                _photoClusters.value = emptyList()
                showAutoMarkMessage("未找到带位置信息的照片")
                return@launch
            }
            val clusters = PhotoClusterer.cluster(photos)
            _photoClusters.value = clusters
            controller.clearImageMarkers()
            for (cluster in clusters) {
                controller.addImageMarker(cluster.id, cluster.latitude, cluster.longitude, cluster.coverPath, cluster.count)
            }
        }
    }

    fun markFootprint(regionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintService.markFootprint(userId, regionId, level)
            invalidateCaches()
            refreshRegions()
            updateOverlayColor(regionId, level)
            if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun removeFootprint(regionId: String) {
        vmScope.launch {
            footprintService.removeFootprint(userId, regionId)
            invalidateCaches()
            refreshRegions()
        }
    }

    fun markAttractionVisit(attractionId: String, regionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintService.markAttractionVisit(userId, attractionId, regionId, level)
            invalidateCaches()
            refreshAttractions()
            refreshRegions()
            if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun removeAttractionVisit(attractionId: String) {
        vmScope.launch {
            footprintService.removeAttractionVisit(userId, attractionId)
            invalidateCaches()
            refreshAttractions()
            refreshRegions()
        }
    }

    private fun refreshAttractions() {
        val currentParentId = _currentPath.value.lastOrNull()?.id
        if (currentParentId != null) {
            vmScope.launch {
                loadAttractionsForRegion(currentParentId)
                loadAttractionsForSelectedRegion(currentParentId)
            }
        }
        val selectedId = _selectedRegion.value?.regionId
        if (selectedId != null) {
            vmScope.launch { loadAttractionsForSelectedRegion(selectedId) }
        }
    }

    private suspend fun loadAttractionsForRegion(regionId: String) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        val visits = getAttractionVisitsCache()
        _attractions.value = list.map { attraction ->
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visits[attraction.id]
            )
        }
        syncMarkersToMap()
    }

    private suspend fun loadAttractionsForSelectedRegion(regionId: String) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        val visits = getAttractionVisitsCache()
        _selectedRegionAttractions.value = list.map { attraction ->
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visits[attraction.id]
            )
        }
    }

    private fun loadTopLevelRegions() {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        if (provinces.isEmpty()) return
        val footprints = getFootprintCache()
        val boundaries = regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)
        lastBoundaries = boundaries

        _regions.value = provinces.map { region ->
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
        syncOverlaysToMap(boundaries)
    }

    private fun loadChildRegions(parentId: String) {
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
        lastBoundaries = boundaries

        _regions.value = children.map { region ->
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
        syncOverlaysToMap(boundaries)
    }

    private fun refreshCoverage() {
        val footprints = getFootprintCache()
        val currentRegions = _regions.value
        if (currentRegions.isEmpty()) return

        val coverageMap = computeCoverageBatch(currentRegions.map { it.regionId }, footprints)
        _regions.value = currentRegions.map { region ->
            region.copy(childCoverageRate = coverageMap[region.regionId] ?: 0f)
        }
        val parentId = _currentPath.value.lastOrNull()?.id
        val boundaries = if (parentId != null) {
            regionRepository.getBoundariesByParentId(parentId)
        } else {
            regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)
        }
        syncOverlaysToMap(boundaries)
    }

    private fun refreshRegions() {
        val parentId = _currentPath.value.lastOrNull()?.id
        vmScope.launch {
            if (parentId != null) {
                loadChildRegions(parentId)
            } else {
                loadTopLevelRegions()
            }
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
        val path = _currentPath.value
        if (path.size > 1) {
            _currentPath.value = path.dropLast(1)
            val parent = _currentPath.value.last()
            _currentLevel.value = when (parent.level) {
                RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
                RegionLevel.CITY -> MapZoomLevel.CITY
                RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
            }
            vmScope.launch {
                loadChildRegions(parent.id)
                loadAttractionsForRegion(parent.id)
            }
        } else if (path.size == 1) {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            vmScope.launch {
                loadTopLevelRegions()
                _attractions.value = emptyList()
                mapController?.clearMarkers()
            }
        }
    }

    private fun syncOverlaysToMap(boundaries: Map<String, String>? = null) {
        val controller = _mapController ?: return
        val regionIds = mutableSetOf<String>()
        for (region in _regions.value) {
            regionIds.add(region.regionId)
            val style = footprintOverlayStyle(region.footprintLevel, region.childCoverageRate)
            val boundary = boundaries?.get(region.regionId)
                ?: regionRepository.getRegionBoundary(region.regionId)
            if (boundary != null) {
                controller.addOverlay(region.regionId, boundary, style)
            }
        }
        controller.removeOverlaysExcept(regionIds)
        lastSyncedRegionIds = regionIds
    }

    private fun syncMarkersToMap() {
        val controller = _mapController ?: return
        controller.clearMarkers()
        val level = _currentLevel.value
        if (level != MapZoomLevel.CITY && level != MapZoomLevel.DISTRICT) return
        for (attraction in _attractions.value) {
            val fullAttraction = attractionService.getAttraction(attraction.id) ?: continue
            controller.addAttractionMarker(
                attraction.id, attraction.name,
                fullAttraction.latitude, fullAttraction.longitude,
                fullAttraction.imageUrl,
                attraction.visitLevel != null
            )
        }
    }

    private fun updateOverlayColor(regionId: String, level: FootprintLevel) {
        val controller = _mapController ?: return
        val region = _regions.value.find { it.regionId == regionId }
        val coverage = region?.childCoverageRate ?: 0f
        val style = footprintOverlayStyle(level, coverage)
        val boundary = regionRepository.getRegionBoundary(regionId)
        if (boundary != null) {
            controller.removeOverlay(regionId)
            controller.addOverlay(regionId, boundary, style)
        }
    }

    private fun onCameraZoomChanged(zoom: Float) {
        if (_programmaticCamera) {
            _programmaticCamera = false
            return
        }

        if (_currentPath.value.isEmpty()) return

        val currentZoomLevel = _currentLevel.value

        val targetLevel = when {
            zoom < 5f -> MapZoomLevel.NATIONAL
            zoom < 7.5f -> MapZoomLevel.PROVINCIAL
            zoom < 10f -> MapZoomLevel.CITY
            else -> MapZoomLevel.DISTRICT
        }

        if (targetLevel == currentZoomLevel) return

        if (targetLevel.ordinal < currentZoomLevel.ordinal) {
            zoomOutToParent()
        }
    }

    private fun moveCameraToRegion(region: Region) {
        val controller = _mapController ?: return
        val zoom = when (region.level) {
            RegionLevel.PROVINCE -> 7f
            RegionLevel.CITY -> 8f
            RegionLevel.DISTRICT -> 9.5f
        }
        val center = regionRepository.getRegionCenter(region.id)
        if (center != null) {
            savedCameraLat = center.first
            savedCameraLng = center.second
            savedCameraZoom = zoom
            _programmaticCamera = true
            controller.setCamera(center.first, center.second, zoom, true)
        }
    }

    private fun footprintOverlayStyle(level: FootprintLevel?, childCoverageRate: Float = 0f): OverlayStyle {
        if (level == null) {
            val rate = childCoverageRate.coerceIn(0f, 1f)
            if (rate == 0f) {
                return OverlayStyle(
                    fillColor = 0xFF0D7377L,
                    strokeColor = 0xFF6BAAAEL,
                    strokeWidth = 0.5f,
                    alpha = 0.07f
                )
            }
            val alpha = 0.08f + rate * 0.22f
            return OverlayStyle(
                fillColor = 0xFF14A3A8L,
                strokeColor = 0xFF0D7377L,
                strokeWidth = 0.8f + rate * 0.7f,
                alpha = alpha
            )
        }
        return when (level) {
            FootprintLevel.DEEP -> OverlayStyle(
                fillColor = 0xFFC84530L,
                strokeColor = 0xFF5A1C10L,
                strokeWidth = 2.0f,
                alpha = 0.55f
            )
            FootprintLevel.SHORT_VISIT -> OverlayStyle(
                fillColor = 0xFFD48840L,
                strokeColor = 0xFF6B4020L,
                strokeWidth = 1.8f,
                alpha = 0.45f
            )
            FootprintLevel.PASS_BY -> OverlayStyle(
                fillColor = 0xFFC8A040L,
                strokeColor = 0xFF5A4A20L,
                strokeWidth = 1.5f,
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
