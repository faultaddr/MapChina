package com.mapchina.ui.achievement

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.ProvinceConquestInfo
import com.mapchina.domain.service.AchievementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

data class ProvinceConquestUi(
    val provinces: List<ProvinceConquestInfo> = emptyList(),
    val visitedProvinceCount: Int = 0,
    val totalProvinceCount: Int = 0,
    val completedProvinceCount: Int = 0,
    val isLoading: Boolean = false
)

data class ProvinceDetailUi(
    val info: ProvinceConquestInfo? = null,
    val provinceAchievements: List<AchievementWithProgress> = emptyList()
)

class ProvinceConquestViewModel(
    private val achievementService: AchievementService,
    private val achievementRepository: AchievementRepository,
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _ui = MutableStateFlow(ProvinceConquestUi())
    val ui: StateFlow<ProvinceConquestUi> = _ui.asStateFlow()

    private val _detailUi = MutableStateFlow(ProvinceDetailUi())
    val detailUi: StateFlow<ProvinceDetailUi> = _detailUi.asStateFlow()

    fun refresh() {
        _ui.value = _ui.value.copy(isLoading = true)
        vmScope.launch {
            val provinces = achievementService.getProvinceConquestInfo(userId)
            val visited = provinces.count { it.visitedCities > 0 || it.visitedAttractions > 0 }
            val completed = provinces.count { it.hasCompleteBadge }
            _ui.value = ProvinceConquestUi(
                provinces = provinces.sortedByDescending { it.completionPercent },
                visitedProvinceCount = visited,
                totalProvinceCount = provinces.size,
                completedProvinceCount = completed,
                isLoading = false
            )
        }
    }

    fun loadProvinceDetail(provinceCode: String) {
        vmScope.launch {
            val provinces = achievementService.getProvinceConquestInfo(userId)
            val info = provinces.find { it.provinceId.substring(0, 2) == provinceCode }

            val allDefs = achievementRepository.getAllDefinitions()
            val userAchievements = achievementRepository.getUserAchievements(userId)
            val userMap = userAchievements.associateBy { it.achievementId }

            val provinceAchievements = allDefs
                .filter { it.id.startsWith("province_visit_$provinceCode") || it.id.startsWith("province_complete_$provinceCode") }
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

            _detailUi.value = ProvinceDetailUi(info = info, provinceAchievements = provinceAchievements)
        }
    }

    fun onCleared() {
        vmScope.cancel()
    }
}
