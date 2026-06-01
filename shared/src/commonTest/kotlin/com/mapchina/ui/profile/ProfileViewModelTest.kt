package com.mapchina.ui.profile

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.service.AuthService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProfileViewModelTest {

    private lateinit var authService: AuthService
    private lateinit var userScoreRepo: UserScoreRepository

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        authService = AuthService()
        userScoreRepo = UserScoreRepository(database)
    }

    @Test
    fun initialProfile_notLoggedIn() {
        val vm = ProfileViewModel(authService, userScoreRepo)
        assertEquals("", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
    }

    @Test
    fun loadProfile_notLoggedIn_showsDefault() {
        val vm = ProfileViewModel(authService, userScoreRepo)
        vm.loadProfile()
        assertEquals("未登录", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
    }

    @Test
    fun logout_clearsState() {
        val vm = ProfileViewModel(authService, userScoreRepo)
        vm.loadProfile()
        vm.logout()
        assertEquals("未登录", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
    }
}
