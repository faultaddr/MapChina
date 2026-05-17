package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel

data class FootprintResult(val isSuccess: Boolean, val footprint: com.mapchina.domain.model.Footprint? = null)

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
    private val regionRepository: RegionRepository
) {
    fun markFootprint(userId: String, regionId: String, level: FootprintLevel): FootprintResult {
        footprintRepository.markFootprint(userId, regionId, level)
        val footprint = footprintRepository.getFootprint(userId, regionId)
        return FootprintResult(isSuccess = true, footprint = footprint)
    }

    fun markAttractionVisit(userId: String, attractionId: String, regionId: String, level: FootprintLevel): FootprintResult {
        footprintRepository.markAttractionVisit(userId, attractionId, regionId, level)
        val footprint = footprintRepository.getFootprint(userId, regionId)
        return FootprintResult(isSuccess = true, footprint = footprint)
    }

    fun getCoverageStats(userId: String): CoverageStats {
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val allProvinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        val allDistricts = regionRepository.getRegionsByLevel(RegionLevel.DISTRICT)

        val visitedRegionIds = footprints.map { it.regionId }.toSet()

        val visitedProvinces = allProvinces.count { it.id in visitedRegionIds }
        val visitedCities = allCities.count { it.id in visitedRegionIds }
        val visitedDistricts = allDistricts.count { it.id in visitedRegionIds }

        val counts = footprintRepository.getFootprintCountsByLevel(userId)
        val visitedAttractions = counts.values.sum()

        return CoverageStats(
            visitedProvinces = visitedProvinces,
            totalProvinces = allProvinces.size,
            visitedCities = visitedCities,
            totalCities = allCities.size,
            visitedDistricts = visitedDistricts,
            totalDistricts = allDistricts.size,
            visitedAttractions = visitedAttractions,
            totalAttractions = 0
        )
    }

    fun getFootprintsForUser(userId: String) = footprintRepository.getFootprintsByUser(userId)

    fun getFootprint(userId: String, regionId: String) = footprintRepository.getFootprint(userId, regionId)
}
