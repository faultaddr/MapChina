package com.mapchina.domain.model

data class Attraction(
    val id: String,
    val name: String,
    val regionId: String,
    val level: AttractionLevel,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val imageUrl: String? = null,
    val isCustom: Boolean = false,
    val userId: String? = null
)

enum class AttractionLevel {
    A5, A4, CUSTOM
}
