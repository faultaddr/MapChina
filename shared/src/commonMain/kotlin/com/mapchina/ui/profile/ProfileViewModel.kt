package com.mapchina.ui.profile

import com.mapchina.domain.service.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUi(
    val nickname: String,
    val phone: String?,
    val avatar: String?
)

class ProfileViewModel(
    private val authService: AuthService
) {
    private val _profile = MutableStateFlow(ProfileUi("", null, null))
    val profile: StateFlow<ProfileUi> = _profile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun loadProfile() {
        val user = authService.getCurrentUser()
        _profile.value = ProfileUi(
            nickname = user?.nickname ?: "未登录",
            phone = user?.phone,
            avatar = user?.avatar
        )
        _isLoggedIn.value = user != null
    }

    fun logout() {
        authService.onLogout()
        _profile.value = ProfileUi("未登录", null, null)
        _isLoggedIn.value = false
    }
}
