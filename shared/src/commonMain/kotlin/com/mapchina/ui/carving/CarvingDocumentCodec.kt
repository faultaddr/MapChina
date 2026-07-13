package com.mapchina.ui.carving.v2

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object CarvingDocumentCodec {
    private const val MIN_SIZE_FRACTION = 0.005f
    private const val MAX_SIZE_FRACTION = 0.30f

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun encode(document: CarvingDocument): String {
        require(document.version == CURRENT_CARVING_VERSION) {
            "Only V2 carving documents can be encoded"
        }
        require(document.canvasAspectRatio.isFinite()) {
            "canvasAspectRatio must be finite"
        }
        return json.encodeToString(document.normalized())
    }

    @Suppress("UNUSED_PARAMETER")
    fun decode(data: String, previewAspectRatio: Float? = null): CarvingDecodeResult {
        if (data.isBlank()) return CarvingDecodeResult.Empty

        return try {
            val element = json.parseToJsonElement(data)
            if (element !is JsonObject) {
                return CarvingDecodeResult.Invalid("Expected a V2 document object")
            }

            val document = json.decodeFromJsonElement<CarvingDocument>(element)
            if (document.version != CURRENT_CARVING_VERSION) {
                return CarvingDecodeResult.Invalid("Unsupported carving version: ${document.version}")
            }
            if (!document.canvasAspectRatio.isFinite()) {
                return CarvingDecodeResult.Invalid("canvasAspectRatio must be finite")
            }

            CarvingDecodeResult.Success(
                document = document.normalized(),
                sourceVersion = document.version
            )
        } catch (error: SerializationException) {
            CarvingDecodeResult.Invalid(error.message ?: "Invalid carving document")
        } catch (error: IllegalArgumentException) {
            CarvingDecodeResult.Invalid(error.message ?: "Invalid carving document")
        }
    }

    private fun CarvingDocument.normalized(): CarvingDocument {
        return copy(
            strokes = strokes.mapNotNull { it.normalizedOrNull() }
        )
    }

    private fun CarvingStroke.normalizedOrNull(): CarvingStroke? {
        val validPoints = points.filter { point ->
            point.x.isFinite() && point.y.isFinite() && point.pressure.isFinite()
        }
        if (validPoints.size < 2) return null

        var lastElapsedTimeMillis = Long.MIN_VALUE
        val normalizedPoints = validPoints.map { point ->
            val elapsedTimeMillis = maxOf(lastElapsedTimeMillis, point.elapsedTimeMillis)
            lastElapsedTimeMillis = elapsedTimeMillis
            point.copy(
                x = point.x.coerceIn(0f, 1f),
                y = point.y.coerceIn(0f, 1f),
                pressure = point.pressure.coerceIn(0f, 1f),
                elapsedTimeMillis = elapsedTimeMillis
            )
        }

        return copy(
            sizeFraction = sizeFraction.coerceIn(MIN_SIZE_FRACTION, MAX_SIZE_FRACTION),
            points = normalizedPoints
        )
    }
}
