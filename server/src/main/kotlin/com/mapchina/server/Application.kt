package com.mapchina.server

import com.mapchina.server.auth.JwtConfig
import com.mapchina.server.auth.configureSecurity
import com.mapchina.server.auth.installRateLimiting
import com.mapchina.server.auth.logoutRoutes
import com.mapchina.server.auth.startBlacklistCleanup
import com.mapchina.server.database.configureDatabase
import com.mapchina.server.routes.authRoutes
import com.mapchina.server.routes.dataRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = """{"code":"INTERNAL_ERROR","message":"${cause.localizedMessage}"}""", status = HttpStatusCode.InternalServerError)
        }
    }

    installRateLimiting()

    configureDatabase()

    val jwtProvider = JwtConfig.configure(this)
    configureSecurity(jwtProvider)
    configureRouting(jwtProvider)

    startBlacklistCleanup()
}

fun Application.configureRouting(jwtProvider: com.mapchina.server.auth.JwtProvider) {
    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes(jwtProvider)
        logoutRoutes()
        dataRoutes(jwtProvider)
    }
}
