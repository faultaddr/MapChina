package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel

class AttractionRepository(private val database: MapChinaDatabase) {

    fun insertAttraction(attraction: Attraction) {
        database.attractionQueries.insertAttraction(
            attraction.id, attraction.name, attraction.regionId,
            attraction.level.name, attraction.latitude, attraction.longitude,
            attraction.description
        )
    }

    fun getAttraction(id: String): Attraction? {
        val row = database.attractionQueries.selectById(id).executeAsOneOrNull() ?: return null
        return rowToAttraction(row)
    }

    fun getAttractionsByRegion(regionId: String): List<Attraction> {
        return database.attractionQueries.selectByRegionId(regionId).executeAsList().map { rowToAttraction(it) }
    }

    fun searchAttractions(query: String): List<Attraction> {
        return database.attractionQueries.searchByName("%$query%").executeAsList().map { rowToAttraction(it) }
    }

    private fun rowToAttraction(row: com.mapchina.data.local.Attraction): Attraction =
        Attraction(
            id = row.id,
            name = row.name,
            regionId = row.region_id,
            level = AttractionLevel.valueOf(row.level),
            latitude = row.latitude,
            longitude = row.longitude,
            description = row.description
        )
}
