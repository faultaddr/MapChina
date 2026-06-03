package com.mapchina.ui.achievement

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailScreen(
    item: AchievementWithProgress?,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        TopAppBar(
            title = { Text("徽章详情", color = MapChinaColors.TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到成就", color = MapChinaColors.TextTertiary)
            }
            return
        }

        val rarityColor = when (item.definition.rarity) {
            AchievementRarity.COMMON -> MapChinaColors.AccentBlue
            AchievementRarity.RARE -> MapChinaColors.RarityRare
            AchievementRarity.EPIC -> MapChinaColors.RarityEpic
            AchievementRarity.LEGENDARY -> MapChinaColors.AccentGold
        }
        val rarityLabel = when (item.definition.rarity) {
            AchievementRarity.COMMON -> "普通"
            AchievementRarity.RARE -> "进阶"
            AchievementRarity.EPIC -> "稀有"
            AchievementRarity.LEGENDARY -> "传奇"
        }
        val alpha = if (item.isUnlocked) 1f else 0.3f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else MapChinaColors.CardBackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(badgeDrawable(item.definition.icon)),
                    contentDescription = item.definition.name,
                    modifier = Modifier.size(88.dp),
                    alpha = alpha,
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(item.definition.name, color = MapChinaColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                rarityLabel,
                color = rarityColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(rarityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("描述", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                    Text(item.definition.description, color = MapChinaColors.TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("解锁条件", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                    Text(item.definition.description, color = MapChinaColors.TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("进度", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                        Text("${item.progressValue}/${item.progressTarget}", color = MapChinaColors.Primary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { item.progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MapChinaColors.Primary,
                        trackColor = MapChinaColors.Background
                    )
                    if (item.isUnlocked && item.unlockTime != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("解锁时间：${item.unlockTime}", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("山河值奖励", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                        Text("+${item.definition.rewardScore}", color = MapChinaColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
