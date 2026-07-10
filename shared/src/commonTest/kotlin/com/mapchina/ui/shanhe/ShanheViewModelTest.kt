package com.mapchina.ui.shanhe

import com.mapchina.domain.model.Achievement
import com.mapchina.domain.model.AchievementCategory
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.TriggerType
import com.mapchina.domain.model.UserLevelInfo
import com.mapchina.ui.achievement.AchievementUi
import com.mapchina.ui.achievement.AchievementWithProgress
import com.mapchina.ui.stats.StatsUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShanheViewModelTest {
    @Test
    fun buildShanheUi_usesGrowthAndCoverageData() {
        val target = achievementProgress(
            id = "visit_10_districts",
            name = "百县行者",
            description = "再访 3 个区县",
            progress = 7,
            target = 10,
            isUnlocked = false
        )
        val unlocked = achievementProgress(
            id = "first_sichuan_visit",
            name = "初见巴蜀",
            description = "第一次点亮四川",
            progress = 1,
            target = 1,
            isUnlocked = true
        )
        val achievementUi = AchievementUi(
            levelInfo = UserLevelInfo(
                userId = "u1",
                currentScore = 320,
                currentLevel = 3,
                currentTitle = "远游者",
                nextLevelScore = 800,
                nextTitle = "山河行者"
            ),
            unlockedCount = 1,
            totalCount = 12,
            recentUnlocked = listOf(unlocked),
            nextTarget = target
        )
        val statsUi = StatsUi(
            visitedProvinces = 2,
            totalProvinces = 34,
            visitedCities = 5,
            totalCities = 333,
            visitedDistricts = 7,
            totalDistricts = 2844
        )

        val ui = buildShanheUi(achievementUi, statsUi)

        assertEquals(3, ui.levelNumber)
        assertEquals("远游者", ui.levelTitle)
        assertEquals(320, ui.currentScore)
        assertEquals("山河行者", ui.nextLevelTitle)
        assertEquals(480, ui.remainingScore)
        assertTrue(ui.levelProgress > 0f)
        assertEquals("百县行者", ui.targetTitle)
        assertEquals("再访 3 个区县", ui.targetBody)
        assertEquals("7 / 10", ui.targetProgressLabel)
        assertEquals(0.7f, ui.targetProgress)
        assertEquals(2, ui.visitedProvinces)
        assertEquals(5, ui.visitedCities)
        assertEquals(7, ui.visitedDistricts)
        assertEquals(listOf("初见巴蜀"), ui.recentUnlocks)
    }

    @Test
    fun buildShanheUiForUser_hidesStateOwnedByAnotherUser() {
        val achievementUi = AchievementUi(
            userId = "user-a",
            levelInfo = UserLevelInfo(
                userId = "user-a",
                currentScore = 1600,
                currentLevel = 5,
                currentTitle = "九州旅人",
                nextLevelScore = 3000,
                nextTitle = "华境探索家"
            ),
            unlockedCount = 8,
            totalCount = 12
        )
        val statsUi = StatsUi(
            userId = "user-a",
            visitedProvinces = 9,
            totalProvinces = 34
        )

        val ui = buildShanheUiForUser(
            currentUserId = "user-b",
            achievement = achievementUi,
            stats = statsUi
        )

        assertEquals(ShanheUi(), ui)
    }

    @Test
    fun buildShanheUi_showsCompletionCopyWhenAllAchievementsAreUnlocked() {
        val ui = buildShanheUi(
            achievement = AchievementUi(
                unlockedCount = 100,
                totalCount = 100,
                nextTarget = null
            ),
            stats = StatsUi(visitedProvinces = 34, totalProvinces = 34)
        )

        assertEquals("继续丈量山河", ui.targetTitle)
        assertEquals("全部勋章已解锁，去发现下一块未点亮版图", ui.targetBody)
        assertEquals("已完成", ui.targetProgressLabel)
        assertEquals(1f, ui.targetProgress)
    }

    private fun achievementProgress(
        id: String,
        name: String,
        description: String,
        progress: Int,
        target: Int,
        isUnlocked: Boolean
    ) = AchievementWithProgress(
        definition = Achievement(
            id = id,
            category = AchievementCategory.REGION,
            subCategory = "coverage",
            name = name,
            description = description,
            icon = "Map",
            rarity = AchievementRarity.COMMON,
            triggerType = TriggerType.COUNT,
            triggerCondition = "count:$target",
            rewardScore = 10,
            sortOrder = 1
        ),
        progressValue = progress,
        progressTarget = target,
        isUnlocked = isUnlocked,
        unlockTime = null
    )
}
