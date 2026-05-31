package com.mapchina.domain.model

data class UserLevelInfo(
    val userId: String,
    val currentScore: Int,
    val currentLevel: Int,
    val currentTitle: String,
    val nextLevelScore: Int,
    val nextTitle: String
) {
    val progressToNext: Float get() {
        val prevThreshold = LEVEL_THRESHOLDS.getOrElse(currentLevel - 1) { 0 }
        if (nextLevelScore <= prevThreshold) return 1f
        return ((currentScore - prevThreshold).toFloat() / (nextLevelScore - prevThreshold)).coerceIn(0f, 1f)
    }

    val isMaxLevel: Boolean get() = currentLevel >= LEVEL_DEFINITIONS.last().level
}

data class LevelDef(val level: Int, val title: String, val scoreThreshold: Int)

val LEVEL_DEFINITIONS = listOf(
    LevelDef(1, "初行者", 0),
    LevelDef(2, "识途者", 100),
    LevelDef(3, "远游者", 300),
    LevelDef(4, "山河行者", 800),
    LevelDef(5, "九州旅人", 1500),
    LevelDef(6, "华境探索家", 3000),
    LevelDef(7, "山海征途者", 6000),
    LevelDef(8, "大地巡礼者", 10000),
    LevelDef(9, "中华丈量师", 16000),
    LevelDef(10, "MapChina 宗师", 25000)
)

val LEVEL_THRESHOLDS = LEVEL_DEFINITIONS.associate { it.level to it.scoreThreshold }

fun resolveLevel(score: Int): LevelDef =
    LEVEL_DEFINITIONS.last { score >= it.scoreThreshold }

fun resolveNextLevel(currentLevel: Int): LevelDef? =
    LEVEL_DEFINITIONS.find { it.level == currentLevel + 1 }
