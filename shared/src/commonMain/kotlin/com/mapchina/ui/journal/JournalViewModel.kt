package com.mapchina.ui.journal

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.Journal
import com.mapchina.domain.model.JournalDetail
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.JournalService
import com.mapchina.platform.PhotoPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationItem(
    val id: String,
    val name: String,
    val type: LocationType,
    val subtitle: String = ""
)

enum class LocationType { PROVINCE, CITY, DISTRICT, ATTRACTION }

data class JournalListUi(
    val journals: List<Journal> = emptyList(),
    val isLoading: Boolean = false
)

data class JournalDetailUi(
    val detail: JournalDetail? = null,
    val isLoading: Boolean = false
)

data class JournalCreateUi(
    val selectedPhotoPaths: List<String> = emptyList(),
    val selectedLocation: LocationItem? = null,
    val searchQuery: String = "",
    val searchResults: List<LocationItem> = emptyList()
)

class JournalViewModel(
    private val journalService: JournalService,
    private val regionRepository: RegionRepository,
    private val attractionRepository: AttractionRepository,
    private val photoPicker: PhotoPicker? = null,
    private val userId: String = ""
) {
    private val _listUi = MutableStateFlow(JournalListUi())
    val listUi: StateFlow<JournalListUi> = _listUi.asStateFlow()

    private val _detailUi = MutableStateFlow(JournalDetailUi())
    val detailUi: StateFlow<JournalDetailUi> = _detailUi.asStateFlow()

    private val _createUi = MutableStateFlow(JournalCreateUi())
    val createUi: StateFlow<JournalCreateUi> = _createUi.asStateFlow()

    fun loadJournals() {
        _listUi.value = _listUi.value.copy(isLoading = true)
        val journals = journalService.getJournals(userId)
        _listUi.value = JournalListUi(journals = journals, isLoading = false)
    }

    fun loadJournalDetail(journalId: String) {
        _detailUi.value = _detailUi.value.copy(isLoading = true)
        val detail = journalService.getJournalDetail(journalId)
        _detailUi.value = JournalDetailUi(detail = detail, isLoading = false)
    }

    fun createJournal(title: String, description: String, startTime: Long): Journal {
        val create = _createUi.value
        val regionId = create.selectedLocation?.takeIf { it.type != LocationType.ATTRACTION }?.id
        val attractionId = create.selectedLocation?.takeIf { it.type == LocationType.ATTRACTION }?.id
        val journal = journalService.createJournal(userId, title, description, regionId, attractionId, startTime)
        for (path in create.selectedPhotoPaths) {
            journalService.addPhoto(journal.id, path, null, null, null)
        }
        _createUi.value = JournalCreateUi()
        return journal
    }

    fun updateJournal(journalId: String, title: String, description: String, regionId: String?, endTime: Long?) {
        journalService.updateJournal(journalId, title, description, regionId, null, endTime)
    }

    fun addPhoto(journalId: String, localPath: String, latitude: Double?, longitude: Double?, takenAt: Long?) {
        journalService.addPhoto(journalId, localPath, latitude, longitude, takenAt)
    }

    fun removePhoto(photoId: String) {
        journalService.deletePhoto(photoId)
    }

    fun pickPhotos() {
        photoPicker?.pickPhotos { paths ->
            val current = _createUi.value.selectedPhotoPaths
            _createUi.value = _createUi.value.copy(selectedPhotoPaths = current + paths)
        }
    }

    fun removeSelectedPhoto(path: String) {
        _createUi.value = _createUi.value.copy(
            selectedPhotoPaths = _createUi.value.selectedPhotoPaths.filter { it != path }
        )
    }

    fun canPickPhotos(): Boolean = photoPicker?.isAvailable() == true

    fun selectLocation(item: LocationItem) {
        _createUi.value = _createUi.value.copy(selectedLocation = item, searchQuery = "", searchResults = emptyList())
    }

    fun clearLocation() {
        _createUi.value = _createUi.value.copy(selectedLocation = null)
    }

    fun setInitialAttraction(attractionId: String) {
        if (_createUi.value.selectedLocation != null) return
        val attraction = attractionRepository.getAttraction(attractionId) ?: return
        val regionName = attraction.regionId.let { regionRepository.getRegion(it)?.name }
        _createUi.value = _createUi.value.copy(
            selectedLocation = LocationItem(
                id = attraction.id,
                name = attraction.name,
                type = LocationType.ATTRACTION,
                subtitle = regionName ?: "景点"
            )
        )
    }

    fun searchLocations(query: String) {
        _createUi.value = _createUi.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _createUi.value = _createUi.value.copy(searchResults = emptyList())
            return
        }
        val results = mutableListOf<LocationItem>()
        val regions = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE) +
                regionRepository.getRegionsByLevel(RegionLevel.CITY) +
                regionRepository.getRegionsByLevel(RegionLevel.DISTRICT)
        for (region in regions) {
            if (region.name.contains(query, ignoreCase = true)) {
                val type = when (region.level) {
                    RegionLevel.PROVINCE -> LocationType.PROVINCE
                    RegionLevel.CITY -> LocationType.CITY
                    RegionLevel.DISTRICT -> LocationType.DISTRICT
                }
                val label = when (type) {
                    LocationType.PROVINCE -> "省"
                    LocationType.CITY -> "市"
                    LocationType.DISTRICT -> "县/区"
                    else -> ""
                }
                results.add(LocationItem(region.id, region.name, type, label))
            }
        }
        val attractions = attractionRepository.searchAttractions(query)
        for (attraction in attractions) {
            val regionName = attraction.regionId.let { regionRepository.getRegion(it)?.name }
            results.add(LocationItem(attraction.id, attraction.name, LocationType.ATTRACTION, regionName ?: "景点"))
        }
        _createUi.value = _createUi.value.copy(searchResults = results.take(30))
    }

    fun addTrackPoints(journalId: String, points: List<Triple<Double, Double, Long>>) {
        journalService.addTrackPoints(journalId, points)
    }

    fun deleteJournal(journalId: String) {
        journalService.deleteJournal(journalId)
        loadJournals()
    }

    fun clearDetail() {
        _detailUi.value = JournalDetailUi()
    }

    fun getJournalsByAttraction(attractionId: String): List<Journal> {
        return journalService.getJournalsByAttraction(attractionId)
    }
}
