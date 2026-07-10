package com.mapchina.ui.shanhe

import com.mapchina.ui.achievement.AchievementUi
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.stats.StatsUi
import com.mapchina.ui.stats.StatsViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ShanheUi(
    val levelNumber: Int = 1,
    val levelTitle: String = "初行者",
    val currentScore: Int = 0,
    val nextLevelTitle: String = "识途者",
    val remainingScore: Int = 100,
    val levelProgress: Float = 0f,
    val targetTitle: String = "点亮第一块版图",
    val targetBody: String = "从足迹页确认一个去过的地方",
    val targetProgressLabel: String = "0 / 1",
    val targetProgress: Float = 0f,
    val unlockedCount: Int = 0,
    val totalAchievementCount: Int = 0,
    val visitedProvinces: Int = 0,
    val totalProvinces: Int = 34,
    val visitedCities: Int = 0,
    val visitedDistricts: Int = 0,
    val recentUnlocks: List<String> = emptyList()
)

fun buildShanheUi(achievement: AchievementUi, stats: StatsUi): ShanheUi {
    val level = achievement.levelInfo
    val target = achievement.nextTarget
    return ShanheUi(
        levelNumber = level?.currentLevel ?: 1,
        levelTitle = level?.currentTitle ?: "初行者",
        currentScore = level?.currentScore ?: 0,
        nextLevelTitle = level?.nextTitle ?: "识途者",
        remainingScore = ((level?.nextLevelScore ?: 100) - (level?.currentScore ?: 0)).coerceAtLeast(0),
        levelProgress = level?.progressToNext ?: 0f,
        targetTitle = target?.definition?.name ?: "点亮第一块版图",
        targetBody = target?.definition?.description ?: "从足迹页确认一个去过的地方",
        targetProgressLabel = target?.let { "${it.progressValue} / ${it.progressTarget}" } ?: "0 / 1",
        targetProgress = target?.progressPercent?.coerceIn(0f, 1f) ?: 0f,
        unlockedCount = achievement.unlockedCount,
        totalAchievementCount = achievement.totalCount,
        visitedProvinces = stats.visitedProvinces,
        totalProvinces = stats.totalProvinces.takeIf { it > 0 } ?: 34,
        visitedCities = stats.visitedCities,
        visitedDistricts = stats.visitedDistricts,
        recentUnlocks = achievement.recentUnlocked.map { it.definition.name }
    )
}

class ShanheViewModel(
    private val achievementViewModel: AchievementViewModel? = null,
    private val statsViewModel: StatsViewModel? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val fallbackUi = MutableStateFlow(ShanheUi())

    val ui: StateFlow<ShanheUi> = if (achievementViewModel != null && statsViewModel != null) {
        combine(achievementViewModel.ui, statsViewModel.stats, ::buildShanheUi)
            .stateIn(scope, SharingStarted.Eagerly, ShanheUi())
    } else {
        fallbackUi.asStateFlow()
    }

    fun refresh() {
        achievementViewModel?.refresh()
        statsViewModel?.refreshStats()
    }

    fun onCleared() {
        scope.cancel()
    }
}
