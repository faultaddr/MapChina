package com.mapchina.domain.model

data class Region(
    val id: String,
    val name: String,
    val level: RegionLevel,
    val parentId: String? = null
)

enum class RegionLevel {
    PROVINCE, CITY, DISTRICT
}
