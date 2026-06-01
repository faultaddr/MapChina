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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.stats.StatsUi
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.ui.stats.VisitedAttractionUi
import com.mapchina.ui.theme.MapChinaColors

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
            .background(Color(0xFF0F1923))
    ) {
        Text(
            "成就",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MapChinaColors.Primary,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MapChinaColors.Primary else Color.Gray,
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6B6B))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("省份征服", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("征服每个省份，点亮你的中国版图", color = Color.Gray, fontSize = 13.sp)
                    }
                    Text("→", color = Color.Gray, fontSize = 18.sp)
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAtlas?.invoke() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFCE93D8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCE93D8))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("主题图鉴", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("按主题收集中国之美", color = Color.Gray, fontSize = 13.sp)
                    }
                    Text("→", color = Color.Gray, fontSize = 18.sp)
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A2C3D))
                    .clickable { onNavigateToBadgeWall?.invoke() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("查看全部徽章", color = MapChinaColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
                    color = Color.White,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无统计数据，开始探索吧", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(ui: AchievementUi) {
    val level = ui.levelInfo
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
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
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        level?.currentTitle ?: "初行者",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${level?.currentScore ?: 0} 山河值",
                        color = Color.Gray,
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
                    Text("下一级：${level.nextTitle}", color = Color.Gray, fontSize = 12.sp)
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
                    trackColor = Color(0xFF0F1923)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("已解锁 ${ui.unlockedCount}/${ui.totalCount} 个成就", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NextTargetCard(target: AchievementWithProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("下一目标", color = MapChinaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(target.definition.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(target.definition.description, color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { target.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MapChinaColors.Primary,
                trackColor = Color(0xFF0F1923)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = target.progressTarget - target.progressValue
            if (remaining > 0) {
                Text("再点亮 $remaining 个即可获得", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RecentUnlockedSection(recent: List<AchievementWithProgress>) {
    Column {
        Text("最近获得", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        recent.forEach { item ->
            AchievementRow(item)
        }
    }
}

@Composable
private fun AchievementSection(title: String, achievements: List<AchievementWithProgress>) {
    Column {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        achievements.forEach { item -> AchievementRow(item) }
    }
}

@Composable
private fun AchievementRow(item: AchievementWithProgress) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> Color(0xFF90CAF9)
        AchievementRarity.RARE -> Color(0xFF69F0AE)
        AchievementRarity.EPIC -> Color(0xFFCE93D8)
        AchievementRarity.LEGENDARY -> Color(0xFFFFD700)
    }
    val bgColor = if (item.isUnlocked) Color(0xFF1A2C3D) else Color(0xFF1F1F33)

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
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else Color(0xFF213647)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (item.isUnlocked) rarityColor else Color.Gray)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.definition.name,
                    color = if (item.isUnlocked) Color.White else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(item.definition.description, color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.progressValue}/${item.progressTarget}",
                    color = if (item.isUnlocked) rarityColor else Color.Gray,
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
            .background(Color(0xFF1A2C3D))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
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
            trackColor = Color(0xFF0F1923)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(percent * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color.Gray
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
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
                    color = if (attraction.level == "A5") Color(0xFFFFD700) else Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") Color(0xFF332E00) else Color(0xFF0F3347),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = attraction.name,
                    color = Color.White,
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
