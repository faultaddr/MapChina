package com.mapchina.ui.profile

import com.mapchina.sync.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProfileSyncPresentationTest {

    @Test
    fun loggedOut_isClearlyLocalOnly() {
        assertEquals(
            ProfileSyncPresentation(
                title = "本地保存",
                subtitle = "登录后可在多台设备同步足迹与地图偏好",
                actionLabel = "登录",
                actionEnabled = true
            ),
            profileSyncPresentation(false, 0L, SyncStatus.IDLE)
        )
    }

    @Test
    fun loggedInPending_surfacesQueueCount() {
        assertEquals(
            "3 项待同步",
            profileSyncPresentation(true, 3L, SyncStatus.IDLE).title
        )
    }

    @Test
    fun syncing_disablesDuplicateAction() {
        val result = profileSyncPresentation(true, 2L, SyncStatus.SYNCING)

        assertEquals("正在同步", result.title)
        assertEquals("同步中", result.actionLabel)
        assertFalse(result.actionEnabled)
    }

    @Test
    fun offline_keepsLocalDataMessage() {
        val result = profileSyncPresentation(true, 2L, SyncStatus.OFFLINE)

        assertEquals("离线，2 项待同步", result.title)
        assertEquals("数据已安全保存在本机", result.subtitle)
        assertEquals("重试", result.actionLabel)
    }

    @Test
    fun synced_confirmsCompletion() {
        assertEquals(
            "已同步",
            profileSyncPresentation(true, 0L, SyncStatus.SYNCED).title
        )
    }
}
