package com.mapchina.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.map.MapTheme
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

private val ProfileSurfaceShape = RoundedCornerShape(8.dp)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
    settingsRepository: SettingsRepository? = null,
    modifier: Modifier = Modifier
) {
    val profile by (viewModel?.profile?.collectAsState()
        ?: remember { mutableStateOf(ProfileUi("未登录", null, null)) })
    val isLoggedIn by (viewModel?.isLoggedIn?.collectAsState()
        ?: remember { mutableStateOf(false) })
    val bottomPadding = LocalScaffoldBottomPadding.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel?.loadProfile()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 20.dp,
            end = 16.dp,
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ProfileHeader() }
        item {
            AccountAndSyncSection(
                profile = profile,
                isLoggedIn = isLoggedIn,
                syncPresentation = profileSyncPresentation(
                    isLoggedIn = isLoggedIn,
                    pendingSyncCount = profile.pendingSyncCount,
                    status = profile.syncStatus
                ),
                onAction = if (isLoggedIn) onSyncNow else onNavigateToLogin
            )
        }
        item { FootprintSettingsSection(settingsRepository) }
        item { MapAppearanceSection(settingsRepository) }
        item {
            AboutSection(
                isLoggedIn = isLoggedIn,
                onLogout = { viewModel?.logout() }
            )
        }
    }
}

@Composable
private fun ProfileHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "我的",
            color = MapChinaColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "账号、同步与地图偏好",
            color = MapChinaColors.TextSecondary,
            style = MapChinaTypography.Body
        )
    }
}

