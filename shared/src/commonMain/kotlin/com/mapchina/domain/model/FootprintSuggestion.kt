package com.mapchina.domain.model

import kotlin.time.Clock

enum class FootprintSuggestionSource {
    LOCATION,
    ATTRACTION_VISIT,
    PHOTO,
    MANUAL_SEARCH
}

enum class FootprintSuggestionStatus {
    PENDING,
    CONFIRMED,
    DISMISSED
}

data class FootprintSuggestion(
    val id: String,
    val source: FootprintSuggestionSource,
    val regionId: String,
    val regionName: String,
    val parentPath: String,
    val evidenceLabel: String,
    val suggestedLevel: FootprintLevel?,
    val confidenceLabel: String,
    val createdAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val status: FootprintSuggestionStatus = FootprintSuggestionStatus.PENDING
)
