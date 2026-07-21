package com.mapchina.ui.profile

import com.mapchina.sync.SyncStatus

data class ProfileSyncPresentation(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val actionEnabled: Boolean
)

fun profileSyncPresentation(
    isLoggedIn: Boolean,
    pendingSyncCount: Long,
    status: SyncStatus
): ProfileSyncPresentation {
    if (!isLoggedIn) {
        return ProfileSyncPresentation(
            title = "本地保存",
            subtitle = "登录后可在多台设备同步足迹与地图偏好",
            actionLabel = "登录",
            actionEnabled = true
        )
    }

    return when (status) {
        SyncStatus.SYNCING -> ProfileSyncPresentation(
            title = "正在同步",
            subtitle = "正在整理足迹、景点和地图偏好",
            actionLabel = "同步中",
            actionEnabled = false
        )

        SyncStatus.OFFLINE -> ProfileSyncPresentation(
            title = if (pendingSyncCount > 0) {
                "离线，${pendingSyncCount} 项待同步"
            } else {
                "当前离线"
            },
            subtitle = "数据已安全保存在本机",
            actionLabel = "重试",
            actionEnabled = true
        )

        SyncStatus.ERROR -> ProfileSyncPresentation(
            title = "同步未完成",
            subtitle = "数据仍保存在本机，可稍后重试",
            actionLabel = "重试",
            actionEnabled = true
        )

        SyncStatus.SYNCED -> ProfileSyncPresentation(
            title = "已同步",
            subtitle = "足迹与地图偏好已更新",
            actionLabel = "再次同步",
            actionEnabled = true
        )

        SyncStatus.IDLE -> if (pendingSyncCount > 0) {
            ProfileSyncPresentation(
                title = "${pendingSyncCount} 项待同步",
                subtitle = "本机记录等待上传到云端",
                actionLabel = "立即同步",
                actionEnabled = true
            )
        } else {
            ProfileSyncPresentation(
                title = "云端同步已连接",
                subtitle = "足迹与地图偏好将自动同步",
                actionLabel = "立即同步",
                actionEnabled = true
            )
        }
    }
}
