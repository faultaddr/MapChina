package com.mapchina.ui.carving

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

    fun loadCarvingForRegion(regionId: String) {
        val existing = carvingRepository.getCarvingsByRegion(regionId).firstOrNull()
        _currentCarving.value = existing
    }

    fun saveCarving(regionId: String, regionName: String, strokeData: String, imagePath: String? = null) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "carving_${regionId}_${userId.hashCode().toUInt()}"
        val carving = Carving(
            id = id,
            userId = userId,
            regionId = regionId,
            regionName = regionName,
            imagePath = imagePath,
            strokeData = strokeData,
            createdAt = now
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
