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
import com.mapchina.ui.theme.MapChinaCard

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
            .background(MapChinaColors.Background)
    ) {
        TopAppBar(
            title = { Text(progress?.atlasName ?: "图鉴详情", color = MapChinaColors.TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        if (progress == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中...", color = MapChinaColors.TextTertiary)
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
                    colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(progress.atlasDescription, color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("已收录", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
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
                            trackColor = MapChinaColors.Background
                        )
                    }
                }
            }

            if (detailUi.atlasAchievements.isNotEmpty()) {
                item {
                    Text("图鉴成就", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                items(detailUi.atlasAchievements, key = { it.definition.id }) { badge ->
                    AtlasAchievementRow(badge)
                }
            }

            if (detailUi.items.isNotEmpty()) {
                item {
                    Text("收录项目", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
        AchievementRarity.COMMON -> MapChinaColors.AccentBlue
        AchievementRarity.RARE -> MapChinaColors.RarityRare
        AchievementRarity.EPIC -> MapChinaColors.RarityEpic
        AchievementRarity.LEGENDARY -> MapChinaColors.AccentGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.isUnlocked) MapChinaColors.SurfaceElevated else MapChinaColors.CardBackgroundLight)
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
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else MapChinaColors.CardBackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (item.isUnlocked) rarityColor else MapChinaColors.TextTertiary)
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
            Text(
                "${item.progressValue}/${item.progressTarget}",
                color = if (item.isUnlocked) rarityColor else MapChinaColors.TextTertiary,
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
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
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
                    .background(if (item.isVisited) MapChinaColors.Primary else MapChinaColors.CardBackgroundLight)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName, color = if (item.isVisited) MapChinaColors.TextPrimary else MapChinaColors.TextTertiary, fontSize = 14.sp)
                Text("${item.province} ${item.city}", color = MapChinaColors.TextTertiary, fontSize = 11.sp)
            }
            if (item.isVisited) {
                Text("已点亮", color = MapChinaColors.Primary, fontSize = 11.sp)
            }
        }
    }
}
