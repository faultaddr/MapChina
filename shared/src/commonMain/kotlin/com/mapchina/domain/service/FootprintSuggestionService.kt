package com.mapchina.domain.service

import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.model.FootprintSuggestionSource
import com.mapchina.domain.model.FootprintSuggestionStatus
import com.mapchina.domain.model.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FootprintSuggestionService(
    private val regionRepository: RegionRepository,
    private val footprintService: FootprintService
) {
    private val pendingById = linkedMapOf<String, FootprintSuggestion>()
    private val dismissedIds = mutableSetOf<String>()
    private val _suggestions = MutableStateFlow<List<FootprintSuggestion>>(emptyList())
    val suggestions: StateFlow<List<FootprintSuggestion>> = _suggestions.asStateFlow()

    fun offerFromAttractionVisit(
        regionId: String,
        attractionName: String,
        level: FootprintLevel
    ): FootprintSuggestion? {
        val region = regionRepository.getRegion(regionId) ?: return null
        return offer(
            source = FootprintSuggestionSource.ATTRACTION_VISIT,
            region = region,
            evidenceLabel = attractionName,
            suggestedLevel = level,
            confidenceLabel = "高"
        )
    }

    fun offerFromAttractionVisit(
        userId: String,
        attractionId: String,
        regionId: String,
        attractionName: String,
        level: FootprintLevel
    ): FootprintSuggestion? {
        footprintService.recordAttractionVisit(userId, attractionId, level)
        return offerFromAttractionVisit(regionId, attractionName, level)
    }

    fun offerFromLocation(match: RegionMatch): FootprintSuggestion? {
        val region = match.district ?: match.city ?: match.province ?: return null
        val confidence = when {
            match.district != null -> "高"
            match.city != null -> "中"
            else -> "低"
        }
        return offer(
            source = FootprintSuggestionSource.LOCATION,
            region = region,
            evidenceLabel = "当前位置",
            suggestedLevel = FootprintLevel.PASS_BY,
            confidenceLabel = confidence
        )
    }

    fun confirm(userId: String, suggestionId: String, level: FootprintLevel): FootprintResult? {
        val suggestion = pendingById.remove(suggestionId) ?: return null
        _suggestions.value = pendingById.values.toList()
        val result = footprintService.markFootprint(userId, suggestion.regionId, level)
        markParentsAsPassBy(userId, suggestion.regionId)
        return result
    }

    fun dismiss(suggestionId: String) {
        pendingById.remove(suggestionId)
        dismissedIds.add(suggestionId)
        _suggestions.value = pendingById.values.toList()
    }

    private fun offer(
        source: FootprintSuggestionSource,
        region: Region,
        evidenceLabel: String,
        suggestedLevel: FootprintLevel?,
        confidenceLabel: String
    ): FootprintSuggestion? {
        val id = "${source.name}:${region.id}"
        if (id in dismissedIds) return null
        val suggestion = FootprintSuggestion(
            id = id,
            source = source,
            regionId = region.id,
            regionName = region.name,
            parentPath = buildPath(region),
            evidenceLabel = evidenceLabel,
            suggestedLevel = suggestedLevel,
            confidenceLabel = confidenceLabel,
            status = FootprintSuggestionStatus.PENDING
        )
        pendingById[id] = suggestion
        _suggestions.value = pendingById.values.toList()
        return suggestion
    }

    private fun buildPath(region: Region): String {
        val path = mutableListOf(region.name)
        var parentId = region.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            path.add(0, parent.name)
            parentId = parent.parentId
        }
        return path.joinToString(" / ")
    }

    private fun markParentsAsPassBy(userId: String, regionId: String) {
        var parentId = regionRepository.getRegion(regionId)?.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            footprintService.markPassiveFootprint(userId, parent.id, FootprintLevel.PASS_BY)
            parentId = parent.parentId
        }
    }
}
