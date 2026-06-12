package com.mapchina.ui.achievement

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.common.EmptyState
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.stats.StatsUi
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.ui.stats.VisitedAttractionUi
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.InkTabIndicator
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel? = null,
    statsViewModel: StatsViewModel? = null,
    onNavigateToBadgeWall: (() -> Unit)? = null,
    onNavigateToProvinceConquest: (() -> Unit)? = null,
    onNavigateToAtlas: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ui by (viewModel?.ui?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AchievementUi()) })
    val stats by (statsViewModel?.stats?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(StatsUi()) })

    LaunchedEffect(viewModel) { viewModel?.refresh() }
    LaunchedEffect(statsViewModel) { statsViewModel?.refreshStats() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("成就", "统计")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MapChinaColors.Background)
    ) {
        Text(
            "成就",
            color = MapChinaColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

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
            0 -> AchievementTab(
                ui = ui,
                onNavigateToBadgeWall = onNavigateToBadgeWall,
                onNavigateToProvinceConquest = onNavigateToProvinceConquest,
                onNavigateToAtlas = onNavigateToAtlas
            )
            1 -> StatsTab(stats = stats)
        }
    }
}

@Composable
private fun AchievementTab(
    ui: AchievementUi,
    onNavigateToBadgeWall: (() -> Unit)?,
    onNavigateToProvinceConquest: (() -> Unit)?,
    onNavigateToAtlas: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                        Text(Copy.FEATURE_PROVINCE_TITLE, style = MapChinaTypography.Headline, color = MapChinaColors.TextPrimary)
                        Text(Copy.FEATURE_PROVINCE_SUBTITLE, style = MapChinaTypography.Body, color = MapChinaColors.TextTertiary)
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
                        Text(Copy.FEATURE_ATLAS_TITLE, style = MapChinaTypography.Headline, color = MapChinaColors.TextPrimary)
                        Text(Copy.FEATURE_ATLAS_SUBTITLE, style = MapChinaTypography.Body, color = MapChinaColors.TextTertiary)
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
                Text("查看全部${Copy.FEATURE_BADGE_TITLE}", color = MapChinaColors.Primary, style = MapChinaTypography.Title, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatsTab(stats: StatsUi) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CoverageSection("省份", stats.visitedProvinces, stats.totalProvinces, stats.provincePercent) }
        item { CoverageSection("城市", stats.visitedCities, stats.totalCities, stats.cityPercent) }
        item { CoverageSection("区县", stats.visitedDistricts, stats.totalDistricts, stats.districtPercent) }

        if (stats.visitedAttractionList.isNotEmpty()) {
            item {
                Text(
                    "已到访景点 (${stats.visitedAttractionList.size})",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            items(stats.visitedAttractionList.size) { index ->
                VisitedAttractionCard(stats.visitedAttractionList[index])
            }
        }

        if (stats.visitedAttractionList.isEmpty() && stats.totalProvinces == 0) {
            item {
                EmptyState(
                    icon = Icons.Default.Explore,
                    title = "暂无统计数据",
                    subtitle = "开始探索吧",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MapChinaColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Lv${level?.currentLevel ?: 1}",
                        color = MapChinaColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
        recent.forEach { item ->
            AchievementRow(item)
        }
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
private fun VisitedAttractionCard(attraction: VisitedAttractionUi) {
    val levelBadge = when (attraction.level) {
        "A5" -> "5A"
        "A4" -> "4A"
        else -> attraction.level
    }
    val visitLabel = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> "深度"
        FootprintLevel.SHORT_VISIT -> "短玩"
        FootprintLevel.PASS_BY -> "路过"
    }
    val visitColor = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = levelBadge,
                    color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.2f) else MapChinaColors.AccentBlue.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = attraction.name,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = visitLabel,
                color = visitColor,
                fontSize = 12.sp
            )
        }
    }
}
