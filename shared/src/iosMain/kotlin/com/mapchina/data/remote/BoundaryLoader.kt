package com.mapchina.data.remote

actual class BoundaryLoader {
    actual fun loadBoundary(regionId: String): String? {
        return null
    }

    actual fun loadChildRegions(parentId: String): List<ChildRegionBoundary>? {
        return null
    }

    actual fun loadAttractionSeeds(): List<AttractionSeed>? {
        return null
    }

    actual fun getAvailableRegionIds(): List<String> {
        return emptyList()
    }
}
