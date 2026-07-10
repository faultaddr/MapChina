package com.mapchina.ui.profile

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.domain.service.AuthService
import com.mapchina.sync.RemoteSyncClient
import com.mapchina.sync.SyncDelta
import com.mapchina.sync.SyncEngine
import com.mapchina.sync.SyncQueueItem
import com.mapchina.sync.SyncStatus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProfileViewModelTest {

    private lateinit var authService: AuthService
    private lateinit var database: MapChinaDatabase

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        authService = AuthService()
    }

    @Test
    fun initialProfile_notLoggedIn() {
        val vm = ProfileViewModel(authService)
        assertEquals("未登录", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
        assertEquals(SyncStatus.IDLE, vm.profile.value.syncStatus)
    }

    @Test
    fun loadProfile_notLoggedIn_showsDefault() {
        val vm = ProfileViewModel(authService)
        vm.loadProfile()
        assertEquals("未登录", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
    }

    @Test
    fun logout_clearsState() {
        val vm = ProfileViewModel(authService)
        vm.loadProfile()
        vm.logout()
        assertEquals("未登录", vm.profile.value.nickname)
        assertFalse(vm.isLoggedIn.value)
    }

    @Test
    fun loadProfile_readsPendingSyncCount() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "u1:510000", "UPSERT", "{}", 1_000L)
        val vm = ProfileViewModel(authService, database = database)
        vm.loadProfile()
        assertEquals(1L, vm.profile.value.pendingSyncCount)
    }

    @Test
    fun syncEngineStatus_updatesProfile() = runTest {
        val syncEngine = SyncEngine(
            apiClient = object : RemoteSyncClient {
                override suspend fun pushChanges(items: List<SyncQueueItem>) = true

                override suspend fun pullDelta(sinceTimestamp: Long) = SyncDelta(timestamp = 2_000L)
            },
            database = database
        )
        val vm = ProfileViewModel(
            authService = authService,
            database = database,
            syncEngine = syncEngine,
            dispatcher = UnconfinedTestDispatcher()
        )

        syncEngine.pullChanges(0L)

        assertEquals(SyncStatus.SYNCED, vm.profile.value.syncStatus)
    }
}
