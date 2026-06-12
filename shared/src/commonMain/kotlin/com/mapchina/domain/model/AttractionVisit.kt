package com.mapchina.domain.model

import kotlin.time.Instant

data class AttractionVisit(
    val userId: String,
    val attractionId: String,
    val level: FootprintLevel,
    val timestamp: Instant,
    val note: String? = null
)
