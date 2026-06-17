package com.mapchina.server.routes

import com.mapchina.server.database.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*

@Serializable
data class PublicAttractionResponse(
    val id: String,
    val name: String,
    val regionId: String,
    val level: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val visitCount: Long = 0
)

@Serializable
data class PublicRegionResponse(
    val id: String,
    val name: String,
    val level: String,
    val parentId: String? = null
)

@Serializable
data class PublicCommunityPostResponse(
    val id: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val title: String,
    val content: String,
    val coverImage: String? = null,
    val regionId: String? = null,
    val attractionId: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: Long
)

@Serializable
data class PaginatedPublicResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val limit: Int
)

fun Route.publicRoutes() {
    route("/public") {
        route("/attractions") {
            get {
                val page = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                val regionId = call.parameters["regionId"]
                val sort = call.parameters["sort"] ?: "popular"
                val offset = ((page - 1) * limit).toLong()

                val result = dbQuery {
                    // Count visits per attraction
                    val visitCounts = AttractionVisits
                        .select(AttractionVisits.attractionId, AttractionVisits.attractionId.count())
                        .groupBy(AttractionVisits.attractionId)
                        .associate { it[AttractionVisits.attractionId] to it[AttractionVisits.attractionId.count()] }

                    // Query attractions
                    val query = Attractions.selectAll()
                    if (regionId != null) {
                        query.andWhere { Attractions.regionId eq regionId }
                    }

                    val totalCount = query.count()

                    val items = when (sort) {
                        "popular" -> {
                            val all = query.map { it.toPublicAttractionResponse(visitCounts) }
                            all.sortedByDescending { it.visitCount }
                        }
                        "name" -> {
                            query.orderBy(Attractions.name, SortOrder.ASC)
                                .map { it.toPublicAttractionResponse(visitCounts) }
                        }
                        else -> {
                            val all = query.map { it.toPublicAttractionResponse(visitCounts) }
                            all.sortedByDescending { it.visitCount }
                        }
                    }.drop(offset.toInt()).take(limit)

                    PaginatedPublicResponse(
                        data = items,
                        total = totalCount,
                        page = page,
                        limit = limit
                    )
                }
                call.respond(result)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"景点ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val result = dbQuery {
                    val row = Attractions.selectAll().where { Attractions.id eq id }.singleOrNull()
                        ?: return@dbQuery null

                    val visitCount = AttractionVisits
                        .select(AttractionVisits.attractionId.count())
                        .where { AttractionVisits.attractionId eq id }
                        .singleOrNull()
                        ?.get(AttractionVisits.attractionId.count()) ?: 0L

                    row.toPublicAttractionResponse(mapOf(id to visitCount))
                }
                if (result != null) call.respond(result)
                else call.respondText(
                    """{"code":"NOT_FOUND","message":"景点不存在"}""",
                    status = HttpStatusCode.NotFound
                )
            }
        }

        route("/regions") {
            get {
                val level = call.parameters["level"]
                val parentId = call.parameters["parentId"]
                val page = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                val offset = ((page - 1) * limit).toLong()

                val result = dbQuery {
                    val query = Regions.selectAll()
                    if (parentId != null) {
                        query.andWhere { Regions.parentId eq parentId }
                    } else if (level != null) {
                        query.andWhere { Regions.level eq level }
                    }

                    val totalCount = query.count()
                    val items = query.orderBy(Regions.name, SortOrder.ASC)
                        .limit(limit, offset)
                        .map { it.toPublicRegionResponse() }

                    PaginatedPublicResponse(
                        data = items,
                        total = totalCount,
                        page = page,
                        limit = limit
                    )
                }
                call.respond(result)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val result = dbQuery {
                    Regions.selectAll().where { Regions.id eq id }.singleOrNull()?.toPublicRegionResponse()
                }
                if (result != null) call.respond(result)
                else call.respondText(
                    """{"code":"NOT_FOUND","message":"区域不存在"}""",
                    status = HttpStatusCode.NotFound
                )
            }
        }

        route("/community") {
            get("/feed") {
                val page = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                val offset = ((page - 1) * limit).toLong()

                val result = dbQuery {
                    val totalCount = CommunityPosts.selectAll().count()
                    val items = CommunityPosts.selectAll()
                        .orderBy(CommunityPosts.createdAt, SortOrder.DESC)
                        .limit(limit, offset)
                        .map { it.toPublicPostResponse() }

                    PaginatedPublicResponse(
                        data = items,
                        total = totalCount,
                        page = page,
                        limit = limit
                    )
                }
                call.respond(result)
            }

            get("/posts/{id}") {
                val postId = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val result = dbQuery {
                    CommunityPosts.selectAll().where { CommunityPosts.id eq postId }
                        .singleOrNull()?.toPublicPostResponse()
                }
                if (result != null) call.respond(result)
                else call.respondText(
                    """{"code":"NOT_FOUND","message":"帖子不存在"}""",
                    status = HttpStatusCode.NotFound
                )
            }
        }
    }
}

private fun ResultRow.toPublicAttractionResponse(visitCounts: Map<String, Long>): PublicAttractionResponse {
    val attractionId = this[Attractions.id].value
    return PublicAttractionResponse(
        id = attractionId,
        name = this[Attractions.name],
        regionId = this[Attractions.regionId],
        level = this[Attractions.level],
        latitude = this[Attractions.latitude],
        longitude = this[Attractions.longitude],
        description = this[Attractions.description],
        visitCount = visitCounts[attractionId] ?: 0L
    )
}

private fun ResultRow.toPublicRegionResponse() = PublicRegionResponse(
    id = this[Regions.id].value,
    name = this[Regions.name],
    level = this[Regions.level],
    parentId = this[Regions.parentId]
)

private suspend fun ResultRow.toPublicPostResponse(): PublicCommunityPostResponse {
    val postUserId = this[CommunityPosts.userId]
    val userRow = dbQuery {
        Users.selectAll().where { Users.id eq postUserId }.singleOrNull()
    }
    val nickname = userRow?.get(Users.nickname) ?: "匿名用户"
    val avatarUrl = userRow?.get(Users.avatar)
    return PublicCommunityPostResponse(
        id = this[CommunityPosts.id].value,
        nickname = nickname,
        avatarUrl = avatarUrl,
        title = this[CommunityPosts.title],
        content = this[CommunityPosts.content],
        coverImage = this[CommunityPosts.coverImage],
        regionId = this[CommunityPosts.regionId],
        attractionId = this[CommunityPosts.attractionId],
        likeCount = this[CommunityPosts.likeCount],
        commentCount = this[CommunityPosts.commentCount],
        createdAt = this[CommunityPosts.createdAt]
    )
}
