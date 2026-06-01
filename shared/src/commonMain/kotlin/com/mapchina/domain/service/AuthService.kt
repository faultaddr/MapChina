package com.mapchina.domain.service

import com.mapchina.data.model.UserDto
import kotlinx.datetime.Clock

class AuthService {
    private var currentUser: UserDto? = null

    fun isLoggedIn(): Boolean = currentUser != null

    fun getCurrentUser(): UserDto? = currentUser

    fun onLogin(user: UserDto) {
        currentUser = user
    }

    fun quickStart(nickname: String) {
        val userId = "local_${nickname.hashCode().toUInt()}"
        currentUser = UserDto(
            id = userId,
            phone = "",
            nickname = nickname,
            avatar = null,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun onLogout() {
        currentUser = null
    }
}
