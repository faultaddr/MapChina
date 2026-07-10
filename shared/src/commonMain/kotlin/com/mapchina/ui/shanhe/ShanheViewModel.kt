package com.mapchina.ui.shanhe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class ShanheViewModel {
    private val _ui = MutableStateFlow(ShanheUi())
    val ui: StateFlow<ShanheUi> = _ui.asStateFlow()
}
