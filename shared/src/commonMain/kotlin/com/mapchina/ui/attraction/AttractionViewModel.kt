package com.mapchina.ui.attraction

import com.mapchina.data.remote.AttractionDetail
import com.mapchina.data.remote.AttractionDetailProvider
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.service.AttractionService
import com.mapchina.domain.service.FootprintService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
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
    val visitLevel: FootprintLevel?,
    val isCustom: Boolean = false
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

    fun onCleared() {
        vmScope.cancel()
    }

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
            cacheImageUrls(all.filter { it.imageUrl == null })
        }
    }

    private fun cacheImageUrls(attractionsWithoutImage: List<Attraction>) {
        if (detailProvider == null || attractionsWithoutImage.isEmpty()) return
        vmScope.launch {
            attractionsWithoutImage.forEach { attraction ->
                try {
                    val detail = detailProvider.getAttractionDetail(attraction.id)
                    val url = detail?.imageUrls?.firstOrNull()
                    if (url != null) {
                        attractionRepository.updateImageUrl(attraction.id, url)
                    }
                } catch (_: Exception) { }
            }
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

    fun createCustomAttraction(name: String, description: String?, regionId: String, latitude: Double, longitude: Double, imageUrl: String? = null) {
        vmScope.launch {
            val id = "custom_${System.currentTimeMillis()}"
            val attraction = Attraction(
                id = id,
                name = name,
                regionId = regionId,
                level = AttractionLevel.CUSTOM,
                latitude = latitude,
                longitude = longitude,
                description = description,
                imageUrl = imageUrl,
                isCustom = true,
                userId = if (userId.isNotBlank()) userId else "local"
            )
            attractionRepository.insertAttraction(attraction)
            loadAllAttractions()
        }
    }

    private fun refreshAttractions() {
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            searchAttractions(query)
        }
    }

    private fun Attraction.toUi(): AttractionUi {
        val visit = footprintRepository.getAttractionVisit(this@AttractionViewModel.userId, id)
        return AttractionUi(
            id = id,
            name = name,
            level = level.name,
            regionId = regionId,
            latitude = latitude,
            longitude = longitude,
            description = description,
            imageUrl = imageUrl ?: detailProvider?.getAttractionDetail(id)?.imageUrls?.firstOrNull(),
            visitLevel = visit?.level,
            isCustom = isCustom
        )
    }
}
