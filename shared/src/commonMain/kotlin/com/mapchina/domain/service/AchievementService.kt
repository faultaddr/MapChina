package com.mapchina.domain.service

import kotlin.time.Clock

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
            val (progress, target) = computeProgressAndTarget(def, stats, provinceStats, atlasStats, userId)
            val wasUnlocked = existingMap[def.id]?.isUnlocked == true

            achievementRepository.updateProgress(userId, def.id, progress, target)

            if (progress >= target && target > 0 && !wasUnlocked) {
                newlyUnlocked.add(
                    UserAchievement(userId, def.id, progress, target, AchievementStatus.UNLOCKED, Clock.System.now())
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

    private fun computeProgressAndTarget(def: Achievement, stats: UserStats, provinceStats: Map<String, ProvinceStat>, atlasStats: Map<String, Int>, userId: String): ProgressResult {
        val cond = def.triggerCondition
        val key = cond.substringBefore(":")

        return when (def.category) {
            AchievementCategory.PROVINCE -> computeProvinceProgress(def, cond, key, provinceStats)
            AchievementCategory.ATLAS -> computeAtlasProgress(cond, key, atlasStats)
            AchievementCategory.GEOGRAPHY -> computeGeographyProgress(cond, key, provinceStats, userId)
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

    private val northProvinces = setOf("23") // 黑龙江
    private val southProvinces = setOf("46") // 海南
    private val silkRoadProvinces = setOf("62", "61") // 甘肃, 陕西
    private val coastalProvinces = setOf("13", "21", "33", "35", "37", "44", "45", "46") // 河北, 辽宁, 浙江, 福建, 山东, 广东, 广西, 海南
    private val northOfYangtze = setOf("11", "12", "13", "14", "15", "21", "22", "23", "61", "62", "63", "64", "65") // 北京,天津,河北,山西,内蒙,辽宁,吉林,黑龙江,陕西,甘肃,青海,宁夏,新疆
    private val southOfYangtze = setOf("31", "32", "33", "34", "35", "36", "37", "41", "42", "43", "44", "45", "46", "50", "51", "52", "53", "54") // 上海,江苏,浙江,安徽,福建,江西,山东,河南,湖北,湖南,广东,广西,海南,重庆,四川,贵州,云南,西藏

    private fun computeGeographyProgress(cond: String, key: String, provinceStats: Map<String, ProvinceStat>, userId: String): ProgressResult {
        val allProvinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val visitedProvinceCodes = allProvinces
            .filter { provinceStats.containsKey(it.id.substring(0, 2)) && (provinceStats[it.id.substring(0, 2)]?.visitedCities ?: 0) > 0 }
            .map { it.id.substring(0, 2) }
            .toSet()

        return when (key) {
            "geo" -> {
                val parts = cond.split(":")
                if (parts.size < 3) return ProgressResult(0, 1)
                val geoType = parts[1]
                val target = parts[2].toIntOrNull() ?: 1
                val progress = when (geoType) {
                    "north" -> visitedProvinceCodes.count { it in northProvinces }
                    "south" -> visitedProvinceCodes.count { it in southProvinces }
                    "silk_road" -> visitedProvinceCodes.count { it in silkRoadProvinces }
                    "coast" -> visitedProvinceCodes.count { it in coastalProvinces }
                    "cross_river" -> {
                        val hasNorth = visitedProvinceCodes.any { it in northOfYangtze }
                        val hasSouth = visitedProvinceCodes.any { it in southOfYangtze }
                        (if (hasNorth) 1 else 0) + (if (hasSouth) 1 else 0)
                    }
                    "same_day_3" -> computeSameDayProvinceCount(footprintRepository.getFootprintsByUser(userId))
                    else -> 0
                }
                ProgressResult(progress, target)
            }
            else -> ProgressResult(0, 1)
        }
    }

    private fun computeSameDayProvinceCount(footprints: List<Footprint>): Int {
        if (footprints.isEmpty()) return 0
        val byDay = footprints.groupBy { fp ->
            fp.timestamp.toEpochMilliseconds() / (24 * 60 * 60 * 1000L)
        }
        val maxProvincesInDay = byDay.values.maxOfOrNull { dayFootprints ->
            dayFootprints.map { it.regionId.substring(0, 2) }.distinct().size
        } ?: 0
        return if (maxProvincesInDay >= 3) 3 else maxProvincesInDay
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

        val allCities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        val allDistricts = regionRepository.getRegionsByLevel(RegionLevel.DISTRICT)
        val allAttractions = attractionRepository.getAllAttractions()
        val userAchievements = achievementRepository.getUserAchievements(userId)
        val unlockedIds = userAchievements.filter { it.isUnlocked }.map { it.achievementId }.toSet()

        val attractionsByProvince = allAttractions.groupBy { it.regionId.substring(0, 2) }
        val citiesByProvince = allCities.groupBy { it.id.substring(0, 2) }
        val districtsByProvince = allDistricts.groupBy { it.id.substring(0, 2) }
        val visitAttractionIds = visits.map { it.attractionId }.toSet()

        return allProvinces.mapNotNull { province ->
            val code = province.id.substring(0, 2)
            val stat = provinceStats[code] ?: return@mapNotNull null

            val provinceAttractions = attractionsByProvince[code] ?: emptyList()
            val visitedAttrCount = provinceAttractions.count { it.id in visitAttractionIds }
            val provinceCities = (citiesByProvince[code] ?: emptyList()).filter { it.id != province.id }
            val visitedCityCount = provinceCities.count { it.id in visitedRegionIds }
            val provinceDistricts = districtsByProvince[code] ?: emptyList()
            val visitedDistrictCount = provinceDistricts.count { it.id in visitedRegionIds }

            ProvinceConquestInfo(
                provinceId = province.id,
                provinceName = province.name,
                visitedAttractions = visitedAttrCount,
                totalAttractions = provinceAttractions.size,
                visitedCities = visitedCityCount,
                totalCities = provinceCities.size,
                visitedDistricts = visitedDistrictCount,
                totalDistricts = provinceDistricts.size,
                hasVisitBadge = "province_visit_$code" in unlockedIds,
                hasCompleteBadge = "province_complete_$code" in unlockedIds
            )
        }
    }
}
