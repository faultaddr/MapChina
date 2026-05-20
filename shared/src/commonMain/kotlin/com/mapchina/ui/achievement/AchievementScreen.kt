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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel? = null,
    onNavigateToBadgeWall: (() -> Unit)? = null,
    onNavigateToProvinceConquest: (() -> Unit)? = null,
    onNavigateToAtlas: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ui by (viewModel?.ui?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AchievementUi()) })

    LaunchedEffect(viewModel) { viewModel?.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Text(
            "成就",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
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
                        .background(Color(0xFF2D2D44))
                        .clickable { onNavigateToBadgeWall?.invoke() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("查看全部徽章", color = MapChinaColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
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
                    trackColor = Color(0xFF1A1A2E)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
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
                trackColor = Color(0xFF1A1A2E)
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
    val bgColor = if (item.isUnlocked) Color(0xFF2D2D44) else Color(0xFF1F1F33)

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
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else Color(0xFF3D3D5C)),
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
