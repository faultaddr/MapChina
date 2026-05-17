package com.mapchina.domain.service

import com.mapchina.data.model.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {

    @Test
    fun `initially_not_logged_in`() {
        val service = AuthService()
        assertFalse(service.isLoggedIn())
        assertNull(service.getCurrentUser())
    }

    @Test
    fun `on_login_sets_current_user`() {
        val service = AuthService()
        val user = UserDto("user1", "13800001111", "旅行者1111", null, System.currentTimeMillis())
        service.onLogin(user)
        assertTrue(service.isLoggedIn())
        assertEquals("user1", service.getCurrentUser()?.id)
    }

    @Test
    fun `on_logout_clears_current_user`() {
        val service = AuthService()
        service.onLogin(UserDto("user1", "13800001111", "旅行者1111", null, 0L))
        service.onLogout()
        assertFalse(service.isLoggedIn())
        assertNull(service.getCurrentUser())
    }

    @Test
    fun `re_login_replaces_current_user`() {
        val service = AuthService()
        service.onLogin(UserDto("user1", "13800001111", "旅行者1111", null, 0L))
        service.onLogin(UserDto("user2", "13800002222", "旅行者2222", null, 0L))
        assertEquals("user2", service.getCurrentUser()?.id)
    }
}
