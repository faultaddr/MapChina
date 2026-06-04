package com.mapchina.data.remote

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

actual class AttractionDetailProvider(private val context: Context) {

    private val cache: Map<String, AttractionDetail> by lazy { loadAll() }

    private fun loadAll(): Map<String, AttractionDetail> {
        val result = mutableMapOf<String, AttractionDetail>()
        try {
            val json = context.assets.open("attraction_details.json")
                .bufferedReader().use { it.readText() }
            val array = Json.parseToJsonElement(json).jsonArray
            for (item in array) {
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val imageUrls = obj["iu"]?.jsonArray?.mapNotNull {
                    (it as? JsonElement)?.jsonPrimitive?.content?.replace("http://", "https://")
                } ?: emptyList()
                val detail = AttractionDetail(
                    imageUrls = imageUrls,
                    rating = obj["r"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                    cost = obj["c"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                    openTime = obj["ot"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                    tel = obj["t"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                    website = obj["w"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                    appointmentUrl = obj["au"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                )
                result[id] = detail
            }
        } catch (_: Exception) {
        }
        return result
    }

    actual fun getAttractionDetail(attractionId: String): AttractionDetail? {
        return cache[attractionId]
    }
}
