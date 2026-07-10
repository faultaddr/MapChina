package com.mapchina.ui.profile

import app.cash.sqldelight.Query
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.domain.service.AuthService
import com.mapchina.sync.SyncEngine
import com.mapchina.sync.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUi(
    val nickname: String,
    val phone: String?,
    val avatar: String?,
    val pendingSyncCount: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

class ProfileViewModel(
    private val authService: AuthService,
    val settingsRepository: SettingsRepository? = null,
    private val database: MapChinaDatabase? = null,
    syncEngine: SyncEngine? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val syncStatus = syncEngine?.status ?: MutableStateFlow(SyncStatus.IDLE)
    private val pendingCountQuery = database?.syncQueueQueries?.countPending()

    private val _profile = MutableStateFlow(ProfileUi("未登录", null, null))
    val profile: StateFlow<ProfileUi> = _profile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val pendingCountListener = Query.Listener {
        _profile.value = _profile.value.copy(pendingSyncCount = pendingSyncCount())
    }

    init {
        pendingCountQuery?.addListener(pendingCountListener)
        vmScope.launch {
            combine(authService.currentUserFlow, syncStatus) { user, status ->
                val loggedIn = user != null
                loggedIn to ProfileUi(
                    nickname = user?.nickname ?: "未登录",
                    phone = user?.phone,
                    avatar = user?.avatar,
                    pendingSyncCount = pendingSyncCount(),
                    syncStatus = status
                )
            }.collect { (loggedIn, profile) ->
                _isLoggedIn.value = loggedIn
                _profile.value = profile
            }
        }
    }

    fun loadProfile() {
        val user = authService.getCurrentUser()
        _isLoggedIn.value = user != null
        _profile.value = ProfileUi(
            nickname = user?.nickname ?: "未登录",
            phone = user?.phone,
            avatar = user?.avatar,
            pendingSyncCount = pendingSyncCount(),
            syncStatus = syncStatus.value
        )
    }

    fun logout() {
        authService.onLogout()
    }

    fun onCleared() {
        pendingCountQuery?.removeListener(pendingCountListener)
        vmScope.cancel()
    }

    private fun pendingSyncCount(): Long =
        pendingCountQuery?.executeAsOne() ?: 0L
}
