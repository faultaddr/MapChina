package com.mapchina.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.map.MapTheme
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.LEVEL_DEFINITIONS
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.achievement.AchievementWithProgress
import com.mapchina.ui.stats.StatsUi
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaMotion
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel? = null,
    achievementViewModel: AchievementViewModel? = null,
    statsViewModel: StatsViewModel? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToJournals: (() -> Unit)? = null,
    onNavigateToBadgeWall: (() -> Unit)? = null,
    onNavigateToProvinceConquest: (() -> Unit)? = null,
    onNavigateToAtlas: (() -> Unit)? = null,
    onNavigateToCarvings: (() -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    onLoginSuccess: (() -> Unit)? = null,
    settingsRepository: SettingsRepository? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel?.loadProfile() }
    val profile by (viewModel?.profile?.collectAsState() ?: remember { mutableStateOf(ProfileUi("未登录", null, null)) })
    val isLoggedIn by (viewModel?.isLoggedIn?.collectAsState() ?: remember { mutableStateOf(false) })
    val achievementUi by (achievementViewModel?.ui?.collectAsState() ?: remember { mutableStateOf(com.mapchina.ui.achievement.AchievementUi()) })
    val stats by (statsViewModel?.stats?.collectAsState() ?: remember { mutableStateOf(StatsUi()) })

    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            statsViewModel?.refreshStats()
            achievementViewModel?.refresh()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User info card - 白底+碧玉点缀
        item {
            UserInfoCard(
                profile = profile,
                isLoggedIn = isLoggedIn,
                onNavigateToLogin = onNavigateToLogin
            )
        }

        // 统计数据 - 大字醒目
        item {
            StatsBar(stats = stats)
        }

        // Feature grid - 简约图标+名称
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureEntry(
                        icon = Icons.Default.Book,
                        title = Copy.FEATURE_JOURNAL_TITLE,
                        tint = MapChinaColors.Primary,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToJournals?.invoke() }
                    )
                    FeatureEntry(
                        icon = Icons.Default.WorkspacePremium,
                        title = Copy.FEATURE_BADGE_TITLE,
                        tint = MapChinaColors.AccentGold,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToBadgeWall?.invoke() }
                    )
                    FeatureEntry(
                        icon = Icons.Default.Map,
                        title = Copy.FEATURE_PROVINCE_TITLE,
                        tint = MapChinaColors.FootprintDeep,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToProvinceConquest?.invoke() }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureEntry(
                        icon = Icons.Default.AutoStories,
                        title = Copy.FEATURE_ATLAS_TITLE,
                        tint = MapChinaColors.RarityEpic,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToAtlas?.invoke() }
                    )
                    FeatureEntry(
                        icon = Icons.Default.Edit,
                        title = Copy.FEATURE_CARVING_TITLE,
                        tint = Color(0xFF8B7355),
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToCarvings?.invoke() }
                    )
                    FeatureEntry(
                        icon = Icons.Outlined.BarChart,
                        title = Copy.FEATURE_STATS_TITLE,
                        tint = MapChinaColors.AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToStats?.invoke() }
                    )
                }
            }
        }

        // Next target card
        val nextTarget = achievementUi.nextTarget
        if (nextTarget != null) {
            item { NextTargetCard(nextTarget) }
        }

        // Settings - 独立分组
        item {
            var photoMarkersVisible by remember { mutableStateOf(settingsRepository?.getString("photo_markers_visible") != "false") }
            var autoMarkFootprint by remember { mutableStateOf(settingsRepository?.getString("auto_mark_footprint") != "false") }

            Column {
                Text(
                    Copy.SETTINGS,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MapChinaColors.TextTertiary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MapChinaRadius.Large,
                    colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(Copy.PHOTO_MARKERS, photoMarkersVisible, { haptic.perform(HapticType.SELECTION); photoMarkersVisible = it; settingsRepository?.setString("photo_markers_visible", if (it) "true" else "false") })
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsRow(Copy.AUTO_FOOTPRINT, autoMarkFootprint, { haptic.perform(HapticType.SELECTION); autoMarkFootprint = it; settingsRepository?.setString("auto_mark_footprint", if (it) "true" else "false") })
                        Spacer(modifier = Modifier.height(16.dp))

                        // 地图主题 - 分隔
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MapChinaColors.BorderSubtle)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("地图主题", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        MapThemeSelector(settingsRepository = settingsRepository)
                    }
                }
            }
        }

        // Logout + version
        item {
            if (isLoggedIn) {
                Text(
                    "退出登录",
                    color = MapChinaColors.Error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptic.perform(HapticType.WARNING); viewModel?.logout() }
                        .padding(vertical = 8.dp)
                )
            }
            Text(
                "MapChina v1.0.1",
                color = MapChinaColors.TextTertiary.copy(alpha = 0.5f),
                style = MapChinaTypography.Overline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    profile: ProfileUi,
    isLoggedIn: Boolean,
    onNavigateToLogin: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MapChinaRadius.Large,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 - 碧玉渐变圆圈
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(MapChinaColors.Primary, MapChinaColors.PrimaryVariant))),
                contentAlignment = Alignment.Center
            ) {
                val initial = profile.nickname.take(1)
                Text(
                    initial,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.nickname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MapChinaColors.TextPrimary
                )
                if (isLoggedIn && profile.levelInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 等级标签
                        Surface(
                            shape = MapChinaRadius.Small,
                            color = MapChinaColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "Lv${profile.levelInfo!!.currentLevel}",
                                color = MapChinaColors.Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            profile.levelInfo!!.currentTitle,
                            color = MapChinaColors.TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${profile.levelInfo!!.currentScore} ${Copy.LEVEL_SCORE_UNIT}",
                            color = MapChinaColors.AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "登录后解锁完整功能",
                        color = MapChinaColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }

            if (!isLoggedIn) {
                Button(
                    onClick = { onNavigateToLogin?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("登录", fontSize = 13.sp, color = Color.White) }
            }
        }
    }
}

