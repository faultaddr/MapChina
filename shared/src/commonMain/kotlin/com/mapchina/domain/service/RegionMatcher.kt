package com.mapchina.domain.service

import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class RegionMatch(
    val province: Region?,
    val city: Region?,
    val district: Region?
)

class RegionMatcher(private val regionRepository: RegionRepository) {

    fun match(lat: Double, lng: Double): RegionMatch {
        val provinces = regionRepository.getRegionsByLevel(RegionLevel.PROVINCE)
        val cities = regionRepository.getRegionsByLevel(RegionLevel.CITY)
        val districts = regionRepository.getRegionsByLevel(RegionLevel.DISTRICT)

        val matchedDistrict = districts.firstOrNull { region ->
            val boundary = regionRepository.getRegionBoundary(region.id) ?: return@firstOrNull false
            pointInPolygon(lat, lng, boundary)
        }

        val matchedCity = if (matchedDistrict != null) {
            cities.firstOrNull { it.id == matchedDistrict.parentId }
        } else {
            cities.firstOrNull { region ->
                val boundary = regionRepository.getRegionBoundary(region.id) ?: return@firstOrNull false
                pointInPolygon(lat, lng, boundary)
            }
        }

        val matchedProvince = when {
            matchedDistrict != null -> {
                val cityParent = matchedCity
                if (cityParent != null) {
                    provinces.firstOrNull { it.id == cityParent.parentId }
                } else {
                    provinces.firstOrNull { matchedDistrict.id.startsWith(it.id.substring(0, 2)) }
                }
            }
            matchedCity != null -> {
                provinces.firstOrNull { it.id == matchedCity.parentId }
                    ?: provinces.firstOrNull { matchedCity.id.startsWith(it.id.substring(0, 2)) }
            }
            else -> {
                provinces.firstOrNull { region ->
                    val boundary = regionRepository.getRegionBoundary(region.id) ?: return@firstOrNull false
                    pointInPolygon(lat, lng, boundary)
                }
            }
        }

        return RegionMatch(
            province = matchedProvince,
            city = matchedCity,
            district = matchedDistrict
        )
    }

    private fun pointInPolygon(lat: Double, lng: Double, boundaryJson: String): Boolean {
        val points = try {
            val coords = kotlinx.serialization.json.Json.decodeFromString<JsonArray>(boundaryJson)
            coords.map { coord ->
                val arr = coord.jsonArray
                Pair(arr[1].jsonPrimitive.double, arr[0].jsonPrimitive.double) // [lng, lat] → (lat, lng)
            }
        } catch (_: Exception) {
            return false
        }
        if (points.size < 3) return false
        return rayCasting(lat, lng, points)
    }

    private fun rayCasting(lat: Double, lng: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1

        for (i in 0 until n) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            if ((yi > lng) != (yj > lng) && lat < (xj - xi) * (lng - yi) / (yj - yi) + xi) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
