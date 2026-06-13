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
import com.mapchina.ui.theme.MapChinaCard
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
        // Hero user card with gradient
        item {
            HeroUserCard(
                profile = profile,
                isLoggedIn = isLoggedIn,
                stats = stats,
                onNavigateToLogin = onNavigateToLogin
            )
        }

        // Feature grid (3x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.Book,
                        title = Copy.FEATURE_JOURNAL_TITLE,
                        subtitle = Copy.FEATURE_JOURNAL_SUBTITLE,
                        tint = MapChinaColors.Primary,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToJournals?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Default.WorkspacePremium,
                        title = Copy.FEATURE_BADGE_TITLE,
                        subtitle = Copy.FEATURE_BADGE_SUBTITLE,
                        tint = MapChinaColors.AccentGold,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToBadgeWall?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Default.Map,
                        title = Copy.FEATURE_PROVINCE_TITLE,
                        subtitle = Copy.FEATURE_PROVINCE_SUBTITLE,
                        tint = MapChinaColors.FootprintDeep,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToProvinceConquest?.invoke() }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.AutoStories,
                        title = Copy.FEATURE_ATLAS_TITLE,
                        subtitle = Copy.FEATURE_ATLAS_SUBTITLE,
                        tint = MapChinaColors.RarityEpic,
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToAtlas?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Default.Edit,
                        title = Copy.FEATURE_CARVING_TITLE,
                        subtitle = Copy.FEATURE_CARVING_SUBTITLE,
                        tint = Color(0xFF8B7355),
                        modifier = Modifier.weight(1f),
                        onClick = { haptic.perform(HapticType.LIGHT); onNavigateToCarvings?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Outlined.BarChart,
                        title = Copy.FEATURE_STATS_TITLE,
                        subtitle = Copy.FEATURE_STATS_SUBTITLE,
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

        // Settings rows
        item {
            var photoMarkersVisible by remember { mutableStateOf(settingsRepository?.getString("photo_markers_visible") != "false") }
            var autoMarkFootprint by remember { mutableStateOf(settingsRepository?.getString("auto_mark_footprint") != "false") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MapChinaCard.shape,
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Copy.SETTINGS, style = MapChinaTypography.Caption, color = MapChinaColors.TextTertiary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsRow(Copy.PHOTO_MARKERS, photoMarkersVisible, { haptic.perform(HapticType.SELECTION); photoMarkersVisible = it; settingsRepository?.setString("photo_markers_visible", if (it) "true" else "false") })
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsRow(Copy.AUTO_FOOTPRINT, autoMarkFootprint, { haptic.perform(HapticType.SELECTION); autoMarkFootprint = it; settingsRepository?.setString("auto_mark_footprint", if (it) "true" else "false") })
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("地图主题", style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    MapThemeSelector(settingsRepository = settingsRepository)
                }
            }
        }

        // Logout + version
        item {
            if (isLoggedIn) {
                Text(
                    "退出登录",
                    color = MapChinaColors.Error,
                    style = MapChinaTypography.Body,
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
private fun HeroUserCard(
    profile: ProfileUi,
    isLoggedIn: Boolean,
    stats: StatsUi,
    onNavigateToLogin: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MapChinaRadius.Large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MapChinaColors.CardHeroGradient)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadgeIcon(
                        level = profile.levelInfo?.currentLevel ?: 1,
                        modifier = Modifier.size(48.dp),
                        useLightIcon = true
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.nickname, style = MapChinaTypography.Headline, color = Color.White)
                        if (isLoggedIn && profile.levelInfo != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(MapChinaRadius.Small)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Lv${profile.levelInfo!!.currentLevel}", color = Color.White, style = MapChinaTypography.Caption, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(profile.levelInfo!!.currentTitle, color = Color.White.copy(alpha = 0.85f), style = MapChinaTypography.Body)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${profile.levelInfo!!.currentScore} ${Copy.LEVEL_SCORE_UNIT}", color = MapChinaColors.AccentGold, style = MapChinaTypography.Caption, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("登录后解锁完整功能", color = Color.White.copy(alpha = 0.7f), style = MapChinaTypography.Body)
                        }
                    }
                    if (!isLoggedIn) {
                        Button(
                            onClick = { onNavigateToLogin?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) { Text("登录", style = MapChinaTypography.Caption, color = Color.White) }
                    }
                }
            }

            // Footprint stats row (embedded)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MapChinaColors.SurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStat("省", stats.visitedProvinces, stats.totalProvinces, MapChinaColors.Primary)
                CompactStat("市", stats.visitedCities, stats.totalCities, MapChinaColors.AccentGold)
                CompactStat("区", stats.visitedDistricts, stats.totalDistricts, MapChinaColors.FootprintShortVisit)
            }
        }
    }
}

@Composable
private fun CompactStat(label: String, visited: Int, total: Int, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$visited", color = accentColor, style = MapChinaTypography.Headline)
            Text("/$total$label", color = MapChinaColors.TextTertiary, style = MapChinaTypography.Caption)
        }
        if (total > 0) {
            Spacer(modifier = Modifier.height(3.dp))
            val percent = visited.toFloat() / total
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.width(60.dp).height(2.5.dp).clip(MapChinaRadius.Small),
                color = accentColor,
                trackColor = MapChinaColors.Background
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
        shape = MapChinaRadius.Medium,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MapChinaRadius.Small)
                    .background(Brush.verticalGradient(
                        colors = listOf(tint.copy(alpha = 0.15f), tint.copy(alpha = 0.06f))
                    )),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary)
            Text(subtitle, style = MapChinaTypography.Caption, color = MapChinaColors.TextTertiary)
        }
    }
}

@Composable
private fun NextTargetCard(target: AchievementWithProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MapChinaCard.shape,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("下一目标", color = MapChinaColors.Primary, style = MapChinaTypography.Caption, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(target.definition.name, style = MapChinaTypography.Headline, color = MapChinaColors.TextPrimary)
            Text(target.definition.description, style = MapChinaTypography.Body, color = MapChinaColors.TextTertiary)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { target.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MapChinaRadius.Small),
                color = MapChinaColors.Primary,
                trackColor = MapChinaColors.Background
            )
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = target.progressTarget - target.progressValue
            if (remaining > 0) {
                Text("再点亮 $remaining 个即可获得", style = MapChinaTypography.Caption, color = MapChinaColors.TextTertiary)
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
        Text(title, style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MapChinaColors.Primary,
                checkedThumbColor = MapChinaColors.SurfaceElevated
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
        level >= 8 -> MapChinaColors.AccentGold.copy(alpha = 0.15f)
        level >= 5 -> MapChinaColors.Primary.copy(alpha = 0.15f)
        else -> MapChinaColors.Primary.copy(alpha = 0.1f)
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
                    style = MapChinaTypography.Caption,
                    color = if (isSelected) MapChinaColors.Primary else MapChinaColors.TextTertiary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
