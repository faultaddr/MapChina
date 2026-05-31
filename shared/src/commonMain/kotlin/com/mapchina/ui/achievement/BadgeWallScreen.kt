package com.mapchina.ui.achievement

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.animation.staggeredEntrance
import com.mapchina.ui.animation.AnimationSpecs
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.ui.theme.MapChinaColors

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
    val tabs = listOf("全部", "地区", "景点")

    val filteredAchievements = when (selectedTab) {
        1 -> ui.regionAchievements
        2 -> ui.scenicAchievements
        else -> ui.allAchievements
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        TopAppBar(
            title = { Text("徽章墙", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E))
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MapChinaColors.Primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) Color.White else Color.Gray) }
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
            itemsIndexed(filteredAchievements, key = { _, item -> item.definition.id }) { index, item ->
                BadgeGridItem(
                    item = item,
                    onClick = { onBadgeClick?.invoke(item.definition.id) },
                    modifier = Modifier.staggeredEntrance(index, AnimationSpecs.Stagger.gridItem)
                )
            }
        }
    }
}

@Composable
private fun BadgeGridItem(
    item: AchievementWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rarityColor = when (item.definition.rarity) {
        AchievementRarity.COMMON -> Color(0xFF90CAF9)
        AchievementRarity.RARE -> Color(0xFF69F0AE)
        AchievementRarity.EPIC -> Color(0xFFCE93D8)
        AchievementRarity.LEGENDARY -> Color(0xFFFFD700)
    }
    val alpha = if (item.isUnlocked) 1f else 0.3f

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D2D44))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (item.isUnlocked) rarityColor.copy(alpha = 0.2f) else Color(0xFF3D3D5C)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (item.isUnlocked) rarityColor.copy(alpha = alpha) else Color.Gray.copy(alpha = alpha))
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
        Text(
            item.definition.name,
            color = if (item.isUnlocked) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            "${item.progressValue}/${item.progressTarget}",
            color = if (item.isUnlocked) rarityColor else Color.Gray,
            fontSize = 10.sp
        )
    }
}
