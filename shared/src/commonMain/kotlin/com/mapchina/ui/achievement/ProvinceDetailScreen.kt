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
import com.mapchina.domain.model.ProvinceConquestInfo
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvinceDetailScreen(
    viewModel: ProvinceConquestViewModel? = null,
    provinceCode: String = "",
    modifier: Modifier = Modifier
) {
    val detailUi by (viewModel?.detailUi?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(ProvinceDetailUi()) })

    LaunchedEffect(provinceCode) {
        viewModel?.loadProvinceDetail(provinceCode)
    }

    val info = detailUi.info

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        TopAppBar(
            title = { Text(info?.provinceName ?: "省份详情", color = MapChinaColors.TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        if (info == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中...", color = MapChinaColors.TextTertiary)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 进度总览
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("完成进度", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))

                    ProgressRow("城市", info.visitedCities, info.totalCities)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressRow("区县", info.visitedDistricts, info.totalDistricts)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressRow("景点", info.visitedAttractions, info.totalAttractions)
                }
            }

            // 省份徽章
            if (detailUi.provinceAchievements.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("省份徽章", color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        detailUi.provinceAchievements.forEach { badge ->
                            ProvinceBadgeRow(badge)
                        }
                    }
                }
            }

            // 提示
            if (!info.hasCompleteBadge && info.visitedCities > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("继续探索", color = MapChinaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val remaining = info.totalCities - info.visitedCities
                        if (remaining > 0) {
                            Text("再点亮 $remaining 个城市即可获得「${info.provinceName}通行者」徽章", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, visited: Int, total: Int) {
    val percent = if (total > 0) visited.toFloat() / total else 0f
    val percentColor = when {
        percent >= 1f -> MapChinaColors.AccentGold
        percent >= 0.7f -> MapChinaColors.Error
        percent >= 0.3f -> MapChinaColors.Primary
        percent > 0f -> MapChinaColors.AccentBlue.copy(alpha = 0.7f)
        else -> MapChinaColors.CardBackgroundLight
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MapChinaColors.TextTertiary, fontSize = 13.sp)
            Text("$visited/$total", color = percentColor, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = percentColor,
            trackColor = MapChinaColors.Background
        )
    }
}

@Composable
private fun ProvinceBadgeRow(item: AchievementWithProgress) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> MapChinaColors.AccentBlue
        AchievementRarity.RARE -> MapChinaColors.RarityRare
        AchievementRarity.EPIC -> MapChinaColors.RarityEpic
        AchievementRarity.LEGENDARY -> MapChinaColors.AccentGold
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
        if (item.isUnlocked) {
            Text("已解锁", color = rarityColor, fontSize = 12.sp)
        }
    }
}
