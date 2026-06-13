package com.mapchina.domain.model

import kotlin.time.Instant

data class Footprint(
    val userId: String,
    val regionId: String,
    val level: FootprintLevel,
    val timestamp: Instant
)

enum class FootprintLevel {
    PASS_BY, SHORT_VISIT, DEEP;

    fun upgradeTo(newLevel: FootprintLevel): FootprintLevel =
        if (newLevel > this) newLevel else this

    companion object {
        fun resolveConflict(local: FootprintLevel, remote: FootprintLevel): FootprintLevel =
            if (local > remote) local else remote
    }
}
