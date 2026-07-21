package com.mapchina.domain.service

import com.mapchina.data.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class AuthService {
    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUserFlow: StateFlow<UserDto?> = _currentUser.asStateFlow()

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun getCurrentUser(): UserDto? = _currentUser.value

    fun onLogin(user: UserDto, accessToken: String? = null, refreshToken: String? = null) {
        _currentUser.value = user
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun quickStart(nickname: String) {
        val userId = "local_${nickname.hashCode().toUInt()}"
        _currentUser.value = UserDto(
            id = userId,
            phone = "",
            nickname = nickname,
            avatar = null,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        accessToken = null
        refreshToken = null
    }

    fun onLogout() {
        _currentUser.value = null
        accessToken = null
        refreshToken = null
    }
}
