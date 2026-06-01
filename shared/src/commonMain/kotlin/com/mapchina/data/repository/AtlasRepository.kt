package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.AtlasDefinition
import com.mapchina.domain.model.AtlasItem

class AtlasRepository(private val database: MapChinaDatabase) {

    fun getAllDefinitions(): List<AtlasDefinition> =
        database.atlasDefinitionQueries.selectAll().executeAsList().map { it.toDomain() }

    fun getDefinitionById(id: String): AtlasDefinition? {
        val row = database.atlasDefinitionQueries.selectById(id).executeAsOneOrNull()
        return row?.toDomain()
    }

    fun insertDefinition(id: String, name: String, description: String, coverImage: String) {
        database.atlasDefinitionQueries.insertDefinition(id, name, description, coverImage, "active")
    }

    fun getItemsByAtlas(atlasId: String): List<AtlasItem> =
        database.atlasItemQueries.selectByAtlas(atlasId).executeAsList().map { it.toDomain() }

    fun getItemsByAttraction(attractionId: String): List<AtlasItem> =
        database.atlasItemQueries.selectByAttraction(attractionId).executeAsList().map { it.toDomain() }

    fun countItemsByAtlas(atlasId: String): Int =
        database.atlasItemQueries.countByAtlas(atlasId).executeAsOne().toInt()

    fun insertItem(atlasId: String, attractionId: String, itemName: String, province: String, city: String) {
        database.atlasItemQueries.insertItem(atlasId, attractionId, itemName, province, city)
    }

    fun insertItemsInTransaction(items: List<AtlasItem>) {
        database.atlasItemQueries.transaction {
            for (item in items) {
                database.atlasItemQueries.insertItem(item.atlasId, item.attractionId, item.itemName, item.province, item.city)
            }
        }
    }

    fun isSeeded(): Boolean =
        database.atlasDefinitionQueries.selectAll().executeAsList().isNotEmpty()

    private fun com.mapchina.data.local.Atlas_definition.toDomain() = AtlasDefinition(
        id = atlas_id,
        name = name,
        description = description,
        coverImage = cover_image
    )

    private fun com.mapchina.data.local.Atlas_item.toDomain() = AtlasItem(
        atlasId = atlas_id,
        attractionId = attraction_id,
        itemName = item_name,
        province = province,
        city = city
    )
}
