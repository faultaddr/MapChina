package com.mapchina.domain.model

data class ProvinceConquestInfo(
    val provinceId: String,
    val provinceName: String,
    val visitedAttractions: Int,
    val totalAttractions: Int,
    val visitedCities: Int,
    val totalCities: Int,
    val visitedDistricts: Int,
    val totalDistricts: Int,
    val hasVisitBadge: Boolean,
    val hasCompleteBadge: Boolean
) {
    val completionPercent: Int get() {
        if (totalCities == 0) return 0
        return (visitedCities * 100) / totalCities
    }

    val colorLevel: Int get() = when {
        visitedCities == 0 -> 0
        completionPercent < 30 -> 1
        completionPercent < 70 -> 2
        completionPercent < 100 -> 3
        else -> 4
    }
}
