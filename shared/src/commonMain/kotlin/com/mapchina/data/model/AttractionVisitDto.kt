package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AttractionVisitDto(
    val userId: String,
    val attractionId: String,
    val level: FootprintLevel,
    val timestamp: Long,
    val note: String? = null
)
