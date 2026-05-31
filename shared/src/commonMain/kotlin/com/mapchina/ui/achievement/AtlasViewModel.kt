package com.mapchina.ui.achievement

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.AtlasProgress
import com.mapchina.domain.service.AchievementService
import com.mapchina.domain.service.AtlasService
import com.mapchina.domain.service.AtlasItemVisitStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AtlasUi(
    val atlasProgress: List<AtlasProgress> = emptyList(),
    val totalAtlas: Int = 0,
    val completedAtlas: Int = 0
)

data class AtlasDetailUi(
    val progress: AtlasProgress? = null,
    val items: List<AtlasItemVisitStatus> = emptyList(),
    val atlasAchievements: List<AchievementWithProgress> = emptyList()
)

class AtlasViewModel(
    private val atlasService: AtlasService,
    private val achievementService: AchievementService,
    private val achievementRepository: AchievementRepository,
    private val userId: String = ""
) {
    private val _ui = MutableStateFlow(AtlasUi())
    val ui: StateFlow<AtlasUi> = _ui.asStateFlow()

    private val _detailUi = MutableStateFlow(AtlasDetailUi())
    val detailUi: StateFlow<AtlasDetailUi> = _detailUi.asStateFlow()

    fun refresh() {
        val progress = atlasService.getAtlasProgress(userId)
        val completed = progress.count { it.isCompleted }
        _ui.value = AtlasUi(
            atlasProgress = progress.sortedByDescending { it.completionPercent },
            totalAtlas = progress.size,
            completedAtlas = completed
        )
    }

    fun loadAtlasDetail(atlasId: String) {
        val progress = atlasService.getAtlasDetail(userId, atlasId)
        val items = atlasService.getAtlasItemsWithVisitStatus(userId, atlasId)

        val allDefs = achievementRepository.getAllDefinitions()
        val userAchievements = achievementRepository.getUserAchievements(userId)
        val userMap = userAchievements.associateBy { it.achievementId }

        val atlasAchievements = allDefs
            .filter { it.category.name == "ATLAS" && it.subCategory == atlasId }
            .map { def ->
                val ua = userMap[def.id]
                AchievementWithProgress(
                    definition = def,
                    progressValue = ua?.progressValue ?: 0,
                    progressTarget = ua?.progressTarget ?: 1,
                    isUnlocked = ua?.isUnlocked == true,
                    unlockTime = ua?.unlockTime
                )
            }

        _detailUi.value = AtlasDetailUi(
            progress = progress,
            items = items,
            atlasAchievements = atlasAchievements
        )
    }
}
