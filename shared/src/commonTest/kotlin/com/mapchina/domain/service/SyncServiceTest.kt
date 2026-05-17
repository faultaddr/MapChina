package com.mapchina.domain.service

import com.mapchina.data.model.FootprintDto
import com.mapchina.data.model.FootprintLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncServiceTest {

    @Test
    fun resolveFootprintConflict_higherLevelWins() {
        val local = FootprintDto("u1", "510000", FootprintLevel.PASS_BY, 1000L)
        val remote = FootprintDto("u1", "510000", FootprintLevel.DEEP, 2000L)
        val resolved = SyncService.resolveFootprintConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved.level)
    }

    @Test
    fun resolveFootprintConflict_localHigherWins() {
        val local = FootprintDto("u1", "510000", FootprintLevel.DEEP, 1000L)
        val remote = FootprintDto("u1", "510000", FootprintLevel.PASS_BY, 2000L)
        val resolved = SyncService.resolveFootprintConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved.level)
    }

    @Test
    fun resolveFootprintConflict_sameLevel_returnsSame() {
        val local = FootprintDto("u1", "510000", FootprintLevel.SHORT_VISIT, 1000L)
        val remote = FootprintDto("u1", "510000", FootprintLevel.SHORT_VISIT, 2000L)
        val resolved = SyncService.resolveFootprintConflict(local, remote)
        assertEquals(FootprintLevel.SHORT_VISIT, resolved.level)
    }

    @Test
    fun resolveFootprintConflict_usesLatestTimestamp() {
        val local = FootprintDto("u1", "510000", FootprintLevel.DEEP, 1000L)
        val remote = FootprintDto("u1", "510000", FootprintLevel.DEEP, 3000L)
        val resolved = SyncService.resolveFootprintConflict(local, remote)
        assertEquals(3000L, resolved.timestamp)
    }
}
