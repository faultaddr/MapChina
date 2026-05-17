package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel

class RegionRepository(private val database: MapChinaDatabase) {

    fun insertRegion(region: Region) {
        database.regionQueries.insertRegion(
            region.id, region.name, region.level.name, region.parentId, null
        )
    }

    fun getRegion(id: String): Region? {
        val row = database.regionQueries.selectById(id).executeAsOneOrNull() ?: return null
        return Region(row.id, row.name, RegionLevel.valueOf(row.level), row.parent_id)
    }

    fun getChildRegions(parentId: String): List<Region> {
        return database.regionQueries.selectByParentId(parentId).executeAsList().map {
            Region(it.id, it.name, RegionLevel.valueOf(it.level), it.parent_id)
        }
    }

    fun getRegionsByLevel(level: RegionLevel): List<Region> {
        return database.regionQueries.selectByLevel(level.name).executeAsList().map {
            Region(it.id, it.name, RegionLevel.valueOf(it.level), it.parent_id)
        }
    }

    fun getRegionBoundary(regionId: String): String? {
        return database.regionQueries.selectById(regionId).executeAsOneOrNull()?.boundary_json
    }

    fun updateBoundary(regionId: String, boundaryJson: String) {
        database.regionQueries.updateBoundary(boundaryJson, regionId)
    }
}
