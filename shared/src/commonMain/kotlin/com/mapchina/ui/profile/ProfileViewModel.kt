package com.mapchina.ui.profile

import com.mapchina.data.repository.SettingsRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.UserLevelInfo
import com.mapchina.domain.service.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUi(
    val nickname: String,
    val phone: String?,
    val avatar: String?,
    val levelInfo: UserLevelInfo? = null,
    val badgeCount: Int = 0
)

class ProfileViewModel(
    private val authService: AuthService,
    private val userScoreRepository: UserScoreRepository,
    val settingsRepository: SettingsRepository? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _profile = MutableStateFlow(ProfileUi("", null, null))
    val profile: StateFlow<ProfileUi> = _profile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        vmScope.launch {
            authService.currentUserFlow.collect { user ->
                _isLoggedIn.value = user != null
                val levelInfo = user?.id?.let { userScoreRepository.getScore(it) }
                _profile.value = ProfileUi(
                    nickname = user?.nickname ?: "未登录",
                    phone = user?.phone,
                    avatar = user?.avatar,
                    levelInfo = levelInfo
                )
            }
        }
    }

    fun loadProfile() {
        val user = authService.getCurrentUser()
        _isLoggedIn.value = user != null
        val levelInfo = user?.id?.let { userScoreRepository.getScore(it) }
        _profile.value = ProfileUi(
            nickname = user?.nickname ?: "未登录",
            phone = user?.phone,
            avatar = user?.avatar,
            levelInfo = levelInfo
        )
    }

    fun logout() {
        authService.onLogout()
    }

    fun onCleared() {
        vmScope.cancel()
    }
}
