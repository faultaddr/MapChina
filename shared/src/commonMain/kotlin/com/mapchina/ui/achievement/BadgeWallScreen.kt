package com.mapchina.ui.achievement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AchievementCategory
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.ui.theme.InkTabIndicator
import com.mapchina.ui.theme.MapChinaColors
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.badge_5a
import mapchina.shared.generated.resources.badge_atlas_heritage
import mapchina.shared.generated.resources.badge_atlas_mountain
import mapchina.shared.generated.resources.badge_atlas_museum
import mapchina.shared.generated.resources.badge_city
import mapchina.shared.generated.resources.badge_district
import mapchina.shared.generated.resources.badge_geo
import mapchina.shared.generated.resources.badge_province
import mapchina.shared.generated.resources.badge_province_complete
import mapchina.shared.generated.resources.badge_province_visit
import mapchina.shared.generated.resources.badge_total
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal fun badgeDrawable(icon: String): DrawableResource = when (icon) {
    "badge_district" -> Res.drawable.badge_district
    "badge_city" -> Res.drawable.badge_city
    "badge_province" -> Res.drawable.badge_province
    "badge_5a" -> Res.drawable.badge_5a
    "badge_total" -> Res.drawable.badge_total
    "badge_atlas_heritage" -> Res.drawable.badge_atlas_heritage
    "badge_atlas_museum" -> Res.drawable.badge_atlas_museum
    "badge_atlas_mountain" -> Res.drawable.badge_atlas_mountain
    "badge_province_visit" -> Res.drawable.badge_province_visit
    "badge_province_complete" -> Res.drawable.badge_province_complete
    "badge_geo" -> Res.drawable.badge_geo
    else -> Res.drawable.badge_district
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeWallScreen(
    viewModel: AchievementViewModel? = null,
    onBadgeClick: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ui by (viewModel?.ui?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AchievementUi()) })
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) { viewModel?.refresh() }
    val tabs = listOf("全部", "地区", "景点")

    val filteredAchievements = when (selectedTab) {
        1 -> ui.regionAchievements
        2 -> ui.scenicAchievements
        else -> ui.allAchievements
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        TopAppBar(
            title = { Text("徽章墙", color = MapChinaColors.TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MapChinaColors.Background,
            contentColor = MapChinaColors.TextPrimary,
            indicator = { tabPositions ->
                InkTabIndicator(currentTabPosition = tabPositions[selectedTab])
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) MapChinaColors.Primary else MapChinaColors.TextTertiary) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredAchievements, key = { it.definition.id }) { item ->
                BadgeGridItem(
                    item = item,
                    onClick = { onBadgeClick?.invoke(item.definition.id) }
                )
            }
        }
    }
}

@Composable
private fun BadgeGridItem(
    item: AchievementWithProgress,
    onClick: () -> Unit
) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> MapChinaColors.AccentBlue
        AchievementRarity.RARE -> MapChinaColors.RarityRare
        AchievementRarity.EPIC -> MapChinaColors.RarityEpic
        AchievementRarity.LEGENDARY -> MapChinaColors.AccentGold
    }
    val alpha = if (item.isUnlocked) 1f else 0.3f
    val bgColor = if (item.isUnlocked) rarityColor.copy(alpha = 0.15f) else MapChinaColors.CardBackgroundLight

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MapChinaColors.SurfaceElevated)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(badgeDrawable(item.definition.icon)),
                contentDescription = item.definition.name,
                modifier = Modifier
                    .size(40.dp)
                    .padding(2.dp),
                alpha = alpha,
                contentScale = ContentScale.Fit
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
        Text(
            item.definition.name,
            color = if (item.isUnlocked) MapChinaColors.TextPrimary else MapChinaColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            "${item.progressValue}/${item.progressTarget}",
            color = if (item.isUnlocked) rarityColor else MapChinaColors.TextTertiary,
            fontSize = 10.sp
        )
    }
}
