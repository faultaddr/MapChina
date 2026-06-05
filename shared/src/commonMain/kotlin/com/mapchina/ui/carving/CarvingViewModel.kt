package com.mapchina.ui.carving

import androidx.ink.strokes.Stroke
import com.mapchina.data.repository.CarvingRepository
import com.mapchina.domain.model.Carving
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class CarvingViewModel(
    private val carvingRepository: CarvingRepository,
    private val userId: String = ""
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _savedCarvings = MutableStateFlow<List<Carving>>(emptyList())
    val savedCarvings: StateFlow<List<Carving>> = _savedCarvings.asStateFlow()

    private val _currentCarving = MutableStateFlow<Carving?>(null)
    val currentCarving: StateFlow<Carving?> = _currentCarving.asStateFlow()

    private val _existingStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val existingStrokes: StateFlow<List<Stroke>> = _existingStrokes.asStateFlow()

    private val _carvingList = MutableStateFlow<List<Carving>>(emptyList())
    val carvingList: StateFlow<List<Carving>> = _carvingList.asStateFlow()

    fun loadCarvingsByRegion(regionId: String) {
        _carvingList.value = carvingRepository.getCarvingsByRegion(regionId)
    }

    fun loadCarvingsByAttraction(attractionId: String) {
        _carvingList.value = carvingRepository.getCarvingsByAttraction(attractionId)
    }

    fun loadAllCarvings() {
        _carvingList.value = carvingRepository.getAllCarvings()
    }

    fun loadCarvingForRegion(regionId: String) {
        val existing = carvingRepository.getCarvingsByRegion(regionId)
        _currentCarving.value = existing.firstOrNull()
        _existingStrokes.value = emptyList()
    }

    fun saveCarving(
        regionId: String,
        regionName: String,
        strokes: List<Stroke>,
        imagePath: String? = null,
        attractionId: String? = null,
        attractionName: String? = null
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "carving_${regionId}_${attractionId ?: "region"}_${userId.hashCode().toUInt()}"
        val carving = Carving(
            id = id,
            userId = userId,
            regionId = regionId,
            regionName = regionName,
            imagePath = imagePath,
            strokeData = "",
            createdAt = now,
            attractionId = attractionId,
            attractionName = attractionName
        )
        vmScope.launch {
            carvingRepository.insertCarving(carving)
            _currentCarving.value = carving
        }
    }

    fun deleteCarving(id: String) {
        vmScope.launch {
            carvingRepository.deleteCarving(id)
            _currentCarving.value = null
        }
    }
}
