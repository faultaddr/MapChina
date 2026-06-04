package com.mapchina.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DirectionsWalk
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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.data.repository.SettingsRepository
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.LEVEL_DEFINITIONS
import com.mapchina.ui.achievement.AchievementUi
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.achievement.AchievementWithProgress
import com.mapchina.ui.stats.StatsUi
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.ui.stats.VisitedAttractionUi
import com.mapchina.ui.theme.InkTabIndicator
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard

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
    onLoginSuccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val profile by (viewModel?.profile?.collectAsState() ?: remember { mutableStateOf(ProfileUi("未登录", null, null)) })
    val isLoggedIn by (viewModel?.isLoggedIn?.collectAsState() ?: remember { mutableStateOf(false) })
    val achievementUi by (achievementViewModel?.ui?.collectAsState() ?: remember { mutableStateOf(AchievementUi()) })
    val stats by (statsViewModel?.stats?.collectAsState() ?: remember { mutableStateOf(StatsUi()) })

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("我的", "成就")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding()
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MapChinaColors.TextPrimary,
            indicator = { tabPositions ->
                InkTabIndicator(currentTabPosition = tabPositions[selectedTab])
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MapChinaColors.Primary else MapChinaColors.TextTertiary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ProfileTab(
                profile = profile,
                isLoggedIn = isLoggedIn,
                stats = stats,
                onNavigateToJournals = onNavigateToJournals,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToBadgeWall = onNavigateToBadgeWall,
                onNavigateToProvinceConquest = onNavigateToProvinceConquest,
                onNavigateToAtlas = onNavigateToAtlas,
                settingsRepository = viewModel?.settingsRepository,
                onLogout = { viewModel?.logout() }
            )
            1 -> AchievementTabContent(
                ui = achievementUi,
                stats = stats,
                onNavigateToBadgeWall = onNavigateToBadgeWall,
                onNavigateToProvinceConquest = onNavigateToProvinceConquest,
                onNavigateToAtlas = onNavigateToAtlas
            )
        }
    }
}

