package com.mapchina.server.auth

import com.mapchina.server.database.RefreshTokenBlacklist
import com.mapchina.server.database.dbQuery
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

@Serializable
data class LogoutRequest(val refreshToken: String)

fun Route.logoutRoutes() {
    authenticate("auth-jwt") {
        post("/auth/logout") {
            val request = call.receiveNullable<LogoutRequest>()

            if (request?.refreshToken != null) {
                blacklistToken(request.refreshToken)
            }

            call.respondText("""{"code":"SUCCESS","message":"已登出"}""")
        }
    }
}

internal suspend fun blacklistToken(token: String) {
    val decoded = try {
        com.auth0.jwt.JWT.decode(token)
    } catch (_: Exception) {
        return
    }

    val expiresAt = decoded.expiresAt?.time ?: return
    val type = decoded.getClaim("type")?.asString()
    if (type != "refresh") return

    dbQuery {
        RefreshTokenBlacklist.insert {
            it[RefreshTokenBlacklist.token] = token
            it[RefreshTokenBlacklist.expiresAt] = expiresAt
        }
    }
}

fun Application.startBlacklistCleanup() {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            delay(3_600_000)
            try {
                val now = System.currentTimeMillis()
                dbQuery {
                    RefreshTokenBlacklist.deleteWhere {
                        RefreshTokenBlacklist.expiresAt lessEq now
                    }
                }
            } catch (_: Exception) {
                // log and continue
            }
        }
    }
}
