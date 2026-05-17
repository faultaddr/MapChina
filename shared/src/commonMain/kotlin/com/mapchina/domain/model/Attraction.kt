package com.mapchina.domain.model

data class Attraction(
    val id: String,
    val name: String,
    val regionId: String,
    val level: AttractionLevel,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null
)

enum class AttractionLevel {
    A5, A4
}
