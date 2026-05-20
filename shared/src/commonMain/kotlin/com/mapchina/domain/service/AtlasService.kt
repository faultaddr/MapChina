package com.mapchina.domain.service

import com.mapchina.data.repository.AtlasRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.domain.model.AtlasProgress

class AtlasService(
    private val atlasRepository: AtlasRepository,
    private val footprintRepository: FootprintRepository
) {

    fun getAtlasProgress(userId: String): List<AtlasProgress> {
        val definitions = atlasRepository.getAllDefinitions()
        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val visitedIds = visits.map { it.attractionId }.toSet()

        return definitions.map { def ->
            val items = atlasRepository.getItemsByAtlas(def.id)
            val visitedCount = items.count { it.attractionId in visitedIds }
            val percent = if (items.isNotEmpty()) (visitedCount * 100 / items.size) else 0

            AtlasProgress(
                atlasId = def.id,
                atlasName = def.name,
                atlasDescription = def.description,
                totalItems = items.size,
                visitedItems = visitedCount,
                completionPercent = percent
            )
        }
    }

    fun getAtlasDetail(userId: String, atlasId: String): AtlasProgress? {
        val def = atlasRepository.getDefinitionById(atlasId) ?: return null
        val items = atlasRepository.getItemsByAtlas(atlasId)
        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val visitedIds = visits.map { it.attractionId }.toSet()
        val visitedCount = items.count { it.attractionId in visitedIds }
        val percent = if (items.isNotEmpty()) (visitedCount * 100 / items.size) else 0

        return AtlasProgress(
            atlasId = def.id,
            atlasName = def.name,
            atlasDescription = def.description,
            totalItems = items.size,
            visitedItems = visitedCount,
            completionPercent = percent
        )
    }

    fun getAtlasItemsForAttraction(attractionId: String): List<AtlasProgress> {
        val items = atlasRepository.getItemsByAttraction(attractionId)
        if (items.isEmpty()) return emptyList()

        val definitions = atlasRepository.getAllDefinitions()
        return items.mapNotNull { item ->
            val def = definitions.find { it.id == item.atlasId } ?: return@mapNotNull null
            val totalItems = atlasRepository.countItemsByAtlas(item.atlasId)
            AtlasProgress(
                atlasId = def.id,
                atlasName = def.name,
                atlasDescription = def.description,
                totalItems = totalItems,
                visitedItems = 0,
                completionPercent = 0
            )
        }
    }

    fun getAtlasItemsWithVisitStatus(userId: String, atlasId: String): List<AtlasItemVisitStatus> {
        val items = atlasRepository.getItemsByAtlas(atlasId)
        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val visitedIds = visits.map { it.attractionId }.toSet()

        return items.map { item ->
            AtlasItemVisitStatus(
                atlasId = item.atlasId,
                attractionId = item.attractionId,
                itemName = item.itemName,
                province = item.province,
                city = item.city,
                isVisited = item.attractionId in visitedIds
            )
        }
    }
}

data class AtlasItemVisitStatus(
    val atlasId: String,
    val attractionId: String,
    val itemName: String,
    val province: String,
    val city: String,
    val isVisited: Boolean
)