@Composable
private fun ProfileTab(
    profile: ProfileUi,
    isLoggedIn: Boolean,
    stats: StatsUi,
    onNavigateToJournals: (() -> Unit)?,
    onNavigateToLogin: (() -> Unit)?,
    onNavigateToBadgeWall: (() -> Unit)?,
    onNavigateToProvinceConquest: (() -> Unit)?,
    onNavigateToAtlas: (() -> Unit)?,
    settingsRepository: SettingsRepository?,
    onLogout: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        // Compact user card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MapChinaCard.shape,
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevelBadgeIcon(
                        level = profile.levelInfo?.currentLevel ?: 1,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MapChinaColors.TextPrimary)
                        if (isLoggedIn && profile.levelInfo != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MapChinaColors.Primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Lv${profile.levelInfo!!.currentLevel}", color = MapChinaColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(profile.levelInfo!!.currentTitle, color = MapChinaColors.TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${profile.levelInfo!!.currentScore} 山河值", color = MapChinaColors.AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("登录后解锁完整功能", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                        }
                    }
                    if (!isLoggedIn) {
                        Button(
                            onClick = { onNavigateToLogin?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) { Text("登录", fontSize = 13.sp) }
                    }
                }
            }
        }

        // Footprint stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FootprintStatCard("省", stats.visitedProvinces, stats.totalProvinces, MapChinaColors.Primary, modifier = Modifier.weight(1f))
                FootprintStatCard("市", stats.visitedCities, stats.totalCities, MapChinaColors.AccentGold, modifier = Modifier.weight(1f))
                FootprintStatCard("区", stats.visitedDistricts, stats.totalDistricts, MapChinaColors.FootprintShortVisit, modifier = Modifier.weight(1f))
            }
        }

        // Feature grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.Book,
                        title = "我的游记",
                        subtitle = "记录旅途故事",
                        tint = MapChinaColors.Primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToJournals?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Default.WorkspacePremium,
                        title = "徽章墙",
                        subtitle = "收集旅行荣誉",
                        tint = MapChinaColors.AccentGold,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToBadgeWall?.invoke() }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.Map,
                        title = "省份征服",
                        subtitle = "点亮中国版图",
                        tint = MapChinaColors.FootprintDeep,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToProvinceConquest?.invoke() }
                    )
                    FeatureCard(
                        icon = Icons.Default.AutoStories,
                        title = "主题图鉴",
                        subtitle = "按主题探索中国",
                        tint = MapChinaColors.RarityEpic,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToAtlas?.invoke() }
                    )
                }
            }
        }

        // Level progress (if logged in)
        if (isLoggedIn && profile.levelInfo != null && !profile.levelInfo!!.isMaxLevel) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MapChinaCard.shape,
                    colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                    border = MapChinaCard.border,
                    elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("距离 ${profile.levelInfo!!.nextTitle}", color = MapChinaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("还差 ${profile.levelInfo!!.nextLevelScore - profile.levelInfo!!.currentScore} 山河值", color = MapChinaColors.Primary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { profile.levelInfo!!.progressToNext },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MapChinaColors.Primary,
                            trackColor = MapChinaColors.Background
                        )
                    }
                }
            }
        }

        // Settings section
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
                    Text("设置", color = MapChinaColors.TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsRow("照片标记", photoMarkersVisible, { photoMarkersVisible = it; settingsRepository?.setString("photo_markers_visible", if (it) "true" else "false") })
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsRow("自动标记足迹", autoMarkFootprint, { autoMarkFootprint = it; settingsRepository?.setString("auto_mark_footprint", if (it) "true" else "false") })
                }
            }
        }

        // Logout / version
        item {
            if (isLoggedIn) {
                Text(
                    "退出登录",
                    color = MapChinaColors.Error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogout?.invoke() }
                        .padding(vertical = 8.dp)
                )
            }
            Text(
                "MapChina v1.0.0",
                color = MapChinaColors.TextTertiary.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FootprintStatCard(
    label: String,
    visited: Int,
    total: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) visited.toFloat() / total else 0f
    Card(
        modifier = modifier,
        shape = MapChinaCard.shape,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$visited", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("/$total$label", color = MapChinaColors.TextTertiary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)),
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
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MapChinaCard.shape,
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = MapChinaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MapChinaColors.TextTertiary, fontSize = 11.sp)
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
        Text(title, color = MapChinaColors.TextPrimary, fontSize = 14.sp)
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
private fun AchievementTabContent(
    ui: AchievementUi,
    stats: StatsUi,
    onNavigateToBadgeWall: (() -> Unit)?,
    onNavigateToProvinceConquest: (() -> Unit)?,
    onNavigateToAtlas: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LevelCard(ui) }
        if (ui.nextTarget != null) item { NextTargetCard(ui.nextTarget!!) }
        if (ui.recentUnlocked.isNotEmpty()) item { RecentUnlockedSection(ui.recentUnlocked) }
        if (ui.regionAchievements.isNotEmpty()) item { AchievementSection("地区征服", ui.regionAchievements) }
        if (ui.scenicAchievements.isNotEmpty()) item { AchievementSection("景点收集", ui.scenicAchievements) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProvinceConquest?.invoke() },
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MapChinaColors.FootprintDeep.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MapChinaColors.FootprintDeep, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("省份征服", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("征服每个省份，点亮你的中国版图", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAtlas?.invoke() },
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MapChinaColors.RarityEpic.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = MapChinaColors.RarityEpic, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("主题图鉴", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("按主题收集中国之美", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MapChinaColors.SurfaceElevated)
                    .clickable { onNavigateToBadgeWall?.invoke() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("查看全部徽章", color = MapChinaColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        // Stats section
        item { CoverageSection("省份", stats.visitedProvinces, stats.totalProvinces, stats.provincePercent) }
        item { CoverageSection("城市", stats.visitedCities, stats.totalCities, stats.cityPercent) }
        item { CoverageSection("区县", stats.visitedDistricts, stats.totalDistricts, stats.districtPercent) }
    }
}

@Composable
private fun LevelCard(ui: AchievementUi) {
    val level = ui.levelInfo
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadgeIcon(
                    level = level?.currentLevel ?: 1,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        level?.currentTitle ?: "初行者",
                        color = MapChinaColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${level?.currentScore ?: 0} 山河值",
                        color = MapChinaColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (level != null && !level.isMaxLevel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("下一级：${level.nextTitle}", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                    Text("${level.nextLevelScore - level.currentScore} 山河值", color = MapChinaColors.Primary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { level.progressToNext },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MapChinaColors.Primary,
                    trackColor = MapChinaColors.Background
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("已解锁 ${ui.unlockedCount}/${ui.totalCount} 个成就", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NextTargetCard(target: AchievementWithProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        border = MapChinaCard.border,
        elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("下一目标", color = MapChinaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(target.definition.name, color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(target.definition.description, color = MapChinaColors.TextTertiary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { target.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MapChinaColors.Primary,
                trackColor = MapChinaColors.Background
            )
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = target.progressTarget - target.progressValue
            if (remaining > 0) {
                Text("再点亮 $remaining 个即可获得", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RecentUnlockedSection(recent: List<AchievementWithProgress>) {
    Column {
        Text("最近获得", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        recent.forEach { item -> AchievementRow(item) }
    }
}

@Composable
private fun AchievementSection(title: String, achievements: List<AchievementWithProgress>) {
    Column {
        Text(title, color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        achievements.forEach { item -> AchievementRow(item) }
    }
}

@Composable
private fun AchievementRow(item: AchievementWithProgress) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> MapChinaColors.AccentBlue
        AchievementRarity.RARE -> MapChinaColors.RarityRare
        AchievementRarity.EPIC -> MapChinaColors.RarityEpic
        AchievementRarity.LEGENDARY -> MapChinaColors.AccentGold
    }
    val bgColor = if (item.isUnlocked) MapChinaColors.SurfaceElevated else MapChinaColors.CardBackgroundLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.12f) else MapChinaColors.CardBackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (item.definition.rarity) {
                        AchievementRarity.LEGENDARY -> Icons.Default.WorkspacePremium
                        AchievementRarity.EPIC -> Icons.Default.EmojiEvents
                        AchievementRarity.RARE -> Icons.Default.Star
                        AchievementRarity.COMMON -> Icons.Default.MilitaryTech
                    },
                    contentDescription = null,
                    tint = if (item.isUnlocked) rarityColor else MapChinaColors.TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.definition.name,
                    color = if (item.isUnlocked) MapChinaColors.TextPrimary else MapChinaColors.TextTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(item.definition.description, color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.progressValue}/${item.progressTarget}",
                    color = if (item.isUnlocked) rarityColor else MapChinaColors.TextTertiary,
                    fontSize = 13.sp
                )
                Text(
                    "+${item.definition.rewardScore}",
                    color = MapChinaColors.Primary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CoverageSection(
    label: String,
    visited: Int,
    total: Int,
    percent: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MapChinaColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(
                "$visited / $total",
                color = MapChinaColors.Primary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MapChinaColors.Primary,
            trackColor = MapChinaColors.Background
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(percent * 100).toInt()}%",
            fontSize = 12.sp,
            color = MapChinaColors.TextTertiary
        )
    }
}

@Composable
fun LevelBadgeIcon(level: Int, modifier: Modifier = Modifier) {
    val levelDef = LEVEL_DEFINITIONS.find { it.level == level }
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
        level >= 5 -> MapChinaColors.Primary
        else -> MapChinaColors.Primary
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = levelDef?.title ?: "等级",
            tint = tint,
            modifier = Modifier.size(if (level >= 8) 36.dp else 32.dp)
        )
    }
}
