package com.mapchina.ui.achievement

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AchievementUi(
    val levelInfo: UserLevelInfo? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val recentUnlocked: List<AchievementWithProgress> = emptyList(),
    val nextTarget: AchievementWithProgress? = null,
    val regionAchievements: List<AchievementWithProgress> = emptyList(),
    val scenicAchievements: List<AchievementWithProgress> = emptyList(),
    val allAchievements: List<AchievementWithProgress> = emptyList()
)

data class AchievementWithProgress(
    val definition: Achievement,
    val progressValue: Int,
    val progressTarget: Int,
    val isUnlocked: Boolean,
    val unlockTime: kotlinx.datetime.Instant?
) {
    val progressPercent: Float get() = if (progressTarget > 0) progressValue.toFloat() / progressTarget else 0f
}

class AchievementViewModel(
    private val achievementRepository: AchievementRepository,
    private val userScoreRepository: UserScoreRepository,
    private val userId: String = ""
) {
    private val _ui = MutableStateFlow(AchievementUi())
    val ui: StateFlow<AchievementUi> = _ui.asStateFlow()

    fun refresh() {
        val levelInfo = userScoreRepository.getScore(userId)
        val allDefs = achievementRepository.getAllDefinitions()
        val userAchievements = achievementRepository.getUserAchievements(userId)
        val userMap = userAchievements.associateBy { it.achievementId }

        val allWithProgress = allDefs.map { def ->
            val ua = userMap[def.id]
            AchievementWithProgress(
                definition = def,
                progressValue = ua?.progressValue ?: 0,
                progressTarget = ua?.progressTarget ?: def.triggerCondition.substringAfterLast(":").toIntOrNull() ?: 0,
                isUnlocked = ua?.isUnlocked == true,
                unlockTime = ua?.unlockTime
            )
        }

        val unlocked = allWithProgress.filter { it.isUnlocked }
        val locked = allWithProgress.filter { !it.isUnlocked }

        val nextTarget = locked.minByOrNull {
            if (it.progressTarget > 0) it.progressTarget - it.progressValue else Int.MAX_VALUE
        }

        _ui.value = AchievementUi(
            levelInfo = levelInfo,
            unlockedCount = unlocked.size,
            totalCount = allWithProgress.size,
            recentUnlocked = unlocked.sortedByDescending { it.unlockTime }.take(3),
            nextTarget = nextTarget,
            regionAchievements = allWithProgress.filter { it.definition.category == AchievementCategory.REGION },
            scenicAchievements = allWithProgress.filter { it.definition.category == AchievementCategory.SCENIC },
            allAchievements = allWithProgress
        )
    }
}
