package com.mapchina.ui.map

import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.OverlayStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class MapViewModel(
    private val footprintService: FootprintService,
    private val regionRepository: RegionRepository,
    private val footprintRepository: FootprintRepository,
    private val attractionService: AttractionService,
    private val boundaryLoader: BoundaryLoader? = null,
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private var footprintCache: Map<String, FootprintLevel>? = null

    private var _mapController: MapController? = null
    private var _programmaticCamera = false
    var mapController: MapController?
        get() = _mapController
        set(value) {
            _mapController = value
            if (value != null) {
                if (_regions.value.isNotEmpty()) {
                    syncOverlaysToMap()
                }
                value.setOnCameraZoomChangeListener { zoom ->
                    onCameraZoomChanged(zoom)
                }
            }
        }

    // Cache attraction counts per region to avoid repeated queries
    private val attractionCountCache = mutableMapOf<String, Int>()

    init {
        loadTopLevelRegions()
    }

    fun reloadData() {
        footprintCache = null
        attractionCountCache.clear()
        if (_currentPath.value.isEmpty()) {
            loadTopLevelRegions()
        } else {
            refreshRegions()
        }
    }

    private fun getFootprintCache(): Map<String, FootprintLevel> {
        if (footprintCache == null) {
            footprintCache = footprintRepository.getFootprintsByUser(userId)
                .associate { it.regionId to it.level }
        }
        return footprintCache!!
    }

    fun drillIntoRegion(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        _currentPath.value = _currentPath.value + region
        _currentLevel.value = when (region.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }
        loadChildRegions(regionId)
        moveCameraToRegion(region)
        loadAttractionsForRegion(regionId)
    }

    fun navigateUp() {
        if (_currentPath.value.size > 1) {
            _currentPath.value = _currentPath.value.dropLast(1)
            val parent = _currentPath.value.last()
            _currentLevel.value = when (parent.level) {
                RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
                RegionLevel.CITY -> MapZoomLevel.CITY
                RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
            }
            loadChildRegions(parent.id)
            moveCameraToRegion(parent)
            loadAttractionsForRegion(parent.id)
        } else {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            loadTopLevelRegions()
            _attractions.value = emptyList()
            mapController?.clearMarkers()
            _programmaticCamera = true
            mapController?.setCamera(34.5, 106.0, 3.8f, true)
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
        loadChildRegions(regionId)
        moveCameraToRegion(region)
        loadAttractionsForRegion(regionId)
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
        loadAttractionsForSelectedRegion(regionId)
    }

    fun clearSelection() {
        _selectedRegion.value = null
        _selectedRegionAttractions.value = emptyList()
    }

    fun dismissAchievementUnlock() {
        _achievementUnlock.value = null
    }

    fun dismissDrillDownHint() {
        _drillDownHint.value = null
    }

    fun getAttractionCountForRegion(regionId: String): Int {
        return attractionCountCache.getOrPut(regionId) {
            attractionService.getAttractionsByParentRegion(regionId).size
        }
    }

    fun markFootprint(regionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintService.markFootprint(userId, regionId, level)
            footprintCache = null
            refreshRegions()
            updateOverlayColor(regionId, level)
            if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
                _achievementUnlock.value = result.achievementResult
            }
        }
    }

    fun markAttractionVisit(attractionId: String, regionId: String, level: FootprintLevel) {
        vmScope.launch {
            val result = footprintService.markAttractionVisit(userId, attractionId, regionId, level)
            footprintCache = null
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
            footprintCache = null
            refreshAttractions()
            refreshRegions()
        }
    }

    private fun refreshAttractions() {
        val currentParentId = _currentPath.value.lastOrNull()?.id
        if (currentParentId != null) {
            loadAttractionsForRegion(currentParentId)
        }
    }

    private fun loadAttractionsForRegion(regionId: String) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        _attractions.value = list.map { attraction ->
            val visit = footprintRepository.getAttractionVisit(userId, attraction.id)
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visit?.level
            )
        }
        syncMarkersToMap()
    }

    private fun loadAttractionsForSelectedRegion(regionId: String) {
        val list = attractionService.getAttractionsByParentRegion(regionId)
        _selectedRegionAttractions.value = list.map { attraction ->
            val visit = footprintRepository.getAttractionVisit(userId, attraction.id)
            AttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                description = attraction.description,
                visitLevel = visit?.level
            )
        }
    }

    private fun loadTopLevelRegions() {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        if (provinces.isEmpty()) return
        val footprints = getFootprintCache()
        val boundaries = regionRepository.getBoundariesByLevel(RegionLevel.PROVINCE)

        // Fast path: render with coverage = 0 first
        _regions.value = provinces.map { region ->
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = 0f
            )
        }
        syncOverlaysToMap(boundaries)

        // Async: compute coverage and update overlays
        vmScope.launch {
            val updated = provinces.map { region ->
                val coverage = computeChildCoverageBatch(region.id, footprints)
                RegionFootprintUi(
                    regionId = region.id,
                    name = region.name,
                    footprintLevel = footprints[region.id],
                    normalizedPath = emptyList(),
                    bounds = RegionBounds(0f, 0f, 0f, 0f),
                    childCoverageRate = coverage
                )
            }
            _regions.value = updated
            syncOverlaysToMap(boundaries)
        }
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
            }
        }

        val footprints = getFootprintCache()
        val boundaries = regionRepository.getBoundariesByParentId(parentId)

        // Fast path
        _regions.value = children.map { region ->
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprints[region.id],
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = 0f
            )
        }
        syncOverlaysToMap(boundaries)

        // Async: compute coverage
        vmScope.launch {
            val updated = children.map { region ->
                val coverage = computeChildCoverageBatch(region.id, footprints)
                RegionFootprintUi(
                    regionId = region.id,
                    name = region.name,
                    footprintLevel = footprints[region.id],
                    normalizedPath = emptyList(),
                    bounds = RegionBounds(0f, 0f, 0f, 0f),
                    childCoverageRate = coverage
                )
            }
            _regions.value = updated
            syncOverlaysToMap(boundaries)
        }
    }

    private fun refreshRegions() {
        val parentId = _currentPath.value.lastOrNull()?.id
        if (parentId != null) {
            loadChildRegions(parentId)
        } else {
            loadTopLevelRegions()
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
            loadChildRegions(parent.id)
            loadAttractionsForRegion(parent.id)
        } else if (path.size == 1) {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            loadTopLevelRegions()
            _attractions.value = emptyList()
            mapController?.clearMarkers()
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
    }

    private fun syncMarkersToMap() {
        val controller = _mapController ?: return
        controller.clearMarkers()
        for (attraction in _attractions.value) {
            val fullAttraction = attractionService.getAttraction(attraction.id) ?: continue
            controller.addMarker(
                attraction.id, attraction.name,
                fullAttraction.latitude, fullAttraction.longitude,
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

        val currentZoomLevel = _currentLevel.value

        val targetLevel = when {
            zoom < 5f -> MapZoomLevel.NATIONAL
            zoom < 7.5f -> MapZoomLevel.PROVINCIAL
            zoom < 10f -> MapZoomLevel.CITY
            else -> MapZoomLevel.DISTRICT
        }

        if (targetLevel == currentZoomLevel) return

        when {
            targetLevel.ordinal > currentZoomLevel.ordinal -> {
                val regionName = _currentPath.value.lastOrNull()?.name ?: "当前区域"
                _drillDownHint.value = regionName
            }
            targetLevel.ordinal < currentZoomLevel.ordinal -> {
                zoomOutToParent()
            }
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
            _programmaticCamera = true
            controller.setCamera(center.first, center.second, zoom, true)
        }
    }

    private fun footprintOverlayStyle(level: FootprintLevel?, childCoverageRate: Float = 0f): OverlayStyle {
        if (level == null) {
            val rate = childCoverageRate.coerceIn(0f, 1f)
            val alpha = 0.12f + rate * 0.48f
            return OverlayStyle(
                fillColor = 0xFF2EC4B6L,
                strokeColor = 0xFF1A8A7E,
                strokeWidth = 2f,
                alpha = alpha
            )
        }
        val baseColor = when (level) {
            FootprintLevel.DEEP -> 0xFFE76F51L
            FootprintLevel.SHORT_VISIT -> 0xFFF4A261L
            FootprintLevel.PASS_BY -> 0xFFE9C46AL
        }
        return OverlayStyle(
            fillColor = baseColor,
            strokeColor = 0xFF264653L,
            strokeWidth = 2f,
            alpha = 0.6f
        )
    }

    private fun computeChildCoverageBatch(regionId: String, footprints: Map<String, FootprintLevel>): Float {
        val children = regionRepository.getChildRegions(regionId)
        if (children.isEmpty()) {
            return if (footprints.containsKey(regionId)) 1f else 0f
        }
        val visited = children.count { child ->
            footprints.containsKey(child.id) || hasDescendantInCache(child.id, footprints)
        }
        return visited.toFloat() / children.size
    }

    private fun hasDescendantInCache(regionId: String, footprints: Map<String, FootprintLevel>): Boolean {
        val children = regionRepository.getChildRegions(regionId)
        if (children.isEmpty()) return false
        return children.any { child ->
            footprints.containsKey(child.id) || hasDescendantInCache(child.id, footprints)
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
