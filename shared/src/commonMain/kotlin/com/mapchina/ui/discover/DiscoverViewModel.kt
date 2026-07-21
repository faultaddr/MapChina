package com.mapchina.ui.discover

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.service.FootprintSuggestionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val levelLabel: String,
    val reason: String,
    val imageUrl: String?
)

data class DiscoverUi(
    val searchQuery: String = "",
    val pendingSuggestions: List<FootprintSuggestion> = emptyList(),
    val recommendations: List<DiscoverRecommendation> = emptyList()
)

class DiscoverViewModel(
    private val attractionRepository: AttractionRepository,
    private val footprintRepository: FootprintRepository,
    private val regionRepository: RegionRepository,
    private val suggestionService: FootprintSuggestionService,
    private val userId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _ui = MutableStateFlow(DiscoverUi())
    val ui: StateFlow<DiscoverUi> = _ui.asStateFlow()

    init {
        refresh()
        scope.launch {
            suggestionService.suggestions.collect { pending ->
                _ui.value = _ui.value.copy(pendingSuggestions = pending)
            }
        }
    }

    fun refresh() {
        _ui.value = _ui.value.copy(
            pendingSuggestions = suggestionService.suggestions.value,
            recommendations = recommendationsFrom(
                attractions = attractionRepository.getAllAttractions(),
                limit = 12,
                sortForDiscovery = true
            )
        )
    }

    fun search(query: String) {
        val results = if (query.isBlank()) {
            attractionRepository.getAllAttractions()
        } else {
            attractionRepository.searchAttractions(query)
        }
        _ui.value = _ui.value.copy(
            searchQuery = query,
            recommendations = recommendationsFrom(results, limit = 20, sortForDiscovery = query.isBlank())
        )
    }

    private fun recommendationsFrom(
        attractions: List<Attraction>,
        limit: Int,
        sortForDiscovery: Boolean
    ): List<DiscoverRecommendation> {
        val visitedAttractions = footprintRepository
            .getAttractionVisitsByUser(userId)
            .map { it.attractionId }
            .toSet()
        val source = if (sortForDiscovery) {
            attractions.sortedWith(compareByDescending<Attraction> { it.level == AttractionLevel.A5 }.thenBy { it.name })
        } else {
            attractions
        }
        return source
            .filter { it.id !in visitedAttractions }
            .take(limit)
            .map(::toRecommendation)
    }

    private fun toRecommendation(attraction: Attraction): DiscoverRecommendation {
        return DiscoverRecommendation(
            id = attraction.id,
            title = attraction.name,
            subtitle = attraction.description ?: regionRepository.getRegion(attraction.regionId)?.name.orEmpty(),
            levelLabel = when (attraction.level) {
                AttractionLevel.A5 -> "5A"
                AttractionLevel.A4 -> "4A"
                AttractionLevel.CUSTOM -> "自定义"
            },
            reason = "可点亮 ${buildRegionPath(attraction.regionId)}",
            imageUrl = attraction.imageUrl
        )
    }

    private fun buildRegionPath(regionId: String): String {
        val region = regionRepository.getRegion(regionId) ?: return regionId
        val names = mutableListOf(region.name)
        var parentId = region.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            names.add(0, parent.name)
            parentId = parent.parentId
        }
        return names.joinToString(" / ")
    }
}
