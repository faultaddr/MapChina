package com.mapchina.server.routes

import com.auth0.jwt.algorithms.Algorithm
import com.mapchina.server.auth.JwtProvider
import com.mapchina.server.auth.configureSecurity
import com.mapchina.server.database.AttractionVisits
import com.mapchina.server.database.Attractions
import com.mapchina.server.database.CommunityPosts
import com.mapchina.server.database.Footprints
import com.mapchina.server.database.PostComments
import com.mapchina.server.database.PostLikes
import com.mapchina.server.database.RefreshTokenBlacklist
import com.mapchina.server.database.Regions
import com.mapchina.server.database.SyncItems
import com.mapchina.server.database.Users
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun syncPush_acceptsGenericItemsAndPullReturnsDelta() = testApplication {
        val database = Database.connect(
            "jdbc:h2:mem:sync_routes_${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            SchemaUtils.create(
                Regions,
                Attractions,
                Users,
                Footprints,
                AttractionVisits,
                RefreshTokenBlacklist,
                CommunityPosts,
                PostLikes,
                PostComments,
                SyncItems
            )
        }

        val jwtProvider = JwtProvider(
            secret = "test-secret",
            issuer = "mapchina-test",
            audience = "mapchina-test",
            accessTokenTtlMs = 60_000,
            refreshTokenTtlMs = 60_000,
            algorithm = Algorithm.HMAC256("test-secret")
        )

        install(ContentNegotiation) {
            json(this@SyncRoutesTest.json)
        }
        application {
            configureSecurity(jwtProvider)
        }
        routing {
            authRoutes(jwtProvider)
            dataRoutes(jwtProvider)
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"18800001111","code":"123456"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val token = json.decodeFromString<LoginResponse>(loginResponse.bodyAsText()).accessToken

        val pushRequest = GenericSyncPushRequest(
            items = listOf(
                GenericSyncItem(
                    entityType = "FOOTPRINT",
                    entityId = "18800001111:110000",
                    operation = "UPSERT",
                    payload = """{"regionId":"110000","level":"DEEP","timestamp":1000}""",
                    updatedAt = 1000,
                    deleted = false
                ),
                GenericSyncItem(
                    entityType = "CARVING",
                    entityId = "carving-1",
                    operation = "UPSERT",
                    payload = """{"text":"山河已至","style":"CLIFF","createdAt":2000}""",
                    updatedAt = 2000,
                    deleted = false
                )
            )
        )
        val pushResponse = client.post("/sync/push") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(pushRequest))
        }
        assertEquals(HttpStatusCode.OK, pushResponse.status)
        val accepted = json.decodeFromString<GenericSyncPushResponse>(pushResponse.bodyAsText())
        assertEquals(2, accepted.accepted)
        assertTrue(accepted.serverTime >= 2000)

        val pullResponse = client.get("/sync/pull?since=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, pullResponse.status)
        val delta = json.decodeFromString<GenericSyncDeltaResponse>(pullResponse.bodyAsText())
        assertEquals(2, delta.items.size)
        assertEquals(setOf("FOOTPRINT", "CARVING"), delta.items.map { it.entityType }.toSet())
        assertTrue(delta.items.all { it.deleted.not() })
        assertTrue(delta.serverTime >= 2000)
    }
}
