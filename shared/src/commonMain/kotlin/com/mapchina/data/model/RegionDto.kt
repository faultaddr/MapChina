package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegionDto(
    val id: String,
    val name: String,
    val level: RegionLevel,
    val parentId: String? = null
)

@Serializable
enum class RegionLevel {
    PROVINCE, CITY, DISTRICT
}
