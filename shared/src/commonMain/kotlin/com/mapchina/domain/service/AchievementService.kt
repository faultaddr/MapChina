package com.mapchina.domain.service

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
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
    private val attractionRepository: AttractionRepository
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
        val newlyUnlocked = mutableListOf<UserAchievement>()
        var totalScoreFromAchievements = 0

        for (def in allDefs) {
            val progress = computeProgress(def, stats)
            val target = def.triggerCondition.substringAfterLast(":").toIntOrNull() ?: 0
            val wasUnlocked = existingMap[def.id]?.isUnlocked == true

            achievementRepository.updateProgress(userId, def.id, progress, target)

            if (progress >= target && !wasUnlocked) {
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

    private fun computeProgress(def: Achievement, stats: UserStats): Int {
        val key = def.triggerCondition.substringBefore(":")
        return when (key) {
            "district" -> stats.visitedDistricts
            "city" -> stats.visitedCities
            "province" -> stats.visitedProvinces
            "5a" -> stats.visited5a
            "total" -> stats.visitedTotal
            else -> 0
        }
    }

    fun addFootprintScore(userId: String, footprintLevel: com.mapchina.domain.model.FootprintLevel) {
        val delta = when (footprintLevel) {
            com.mapchina.domain.model.FootprintLevel.PASS_BY -> 10
            com.mapchina.domain.model.FootprintLevel.SHORT_VISIT -> 20
            com.mapchina.domain.model.FootprintLevel.DEEP -> 50
        }
        userScoreRepository.addScore(userId, delta)
    }
}
