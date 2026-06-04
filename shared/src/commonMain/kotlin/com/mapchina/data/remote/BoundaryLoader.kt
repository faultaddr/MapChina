package com.mapchina.data.remote

data class ChildRegionBoundary(
    val id: String,
    val name: String,
    val boundary: String
)

data class AttractionSeed(
    val id: String,
    val name: String,
    val regionId: String,
    val level: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val imageUrl: String? = null
)

expect class BoundaryLoader {
    fun loadBoundary(regionId: String): String?
    fun loadChildRegions(parentId: String): List<ChildRegionBoundary>?
    fun loadAttractionSeeds(): List<AttractionSeed>?
    fun getAvailableRegionIds(): List<String>
}
