package com.mapchina.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BoundaryParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): List<List<Pair<Double, Double>>> {
        val element = json.parseToJsonElement(rawJson)
        if (element !is JsonObject) return emptyList()
        val root = element.jsonObject
        return when (root["type"]?.jsonPrimitive?.content) {
            "Feature" -> parseFeatureGeometry(root)
            "FeatureCollection" -> {
                val features = root["features"]?.jsonArray ?: return emptyList()
                if (features.isEmpty()) return emptyList()
                parseFeatureGeometry(features[0].jsonObject)
            }
            else -> emptyList()
        }
    }

    fun parseFlatCoords(boundaryJson: String): List<List<Pair<Double, Double>>> {
        val element = json.parseToJsonElement(boundaryJson)
        if (element !is JsonArray) return emptyList()
        val coords = mutableListOf<Pair<Double, Double>>()
        for (item in element.jsonArray) {
            val arr = item.jsonArray
            if (arr.size >= 2) {
                coords.add(arr[0].jsonPrimitive.double to arr[1].jsonPrimitive.double)
            }
        }
        return if (coords.isNotEmpty()) listOf(coords) else emptyList()
    }

    private fun parseFeatureGeometry(feature: JsonObject): List<List<Pair<Double, Double>>> {
        val geom = feature["geometry"]?.jsonObject ?: return emptyList()
        return when (geom["type"]?.jsonPrimitive?.content) {
            "Polygon" -> parsePolygonRings(geom["coordinates"]?.jsonArray ?: return emptyList())
            "MultiPolygon" -> parseMultiPolygonRings(geom["coordinates"]?.jsonArray ?: return emptyList())
            else -> emptyList()
        }
    }

    private fun parsePolygonRings(coordinates: JsonArray): List<List<Pair<Double, Double>>> {
        val rings = mutableListOf<List<Pair<Double, Double>>>()
        for (ring in coordinates) {
            rings.add(parseCoordinateRing(ring.jsonArray))
        }
        return rings
    }

    private fun parseMultiPolygonRings(coordinates: JsonArray): List<List<Pair<Double, Double>>> {
        val allRings = mutableListOf<List<Pair<Double, Double>>>()
        for (polygon in coordinates) {
            for (ring in polygon.jsonArray) {
                allRings.add(parseCoordinateRing(ring.jsonArray))
            }
        }
        return allRings
    }

    private fun parseCoordinateRing(ring: JsonArray): List<Pair<Double, Double>> {
        return ring.map { coord ->
            val arr = coord.jsonArray
            arr[0].jsonPrimitive.double to arr[1].jsonPrimitive.double
        }
    }
}
