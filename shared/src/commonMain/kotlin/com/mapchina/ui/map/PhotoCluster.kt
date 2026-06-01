package com.mapchina.ui.map

import com.mapchina.platform.DevicePhoto

data class PhotoCluster(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val photos: List<DevicePhoto>,
    val coverPath: String
) {
    val count: Int get() = photos.size
}

object PhotoClusterer {
    private const val CLUSTER_DISTANCE = 0.015

    fun cluster(photos: List<DevicePhoto>): List<PhotoCluster> {
        if (photos.isEmpty()) return emptyList()
        val clusters = mutableListOf<PhotoCluster>()
        val visited = mutableSetOf<Int>()

        for (i in photos.indices) {
            if (i in visited) continue
            val seed = photos[i]
            val group = mutableListOf(seed)
            visited.add(i)

            for (j in (i + 1)..photos.lastIndex) {
                if (j in visited) continue
                val candidate = photos[j]
                val dist = kotlin.math.sqrt(
                    (seed.latitude - candidate.latitude) * (seed.latitude - candidate.latitude) +
                    (seed.longitude - candidate.longitude) * (seed.longitude - candidate.longitude)
                )
                if (dist < CLUSTER_DISTANCE) {
                    group.add(candidate)
                    visited.add(j)
                }
            }

            val avgLat = group.map { it.latitude }.average()
            val avgLng = group.map { it.longitude }.average()
            val cover = group.maxByOrNull { it.dateTaken } ?: group.first()
            clusters.add(PhotoCluster(
                id = "cluster_${seed.id}",
                latitude = avgLat,
                longitude = avgLng,
                photos = group,
                coverPath = cover.filePath
            ))
        }
        return clusters
    }
}
