package com.mapchina.ui.stats

import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatsUi(
    val visitedProvinces: Int,
    val totalProvinces: Int,
    val visitedCities: Int,
    val totalCities: Int,
    val visitedDistricts: Int,
    val totalDistricts: Int,
    val visitedAttractions: Int,
    val totalAttractions: Int
) {
    val provincePercent: Float get() = if (totalProvinces > 0) visitedProvinces.toFloat() / totalProvinces else 0f
    val cityPercent: Float get() = if (totalCities > 0) visitedCities.toFloat() / totalCities else 0f
    val districtPercent: Float get() = if (totalDistricts > 0) visitedDistricts.toFloat() / totalDistricts else 0f
}

class StatsViewModel(
    private val footprintService: FootprintService,
    private val userId: String = ""
) {
    private val _stats = MutableStateFlow(StatsUi(0, 0, 0, 0, 0, 0, 0, 0))
    val stats: StateFlow<StatsUi> = _stats.asStateFlow()

    fun refreshStats() {
        val coverage = footprintService.getCoverageStats(userId)
        _stats.value = StatsUi(
            visitedProvinces = coverage.visitedProvinces,
            totalProvinces = coverage.totalProvinces,
            visitedCities = coverage.visitedCities,
            totalCities = coverage.totalCities,
            visitedDistricts = coverage.visitedDistricts,
            totalDistricts = coverage.totalDistricts,
            visitedAttractions = coverage.visitedAttractions,
            totalAttractions = coverage.totalAttractions
        )
    }
}
