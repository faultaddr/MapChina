package com.mapchina.server.routes

import com.mapchina.server.auth.JwtProvider
import com.mapchina.server.auth.blacklistToken
import com.mapchina.server.database.AttractionVisits
import com.mapchina.server.database.Footprints
import com.mapchina.server.database.RefreshTokenBlacklist
import com.mapchina.server.database.Users
import com.mapchina.server.database.dbQuery
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

@Serializable
data class SendCodeRequest(val phone: String)

@Serializable
data class LoginRequest(val phone: String, val code: String)

@Serializable
data class LoginResponse(val accessToken: String, val refreshToken: String, val userId: String, val nickname: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class RefreshResponse(val accessToken: String, val refreshToken: String)

fun Route.authRoutes(jwtProvider: JwtProvider) {
    route("/auth") {
        post("/send-code") {
            val request = call.receive<SendCodeRequest>()
            if (request.phone.isBlank()) {
                call.respondText("""{"code":"INVALID_PHONE","message":"手机号不能为空"}""", status = HttpStatusCode.BadRequest)
                return@post
            }
            // V1: 固定验证码 123456，生产环境对接短信服务
            call.respondText("""{"code":"SUCCESS","message":"验证码已发送"}""")
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            if (request.code != "123456") {
                call.respondText("""{"code":"INVALID_CODE","message":"验证码错误"}""", status = HttpStatusCode.Unauthorized)
                return@post
            }

            val existingUser = dbQuery {
                Users.selectAll().where { Users.phone eq request.phone }.singleOrNull()
            }

            val userId = if (existingUser != null) {
                existingUser[Users.id].value
            } else {
                val newId = UUID.randomUUID().toString()
                dbQuery {
                    Users.insert {
                        it[id] = newId
                        it[phone] = request.phone
                        it[nickname] = "旅行者${request.phone.takeLast(4)}"
                        it[createdAt] = System.currentTimeMillis()
                    }
                }
                newId
            }

            val accessToken = jwtProvider.createAccessToken(userId)
            val refreshToken = jwtProvider.createRefreshToken(userId)

            call.respond(LoginResponse(accessToken, refreshToken, userId, existingUser?.get(Users.nickname) ?: "旅行者${request.phone.takeLast(4)}"))
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            // V1: 简单验证 refresh token，生产环境应验证 JWT 并检查黑名单
            try {
                val decoded = com.auth0.jwt.JWT.decode(request.refreshToken)
                val type = decoded.getClaim("type")?.asString()
                val userId = decoded.getClaim("userId")?.asString()

                if (type != "refresh" || userId == null) {
                    call.respondText("""{"code":"INVALID_TOKEN","message":"无效的刷新令牌"}""", status = HttpStatusCode.Unauthorized)
                    return@post
                }

                val isBlacklisted = dbQuery {
                    RefreshTokenBlacklist.selectAll().where { RefreshTokenBlacklist.token eq request.refreshToken }.singleOrNull()
                }

                if (isBlacklisted != null) {
                    call.respondText("""{"code":"TOKEN_REVOKED","message":"令牌已撤销"}""", status = HttpStatusCode.Unauthorized)
                    return@post
                }

                // 将旧 refresh token 加入黑名单
                blacklistToken(request.refreshToken)

                val newAccessToken = jwtProvider.createAccessToken(userId)
                val newRefreshToken = jwtProvider.createRefreshToken(userId)

                call.respond(RefreshResponse(newAccessToken, newRefreshToken))
            } catch (e: Exception) {
                call.respondText("""{"code":"INVALID_TOKEN","message":"令牌解析失败"}""", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}
