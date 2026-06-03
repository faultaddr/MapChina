package com.mapchina.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val driverClassName = environment.config.propertyOrNull("database.driver")?.getString() ?: "org.postgresql.Driver"
    val jdbcURL = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()

    val config = HikariConfig().apply {
        this.jdbcUrl = jdbcURL
        this.driverClassName = driverClassName
        this.username = dbUser
        this.password = dbPassword
        maximumPoolSize = 10
        isAutoCommit = false
    }
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(Regions, Attractions, Users, Footprints, AttractionVisits, RefreshTokenBlacklist, CommunityPosts, PostLikes, PostComments)
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
