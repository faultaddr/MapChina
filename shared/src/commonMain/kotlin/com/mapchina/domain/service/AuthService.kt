package com.mapchina.domain.service

import com.mapchina.data.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class AuthService {
    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUserFlow: StateFlow<UserDto?> = _currentUser.asStateFlow()

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun getCurrentUser(): UserDto? = _currentUser.value

    fun onLogin(user: UserDto) {
        _currentUser.value = user
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
    }

    fun onLogout() {
        _currentUser.value = null
    }
}
