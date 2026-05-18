package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.AttractionVisit
import com.mapchina.domain.model.Footprint
import com.mapchina.domain.model.FootprintLevel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FootprintRepository(private val database: MapChinaDatabase) {

    fun markFootprint(userId: String, regionId: String, level: FootprintLevel) {
        val existing = database.footprintQueries
            .selectByUserAndRegion(userId, regionId)
            .executeAsOneOrNull()

        val effectiveLevel = if (existing != null) {
            val currentLevel = FootprintLevel.valueOf(existing.level)
            currentLevel.upgradeTo(level)
        } else {
            level
        }

        database.footprintQueries.upsertFootprint(
            userId, regionId, effectiveLevel.name, Clock.System.now().toEpochMilliseconds()
        )
    }

    fun markAttractionVisit(userId: String, attractionId: String, regionId: String, level: FootprintLevel) {
        database.attractionVisitQueries.upsertVisit(
            userId, attractionId, level.name, Clock.System.now().toEpochMilliseconds(), null
        )
        markFootprint(userId, regionId, level)
        cascadeToParentRegions(userId, regionId)
    }

    private fun cascadeToParentRegions(userId: String, regionId: String) {
        var currentId: String? = regionId
        while (currentId != null) {
            val region = database.regionQueries.selectById(currentId).executeAsOneOrNull() ?: break
            val parentId = region.parent_id ?: break
            markFootprint(userId, parentId, FootprintLevel.PASS_BY)
            currentId = parentId
        }
    }

    fun getFootprint(userId: String, regionId: String): Footprint? {
        val row = database.footprintQueries
            .selectByUserAndRegion(userId, regionId)
            .executeAsOneOrNull()
        return row?.let {
            Footprint(it.user_id, it.region_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }

    fun getFootprintsByUser(userId: String): List<Footprint> {
        return database.footprintQueries.selectByUserId(userId).executeAsList().map {
            Footprint(it.user_id, it.region_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }

    fun getAttractionVisit(userId: String, attractionId: String): AttractionVisit? {
        val row = database.attractionVisitQueries
            .selectByUserAndAttraction(userId, attractionId)
            .executeAsOneOrNull()
        return row?.let {
            AttractionVisit(it.user_id, it.attraction_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }

    fun removeAttractionVisit(userId: String, attractionId: String) {
        database.attractionVisitQueries.deleteByUserAndAttraction(userId, attractionId)
    }

    fun getAttractionVisitCount(userId: String): Int {
        return database.attractionVisitQueries.countByUser(userId).executeAsOne().toInt()
    }

    fun getAttractionVisitsByUser(userId: String): List<AttractionVisit> {
        return database.attractionVisitQueries.selectVisitsByUser(userId).executeAsList().map {
            AttractionVisit(it.user_id, it.attraction_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }

    fun getFootprintCountsByLevel(userId: String): Map<FootprintLevel, Int> {
        val rows = database.footprintQueries.countByUserAndLevel(userId).executeAsList()
        return rows.associate { FootprintLevel.valueOf(it.level) to it.total.toInt() }
    }
}
