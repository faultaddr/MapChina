package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FootprintDto(
    val userId: String,
    val regionId: String,
    val level: FootprintLevel,
    val timestamp: Long
)

@Serializable
enum class FootprintLevel {
    PASS_BY, SHORT_VISIT, DEEP
}
