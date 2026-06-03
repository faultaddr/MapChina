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

    suspend fun getCommunityFeed(page: Int = 1, size: Int = 20): List<CommunityPostDto> {
        return try {
            val response: List<CommunityPostDto> = client.get("$baseUrl/community/feed") {
                accessToken?.let { bearerAuth(it) }
                header("page", page.toString())
                header("size", size.toString())
            }.body()
            response
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getCommunityPost(postId: String): CommunityPostDto? {
        return try {
            client.get("$baseUrl/community/posts/$postId") {
                accessToken?.let { bearerAuth(it) }
            }.body()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createCommunityPost(title: String, content: String, coverImage: String? = null, regionId: String? = null, attractionId: String? = null): Boolean {
        return try {
            client.post("$baseUrl/community/posts") {
                contentType(ContentType.Application.Json)
                accessToken?.let { bearerAuth(it) }
                setBody(mapOf("title" to title, "content" to content, "coverImage" to coverImage, "regionId" to regionId, "attractionId" to attractionId))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun likePost(postId: String): Boolean {
        return try {
            client.post("$baseUrl/community/posts/$postId/like") {
                accessToken?.let { bearerAuth(it) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getComments(postId: String): List<CommentDto> {
        return try {
            client.get("$baseUrl/community/posts/$postId/comments") {
                accessToken?.let { bearerAuth(it) }
            }.body()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createComment(postId: String, content: String): Boolean {
        return try {
            client.post("$baseUrl/community/posts/$postId/comments") {
                contentType(ContentType.Application.Json)
                accessToken?.let { bearerAuth(it) }
                setBody(mapOf("content" to content))
            }
            true
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

@kotlinx.serialization.Serializable
data class CommunityPostDto(
    val id: String,
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val title: String,
    val content: String,
    val coverImage: String? = null,
    val regionId: String? = null,
    val attractionId: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = 0L,
    val likedByMe: Boolean = false
)

@kotlinx.serialization.Serializable
data class CommentDto(
    val id: Long,
    val postId: String,
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val content: String,
    val createdAt: Long = 0L
)
