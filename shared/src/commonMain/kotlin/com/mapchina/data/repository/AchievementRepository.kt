package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.*
import kotlin.time.Clock
import kotlin.time.Instant

class AchievementRepository(private val database: MapChinaDatabase) {

    fun getAllDefinitions(): List<Achievement> =
        database.achievementDefinitionQueries.selectAll().executeAsList().map { it.toDomain() }

    fun getDefinitionsByCategory(category: AchievementCategory): List<Achievement> =
        database.achievementDefinitionQueries.selectByCategory(category.name).executeAsList().map { it.toDomain() }

    fun getDefinitionById(id: String): Achievement? {
        val row = database.achievementDefinitionQueries.selectById(id).executeAsOneOrNull()
        return row?.toDomain()
    }

    fun insertDefinition(
        id: String, category: String, subCategory: String,
        name: String, description: String, icon: String,
        rarity: String, triggerType: String, triggerCondition: String,
        rewardScore: Long, sortOrder: Long
    ) {
        database.achievementDefinitionQueries.insertDefinition(
            id, category, subCategory, name, description, icon,
            rarity, triggerType, triggerCondition, rewardScore, sortOrder, "active"
        )
    }

    fun getUserAchievements(userId: String): List<UserAchievement> =
        database.userAchievementQueries.selectByUser(userId).executeAsList().map { it.toDomain() }

    fun getUnlockedAchievements(userId: String): List<UserAchievement> =
        database.userAchievementQueries.selectUnlockedByUser(userId).executeAsList().map { it.toDomain() }

    fun getUnlockedCount(userId: String): Int =
        database.userAchievementQueries.countUnlockedByUser(userId).executeAsOne().toInt()

    fun getUserAchievement(userId: String, achievementId: String): UserAchievement? {
        val row = database.userAchievementQueries.selectByUserAndAchievement(userId, achievementId).executeAsOneOrNull()
        return row?.toDomain()
    }

    fun initUserAchievements(userId: String) {
        val allDefs = database.achievementDefinitionQueries.selectAll().executeAsList()
        database.userAchievementQueries.transaction {
            for (def in allDefs) {
                val target = parseTarget(def.trigger_condition)
                database.userAchievementQueries.initAchievement(userId, def.achievement_id, target)
            }
        }
    }

    fun updateProgress(userId: String, achievementId: String, progressValue: Int, target: Int) {
        val existing = database.userAchievementQueries.selectByUserAndAchievement(userId, achievementId).executeAsOneOrNull()
        if (existing != null && existing.status.uppercase() == "UNLOCKED") return

        val status = if (progressValue >= target) "UNLOCKED" else "LOCKED"
        val unlockTime = if (status == "UNLOCKED") Clock.System.now().toEpochMilliseconds() else null
        database.userAchievementQueries.upsertProgress(userId, achievementId, progressValue.toLong(), target.toLong(), status, unlockTime)
    }

    fun isInitialized(userId: String): Boolean {
        val count = database.userAchievementQueries.selectByUser(userId).executeAsList().size
        return count > 0
    }

    private fun parseTarget(triggerCondition: String): Long {
        return triggerCondition.substringAfterLast(":").toLongOrNull() ?: 0L
    }

    private fun com.mapchina.data.local.Achievement_definition.toDomain() = Achievement(
        id = achievement_id,
        category = AchievementCategory.valueOf(category),
        subCategory = sub_category,
        name = name,
        description = description,
        icon = icon,
        rarity = AchievementRarity.valueOf(rarity),
        triggerType = TriggerType.valueOf(trigger_type),
        triggerCondition = trigger_condition,
        rewardScore = reward_score.toInt(),
        sortOrder = sort_order.toInt()
    )

    private fun com.mapchina.data.local.User_achievement.toDomain() = UserAchievement(
        userId = user_id,
        achievementId = achievement_id,
        progressValue = progress_value.toInt(),
        progressTarget = progress_target.toInt(),
        status = AchievementStatus.valueOf(status.uppercase()),
        unlockTime = unlock_time?.let { Instant.fromEpochMilliseconds(it) }
    )
}
