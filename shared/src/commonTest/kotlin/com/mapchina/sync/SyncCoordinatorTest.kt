package com.mapchina.sync

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.model.UserDto
import com.mapchina.data.remote.MapChinaApiClient
import com.mapchina.data.remote.createMapChinaHttpClient
import com.mapchina.domain.service.AuthService
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncCoordinatorTest {

    @Test
    fun syncOnLogin_pushesPendingQueueAndStoresPullTimestamp() = runTest {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.syncQueueQueries.insertPending(
            "FOOTPRINT",
            "u1:510000",
            "UPSERT",
            """{"userId":"u1","regionId":"510000","level":"DEEP","timestamp":1000}""",
            1_000L
        )
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/sync/push" -> respond(
                    content = """{"accepted":1,"serverTime":2000}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/sync/pull" -> respond(
                    content = """{"items":[],"serverTime":5000}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request ${request.url}")
            }
        }
        val apiClient = MapChinaApiClient("http://127.0.0.1:8080", createMapChinaHttpClient(engine))
        val authService = AuthService()
        authService.onLogin(
            UserDto("u1", "13800000000", "云同步用户", null, 1_000L),
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )
        val coordinator = SyncCoordinator(authService, apiClient, SyncEngine(apiClient, database), database)

        val synced = coordinator.syncOnLogin()

        assertTrue(synced)
        assertEquals("access-token", apiClient.accessToken)
        assertEquals(0L, database.syncQueueQueries.countPending().executeAsOne())
        assertEquals(
            "5000",
            database.appSettingQueries.selectByKey(SyncCoordinator.LAST_PULL_TIMESTAMP_KEY).executeAsOne().value_
        )
    }

    @Test
    fun syncAfterLocalChange_withoutAccessTokenLeavesQueuePendingAndSkipsNetwork() = runTest {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        database.syncQueueQueries.insertPending("CARVING", "carving-1", "UPSERT", "{}", 1_000L)
        var requestCount = 0
        val engine = MockEngine {
            requestCount += 1
            respond(
                content = """{"accepted":1,"serverTime":2000}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val apiClient = MapChinaApiClient("http://127.0.0.1:8080", createMapChinaHttpClient(engine))
        val coordinator = SyncCoordinator(AuthService(), apiClient, SyncEngine(apiClient, database), database)

        val synced = coordinator.syncAfterLocalChange()

        assertFalse(synced)
        assertEquals(1L, database.syncQueueQueries.countPending().executeAsOne())
        assertEquals(0, requestCount)
    }
}