@Composable
private fun Surface(shape: RoundedCornerShape, color: Color, content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(shape = shape, color = color, content = content)
}

@Composable
private fun StatsBar(stats: StatsUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MapChinaRadius.Large,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BigStat("省份", stats.visitedProvinces, stats.totalProvinces, MapChinaColors.Primary)
            BigStat("景点", stats.visitedCities, stats.totalCities, MapChinaColors.AccentGold)
            BigStat("城市", stats.visitedDistricts, stats.totalDistricts, MapChinaColors.FootprintShortVisit)
        }
    }
}

@Composable
private fun BigStat(label: String, visited: Int, total: Int, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$visited",
                color = accentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "/$total",
                color = MapChinaColors.TextTertiary,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = MapChinaColors.TextTertiary, fontSize = 12.sp)
        if (total > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            val percent = visited.toFloat() / total
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.width(56.dp).height(3.dp).clip(MapChinaRadius.Small),
                color = accentColor,
                trackColor = MapChinaColors.BorderSubtle
            )
        }
    }
}

@Composable
private fun FeatureEntry(
    icon: ImageVector,
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(MapChinaMotion.Instant),
        label = "pressScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                    }
                }
            },
        shape = MapChinaRadius.Large,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
        }
    }
}

@Composable
private fun NextTargetCard(target: AchievementWithProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MapChinaRadius.Large,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("下一目标", color = MapChinaColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(target.definition.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MapChinaColors.TextPrimary)
            Text(target.definition.description, fontSize = 13.sp, color = MapChinaColors.TextTertiary)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { target.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MapChinaRadius.Small),
                color = MapChinaColors.Primary,
                trackColor = MapChinaColors.BorderSubtle
            )
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = target.progressTarget - target.progressValue
            if (remaining > 0) {
                Text("再点亮 $remaining 个即可获得", fontSize = 12.sp, color = MapChinaColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
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
    val bgColor = when {
        level >= 8 -> MapChinaColors.AccentGold.copy(alpha = 0.12f)
        level >= 5 -> MapChinaColors.Primary.copy(alpha = 0.12f)
        else -> MapChinaColors.Primary.copy(alpha = 0.08f)
    }
    val tint = when {
        level >= 8 -> MapChinaColors.AccentGold
        else -> MapChinaColors.Primary
    }
    val iconTint = if (useLightIcon) Color.White else tint
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (useLightIcon) Color.White.copy(alpha = 0.2f) else bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = levelDef?.title ?: "等级",
            tint = iconTint,
            modifier = Modifier.size(if (level >= 8) 36.dp else 32.dp)
        )
    }
}

@Composable
private fun MapThemeSelector(settingsRepository: SettingsRepository?) {
    val haptic = LocalHapticFeedback.current
    var selectedTheme by remember {
        mutableStateOf(MapTheme.fromName(settingsRepository?.getString("map_theme")))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapTheme.entries.forEach { theme ->
            val isSelected = theme == selectedTheme
            val borderCol = if (isSelected) MapChinaColors.Primary else Color.Transparent
            val borderW = if (isSelected) 2.dp else 0.dp

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MapChinaColors.Background)
                    .border(borderW, borderCol, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.perform(HapticType.SELECTION)
                        selectedTheme = theme
                        settingsRepository?.setString("map_theme", theme.name)
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.oceanColor)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    theme.displayName,
                    fontSize = 11.sp,
                    color = if (isSelected) MapChinaColors.Primary else MapChinaColors.TextTertiary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
