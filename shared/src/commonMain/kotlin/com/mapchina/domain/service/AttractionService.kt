package com.mapchina.domain.service

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.domain.model.Attraction

class AttractionService(private val attractionRepository: AttractionRepository) {

    fun getAttraction(id: String): Attraction? = attractionRepository.getAttraction(id)

    fun getAttractionsByRegion(regionId: String): List<Attraction> =
        attractionRepository.getAttractionsByRegion(regionId)

    fun searchAttractions(query: String): List<Attraction> {
        if (query.isBlank()) return emptyList()
        return attractionRepository.searchAttractions(query)
    }
}
