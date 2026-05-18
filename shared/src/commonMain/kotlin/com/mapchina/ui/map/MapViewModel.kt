package com.mapchina.ui.map

import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.remote.BoundaryLoader
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.map.OverlayStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val description: String?,
    val visitLevel: FootprintLevel?
)

enum class ViewMode { MAP, BLOCK }

class MapViewModel(
    private val footprintService: FootprintService,
    private val regionRepository: RegionRepository,
    private val footprintRepository: FootprintRepository,
    private val attractionService: AttractionService,
    private val boundaryLoader: BoundaryLoader? = null,
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

    private val _attractions = MutableStateFlow<List<AttractionUi>>(emptyList())
    val attractions: StateFlow<List<AttractionUi>> = _attractions.asStateFlow()

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

    init {
        loadTopLevelRegions()
    }

    fun drillIntoRegion(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        _currentPath.value = _currentPath.value + region
        // 下钻后显示的是子区域，level 对应子区域级别
        _currentLevel.value = when (region.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }
        loadChildRegions(regionId)
        moveCameraToRegion(region)

        // 加载该区域下的景点
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
            mapController?.setCamera(35.86, 104.19, 4f, true)
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
        updateOverlayColor(regionId, level)
    }

    fun markAttractionVisit(attractionId: String, regionId: String, level: FootprintLevel) {
        footprintService.markAttractionVisit(userId, attractionId, regionId, level)
        refreshAttractions()
        refreshRegions()
    }

    fun removeAttractionVisit(attractionId: String) {
        footprintService.removeAttractionVisit(userId, attractionId)
        refreshAttractions()
        refreshRegions()
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

    private fun loadTopLevelRegions() {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        _regions.value = provinces.map { region ->
            val footprint = footprintRepository.getFootprint(userId, region.id)
            val coverage = computeChildCoverage(region.id)
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprint?.level,
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = coverage
            )
        }
        syncOverlaysToMap()
    }

    private fun loadChildRegions(parentId: String) {
        var children = regionRepository.getChildRegions(parentId)

        // 如果数据库中没有子区域，尝试从 BoundaryLoader 动态加载
        if (children.isEmpty() && boundaryLoader != null) {
            val childBoundaries = boundaryLoader.loadChildRegions(parentId)
            if (childBoundaries != null) {
                for (child in childBoundaries) {
                    regionRepository.insertRegion(
                        Region(child.id, child.name, RegionLevel.DISTRICT, parentId)
                    )
                    regionRepository.updateBoundary(child.id, child.boundary)
                }
                children = regionRepository.getChildRegions(parentId)
            }
        }
        _regions.value = children.map { region ->
            val footprint = footprintRepository.getFootprint(userId, region.id)
            val coverage = computeChildCoverage(region.id)
            RegionFootprintUi(
                regionId = region.id,
                name = region.name,
                footprintLevel = footprint?.level,
                normalizedPath = emptyList(),
                bounds = RegionBounds(0f, 0f, 0f, 0f),
                childCoverageRate = coverage
            )
        }
        syncOverlaysToMap()
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

    private fun syncOverlaysToMap() {
        val controller = _mapController ?: return
        val regionIds = mutableSetOf<String>()
        for (region in _regions.value) {
            regionIds.add(region.regionId)
            val style = footprintOverlayStyle(region.footprintLevel, region.childCoverageRate)
            val boundary = regionRepository.getRegionBoundary(region.regionId)
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
        // 程序控制相机时（双击下钻/面包屑返回），不触发自动层级切换
        if (_programmaticCamera) {
            _programmaticCamera = false
            return
        }

        val currentZoomLevel = _currentLevel.value

        // 根据缩放级别判断应该显示的层级
        val targetLevel = when {
            zoom < 5f -> MapZoomLevel.NATIONAL
            zoom < 7.5f -> MapZoomLevel.PROVINCIAL
            zoom < 10f -> MapZoomLevel.CITY
            else -> MapZoomLevel.DISTRICT
        }

        // 层级没有变化则不处理
        if (targetLevel == currentZoomLevel) return

        when {
            // 放大：不自动下钻，需用户双击指定区域
            targetLevel.ordinal > currentZoomLevel.ordinal -> { }
            // 缩小：返回上一级，不移动相机
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
            // 未到访：用蓝色深浅梯度表示子区域覆盖率
            val rate = childCoverageRate.coerceIn(0f, 1f)
            val alpha = 0.12f + rate * 0.48f
            return OverlayStyle(
                fillColor = 0xFF4A90D9L,
                strokeColor = 0xFF00CCFF,
                strokeWidth = 2f,
                alpha = alpha
            )
        }
        // 已到访：按等级着色
        val baseColor = when (level) {
            FootprintLevel.DEEP -> 0xFFE94560L
            FootprintLevel.SHORT_VISIT -> 0xFFFF6B6BL
            FootprintLevel.PASS_BY -> 0xFFFFA502L
        }
        return OverlayStyle(
            fillColor = baseColor,
            strokeColor = 0xFFFFFF00,
            strokeWidth = 2f,
            alpha = 0.6f
        )
    }

    private fun computeChildCoverage(regionId: String): Float {
        val children = regionRepository.getChildRegions(regionId)
        if (children.isEmpty()) {
            // 子区域尚未加载，检查自身是否有足迹
            return if (footprintRepository.getFootprint(userId, regionId) != null) 1f else 0f
        }
        // 递归计算：子区域自身有足迹 或 子区域的后代有足迹
        val visited = children.count { child ->
            footprintRepository.getFootprint(userId, child.id) != null ||
                hasDescendantFootprint(child.id)
        }
        return visited.toFloat() / children.size
    }

    private fun hasDescendantFootprint(regionId: String): Boolean {
        val children = regionRepository.getChildRegions(regionId)
        if (children.isEmpty()) return false
        return children.any { child ->
            footprintRepository.getFootprint(userId, child.id) != null ||
                hasDescendantFootprint(child.id)
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
