package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel

data class FootprintResult(
    val isSuccess: Boolean,
    val footprint: com.mapchina.domain.model.Footprint? = null,
    val achievementResult: AchievementUnlockResult? = null
)

data class CoverageStats(
    val visitedProvinces: Int,
    val totalProvinces: Int,
    val visitedCities: Int,
    val totalCities: Int,
    val visitedDistricts: Int,
    val totalDistricts: Int,
    val visitedAttractions: Int,
    val totalAttractions: Int
)

class FootprintService(
    private val footprintRepository: FootprintRepository,
    private val regionRepository: RegionRepository,
    private val achievementService: AchievementService? = null
) {
    fun markFootprint(userId: String, regionId: String, level: FootprintLevel): FootprintResult {
        footprintRepository.markFootprint(userId, regionId, level)
        val footprint = footprintRepository.getFootprint(userId, regionId)

        achievementService?.addFootprintScore(userId, level)
        val achievementResult = achievementService?.evaluateAndSettle(userId)

        return FootprintResult(isSuccess = true, footprint = footprint, achievementResult = achievementResult)
    }

    fun markAttractionVisit(userId: String, attractionId: String, regionId: String, level: FootprintLevel): FootprintResult {
        footprintRepository.markAttractionVisit(userId, attractionId, regionId, level)
        val footprint = footprintRepository.getFootprint(userId, regionId)

        achievementService?.addFootprintScore(userId, level)
        val achievementResult = achievementService?.evaluateAndSettle(userId)

        return FootprintResult(isSuccess = true, footprint = footprint, achievementResult = achievementResult)
    }

    fun removeAttractionVisit(userId: String, attractionId: String) {
        footprintRepository.removeAttractionVisit(userId, attractionId)
    }

    fun removeFootprint(userId: String, regionId: String) {
        footprintRepository.removeFootprint(userId, regionId)
    }

    fun getCoverageStats(userId: String): CoverageStats {
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val visitedRegionIds = footprints.map { it.regionId }.toSet()

        // 按区域 ID 模式分类：xx0000=省，xxyy00(非xx0000)=市，其他=区县
        val visitedProvinces = visitedRegionIds.count { it.endsWith("0000") }
        val visitedCities = visitedRegionIds.count { it.endsWith("00") && !it.endsWith("0000") }
        val visitedDistricts = visitedRegionIds.count { !it.endsWith("00") }

        val allProvinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY)

        val visitedAttractions = footprintRepository.getAttractionVisitCount(userId)

        return CoverageStats(
            visitedProvinces = visitedProvinces,
            totalProvinces = allProvinces.size,
            visitedCities = visitedCities,
            totalCities = allCities.size,
            visitedDistricts = visitedDistricts,
            totalDistricts = TOTAL_DISTRICTS,
            visitedAttractions = visitedAttractions,
            totalAttractions = 0
        )
    }

    companion object {
        private const val TOTAL_DISTRICTS = 2844
    }

    fun getFootprintsForUser(userId: String) = footprintRepository.getFootprintsByUser(userId)

    fun getFootprint(userId: String, regionId: String) = footprintRepository.getFootprint(userId, regionId)

    fun getRegionRepository() = regionRepository
}
