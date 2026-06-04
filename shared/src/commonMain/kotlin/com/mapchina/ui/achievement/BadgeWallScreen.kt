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
import mapchina.shared.generated.resources.badge_region_district_1
import mapchina.shared.generated.resources.badge_region_district_10
import mapchina.shared.generated.resources.badge_region_district_30
import mapchina.shared.generated.resources.badge_region_district_100
import mapchina.shared.generated.resources.badge_region_city_1
import mapchina.shared.generated.resources.badge_region_city_10
import mapchina.shared.generated.resources.badge_region_city_30
import mapchina.shared.generated.resources.badge_region_city_100
import mapchina.shared.generated.resources.badge_region_province_1
import mapchina.shared.generated.resources.badge_region_province_5
import mapchina.shared.generated.resources.badge_region_province_10
import mapchina.shared.generated.resources.badge_region_province_20
import mapchina.shared.generated.resources.badge_region_province_31
import mapchina.shared.generated.resources.badge_scenic_5a_1
import mapchina.shared.generated.resources.badge_scenic_5a_10
import mapchina.shared.generated.resources.badge_scenic_5a_30
import mapchina.shared.generated.resources.badge_scenic_5a_50
import mapchina.shared.generated.resources.badge_scenic_5a_100
import mapchina.shared.generated.resources.badge_scenic_total_10
import mapchina.shared.generated.resources.badge_scenic_total_50
import mapchina.shared.generated.resources.badge_scenic_total_100
import mapchina.shared.generated.resources.badge_scenic_total_300
import mapchina.shared.generated.resources.badge_atlas_heritage_1
import mapchina.shared.generated.resources.badge_atlas_heritage_5
import mapchina.shared.generated.resources.badge_atlas_heritage_10
import mapchina.shared.generated.resources.badge_atlas_heritage_20
import mapchina.shared.generated.resources.badge_atlas_museum_5
import mapchina.shared.generated.resources.badge_atlas_museum_20
import mapchina.shared.generated.resources.badge_atlas_museum_50
import mapchina.shared.generated.resources.badge_atlas_mountain_5
import mapchina.shared.generated.resources.badge_atlas_mountain_10
import mapchina.shared.generated.resources.badge_atlas_mountain_20
import mapchina.shared.generated.resources.badge_province_visit_11
import mapchina.shared.generated.resources.badge_province_visit_12
import mapchina.shared.generated.resources.badge_province_visit_13
import mapchina.shared.generated.resources.badge_province_visit_14
import mapchina.shared.generated.resources.badge_province_visit_15
import mapchina.shared.generated.resources.badge_province_visit_21
import mapchina.shared.generated.resources.badge_province_visit_22
import mapchina.shared.generated.resources.badge_province_visit_23
import mapchina.shared.generated.resources.badge_province_visit_31
import mapchina.shared.generated.resources.badge_province_visit_32
import mapchina.shared.generated.resources.badge_province_visit_33
import mapchina.shared.generated.resources.badge_province_visit_34
import mapchina.shared.generated.resources.badge_province_visit_35
import mapchina.shared.generated.resources.badge_province_visit_36
import mapchina.shared.generated.resources.badge_province_visit_37
import mapchina.shared.generated.resources.badge_province_visit_41
import mapchina.shared.generated.resources.badge_province_visit_42
import mapchina.shared.generated.resources.badge_province_visit_43
import mapchina.shared.generated.resources.badge_province_visit_44
import mapchina.shared.generated.resources.badge_province_visit_45
import mapchina.shared.generated.resources.badge_province_visit_46
import mapchina.shared.generated.resources.badge_province_visit_50
import mapchina.shared.generated.resources.badge_province_visit_51
import mapchina.shared.generated.resources.badge_province_visit_52
import mapchina.shared.generated.resources.badge_province_visit_53
import mapchina.shared.generated.resources.badge_province_visit_54
import mapchina.shared.generated.resources.badge_province_visit_61
import mapchina.shared.generated.resources.badge_province_visit_62
import mapchina.shared.generated.resources.badge_province_visit_63
import mapchina.shared.generated.resources.badge_province_visit_64
import mapchina.shared.generated.resources.badge_province_visit_65
import mapchina.shared.generated.resources.badge_province_complete_11
import mapchina.shared.generated.resources.badge_province_complete_12
import mapchina.shared.generated.resources.badge_province_complete_13
import mapchina.shared.generated.resources.badge_province_complete_14
import mapchina.shared.generated.resources.badge_province_complete_15
import mapchina.shared.generated.resources.badge_province_complete_21
import mapchina.shared.generated.resources.badge_province_complete_22
import mapchina.shared.generated.resources.badge_province_complete_23
import mapchina.shared.generated.resources.badge_province_complete_31
import mapchina.shared.generated.resources.badge_province_complete_32
import mapchina.shared.generated.resources.badge_province_complete_33
import mapchina.shared.generated.resources.badge_province_complete_34
import mapchina.shared.generated.resources.badge_province_complete_35
import mapchina.shared.generated.resources.badge_province_complete_36
import mapchina.shared.generated.resources.badge_province_complete_37
import mapchina.shared.generated.resources.badge_province_complete_41
import mapchina.shared.generated.resources.badge_province_complete_42
import mapchina.shared.generated.resources.badge_province_complete_43
import mapchina.shared.generated.resources.badge_province_complete_44
import mapchina.shared.generated.resources.badge_province_complete_45
import mapchina.shared.generated.resources.badge_province_complete_46
import mapchina.shared.generated.resources.badge_province_complete_50
import mapchina.shared.generated.resources.badge_province_complete_51
import mapchina.shared.generated.resources.badge_province_complete_52
import mapchina.shared.generated.resources.badge_province_complete_53
import mapchina.shared.generated.resources.badge_province_complete_54
import mapchina.shared.generated.resources.badge_province_complete_61
import mapchina.shared.generated.resources.badge_province_complete_62
import mapchina.shared.generated.resources.badge_province_complete_63
import mapchina.shared.generated.resources.badge_province_complete_64
import mapchina.shared.generated.resources.badge_province_complete_65
import mapchina.shared.generated.resources.badge_geo_north
import mapchina.shared.generated.resources.badge_geo_south
import mapchina.shared.generated.resources.badge_geo_silk_road
import mapchina.shared.generated.resources.badge_geo_coast
import mapchina.shared.generated.resources.badge_geo_river
import mapchina.shared.generated.resources.badge_geo_same_day_3
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val BADGE_MAP = mapOf(
    "badge_region_district_1" to Res.drawable.badge_region_district_1,
    "badge_region_district_10" to Res.drawable.badge_region_district_10,
    "badge_region_district_30" to Res.drawable.badge_region_district_30,
    "badge_region_district_100" to Res.drawable.badge_region_district_100,
    "badge_region_city_1" to Res.drawable.badge_region_city_1,
    "badge_region_city_10" to Res.drawable.badge_region_city_10,
    "badge_region_city_30" to Res.drawable.badge_region_city_30,
    "badge_region_city_100" to Res.drawable.badge_region_city_100,
    "badge_region_province_1" to Res.drawable.badge_region_province_1,
    "badge_region_province_5" to Res.drawable.badge_region_province_5,
    "badge_region_province_10" to Res.drawable.badge_region_province_10,
    "badge_region_province_20" to Res.drawable.badge_region_province_20,
    "badge_region_province_31" to Res.drawable.badge_region_province_31,
    "badge_scenic_5a_1" to Res.drawable.badge_scenic_5a_1,
    "badge_scenic_5a_10" to Res.drawable.badge_scenic_5a_10,
    "badge_scenic_5a_30" to Res.drawable.badge_scenic_5a_30,
    "badge_scenic_5a_50" to Res.drawable.badge_scenic_5a_50,
    "badge_scenic_5a_100" to Res.drawable.badge_scenic_5a_100,
    "badge_scenic_total_10" to Res.drawable.badge_scenic_total_10,
    "badge_scenic_total_50" to Res.drawable.badge_scenic_total_50,
    "badge_scenic_total_100" to Res.drawable.badge_scenic_total_100,
    "badge_scenic_total_300" to Res.drawable.badge_scenic_total_300,
    "badge_atlas_heritage_1" to Res.drawable.badge_atlas_heritage_1,
    "badge_atlas_heritage_5" to Res.drawable.badge_atlas_heritage_5,
    "badge_atlas_heritage_10" to Res.drawable.badge_atlas_heritage_10,
    "badge_atlas_heritage_20" to Res.drawable.badge_atlas_heritage_20,
    "badge_atlas_museum_5" to Res.drawable.badge_atlas_museum_5,
    "badge_atlas_museum_20" to Res.drawable.badge_atlas_museum_20,
    "badge_atlas_museum_50" to Res.drawable.badge_atlas_museum_50,
    "badge_atlas_mountain_5" to Res.drawable.badge_atlas_mountain_5,
    "badge_atlas_mountain_10" to Res.drawable.badge_atlas_mountain_10,
    "badge_atlas_mountain_20" to Res.drawable.badge_atlas_mountain_20,
    "badge_province_visit_11" to Res.drawable.badge_province_visit_11,
    "badge_province_visit_12" to Res.drawable.badge_province_visit_12,
    "badge_province_visit_13" to Res.drawable.badge_province_visit_13,
    "badge_province_visit_14" to Res.drawable.badge_province_visit_14,
    "badge_province_visit_15" to Res.drawable.badge_province_visit_15,
    "badge_province_visit_21" to Res.drawable.badge_province_visit_21,
    "badge_province_visit_22" to Res.drawable.badge_province_visit_22,
    "badge_province_visit_23" to Res.drawable.badge_province_visit_23,
    "badge_province_visit_31" to Res.drawable.badge_province_visit_31,
    "badge_province_visit_32" to Res.drawable.badge_province_visit_32,
    "badge_province_visit_33" to Res.drawable.badge_province_visit_33,
    "badge_province_visit_34" to Res.drawable.badge_province_visit_34,
    "badge_province_visit_35" to Res.drawable.badge_province_visit_35,
    "badge_province_visit_36" to Res.drawable.badge_province_visit_36,
    "badge_province_visit_37" to Res.drawable.badge_province_visit_37,
    "badge_province_visit_41" to Res.drawable.badge_province_visit_41,
    "badge_province_visit_42" to Res.drawable.badge_province_visit_42,
    "badge_province_visit_43" to Res.drawable.badge_province_visit_43,
    "badge_province_visit_44" to Res.drawable.badge_province_visit_44,
    "badge_province_visit_45" to Res.drawable.badge_province_visit_45,
    "badge_province_visit_46" to Res.drawable.badge_province_visit_46,
    "badge_province_visit_50" to Res.drawable.badge_province_visit_50,
    "badge_province_visit_51" to Res.drawable.badge_province_visit_51,
    "badge_province_visit_52" to Res.drawable.badge_province_visit_52,
    "badge_province_visit_53" to Res.drawable.badge_province_visit_53,
    "badge_province_visit_54" to Res.drawable.badge_province_visit_54,
    "badge_province_visit_61" to Res.drawable.badge_province_visit_61,
    "badge_province_visit_62" to Res.drawable.badge_province_visit_62,
    "badge_province_visit_63" to Res.drawable.badge_province_visit_63,
    "badge_province_visit_64" to Res.drawable.badge_province_visit_64,
    "badge_province_visit_65" to Res.drawable.badge_province_visit_65,
    "badge_province_complete_11" to Res.drawable.badge_province_complete_11,
    "badge_province_complete_12" to Res.drawable.badge_province_complete_12,
    "badge_province_complete_13" to Res.drawable.badge_province_complete_13,
    "badge_province_complete_14" to Res.drawable.badge_province_complete_14,
    "badge_province_complete_15" to Res.drawable.badge_province_complete_15,
    "badge_province_complete_21" to Res.drawable.badge_province_complete_21,
    "badge_province_complete_22" to Res.drawable.badge_province_complete_22,
    "badge_province_complete_23" to Res.drawable.badge_province_complete_23,
    "badge_province_complete_31" to Res.drawable.badge_province_complete_31,
    "badge_province_complete_32" to Res.drawable.badge_province_complete_32,
    "badge_province_complete_33" to Res.drawable.badge_province_complete_33,
    "badge_province_complete_34" to Res.drawable.badge_province_complete_34,
    "badge_province_complete_35" to Res.drawable.badge_province_complete_35,
    "badge_province_complete_36" to Res.drawable.badge_province_complete_36,
    "badge_province_complete_37" to Res.drawable.badge_province_complete_37,
    "badge_province_complete_41" to Res.drawable.badge_province_complete_41,
    "badge_province_complete_42" to Res.drawable.badge_province_complete_42,
    "badge_province_complete_43" to Res.drawable.badge_province_complete_43,
    "badge_province_complete_44" to Res.drawable.badge_province_complete_44,
    "badge_province_complete_45" to Res.drawable.badge_province_complete_45,
    "badge_province_complete_46" to Res.drawable.badge_province_complete_46,
    "badge_province_complete_50" to Res.drawable.badge_province_complete_50,
    "badge_province_complete_51" to Res.drawable.badge_province_complete_51,
    "badge_province_complete_52" to Res.drawable.badge_province_complete_52,
    "badge_province_complete_53" to Res.drawable.badge_province_complete_53,
    "badge_province_complete_54" to Res.drawable.badge_province_complete_54,
    "badge_province_complete_61" to Res.drawable.badge_province_complete_61,
    "badge_province_complete_62" to Res.drawable.badge_province_complete_62,
    "badge_province_complete_63" to Res.drawable.badge_province_complete_63,
    "badge_province_complete_64" to Res.drawable.badge_province_complete_64,
    "badge_province_complete_65" to Res.drawable.badge_province_complete_65,
    "badge_geo_north" to Res.drawable.badge_geo_north,
    "badge_geo_south" to Res.drawable.badge_geo_south,
    "badge_geo_silk_road" to Res.drawable.badge_geo_silk_road,
    "badge_geo_coast" to Res.drawable.badge_geo_coast,
    "badge_geo_river" to Res.drawable.badge_geo_river,
    "badge_geo_same_day_3" to Res.drawable.badge_geo_same_day_3,
)

internal fun badgeDrawable(icon: String): DrawableResource =
    BADGE_MAP[icon] ?: Res.drawable.badge_region_district_1

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
