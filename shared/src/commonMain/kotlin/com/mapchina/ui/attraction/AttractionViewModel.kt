package com.mapchina.ui.attraction

import com.mapchina.data.remote.AttractionDetail
import com.mapchina.data.remote.AttractionDetailProvider
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val imageUrl: String?,
    val visitLevel: FootprintLevel?
)

class AttractionViewModel(
    private val attractionRepository: AttractionRepository,
    private val footprintService: FootprintService,
    private val footprintRepository: FootprintRepository,
    private val detailProvider: AttractionDetailProvider?,
    private val attractionService: AttractionService? = null,
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _attractions = MutableStateFlow<List<AttractionUi>>(emptyList())
    val attractions: StateFlow<List<AttractionUi>> = _attractions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAttraction = MutableStateFlow<AttractionUi?>(null)
    val selectedAttraction: StateFlow<AttractionUi?> = _selectedAttraction.asStateFlow()

    init {
        loadAllAttractions()
    }

    private fun loadAllAttractions() {
        vmScope.launch {
            val all = attractionRepository.getAllAttractions()
            _attractions.value = all.map { it.toUi() }
        }
    }

    fun loadAttractionsByRegion(regionId: String) {
        vmScope.launch {
            val list = attractionService?.getAttractionsByParentRegion(regionId)
                ?: attractionRepository.getAttractionsByRegion(regionId)
            _attractions.value = list.map { it.toUi() }
        }
    }

    fun searchAttractions(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadAllAttractions()
            return
        }
        vmScope.launch {
            val results = attractionRepository.searchAttractions(query)
            _attractions.value = results.map { it.toUi() }
        }
    }

    fun selectAttraction(attractionId: String) {
        _selectedAttraction.value = _attractions.value.find { it.id == attractionId }
    }

    fun getAttractionDetail(attractionId: String): AttractionDetail? {
        return detailProvider?.getAttractionDetail(attractionId)
    }

    fun getAttractionById(attractionId: String): AttractionUi? {
        val existing = _attractions.value.find { it.id == attractionId }
        if (existing != null) return existing
        val domain = attractionRepository.getAttraction(attractionId) ?: return null
        return domain.toUi()
    }

    fun clearSelection() {
        _selectedAttraction.value = null
    }

    fun markVisit(attractionId: String, regionId: String, level: FootprintLevel) {
        vmScope.launch {
            footprintService.markAttractionVisit(userId, attractionId, regionId, level)
            refreshAttractions()
        }
    }

    fun removeVisit(attractionId: String) {
        vmScope.launch {
            footprintService.removeAttractionVisit(userId, attractionId)
            refreshAttractions()
        }
    }

    private fun refreshAttractions() {
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            searchAttractions(query)
        }
    }

    private fun Attraction.toUi(): AttractionUi {
        val visit = footprintRepository.getAttractionVisit(userId, id)
        val detail = detailProvider?.getAttractionDetail(id)
        return AttractionUi(
            id = id,
            name = name,
            level = level.name,
            regionId = regionId,
            latitude = latitude,
            longitude = longitude,
            description = description,
            imageUrl = detail?.imageUrls?.firstOrNull(),
            visitLevel = visit?.level
        )
    }
}
