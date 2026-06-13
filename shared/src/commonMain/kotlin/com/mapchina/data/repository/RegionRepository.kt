package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class RegionBounds(
    val minLng: Double,
    val maxLng: Double,
    val minLat: Double,
    val maxLat: Double
)

class RegionRepository(private val database: MapChinaDatabase) {

    fun insertRegion(region: Region) {
        database.regionQueries.insertRegion(
            region.id, region.name, region.level.name, region.parentId, null
        )
    }

    fun insertRegionsInTransaction(regions: List<Region>) {
        database.regionQueries.transaction {
            for (region in regions) {
                database.regionQueries.insertRegion(
                    region.id, region.name, region.level.name, region.parentId, null
                )
            }
        }
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

    fun getBoundariesByParentId(parentId: String): Map<String, String> {
        return database.regionQueries.selectBoundariesByParentId(parentId).executeAsList()
            .associate { it.id to it.boundary_json }
    }

    fun getBoundariesByLevel(level: RegionLevel): Map<String, String> {
        return database.regionQueries.selectBoundariesByLevel(level.name).executeAsList()
            .associate { it.id to it.boundary_json }
    }

    fun updateBoundary(regionId: String, boundaryJson: String) {
        database.regionQueries.updateBoundary(boundaryJson, regionId)
    }

    fun updateBoundariesInTransaction(updates: List<Pair<String, String>>) {
        database.regionQueries.transaction {
            for ((regionId, boundaryJson) in updates) {
                database.regionQueries.updateBoundary(boundaryJson, regionId)
            }
        }
    }

    fun deleteAllCities() {
        database.regionQueries.deleteByLevel(RegionLevel.CITY.name)
    }

    fun deleteAllNonProvinces() {
        database.regionQueries.deleteByLevel(RegionLevel.CITY.name)
        database.regionQueries.deleteByLevel(RegionLevel.DISTRICT.name)
    }

    fun getRegionCenter(regionId: String): Pair<Double, Double>? {
        val boundary = getRegionBoundary(regionId) ?: return null
        return parseBoundaryCenter(boundary)
    }

    fun getRegionBounds(regionId: String): RegionBounds? {
        val boundary = getRegionBoundary(regionId) ?: return null
        return parseBoundaryBounds(boundary)
    }

    private fun parseBoundaryBounds(boundary: String): RegionBounds? {
        return try {
            val array = Json.decodeFromString<JsonArray>(boundary)
            if (array.isEmpty()) return null
            var minLng = Double.MAX_VALUE; var maxLng = -Double.MAX_VALUE
            var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
            for (item in array) {
                val coord = item.jsonArray
                val lng = coord[0].jsonPrimitive.double
                val lat = coord[1].jsonPrimitive.double
                if (lng < minLng) minLng = lng
                if (lng > maxLng) maxLng = lng
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
            }
            RegionBounds(minLng, maxLng, minLat, maxLat)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBoundaryCenter(boundary: String): Pair<Double, Double>? {
        return try {
            val array = Json.decodeFromString<JsonArray>(boundary)
            if (array.isEmpty()) return null
            var sumLat = 0.0
            var sumLng = 0.0
            var count = 0
            for (item in array) {
                val coord = item.jsonArray
                sumLng += coord[0].jsonPrimitive.double
                sumLat += coord[1].jsonPrimitive.double
                count++
            }
            if (count == 0) null else Pair(sumLat / count, sumLng / count)
        } catch (_: Exception) {
            null
        }
    }
}
