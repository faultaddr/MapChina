@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.mapchina.ui.carving

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SerializableStroke(
    val inputs: List<SerializableInput>,
    val brushSize: Float,
    val brushColorArgb: Int,
    val brushType: String
)

@Serializable
data class SerializableInput(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val elapsedTimeMillis: Long
)

private val json = Json { ignoreUnknownKeys = true }

fun serializeStrokes(strokes: List<Stroke>, brushType: CarvingBrushType, brushColorArgb: Int): String {
    val serializable = strokes.map { stroke ->
        val inputs = stroke.inputs
        val inputList = (0 until inputs.size).map { i ->
            val input: StrokeInput = inputs.get(i)
            val p = input.pressure
            SerializableInput(
                x = input.x,
                y = input.y,
                pressure = if (p > 0f) p else 0.5f,
                elapsedTimeMillis = input.elapsedTimeMillis
            )
        }
        SerializableStroke(
            inputs = inputList,
            brushSize = stroke.brush.size,
            brushColorArgb = stroke.brush.colorIntArgb,
            brushType = brushType.name
        )
    }
    return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(SerializableStroke.serializer()), serializable)
}

fun deserializeStrokes(data: String): List<Stroke> {
    if (data.isBlank()) return emptyList()
    val serializable = json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(SerializableStroke.serializer()), data)
    return serializable.map { ss ->
        val mutableBatch = MutableStrokeInputBatch()
        for (si in ss.inputs) {
            val pressure = if (si.pressure > 0f) si.pressure else 0.5f
            mutableBatch.add(
                InputToolType.TOUCH,
                si.x,
                si.y,
                si.elapsedTimeMillis,
                pressure
            )
        }
        val brush = Brush.Builder()
            .setFamily(BrushFamily())
            .setSize(ss.brushSize)
            .setColorIntArgb(ss.brushColorArgb)
            .setEpsilon(0.1f)
            .build()
        Stroke(brush, mutableBatch.toImmutable())
    }
}
