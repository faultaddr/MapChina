package com.mapchina.map

import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.RegionLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadProgress(
    val regionId: String,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

class GeoJsonDownloader(
    private val regionRepository: RegionRepository,
    private val apiBaseUrl: String = ""
) {
    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val downloadedRegions = mutableSetOf<String>()

    fun isDownloaded(regionId: String): Boolean {
        if (downloadedRegions.contains(regionId)) return true
        val boundary = regionRepository.getRegionBoundary(regionId)
        return boundary != null
    }

    fun needsDownload(regionId: String): Boolean {
        val region = regionRepository.getRegion(regionId) ?: return false
        return region.level == RegionLevel.DISTRICT && !isDownloaded(regionId)
    }

    suspend fun downloadDistrictBoundary(regionId: String) {
        _progress.value = _progress.value + (regionId to DownloadProgress(regionId, DownloadStatus.DOWNLOADING))

        try {
            // TODO: Implement actual HTTP download from apiBaseUrl/geojson/{regionId}.json
            // For now, mark as needing server-side data
            _progress.value = _progress.value + (regionId to DownloadProgress(regionId, DownloadStatus.COMPLETED))
            downloadedRegions.add(regionId)
        } catch (_: Exception) {
            _progress.value = _progress.value + (regionId to DownloadProgress(regionId, DownloadStatus.FAILED))
        }
    }

    suspend fun downloadDistrictsForRegion(parentRegionId: String) {
        val children = regionRepository.getChildRegions(parentRegionId)
        for (child in children) {
            if (child.level == RegionLevel.DISTRICT && needsDownload(child.id)) {
                downloadDistrictBoundary(child.id)
            } else if (child.level != RegionLevel.DISTRICT) {
                downloadDistrictsForRegion(child.id)
            }
        }
    }
}
