package com.mapchina.ui.achievement

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.mapchina.domain.service.AtlasItemVisitStatus
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasDetailScreen(
    viewModel: AtlasViewModel? = null,
    atlasId: String = "",
    modifier: Modifier = Modifier
) {
    val detailUi by (viewModel?.detailUi?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AtlasDetailUi()) })

    LaunchedEffect(atlasId) {
        viewModel?.loadAtlasDetail(atlasId)
    }

    val progress = detailUi.progress

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        TopAppBar(
            title = { Text(progress?.atlasName ?: "图鉴详情", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1923))
        )

        if (progress == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中...", color = Color.Gray)
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(progress.atlasDescription, color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("已收录", color = Color.Gray, fontSize = 13.sp)
                            Text("${progress.visitedItems}/${progress.totalItems}", color = MapChinaColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (progress.completionPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MapChinaColors.Primary,
                            trackColor = Color(0xFF0F1923)
                        )
                    }
                }
            }

            if (detailUi.atlasAchievements.isNotEmpty()) {
                item {
                    Text("图鉴成就", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                items(detailUi.atlasAchievements, key = { it.definition.id }) { badge ->
                    AtlasAchievementRow(badge)
                }
            }

            if (detailUi.items.isNotEmpty()) {
                item {
                    Text("收录项目", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                items(detailUi.items, key = { it.attractionId }) { item ->
                    AtlasItemRow(item)
                }
            }
        }
    }
}

@Composable
private fun AtlasAchievementRow(item: AchievementWithProgress) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> Color(0xFF90CAF9)
        AchievementRarity.RARE -> Color(0xFF69F0AE)
        AchievementRarity.EPIC -> Color(0xFFCE93D8)
        AchievementRarity.LEGENDARY -> Color(0xFFFFD700)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.isUnlocked) Color(0xFF1A2C3D) else Color(0xFF1F1F33))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else Color(0xFF213647)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
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
            Text(
                "${item.progressValue}/${item.progressTarget}",
                color = if (item.isUnlocked) rarityColor else Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AtlasItemRow(item: AtlasItemVisitStatus) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (item.isVisited) MapChinaColors.Primary else Color(0xFF213647))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName, color = if (item.isVisited) Color.White else Color.Gray, fontSize = 14.sp)
                Text("${item.province} ${item.city}", color = Color.Gray, fontSize = 11.sp)
            }
            if (item.isVisited) {
                Text("已点亮", color = MapChinaColors.Primary, fontSize = 11.sp)
            }
        }
    }
}
