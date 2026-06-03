package com.mapchina.server.routes

import com.mapchina.server.auth.JwtProvider
import com.mapchina.server.database.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus

@Serializable
data class CommunityPostResponse(
    val id: String,
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val title: String,
    val content: String,
    val coverImage: String? = null,
    val regionId: String? = null,
    val attractionId: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: Long,
    val likedByMe: Boolean = false
)

@Serializable
data class CreatePostRequest(
    val title: String,
    val content: String,
    val coverImage: String? = null,
    val regionId: String? = null,
    val attractionId: String? = null
)

@Serializable
data class CommentResponse(
    val id: Long,
    val postId: String,
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val content: String,
    val createdAt: Long
)

@Serializable
data class CreateCommentRequest(val content: String)

fun Route.communityRoutes(jwtProvider: JwtProvider) {
    authenticate("auth-jwt") {
        route("/community") {
            get("/feed") {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()

                val posts = dbQuery {
                    CommunityPosts.selectAll()
                        .orderBy(CommunityPosts.createdAt, SortOrder.DESC)
                        .limit(size, offset = ((page - 1) * size).toLong())
                        .map { it.toPostResponse(userId) }
                }
                call.respond(posts)
            }

            get("/posts/{id}") {
                val postId = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val post = dbQuery {
                    CommunityPosts.selectAll().where { CommunityPosts.id eq postId }.singleOrNull()?.toPostResponse(userId)
                }
                if (post != null) call.respond(post)
                else call.respondText("""{"code":"NOT_FOUND","message":"帖子不存在"}""", status = HttpStatusCode.NotFound)
            }

            post("/posts") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<CreatePostRequest>()

                if (request.title.isBlank()) {
                    return@post call.respondText("""{"code":"VALIDATION_ERROR","message":"标题不能为空"}""", status = HttpStatusCode.BadRequest)
                }

                val postId = "post_${System.currentTimeMillis()}"
                dbQuery {
                    CommunityPosts.insert {
                        it[id] = postId
                        it[CommunityPosts.userId] = userId
                        it[title] = request.title
                        it[content] = request.content
                        it[coverImage] = request.coverImage
                        it[regionId] = request.regionId
                        it[attractionId] = request.attractionId
                        it[createdAt] = System.currentTimeMillis()
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"发布成功","id":"$postId"}""")
            }

            delete("/posts/{id}") {
                val postId = call.parameters["id"] ?: return@delete call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()

                val isOwner = dbQuery {
                    CommunityPosts.selectAll().where { (CommunityPosts.id eq postId) and (CommunityPosts.userId eq userId) }.singleOrNull()
                }
                if (isOwner == null) {
                    return@delete call.respondText("""{"code":"FORBIDDEN","message":"无权删除"}""", status = HttpStatusCode.Forbidden)
                }
                dbQuery {
                    CommunityPosts.deleteWhere { CommunityPosts.id eq postId }
                }
                call.respondText("""{"code":"SUCCESS","message":"已删除"}""")
            }

            post("/posts/{id}/like") {
                val postId = call.parameters["id"] ?: return@post call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()

                dbQuery {
                    val existing = PostLikes.selectAll().where {
                        (PostLikes.userId eq userId) and (PostLikes.postId eq postId)
                    }.singleOrNull()

                    if (existing != null) {
                        PostLikes.deleteWhere { (PostLikes.userId eq userId) and (PostLikes.postId eq postId) }
                        CommunityPosts.update({ CommunityPosts.id eq postId }) {
                            it[likeCount] = likeCount - 1
                        }
                    } else {
                        PostLikes.insert {
                            it[PostLikes.userId] = userId
                            it[PostLikes.postId] = postId
                            it[createdAt] = System.currentTimeMillis()
                        }
                        CommunityPosts.update({ CommunityPosts.id eq postId }) {
                            it[likeCount] = likeCount + 1
                        }
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"操作成功"}""")
            }

            get("/posts/{id}/comments") {
                val postId = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val comments = dbQuery {
                    PostComments.selectAll().where { PostComments.postId eq postId }
                        .orderBy(PostComments.createdAt, SortOrder.ASC)
                        .map { it.toCommentResponse() }
                }
                call.respond(comments)
            }

            post("/posts/{id}/comments") {
                val postId = call.parameters["id"] ?: return@post call.respondText(
                    """{"code":"MISSING_ID","message":"帖子ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<CreateCommentRequest>()

                if (request.content.isBlank()) {
                    return@post call.respondText("""{"code":"VALIDATION_ERROR","message":"评论不能为空"}""", status = HttpStatusCode.BadRequest)
                }

                dbQuery {
                    PostComments.insert {
                        it[PostComments.postId] = postId
                        it[PostComments.userId] = userId
                        it[content] = request.content
                        it[createdAt] = System.currentTimeMillis()
                    }
                    CommunityPosts.update({ CommunityPosts.id eq postId }) {
                        it[commentCount] = commentCount + 1
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"评论成功"}""")
            }
        }
    }
}

private suspend fun ResultRow.toPostResponse(currentUserId: String): CommunityPostResponse {
    val postUserId = this[CommunityPosts.userId]
    val userRow = dbQuery {
        Users.selectAll().where { Users.id eq postUserId }.singleOrNull()
    }
    val nickname = userRow?.get(Users.nickname) ?: "匿名用户"
    val avatarUrl = userRow?.get(Users.avatar)
    val postIdValue = this[CommunityPosts.id].value
    val likedByMe = dbQuery {
        PostLikes.selectAll().where {
            (PostLikes.userId eq currentUserId) and (PostLikes.postId eq postIdValue)
        }.singleOrNull() != null
    }
    return CommunityPostResponse(
        id = postIdValue,
        userId = postUserId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        title = this[CommunityPosts.title],
        content = this[CommunityPosts.content],
        coverImage = this[CommunityPosts.coverImage],
        regionId = this[CommunityPosts.regionId],
        attractionId = this[CommunityPosts.attractionId],
        likeCount = this[CommunityPosts.likeCount],
        commentCount = this[CommunityPosts.commentCount],
        createdAt = this[CommunityPosts.createdAt],
        likedByMe = likedByMe
    )
}

private suspend fun ResultRow.toCommentResponse(): CommentResponse {
    val commentUserId = this[PostComments.userId]
    val userRow = dbQuery {
        Users.selectAll().where { Users.id eq commentUserId }.singleOrNull()
    }
    val nickname = userRow?.get(Users.nickname) ?: "匿名用户"
    val avatarUrl = userRow?.get(Users.avatar)
    return CommentResponse(
        id = this[PostComments.id].value,
        postId = this[PostComments.postId],
        userId = commentUserId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        content = this[PostComments.content],
        createdAt = this[PostComments.createdAt]
    )
}
