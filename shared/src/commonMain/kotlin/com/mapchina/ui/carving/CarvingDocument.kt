package com.mapchina.ui.carving.v2

import kotlinx.serialization.Serializable

const val CURRENT_CARVING_VERSION: Int = 2

@Serializable
data class CarvingDocument(
    val version: Int = CURRENT_CARVING_VERSION,
    val canvasAspectRatio: Float,
    val strokes: List<CarvingStroke>
)

@Serializable
data class CarvingStroke(
    val brushType: CarvingBrushType,
    val sizeFraction: Float,
    val colorArgb: Int,
    val points: List<CarvingPoint>
) {
    val brushSpec: CarvingBrushSpec
        get() = CarvingBrushSpec(brushType, sizeFraction, colorArgb)
}

@Serializable
data class CarvingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val elapsedTimeMillis: Long
)

data class CarvingBrushSpec(
    val type: CarvingBrushType = CarvingBrushType.MONUMENTAL,
    val sizeFraction: Float,
    val colorArgb: Int
)

@Serializable
enum class CarvingBrushType(val label: String) {
    IRON_CHISEL("铁錾"),
    MONUMENTAL("榜书"),
    WEATHERED("风化")
}

sealed interface CarvingDecodeResult {
    data class Success(
        val document: CarvingDocument,
        val sourceVersion: Int
    ) : CarvingDecodeResult

    data object Empty : CarvingDecodeResult

    data class Invalid(val reason: String) : CarvingDecodeResult
}
