package com.mapchina.data.remote

import com.mapchina.sync.SyncEntityType
import com.mapchina.sync.SyncOperation
import com.mapchina.sync.SyncQueueItem
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MapChinaApiClientTest {

    @Test
    fun pushChanges_serializesSyncRequestAndDecodesResponse() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"accepted":1,"serverTime":1234}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val apiClient = MapChinaApiClient("http://127.0.0.1:8080", createMapChinaHttpClient(engine))

        val pushed = apiClient.pushChanges(
            listOf(
                SyncQueueItem(
                    entityType = SyncEntityType.CARVING,
                    entityId = "carving-1",
                    operation = SyncOperation.UPSERT,
                    payload = """{"text":"山河"}""",
                    updatedAt = 1_234L
                )
            )
        )

        assertTrue(pushed)
    }
}
