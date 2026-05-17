package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttractionDto(
    val id: String,
    val name: String,
    val regionId: String,
    val level: AttractionLevel,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null
)

@Serializable
data class LatLngDto(
    val lat: Double,
    val lng: Double
)

@Serializable
enum class AttractionLevel {
    A5, A4
}