@Composable
private fun AccountAndSyncSection(
    profile: ProfileUi,
    isLoggedIn: Boolean,
    syncPresentation: ProfileSyncPresentation,
    onAction: (() -> Unit)?
) {
    ProfileSection(title = "账号与同步") {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MapChinaColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn) {
                        Text(
                            text = profile.nickname.take(1),
                            color = MapChinaColors.Primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MapChinaColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.nickname,
                        color = MapChinaColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (isLoggedIn) {
                            profile.phone ?: "账户已连接"
                        } else {
                            "当前记录保存在这台设备"
                        },
                        color = MapChinaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            HorizontalDivider(color = MapChinaColors.BorderSubtle)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = MapChinaColors.AccentGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = syncPresentation.title,
                        color = MapChinaColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = syncPresentation.subtitle,
                        color = MapChinaColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                TextButton(
                    onClick = { onAction?.invoke() },
                    enabled = syncPresentation.actionEnabled
                ) {
                    Text(
                        text = syncPresentation.actionLabel,
                        color = if (syncPresentation.actionEnabled) {
                            MapChinaColors.Primary
                        } else {
                            MapChinaColors.TextTertiary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FootprintSettingsSection(settingsRepository: SettingsRepository?) {
    val haptic = LocalHapticFeedback.current
    var photoMarkersVisible by remember {
        mutableStateOf(settingsRepository?.getString("photo_markers_visible") != "false")
    }
    var autoMarkFootprint by remember {
        mutableStateOf(settingsRepository?.getString("auto_mark_footprint") != "false")
    }

    ProfileSection(title = "足迹记录") {
        Column {
            PreferenceSwitchRow(
                icon = Icons.Default.PhotoLibrary,
                title = Copy.PHOTO_MARKERS,
                subtitle = "仅在本机读取照片中的位置信息",
                checked = photoMarkersVisible,
                onCheckedChange = { enabled ->
                    haptic.perform(HapticType.SELECTION)
                    photoMarkersVisible = enabled
                    settingsRepository?.setString("photo_markers_visible", enabled.toString())
                }
            )
            HorizontalDivider(
                color = MapChinaColors.BorderSubtle,
                modifier = Modifier.padding(start = 52.dp)
            )
            PreferenceSwitchRow(
                icon = Icons.Default.DirectionsWalk,
                title = Copy.AUTO_FOOTPRINT,
                subtitle = "只生成建议，由你确认后点亮",
                checked = autoMarkFootprint,
                onCheckedChange = { enabled ->
                    haptic.perform(HapticType.SELECTION)
                    autoMarkFootprint = enabled
                    settingsRepository?.setString("auto_mark_footprint", enabled.toString())
                }
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MapChinaColors.Primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MapChinaColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = MapChinaColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MapChinaColors.Primary,
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
private fun MapAppearanceSection(settingsRepository: SettingsRepository?) {
    ProfileSection(title = "地图外观") {
        MapThemeSelector(settingsRepository = settingsRepository)
    }
}

@Composable
private fun AboutSection(
    isLoggedIn: Boolean,
    onLogout: () -> Unit
) {
    ProfileSection(title = "关于") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = MapChinaColors.Primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MapChina",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "版本 1.0.1",
                    color = MapChinaColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (isLoggedIn) {
                val haptic = LocalHapticFeedback.current
                TextButton(onClick = {
                    haptic.perform(HapticType.WARNING)
                    onLogout()
                }) {
                    Text("退出登录", color = MapChinaColors.Error)
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MapChinaColors.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ProfileSurfaceShape,
            color = MapChinaColors.SurfaceElevated,
            shadowElevation = 1.dp,
            content = content
        )
    }
}

@Composable
private fun MapThemeSelector(settingsRepository: SettingsRepository?) {
    val haptic = LocalHapticFeedback.current
    var selectedTheme by remember {
        mutableStateOf(MapTheme.fromName(settingsRepository?.getString("map_theme")))
    }

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapTheme.entries.chunked(3).forEach { themes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
                    val isSelected = theme == selectedTheme
                    val borderColor = if (isSelected) MapChinaColors.Primary else MapChinaColors.BorderSubtle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 76.dp)
                            .clip(ProfileSurfaceShape)
                            .background(MapChinaColors.Background)
                            .border(1.dp, borderColor, ProfileSurfaceShape)
                            .clickable {
                                haptic.perform(HapticType.SELECTION)
                                selectedTheme = theme
                                settingsRepository?.setString("map_theme", theme.name)
                            }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(theme.oceanColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (theme == MapTheme.STARRY_NIGHT) {
                                    Icons.Default.Star
                                } else {
                                    Icons.Default.Landscape
                                },
                                contentDescription = null,
                                tint = if (theme == MapTheme.STARRY_NIGHT) {
                                    Color.White.copy(alpha = 0.82f)
                                } else {
                                    MapChinaColors.Primary.copy(alpha = 0.72f)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = theme.displayName,
                            color = if (isSelected) MapChinaColors.Primary else MapChinaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelBadgeIcon(level: Int, modifier: Modifier = Modifier, useLightIcon: Boolean = false) {
    val levelDef = com.mapchina.domain.model.LEVEL_DEFINITIONS.find { it.level == level }
    val icon: ImageVector = when (levelDef?.badgeIconName) {
        "DirectionsWalk" -> Icons.Default.DirectionsWalk
        "NearMe" -> Icons.Default.NearMe
        "Terrain" -> Icons.Default.Terrain
        "Landscape" -> Icons.Default.Landscape
        "Map" -> Icons.Default.Map
        "Explore" -> Icons.Default.Explore
        "EmojiEvents" -> Icons.Default.EmojiEvents
        "Star" -> Icons.Default.Star
        "MilitaryTech" -> Icons.Default.MilitaryTech
        "WorkspacePremium" -> Icons.Default.WorkspacePremium
        else -> Icons.Default.Person
    }
    val backgroundColor = when {
        level >= 8 -> MapChinaColors.AccentGold.copy(alpha = 0.12f)
        level >= 5 -> MapChinaColors.Primary.copy(alpha = 0.12f)
        else -> MapChinaColors.Primary.copy(alpha = 0.08f)
    }
    val tint = if (level >= 8) MapChinaColors.AccentGold else MapChinaColors.Primary
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (useLightIcon) Color.White.copy(alpha = 0.2f) else backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = levelDef?.title ?: "等级",
            tint = if (useLightIcon) Color.White else tint,
            modifier = Modifier.size(if (level >= 8) 36.dp else 32.dp)
        )
    }
}
