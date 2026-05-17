package com.mapchina.data.remote

import com.mapchina.data.model.ApiResponse
import com.mapchina.data.model.AttractionDto
import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.RegionDto
import com.mapchina.data.model.UserDto
import com.mapchina.sync.RemoteSyncClient
import com.mapchina.sync.SyncDelta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MapChinaApiClient(
    private val baseUrl: String,
    private val client: HttpClient
) : RemoteSyncClient {

    var accessToken: String? = null

    suspend fun sendLoginCode(phone: String): ApiResponse<Unit> {
        return client.post("$baseUrl/auth/send-code") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("phone" to phone))
        }.body()
    }

    suspend fun login(phone: String, code: String): ApiResponse<UserDto> {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("phone" to phone, "code" to code))
        }.body()
    }

    suspend fun refreshToken(refreshToken: String): ApiResponse<UserDto> {
        return client.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }

    suspend fun getRegions(level: String? = null, parentId: String? = null): ApiResponse<List<RegionDto>> {
        return client.get("$baseUrl/regions") {
            level?.let { header("level", it) }
            parentId?.let { header("parentId", it) }
            accessToken?.let { bearerAuth(it) }
        }.body()
    }

    suspend fun getRegionDetail(regionId: String): ApiResponse<RegionDto> {
        return client.get("$baseUrl/regions/$regionId") {
            accessToken?.let { bearerAuth(it) }
        }.body()
    }

    suspend fun getAttractions(regionId: String): ApiResponse<List<AttractionDto>> {
        return client.get("$baseUrl/regions/$regionId/attractions") {
            accessToken?.let { bearerAuth(it) }
        }.body()
    }

    suspend fun getFootprints(): ApiResponse<List<FootprintDto>> {
        return client.get("$baseUrl/footprints") {
            accessToken?.let { bearerAuth(it) }
        }.body()
    }

    suspend fun markFootprint(regionId: String, level: String): ApiResponse<FootprintDto> {
        return client.post("$baseUrl/footprints") {
            contentType(ContentType.Application.Json)
            accessToken?.let { bearerAuth(it) }
            setBody(mapOf("regionId" to regionId, "level" to level))
        }.body()
    }

    override suspend fun pushChange(entityType: String, entityId: String, operation: String, payload: String): Boolean {
        return try {
            val response: ApiResponse<Unit> = client.post("$baseUrl/sync/push") {
                contentType(ContentType.Application.Json)
                accessToken?.let { bearerAuth(it) }
                setBody(mapOf("entityType" to entityType, "entityId" to entityId, "operation" to operation, "payload" to payload))
            }.body()
            response.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun pullDelta(sinceTimestamp: Long): SyncDelta {
        val response: ApiResponse<SyncDeltaResponse> = client.get("$baseUrl/sync/delta") {
            accessToken?.let { bearerAuth(it) }
            header("since", sinceTimestamp.toString())
        }.body()
        return if (response.isSuccess()) {
            val data = (response as ApiResponse.Success).data
            SyncDelta(footprints = data.footprints, timestamp = data.timestamp)
        } else {
            SyncDelta()
        }
    }
}

@kotlinx.serialization.Serializable
data class SyncDeltaResponse(
    val footprints: List<FootprintDto> = emptyList(),
    val attractionVisits: List<com.mapchina.data.model.AttractionVisitDto> = emptyList(),
    val timestamp: Long = 0L
)
