package com.mapchina.domain.service

import com.mapchina.data.model.UserDto

class AuthService {
    private var currentUser: UserDto? = null

    fun isLoggedIn(): Boolean = currentUser != null

    fun getCurrentUser(): UserDto? = currentUser

    fun onLogin(user: UserDto) {
        currentUser = user
    }

    fun onLogout() {
        currentUser = null
    }
}
