package com.mapchina.ui.profile

import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.UserLevelInfo
import com.mapchina.domain.service.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUi(
    val nickname: String,
    val phone: String?,
    val avatar: String?,
    val levelInfo: UserLevelInfo? = null,
    val badgeCount: Int = 0
)

class ProfileViewModel(
    private val authService: AuthService,
    private val userScoreRepository: UserScoreRepository
) {
    private val _profile = MutableStateFlow(ProfileUi("", null, null))
    val profile: StateFlow<ProfileUi> = _profile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

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
        _profile.value = ProfileUi("未登录", null, null)
        _isLoggedIn.value = false
    }
}
