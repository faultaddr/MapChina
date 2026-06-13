package com.mapchina.domain.model

import kotlin.time.Instant

data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val subCategory: String,
    val name: String,
    val description: String,
    val icon: String,
    val rarity: AchievementRarity,
    val triggerType: TriggerType,
    val triggerCondition: String,
    val rewardScore: Int,
    val sortOrder: Int
)

data class UserAchievement(
    val userId: String,
    val achievementId: String,
    val progressValue: Int,
    val progressTarget: Int,
    val status: AchievementStatus,
    val unlockTime: Instant?
) {
    val isUnlocked: Boolean get() = status == AchievementStatus.UNLOCKED
    val progressPercent: Float get() = if (progressTarget > 0) progressValue.toFloat() / progressTarget else 0f
}

enum class AchievementCategory { REGION, SCENIC, PROVINCE, ATLAS, GEOGRAPHY }
enum class AchievementRarity { COMMON, RARE, EPIC, LEGENDARY }
enum class AchievementStatus { LOCKED, UNLOCKED }
enum class TriggerType { COUNT, RATIO, FULL_COMPLETE, CUSTOM, GEO }
