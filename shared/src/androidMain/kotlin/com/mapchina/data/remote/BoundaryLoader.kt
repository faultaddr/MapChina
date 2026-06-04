package com.mapchina.data.remote

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

actual class BoundaryLoader(private val context: Context) {

    actual fun loadBoundary(regionId: String): String? {
        return try {
            val fileName = "boundaries/$regionId.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            parseGeoJsonToCoordinates(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    actual fun loadAttractionSeeds(): List<AttractionSeed>? {
        return try {
            val jsonString = context.assets.open("attractions.json").bufferedReader().use { it.readText() }
            parseAttractionSeeds(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    actual fun loadChildRegions(parentId: String): List<ChildRegionBoundary>? {
        return try {
            val fileName = "districts/$parentId.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            parseChildRegions(jsonString, parentId)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseChildRegions(jsonString: String, parentId: String): List<ChildRegionBoundary>? {
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonArray
            val result = mutableListOf<ChildRegionBoundary>()
            for (item in json) {
                val obj = item.jsonObject
                val adcode = obj["adcode"]?.jsonPrimitive?.content ?: continue
                val name = obj["name"]?.jsonPrimitive?.content ?: continue
                val geometry = obj["geometry"]?.jsonObject ?: continue
                val boundary = geometryToCoordinates(geometry) ?: continue
                result.add(ChildRegionBoundary(adcode, name, boundary))
            }
            if (result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private fun geometryToCoordinates(geometry: Map<String, kotlinx.serialization.json.JsonElement>): String? {
        val geomType = geometry["type"]?.jsonPrimitive?.content ?: return null
        val coordinates = geometry["coordinates"]?.jsonArray ?: return null

        return when (geomType) {
            "Polygon" -> {
                val ring = coordinates.getOrNull(0)?.jsonArray ?: return null
                ringToCoordinates(ring)
            }
            "MultiPolygon" -> {
                val largestPolygon = findLargestPolygon(coordinates) ?: return null
                val ring = largestPolygon.getOrNull(0)?.jsonArray ?: return null
                ringToCoordinates(ring)
            }
            else -> null
        }
    }

    private fun parseGeoJsonToCoordinates(jsonString: String): String? {
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val type = json["type"]?.jsonPrimitive?.content ?: return null

            val geometry = when (type) {
                "Feature" -> json["geometry"]?.jsonObject
                "FeatureCollection" -> {
                    val features = json["features"]?.jsonArray ?: return null
                    if (features.isEmpty()) return null
                    features[0].jsonObject["geometry"]?.jsonObject
                }
                else -> null
            } ?: return null

            geometryToCoordinates(geometry)
        } catch (_: Exception) {
            null
        }
    }

    private fun findLargestPolygon(coordinates: JsonArray): JsonArray? {
        var largest: JsonArray? = null
        var largestSize = 0
        for (polygon in coordinates) {
            val ring = polygon.jsonArray.getOrNull(0)?.jsonArray ?: continue
            if (ring.size > largestSize) {
                largestSize = ring.size
                largest = polygon.jsonArray
            }
        }
        return largest
    }

    private fun ringToCoordinates(ring: JsonArray): String {
        val result = StringBuilder("[")
        for (i in 0 until ring.size) {
            val point = ring[i].jsonArray
            val lng = point[0].jsonPrimitive.content
            val lat = point[1].jsonPrimitive.content
            if (i > 0) result.append(",")
            result.append("[$lng,$lat]")
        }
        result.append("]")
        return result.toString()
    }

    private fun parseAttractionSeeds(jsonString: String): List<AttractionSeed>? {
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonArray
            // Load image URL map from attraction_details.json
            val imageMap = loadAttractionImageMap()
            val result = mutableListOf<AttractionSeed>()
            for (item in json) {
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                result.add(AttractionSeed(
                    id = id,
                    name = obj["name"]?.jsonPrimitive?.content ?: continue,
                    regionId = obj["regionId"]?.jsonPrimitive?.content ?: continue,
                    level = obj["level"]?.jsonPrimitive?.content ?: continue,
                    latitude = obj["latitude"]?.jsonPrimitive?.content?.toDouble() ?: continue,
                    longitude = obj["longitude"]?.jsonPrimitive?.content?.toDouble() ?: continue,
                    description = obj["description"]?.jsonPrimitive?.content ?: "",
                    imageUrl = imageMap[id]
                ))
            }
            if (result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAttractionImageMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val detailJson = context.assets.open("attraction_details.json")
                .bufferedReader().use { it.readText() }
            val array = Json.parseToJsonElement(detailJson).jsonArray
            for (item in array) {
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val firstImage = obj["iu"]?.jsonArray?.firstOrNull()
                    ?.jsonPrimitive?.content?.replace("http://", "https://")
                if (firstImage != null) {
                    map[id] = firstImage
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    actual fun getAvailableRegionIds(): List<String> {
        return try {
            context.assets.list("boundaries")?.map { it.replace(".json", "") } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
