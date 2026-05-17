package com.mapchina.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.*

object JwtConfig {

    fun configure(application: Application): JwtProvider {
        val secret = application.environment.config.propertyOrNull("jwt.secret")?.getString()
            ?: "mapchina-dev-secret-change-in-production"
        val issuer = application.environment.config.property("jwt.issuer").getString()
        val audience = application.environment.config.property("jwt.audience").getString()
        val accessTokenTtlMinutes = application.environment.config.property("jwt.accessTokenTtlMinutes").getString().toLong()
        val refreshTokenTtlDays = application.environment.config.property("jwt.refreshTokenTtlDays").getString().toLong()

        return JwtProvider(
            secret = secret,
            issuer = issuer,
            audience = audience,
            accessTokenTtlMs = accessTokenTtlMinutes * 60_000,
            refreshTokenTtlMs = refreshTokenTtlDays * 86_400_000,
            algorithm = Algorithm.HMAC256(secret)
        )
    }
}

class JwtProvider(
    private val secret: String,
    val issuer: String,
    private val audience: String,
    private val accessTokenTtlMs: Long,
    private val refreshTokenTtlMs: Long,
    val algorithm: Algorithm
) {
    val realm = "MapChina"

    fun createAccessToken(userId: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("type", "access")
        .withExpiresAt(Date(System.currentTimeMillis() + accessTokenTtlMs))
        .sign(algorithm)

    fun createRefreshToken(userId: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("type", "refresh")
        .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenTtlMs))
        .sign(algorithm)
}
