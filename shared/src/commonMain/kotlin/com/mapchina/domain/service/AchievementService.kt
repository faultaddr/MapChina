package com.mapchina.domain.service

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.*

data class AchievementUnlockResult(
    val newlyUnlocked: List<UserAchievement>,
    val scoreAdded: Int,
    val levelChanged: Boolean,
    val oldLevel: Int,
    val newLevel: Int
)

class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val footprintRepository: FootprintRepository,
    private val userScoreRepository: UserScoreRepository,
    private val attractionRepository: AttractionRepository,
    private val regionRepository: RegionRepository,
    private val atlasRepository: AtlasRepository
) {
    fun evaluateAndSettle(userId: String): AchievementUnlockResult {
        if (!achievementRepository.isInitialized(userId)) {
            achievementRepository.initUserAchievements(userId)
        }
        userScoreRepository.ensureUserScore(userId)

        val allDefs = achievementRepository.getAllDefinitions()
        val existing = achievementRepository.getUserAchievements(userId)
        val existingMap = existing.associateBy { it.achievementId }

        val stats = computeCurrentStats(userId)
        val provinceStats = computeProvinceStats(userId)
        val atlasStats = computeAtlasStats(userId)
        val newlyUnlocked = mutableListOf<UserAchievement>()
        var totalScoreFromAchievements = 0

        for (def in allDefs) {
            val (progress, target) = computeProgressAndTarget(def, stats, provinceStats, atlasStats)
            val wasUnlocked = existingMap[def.id]?.isUnlocked == true

            achievementRepository.updateProgress(userId, def.id, progress, target)

            if (progress >= target && target > 0 && !wasUnlocked) {
                newlyUnlocked.add(
                    UserAchievement(userId, def.id, progress, target, AchievementStatus.UNLOCKED, kotlinx.datetime.Clock.System.now())
                )
                totalScoreFromAchievements += def.rewardScore
            }
        }

        val oldLevel = userScoreRepository.getCurrentLevel(userId)

        if (totalScoreFromAchievements > 0) {
            userScoreRepository.addScore(userId, totalScoreFromAchievements)
        }

        val newLevel = userScoreRepository.getCurrentLevel(userId)

        return AchievementUnlockResult(
            newlyUnlocked = newlyUnlocked,
            scoreAdded = totalScoreFromAchievements,
            levelChanged = newLevel > oldLevel,
            oldLevel = oldLevel,
            newLevel = newLevel
        )
    }

    private data class UserStats(
        val visitedDistricts: Int,
        val visitedCities: Int,
        val visitedProvinces: Int,
        val visited5a: Int,
        val visitedTotal: Int
    )

    private fun computeCurrentStats(userId: String): UserStats {
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val visitedRegionIds = footprints.map { it.regionId }.toSet()
        val districts = visitedRegionIds.count { !it.endsWith("00") }
        val cities = visitedRegionIds.count { it.endsWith("00") && !it.endsWith("0000") }
        val provinces = visitedRegionIds.count { it.endsWith("0000") }

        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val a5Count = visits.count { v ->
            val attraction = attractionRepository.getAttraction(v.attractionId)
            attraction?.level == AttractionLevel.A5
        }

        return UserStats(districts, cities, provinces, a5Count, visits.size)
    }

    data class ProvinceStat(
        val provinceCode: String,
        val visitedCities: Int,
        val totalCities: Int,
        val visitedAttractions: Int,
        val totalAttractions: Int
    )

    private fun computeProvinceStats(userId: String): Map<String, ProvinceStat> {
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val visitedRegionIds = footprints.map { it.regionId }.toSet()
        val visits = footprintRepository.getAttractionVisitsByUser(userId)

        val allProvinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        val allAttractions = attractionRepository.getAllAttractions()

        val result = mutableMapOf<String, ProvinceStat>()

        for (province in allProvinces) {
            val code = province.id.substring(0, 2)
            val provinceCities = allCities.filter { it.parentId == province.id || it.id.startsWith(code) && it.id.endsWith("00") && it.id != province.id }
            val visitedCities = provinceCities.count { it.id in visitedRegionIds }
            val provinceAttractions = allAttractions.filter { it.regionId.startsWith(code) }
            val visitedAttractions = visits.count { v ->
                provinceAttractions.any { it.id == v.attractionId }
            }
            result[code] = ProvinceStat(code, visitedCities, provinceCities.size, visitedAttractions, provinceAttractions.size)
        }

        return result
    }

    private data class ProgressResult(val progress: Int, val target: Int)

    private fun computeAtlasStats(userId: String): Map<String, Int> {
        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val visitedIds = visits.map { it.attractionId }.toSet()
        val definitions = atlasRepository.getAllDefinitions()
        val result = mutableMapOf<String, Int>()
        for (def in definitions) {
            val items = atlasRepository.getItemsByAtlas(def.id)
            result[def.id] = items.count { it.attractionId in visitedIds }
        }
        return result
    }

    private fun computeProgressAndTarget(def: Achievement, stats: UserStats, provinceStats: Map<String, ProvinceStat>, atlasStats: Map<String, Int>): ProgressResult {
        val cond = def.triggerCondition
        val key = cond.substringBefore(":")

        return when (def.category) {
            AchievementCategory.PROVINCE -> computeProvinceProgress(def, cond, key, provinceStats)
            AchievementCategory.ATLAS -> computeAtlasProgress(cond, key, atlasStats)
            else -> computeRegionScenicProgress(cond, key, stats)
        }
    }

    private fun computeProvinceProgress(def: Achievement, cond: String, key: String, provinceStats: Map<String, ProvinceStat>): ProgressResult {
        return when (key) {
            "province_visit" -> {
                val parts = cond.split(":")
                if (parts.size >= 3) {
                    val provinceCode = parts[1]
                    val target = parts[2].toIntOrNull() ?: 1
                    val stat = provinceStats[provinceCode]
                    val progress = if (stat != null && stat.visitedCities > 0) 1 else 0
                    ProgressResult(progress, target)
                } else ProgressResult(0, 1)
            }
            "province_complete" -> {
                val parts = cond.split(":")
                if (parts.size >= 2) {
                    val provinceCode = parts[1]
                    val stat = provinceStats[provinceCode]
                    if (stat != null && stat.totalCities > 0) {
                        val progress = if (stat.visitedCities >= stat.totalCities) 1 else 0
                        ProgressResult(progress, 1)
                    } else ProgressResult(0, 1)
                } else ProgressResult(0, 1)
            }
            else -> ProgressResult(0, 1)
        }
    }

    private fun computeAtlasProgress(cond: String, key: String, atlasStats: Map<String, Int>): ProgressResult {
        if (key != "atlas") return ProgressResult(0, 1)
        val parts = cond.split(":")
        if (parts.size < 3) return ProgressResult(0, 1)
        val atlasId = parts[1]
        val target = parts[2].toIntOrNull() ?: 1
        val progress = atlasStats[atlasId] ?: 0
        return ProgressResult(progress, target)
    }

    private fun computeRegionScenicProgress(cond: String, key: String, stats: UserStats): ProgressResult {
        val valueStr = cond.substringAfterLast(":")
        val target = valueStr.toIntOrNull() ?: 0
        val progress = when (key) {
            "district" -> stats.visitedDistricts
            "city" -> stats.visitedCities
            "province" -> stats.visitedProvinces
            "5a" -> stats.visited5a
            "total" -> stats.visitedTotal
            else -> 0
        }
        return ProgressResult(progress, target)
    }

    fun addFootprintScore(userId: String, footprintLevel: FootprintLevel) {
        val delta = when (footprintLevel) {
            FootprintLevel.PASS_BY -> 10
            FootprintLevel.SHORT_VISIT -> 20
            FootprintLevel.DEEP -> 50
        }
        userScoreRepository.addScore(userId, delta)
    }

    fun getProvinceConquestInfo(userId: String): List<ProvinceConquestInfo> {
        val provinceStats = computeProvinceStats(userId)
        val allProvinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val visitedRegionIds = footprints.map { it.regionId }.toSet()
        val visits = footprintRepository.getAttractionVisitsByUser(userId)

        return allProvinces.mapNotNull { province ->
            val code = province.id.substring(0, 2)
            val stat = provinceStats[code] ?: return@mapNotNull null

            val allAttractions = attractionRepository.getAttractionsByRegionPrefix("$code%")
            val visitedAttrCount = visits.count { v -> allAttractions.any { it.id == v.attractionId } }
            val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY).filter { it.id.startsWith(code) && it.id != province.id }
            val visitedCityCount = allCities.count { it.id in visitedRegionIds }
            val allDistricts = regionRepository.getRegionsByLevel(RegionLevel.DISTRICT).filter { it.id.startsWith(code) }
            val visitedDistrictCount = allDistricts.count { it.id in visitedRegionIds }

            val userAchievements = achievementRepository.getUserAchievements(userId)
            val hasVisitBadge = userAchievements.any { it.achievementId == "province_visit_$code" && it.isUnlocked }
            val hasCompleteBadge = userAchievements.any { it.achievementId == "province_complete_$code" && it.isUnlocked }

            ProvinceConquestInfo(
                provinceId = province.id,
                provinceName = province.name,
                visitedAttractions = visitedAttrCount,
                totalAttractions = allAttractions.size,
                visitedCities = visitedCityCount,
                totalCities = allCities.size,
                visitedDistricts = visitedDistrictCount,
                totalDistricts = allDistricts.size,
                hasVisitBadge = hasVisitBadge,
                hasCompleteBadge = hasCompleteBadge
            )
        }
    }
}
