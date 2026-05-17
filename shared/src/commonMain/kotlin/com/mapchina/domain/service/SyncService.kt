package com.mapchina.domain.service

import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.FootprintLevel

object SyncService {

    fun resolveFootprintConflict(local: FootprintDto, remote: FootprintDto): FootprintDto {
        val resolvedLevel = FootprintLevel.resolveConflict(local.level, remote.level)
        val latestTimestamp = maxOf(local.timestamp, remote.timestamp)
        return FootprintDto(
            userId = local.userId,
            regionId = local.regionId,
            level = resolvedLevel,
            timestamp = latestTimestamp
        )
    }
}
