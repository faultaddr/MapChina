package com.mapchina.server.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import com.auth0.jwt.JWT

fun Application.configureSecurity(jwtProvider: JwtProvider) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtProvider.realm
            verifier(JWT.require(jwtProvider.algorithm).withIssuer(jwtProvider.issuer).build())
            validate { credential ->
                val type = credential.payload.getClaim("type")?.asString()
                if (type == "access" && credential.payload.getClaim("userId")?.asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respondText("Token is expired or invalid", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}
