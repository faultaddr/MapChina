package com.mapchina.server.routes

import com.mapchina.server.auth.JwtProvider
import com.mapchina.server.database.*
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
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
data class GenericSyncPushRequest(
    val items: List<GenericSyncItem> = emptyList(),
    val footprints: List<SyncFootprintItem> = emptyList(),
    val attractionVisits: List<SyncAttractionVisitItem> = emptyList()
)

@Serializable
data class GenericSyncItem(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val updatedAt: Long,
    val deleted: Boolean = false
)

@Serializable
data class GenericSyncPushResponse(val accepted: Int, val serverTime: Long)

@Serializable
data class GenericSyncDeltaResponse(val items: List<GenericSyncItem>, val serverTime: Long)

@Serializable
data class SyncFootprintItem(val regionId: String, val level: String, val timestamp: Long)

@Serializable
data class SyncAttractionVisitItem(val attractionId: String, val regionId: String, val level: String, val timestamp: Long, val note: String? = null)

@Serializable
private data class SyncFootprintPayload(val regionId: String, val level: String, val timestamp: Long)

@Serializable
private data class SyncAttractionVisitPayload(
    val attractionId: String,
    val regionId: String,
    val level: String,
    val timestamp: Long,
    val note: String? = null
)

@Serializable
data class PaginatedResponse<T>(val data: List<T>, val total: Long, val hasMore: Boolean)

private val syncJson = Json { encodeDefaults = true }

fun Route.dataRoutes(jwtProvider: JwtProvider) {
    authenticate("auth-jwt") {
        route("/regions") {
            get {
                val parentId = call.parameters["parentId"]
                val level = call.parameters["level"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20

                val regions = dbQuery {
                    val query = Regions.selectAll()
                    if (parentId != null) {
                        query.where { Regions.parentId eq parentId }
                    } else if (level != null) {
                        query.where { Regions.level eq level }
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
                    Regions.selectAll().where { Regions.id eq id }.singleOrNull()?.toRegionResponse()
                }
                if (region != null) call.respond(region)
                else call.respondText("""{"code":"NOT_FOUND","message":"区域不存在"}""", status = HttpStatusCode.NotFound)
            }

            get("/{id}/boundary") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val boundary = dbQuery {
                    Regions.selectAll().where { Regions.id eq id }.singleOrNull()?.get(Regions.boundaryJson)
                }
                if (boundary != null) call.respondText(boundary, ContentType.Application.Json)
                else call.respondText("""{"code":"NOT_FOUND","message":"边界数据不存在"}""", status = HttpStatusCode.NotFound)
            }

            get("/{id}/attractions") {
                val id = call.parameters["id"] ?: return@get call.respondText(
                    """{"code":"MISSING_ID","message":"区域ID不能为空"}""", status = HttpStatusCode.BadRequest
                )
                val attractions = dbQuery {
                    Attractions.selectAll().where { Attractions.regionId eq id }
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
                    Attractions.selectAll().where { Attractions.id eq id }.singleOrNull()?.toAttractionResponse()
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
                    val existing = Footprints.selectAll().where {
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
                        Footprints.selectAll().where { (Footprints.userId eq userId) and (Footprints.regionId eq regionId) }
                    } else {
                        Footprints.selectAll().where { Footprints.userId eq userId }
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

                    val existing = Footprints.selectAll().where {
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
                call.respondSyncDelta()
            }

            get("/pull") {
                call.respondSyncDelta()
            }

            post("/push") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<GenericSyncPushRequest>()
                val items = request.effectiveItems(userId)

                val accepted = dbQuery {
                    items.count { item -> upsertSyncItem(userId, item.normalized()) }
                }
                call.respond(GenericSyncPushResponse(accepted, serverTime(items, System.currentTimeMillis())))
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

private suspend fun ApplicationCall.respondSyncDelta() {
    val userId = principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
    val since = parameters["since"]?.toLongOrNull() ?: 0L

    val items = dbQuery {
        SyncItems.selectAll()
            .where { (SyncItems.userId eq userId) and (SyncItems.updatedAt greater since) }
            .orderBy(SyncItems.updatedAt, SortOrder.ASC)
            .map { it.toGenericSyncItem() }
    }
    respond(GenericSyncDeltaResponse(items, serverTime(items, System.currentTimeMillis())))
}

private fun ResultRow.toGenericSyncItem() = GenericSyncItem(
    entityType = this[SyncItems.entityType],
    entityId = this[SyncItems.entityId],
    operation = this[SyncItems.operation],
    payload = this[SyncItems.payload],
    updatedAt = this[SyncItems.updatedAt],
    deleted = this[SyncItems.deleted]
)

private fun GenericSyncPushRequest.effectiveItems(userId: String): List<GenericSyncItem> =
    if (items.isNotEmpty()) {
        items
    } else {
        footprints.map { item ->
            GenericSyncItem(
                entityType = "FOOTPRINT",
                entityId = item.regionId,
                operation = "UPSERT",
                payload = syncJson.encodeToString(SyncFootprintPayload(item.regionId, item.level, item.timestamp)),
                updatedAt = item.timestamp,
                deleted = false
            )
        } + attractionVisits.map { item ->
            GenericSyncItem(
                entityType = "ATTRACTION_VISIT",
                entityId = item.attractionId,
                operation = "UPSERT",
                payload = syncJson.encodeToString(
                    SyncAttractionVisitPayload(
                        attractionId = item.attractionId,
                        regionId = item.regionId,
                        level = item.level,
                        timestamp = item.timestamp,
                        note = item.note
                    )
                ),
                updatedAt = item.timestamp,
                deleted = false
            )
        }
    }

private fun GenericSyncItem.normalized() = copy(
    entityType = entityType.trim().uppercase(),
    entityId = entityId.trim(),
    operation = operation.trim().uppercase(),
    deleted = deleted || operation.equals("DELETE", ignoreCase = true)
)

private fun upsertSyncItem(userId: String, item: GenericSyncItem): Boolean {
    if (item.entityType.isBlank() || item.entityId.isBlank() || item.operation.isBlank()) {
        return false
    }

    val existing = SyncItems.selectAll().where {
        (SyncItems.userId eq userId) and
            (SyncItems.entityType eq item.entityType) and
            (SyncItems.entityId eq item.entityId)
    }.singleOrNull()

    if (existing == null) {
        SyncItems.insert {
            it[SyncItems.userId] = userId
            it[entityType] = item.entityType
            it[entityId] = item.entityId
            it[operation] = item.operation
            it[payload] = item.payload
            it[updatedAt] = item.updatedAt
            it[deleted] = item.deleted
        }
        return true
    }

    if (item.updatedAt < existing[SyncItems.updatedAt]) {
        return false
    }

    SyncItems.update({
        (SyncItems.userId eq userId) and
            (SyncItems.entityType eq item.entityType) and
            (SyncItems.entityId eq item.entityId)
    }) {
        it[operation] = item.operation
        it[payload] = item.payload
        it[updatedAt] = item.updatedAt
        it[deleted] = item.deleted
    }
    return true
}

private fun serverTime(items: List<GenericSyncItem>, now: Long): Long =
    maxOf(now, items.maxOfOrNull { it.updatedAt } ?: 0L)
