package com.mapchina.server.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Regions : IdTable<String>("regions") {
    override val id = varchar("id", 10).entityId()
    val name = varchar("name", 50)
    val level = varchar("level", 10)
    val parentId = varchar("parent_id", 10).nullable()
    val boundaryJson = text("boundary_json").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Attractions : IdTable<String>("attractions") {
    override val id = varchar("id", 20).entityId()
    val name = varchar("name", 100)
    val regionId = varchar("region_id", 10).references(Regions.id, onDelete = ReferenceOption.CASCADE)
    val level = varchar("level", 5)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Users : IdTable<String>("users") {
    override val id = varchar("id", 36).entityId()
    val phone = varchar("phone", 20).uniqueIndex()
    val nickname = varchar("nickname", 50)
    val avatar = varchar("avatar", 500).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Footprints : Table("footprints") {
    val userId = varchar("user_id", 36).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val regionId = varchar("region_id", 10).references(Regions.id, onDelete = ReferenceOption.CASCADE)
    val level = varchar("level", 15)
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(userId, regionId)
}

object AttractionVisits : Table("attraction_visits") {
    val userId = varchar("user_id", 36).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val attractionId = varchar("attraction_id", 20).references(Attractions.id, onDelete = ReferenceOption.CASCADE)
    val level = varchar("level", 15)
    val timestamp = long("timestamp")
    val note = text("note").nullable()

    override val primaryKey = PrimaryKey(userId, attractionId)
}

object RefreshTokenBlacklist : Table("refresh_token_blacklist") {
    val token = varchar("token", 500)
    val expiresAt = long("expires_at")

    override val primaryKey = PrimaryKey(token)
}
