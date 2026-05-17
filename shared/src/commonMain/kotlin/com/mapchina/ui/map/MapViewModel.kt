package com.mapchina.ui.map

import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintService
import com.mapchina.map.MapZoomLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ViewMode { MAP, BLOCK }

class MapViewModel(
    private val footprintService: FootprintService,
    private val regionRepository: RegionRepository,
    private val footprintRepository: FootprintRepository,
    private val userId: String = ""
) {
    private val _currentLevel = MutableStateFlow(MapZoomLevel.NATIONAL)
    val currentLevel: StateFlow<MapZoomLevel> = _currentLevel.asStateFlow()

    private val _currentPath = MutableStateFlow<List<Region>>(emptyList())
    val currentPath: StateFlow<List<Region>> = _currentPath.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.MAP)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _regions = MutableStateFlow<List<RegionFootprintUi>>(emptyList())
    val regions: StateFlow<List<RegionFootprintUi>> = _regions.asStateFlow()

    private val _selectedRegion = MutableStateFlow<RegionFootprintUi?>(null)
    val selectedRegion: StateFlow<RegionFootprintUi?> = _selectedRegion.asStateFlow()

    init {
        loadTopLevelRegions()
    }

    fun drillIntoRegion(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        _currentPath.value = _currentPath.value + region
        _currentLevel.value = mapRegionLevelToZoom(region.level)
        loadChildRegions(regionId)
    }

    fun navigateUp() {
        if (_currentPath.value.size > 1) {
            _currentPath.value = _currentPath.value.dropLast(1)
            val parent = _currentPath.value.last()
            _currentLevel.value = mapRegionLevelToZoom(parent.level)
            loadChildRegions(parent.id)
        } else {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            loadTopLevelRegions()
        }
    }

    fun navigateTo(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        val path = buildPathTo(regionId)
        _currentPath.value = path
        _currentLevel.value = mapRegionLevelToZoom(region.level)
        if (region.level == RegionLevel.PROVINCE) {
            loadChildRegions(regionId)
        } else {
            loadChildRegions(region.parentId ?: regionId)
        }
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.MAP) ViewMode.BLOCK else ViewMode.MAP
    }

    fun selectRegion(regionId: String) {
        val fromList = _regions.value.find { it.regionId == regionId }
        if (fromList != null) {
            _selectedRegion.value = fromList
        } else {
            val region = regionRepository.getRegion(regionId) ?: return
            val footprint = footprintRepository.getFootprint(userId, regionId)
            _selectedRegion.value = RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprint?.level,
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f)
            )
        }
    }

    fun clearSelection() {
        _selectedRegion.value = null
    }

    fun markFootprint(regionId: String, level: FootprintLevel) {
        footprintService.markFootprint(userId, regionId, level)
        refreshRegions()
    }

    private fun loadTopLevelRegions() {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        _regions.value = provinces.map { region ->
            val footprint = footprintRepository.getFootprint(userId, region.id)
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprint?.level,
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f)
            )
        }
    }

    private fun loadChildRegions(parentId: String) {
        val children = regionRepository.getChildRegions(parentId)
        _regions.value = children.map { region ->
            val footprint = footprintRepository.getFootprint(userId, region.id)
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprint?.level,
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f)
            )
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

    private fun buildPathTo(regionId: String): List<Region> {
        val path = mutableListOf<Region>()
        var current = regionRepository.getRegion(regionId)
        while (current != null) {
            path.add(0, current)
            current = current.parentId?.let { regionRepository.getRegion(it) }
        }
        return path
    }

    private fun mapRegionLevelToZoom(level: RegionLevel): MapZoomLevel = when (level) {
        RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
        RegionLevel.CITY -> MapZoomLevel.CITY
        RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
    }
}
