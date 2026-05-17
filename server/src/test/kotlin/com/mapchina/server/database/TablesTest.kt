package com.mapchina.server.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertTrue

class TablesTest {

    @Test
    fun databaseFactory_createsAllTables() {
        val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(Regions, Attractions, Users, Footprints, AttractionVisits, RefreshTokenBlacklist)
        }
        val tables = transaction(database) {
            exec("SHOW TABLES") { rs ->
                generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
            }
        }
        assertTrue(tables?.containsAll(listOf(
            "REGIONS", "ATTRACTIONS", "USERS", "FOOTPRINTS", "ATTRACTION_VISITS", "REFRESH_TOKEN_BLACKLIST"
        )) == true)
    }
}
