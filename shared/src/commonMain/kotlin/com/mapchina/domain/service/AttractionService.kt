package com.mapchina.domain.service

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.domain.model.Attraction

class AttractionService(private val attractionRepository: AttractionRepository) {

    fun getAttraction(id: String): Attraction? = attractionRepository.getAttraction(id)

    fun getAttractionsByRegion(regionId: String): List<Attraction> =
        attractionRepository.getAttractionsByRegion(regionId)

    fun getAttractionsByParentRegion(regionId: String): List<Attraction> {
        val prefix = when {
            regionId.endsWith("0000") -> regionId.substring(0, 2) + "%"
            regionId.endsWith("00") -> regionId.substring(0, 4) + "%"
            else -> regionId
        }
        return attractionRepository.getAttractionsByRegionPrefix(prefix)
    }

    fun searchAttractions(query: String): List<Attraction> {
        if (query.isBlank()) return emptyList()
        return attractionRepository.searchAttractions(query)
    }
}
