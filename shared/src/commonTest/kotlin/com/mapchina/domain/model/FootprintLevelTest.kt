package com.mapchina.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FootprintLevelTest {

    @Test
    fun upgradeFromPassByToShortVisit_succeeds() {
        val result = FootprintLevel.PASS_BY.upgradeTo(FootprintLevel.SHORT_VISIT)
        assertEquals(FootprintLevel.SHORT_VISIT, result)
    }

    @Test
    fun upgradeFromPassByToDeep_succeeds() {
        val result = FootprintLevel.PASS_BY.upgradeTo(FootprintLevel.DEEP)
        assertEquals(FootprintLevel.DEEP, result)
    }

    @Test
    fun downgradeFromDeepToPassBy_ignored() {
        val result = FootprintLevel.DEEP.upgradeTo(FootprintLevel.PASS_BY)
        assertEquals(FootprintLevel.DEEP, result) // 不降级，保持原级
    }

    @Test
    fun downgradeFromDeepToShortVisit_ignored() {
        val result = FootprintLevel.DEEP.upgradeTo(FootprintLevel.SHORT_VISIT)
        assertEquals(FootprintLevel.DEEP, result)
    }

    @Test
    fun sameLevel_noChange() {
        val result = FootprintLevel.SHORT_VISIT.upgradeTo(FootprintLevel.SHORT_VISIT)
        assertEquals(FootprintLevel.SHORT_VISIT, result)
    }

    @Test
    fun resolveConflict_higherLevelWins() {
        val local = FootprintLevel.PASS_BY
        val remote = FootprintLevel.DEEP
        val resolved = FootprintLevel.resolveConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved)
    }

    @Test
    fun resolveConflict_localHigherWins() {
        val local = FootprintLevel.DEEP
        val remote = FootprintLevel.SHORT_VISIT
        val resolved = FootprintLevel.resolveConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved)
    }

    @Test
    fun resolveConflict_sameLevel_returnsSame() {
        val resolved = FootprintLevel.resolveConflict(FootprintLevel.PASS_BY, FootprintLevel.PASS_BY)
        assertEquals(FootprintLevel.PASS_BY, resolved)
    }

    @Test
    fun levelOrdering_isCorrect() {
        assertTrue(FootprintLevel.DEEP > FootprintLevel.SHORT_VISIT)
        assertTrue(FootprintLevel.SHORT_VISIT > FootprintLevel.PASS_BY)
        assertTrue(FootprintLevel.DEEP > FootprintLevel.PASS_BY)
    }
}
