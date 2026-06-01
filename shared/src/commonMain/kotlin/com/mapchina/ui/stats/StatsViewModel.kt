package com.mapchina.ui.stats

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VisitedAttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val visitLevel: FootprintLevel
)

data class LevelDistribution(
    val a5Total: Int,
    val a4Total: Int,
    val a5Visited: Int,
    val a4Visited: Int
)

data class ProvinceVisitUi(
    val provinceId: String,
    val provinceName: String,
    val attractionCount: Int,
    val visitedCount: Int
)

data class StatsUi(
    val visitedProvinces: Int = 0,
    val totalProvinces: Int = 0,
    val visitedCities: Int = 0,
    val totalCities: Int = 0,
    val visitedDistricts: Int = 0,
    val totalDistricts: Int = 0,
    val visitedAttractions: Int = 0,
    val totalAttractions: Int = 0,
    val visitedAttractionList: List<VisitedAttractionUi> = emptyList(),
    val levelDistribution: LevelDistribution = LevelDistribution(0, 0, 0, 0),
    val provinceVisits: List<ProvinceVisitUi> = emptyList(),
    val visitLevelCounts: Map<FootprintLevel, Int> = emptyMap()
) {
    val provincePercent: Float get() = if (totalProvinces > 0) visitedProvinces.toFloat() / totalProvinces else 0f
    val cityPercent: Float get() = if (totalCities > 0) visitedCities.toFloat() / totalCities else 0f
    val districtPercent: Float get() = if (totalDistricts > 0) visitedDistricts.toFloat() / totalDistricts else 0f
}

class StatsViewModel(
    private val footprintService: FootprintService,
    private val attractionRepository: AttractionRepository,
    private val footprintRepository: FootprintRepository,
    private val regionRepository: RegionRepository,
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _stats = MutableStateFlow(StatsUi())
    val stats: StateFlow<StatsUi> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshStats() {
        if (_isLoading.value) return
        vmScope.launch {
            _isLoading.value = true
            _stats.value = computeStats()
            _isLoading.value = false
        }
    }

    private fun computeStats(): StatsUi {
        val coverage = footprintService.getCoverageStats(userId)
        val totalAttractions = attractionRepository.getAttractionCount()

        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val visitedList = visits.mapNotNull { visit ->
            val attraction = attractionRepository.getAttraction(visit.attractionId) ?: return@mapNotNull null
            VisitedAttractionUi(
                id = attraction.id,
                name = attraction.name,
                level = attraction.level.name,
                regionId = attraction.regionId,
                visitLevel = visit.level
            )
        }

        val allAttractions = attractionRepository.getAllAttractions()
        val a5Total = allAttractions.count { it.level.name == "A5" }
        val a4Total = allAttractions.count { it.level.name == "A4" }
        val a5Visited = visitedList.count { it.level == "A5" }
        val a4Visited = visitedList.count { it.level == "A4" }

        val visitLevelCounts = visitedList.groupingBy { it.visitLevel }.eachCount()
        val provinceVisits = buildProvinceVisits(visitedList, allAttractions)

        return StatsUi(
            visitedProvinces = coverage.visitedProvinces,
            totalProvinces = coverage.totalProvinces,
            visitedCities = coverage.visitedCities,
            totalCities = coverage.totalCities,
            visitedDistricts = coverage.visitedDistricts,
            totalDistricts = coverage.totalDistricts,
            visitedAttractions = coverage.visitedAttractions,
            totalAttractions = totalAttractions,
            visitedAttractionList = visitedList,
            levelDistribution = LevelDistribution(a5Total, a4Total, a5Visited, a4Visited),
            provinceVisits = provinceVisits,
            visitLevelCounts = visitLevelCounts
        )
    }

    private fun buildProvinceVisits(
        visitedList: List<VisitedAttractionUi>,
        allAttractions: List<com.mapchina.domain.model.Attraction>
    ): List<ProvinceVisitUi> {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)

        return provinces.mapNotNull { province ->
            val provinceCode = province.id.substring(0, 2)
            val provinceAttractions = allAttractions.filter { it.regionId.startsWith(provinceCode) }
            if (provinceAttractions.isEmpty()) return@mapNotNull null
            val visited = visitedList.count { it.regionId.startsWith(provinceCode) }
            if (visited == 0) return@mapNotNull null
            ProvinceVisitUi(province.id, province.name, provinceAttractions.size, visited)
        }.sortedByDescending { it.visitedCount }
    }
}
