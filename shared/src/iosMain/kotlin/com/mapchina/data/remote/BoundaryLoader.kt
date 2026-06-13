package com.mapchina.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual class BoundaryLoader {

    private val bundle = NSBundle.mainBundle

    actual fun loadBoundary(regionId: String): String? {
        return try {
            val path = bundle.pathForResource(regionId, "json", "boundaries") ?: return null
            val jsonString = readFile(path) ?: return null
            parseGeoJsonToCoordinates(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    actual fun loadChildRegions(parentId: String): List<ChildRegionBoundary>? {
        return try {
            val path = bundle.pathForResource(parentId, "json", "districts") ?: return null
            val jsonString = readFile(path) ?: return null
            parseChildRegions(jsonString, parentId)
        } catch (_: Exception) {
            null
        }
    }

    actual fun loadAttractionSeeds(): List<AttractionSeed>? {
        return try {
            val path = bundle.pathForResource("attractions", "json") ?: return null
            val jsonString = readFile(path) ?: return null
            val imageMap = loadAttractionImageMap()
            val json = Json.parseToJsonElement(jsonString).jsonArray
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

    actual fun getAvailableRegionIds(): List<String> {
        return try {
            val boundariesPath = bundle.resourcePath + "/boundaries"
            val contents = NSFileManager.defaultManager.contentsOfDirectoryAtPath(boundariesPath, null)
                ?: return emptyList()
            contents.filterIsInstance<String>().map { it.replace(".json", "") }
        } catch (_: Exception) {
            emptyList()
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

    private fun loadAttractionImageMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val path = bundle.pathForResource("attraction_details", "json") ?: return map
            val detailJson = readFile(path) ?: return map
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

    private fun readFile(path: String): String? {
        return try {
            val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
            NSString.create(data, NSUTF8StringEncoding)?.toString()
        } catch (_: Exception) {
            null
        }
    }
}
