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
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class RegionResponse(val id: String, val name: String, val level: String, val parentId: String? = null)

@Serializable
data class AttractionResponse(
    val id: String, val name: String, val regionId: String,
    val level: String, val latitude: Double, val longitude: Double, val description: String? = null
)

@Serializable
data class CreateFootprintRequest(val regionId: String, val level: String)

@Serializable
data class CreateAttractionVisitRequest(val attractionId: String, val regionId: String, val level: String, val note: String? = null)

@Serializable
data class SyncPushRequest(val footprints: List<SyncFootprintItem> = emptyList(), val attractionVisits: List<SyncAttractionVisitItem> = emptyList())

@Serializable
data class SyncFootprintItem(val regionId: String, val level: String, val timestamp: Long)

@Serializable
data class SyncAttractionVisitItem(val attractionId: String, val regionId: String, val level: String, val timestamp: Long, val note: String? = null)

@Serializable
data class SyncDeltaResponse(val footprints: List<Map<String, String>>, val attractionVisits: List<Map<String, String?>>, val since: Long)

@Serializable
data class PaginatedResponse<T>(val data: List<T>, val total: Long, val hasMore: Boolean)

fun Route.dataRoutes(jwtProvider: JwtProvider) {
    authenticate("auth-jwt") {
        route("/regions") {
            get {
                val parentId = call.parameters["parentId"]
                val level = call.parameters["level"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20

                val regions = dbQuery {
                    val query = if (parentId != null) {
                        Regions.select { Regions.parentId eq parentId }
                    } else if (level != null) {
                        Regions.select { Regions.level eq level }
                    } else {
                        Regions.selectAll()
                    }
                    query.orderBy(Regions.name, SortOrder.ASC)
                        .limit(size, offset = ((page - 1) * size).toLong())
                        .map { it.toRegionResponse() }
                }
                call.respond(regions)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val region = dbQuery {
                    Regions.select { Regions.id eq id }.singleOrNull()?.toRegionResponse()
                }
                if (region != null) call.respond(region)
                else call.respondText("""{"code":"NOT_FOUND","message":"区域不存在"}""", status = HttpStatusCode.NotFound)
            }

            get("/{id}/boundary") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val boundary = dbQuery {
                    Regions.select { Regions.id eq id }.singleOrNull()?.get(Regions.boundaryJson)
                }
                if (boundary != null) call.respondText(boundary, ContentType.Application.Json)
                else call.respondText("""{"code":"NOT_FOUND","message":"边界数据不存在"}""", status = HttpStatusCode.NotFound)
            }

            get("/{id}/attractions") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val attractions = dbQuery {
                    Attractions.select { Attractions.regionId eq id }
                        .orderBy(Attractions.name, SortOrder.ASC)
                        .map { it.toAttractionResponse() }
                }
                call.respond(attractions)
            }
        }

        route("/attractions") {
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"景点ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val attraction = dbQuery {
                    Attractions.select { Attractions.id eq id }.singleOrNull()?.toAttractionResponse()
                }
                if (attraction != null) call.respond(attraction)
                else call.respondText("""{"code":"NOT_FOUND","message":"景点不存在"}""", status = HttpStatusCode.NotFound)
            }
        }

        route("/footprints") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<CreateFootprintRequest>()
                val newLevel = request.level

                dbQuery {
                    val existing = Footprints.select {
                        (Footprints.userId eq userId) and (Footprints.regionId eq request.regionId)
                    }.singleOrNull()

                    if (existing != null) {
                        val currentLevel = existing[Footprints.level]
                        val effectiveLevel = if (levelValue(newLevel) > levelValue(currentLevel)) newLevel else currentLevel
                        if (effectiveLevel != currentLevel) {
                            Footprints.update({
                                (Footprints.userId eq userId) and (Footprints.regionId eq request.regionId)
                            }) {
                                it[level] = effectiveLevel
                                it[timestamp] = System.currentTimeMillis()
                            }
                        }
                    } else {
                        Footprints.insert {
                            it[Footprints.userId] = userId
                            it[regionId] = request.regionId
                            it[level] = newLevel
                            it[timestamp] = System.currentTimeMillis()
                        }
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"足迹已记录"}""")
            }

            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val regionId = call.parameters["regionId"]

                val footprints = dbQuery {
                    val query = if (regionId != null) {
                        Footprints.select { (Footprints.userId eq userId) and (Footprints.regionId eq regionId) }
                    } else {
                        Footprints.select { Footprints.userId eq userId }
                    }
                    query.map { mapOf("userId" to it[Footprints.userId], "regionId" to it[Footprints.regionId], "level" to it[Footprints.level], "timestamp" to it[Footprints.timestamp].toString()) }
                }
                call.respond(footprints)
            }
        }

        route("/attraction-visits") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<CreateAttractionVisitRequest>()

                dbQuery {
                    AttractionVisits.insert {
                        it[AttractionVisits.userId] = userId
                        it[attractionId] = request.attractionId
                        it[level] = request.level
                        it[timestamp] = System.currentTimeMillis()
                        it[note] = request.note
                    }

                    val existing = Footprints.select {
                        (Footprints.userId eq userId) and (Footprints.regionId eq request.regionId)
                    }.singleOrNull()

                    if (existing != null) {
                        val currentLevel = existing[Footprints.level]
                        val effectiveLevel = if (levelValue(request.level) > levelValue(currentLevel)) request.level else currentLevel
                        if (effectiveLevel != currentLevel) {
                            Footprints.update({
                                (Footprints.userId eq userId) and (Footprints.regionId eq request.regionId)
                            }) {
                                it[level] = effectiveLevel
                            }
                        }
                    } else {
                        Footprints.insert {
                            it[Footprints.userId] = userId
                            it[regionId] = request.regionId
                            it[level] = request.level
                            it[timestamp] = System.currentTimeMillis()
                        }
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"景点访问已记录"}""")
            }
        }

        route("/sync") {
            get("/delta") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val since = call.parameters["since"]?.toLongOrNull() ?: 0L

                val footprints = dbQuery {
                    Footprints.select { (Footprints.userId eq userId) and (Footprints.timestamp greater since) }
                        .map { mapOf("userId" to it[Footprints.userId], "regionId" to it[Footprints.regionId], "level" to it[Footprints.level], "timestamp" to it[Footprints.timestamp].toString()) }
                }
                val visits = dbQuery {
                    AttractionVisits.select { (AttractionVisits.userId eq userId) and (AttractionVisits.timestamp greater since) }
                        .map { mapOf("userId" to it[AttractionVisits.userId], "attractionId" to it[AttractionVisits.attractionId], "level" to it[AttractionVisits.level], "timestamp" to it[AttractionVisits.timestamp].toString(), "note" to it[AttractionVisits.note]) }
                }
                call.respond(SyncDeltaResponse(footprints, visits, System.currentTimeMillis()))
            }

            post("/push") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<SyncPushRequest>()

                dbQuery {
                    request.footprints.forEach { item ->
                        val existing = Footprints.select {
                            (Footprints.userId eq userId) and (Footprints.regionId eq item.regionId)
                        }.singleOrNull()

                        if (existing != null) {
                            val currentLevel = existing[Footprints.level]
                            val effectiveLevel = if (levelValue(item.level) > levelValue(currentLevel)) item.level else currentLevel
                            if (effectiveLevel != currentLevel) {
                                Footprints.update({
                                    (Footprints.userId eq userId) and (Footprints.regionId eq item.regionId)
                                }) {
                                    it[level] = effectiveLevel
                                    it[timestamp] = item.timestamp
                                }
                            }
                        } else {
                            Footprints.insert {
                                it[Footprints.userId] = userId
                                it[regionId] = item.regionId
                                it[level] = item.level
                                it[timestamp] = item.timestamp
                            }
                        }
                    }

                    request.attractionVisits.forEach { item ->
                        AttractionVisits.insert {
                            it[AttractionVisits.userId] = userId
                            it[attractionId] = item.attractionId
                            it[level] = item.level
                            it[timestamp] = item.timestamp
                            it[note] = item.note
                        }
                    }
                }
                call.respondText("""{"code":"SUCCESS","message":"同步完成"}""")
            }
        }
    }
}

private fun ResultRow.toRegionResponse() = RegionResponse(
    id = this[Regions.id].value,
    name = this[Regions.name],
    level = this[Regions.level],
    parentId = this[Regions.parentId]
)

private fun ResultRow.toAttractionResponse() = AttractionResponse(
    id = this[Attractions.id].value,
    name = this[Attractions.name],
    regionId = this[Attractions.regionId],
    level = this[Attractions.level],
    latitude = this[Attractions.latitude],
    longitude = this[Attractions.longitude],
    description = this[Attractions.description]
)

private fun levelValue(level: String): Int = when (level.uppercase()) {
    "DEEP" -> 3
    "SHORT_VISIT" -> 2
    "PASS_BY" -> 1
    else -> 0
}
