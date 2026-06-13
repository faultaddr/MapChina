package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.*
import kotlin.time.Clock

class UserScoreRepository(private val database: MapChinaDatabase) {

    fun getScore(userId: String): UserLevelInfo? {
        val row = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull() ?: return null
        val score = row.current_score.toInt()
        val levelDef = resolveLevel(score)
        val nextLevel = resolveNextLevel(levelDef.level)
        return UserLevelInfo(
            userId = row.user_id,
            currentScore = score,
            currentLevel = levelDef.level,
            currentTitle = levelDef.title,
            nextLevelScore = nextLevel?.scoreThreshold ?: levelDef.scoreThreshold,
            nextTitle = nextLevel?.title ?: levelDef.title
        )
    }

    fun addScore(userId: String, delta: Int) {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()
        if (existing != null) {
            val newScore = existing.current_score + delta
            val newLevel = resolveLevel(newScore.toInt()).level.toLong()
            database.userScoreQueries.upsertScore(userId, newScore, newLevel, now)
        } else {
            val level = resolveLevel(delta)
            database.userScoreQueries.upsertScore(userId, delta.toLong(), level.level.toLong(), now)
        }
    }

    fun ensureUserScore(userId: String) {
        val existing = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()
        if (existing == null) {
            val now = Clock.System.now().toEpochMilliseconds()
            database.userScoreQueries.upsertScore(userId, 0, 1, now)
        }
    }

    fun getCurrentScore(userId: String): Int {
        return database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()?.current_score?.toInt() ?: 0
    }

    fun getCurrentLevel(userId: String): Int {
        return database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()?.current_level?.toInt() ?: 1
    }
}
