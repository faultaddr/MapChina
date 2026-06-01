package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.ui.achievement.AchievementUnlockDialog
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel? = null,
    mapController: MapController = remember { MapController() },
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("足迹地图（初始化中）", color = Color.White)
        }
        return
    }

    viewModel.mapController = mapController

    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val regions by viewModel.regions.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val attractions by viewModel.attractions.collectAsState()
    val selectedRegionAttractions by viewModel.selectedRegionAttractions.collectAsState()
    val drillDownHint by viewModel.drillDownHint.collectAsState()
    val achievementResult by viewModel.achievementUnlock.collectAsState()

    LaunchedEffect(regions) {
        if (regions.isEmpty()) {
            kotlinx.coroutines.delay(500)
            viewModel.reloadData()
        }
    }

    var showRegionCard by remember { mutableStateOf(false) }
    var showAttractionsSheet by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(true) }

    val canDrillDown = currentLevel.nextDrillDown() != null
    val visitedCount = regions.count { it.footprintLevel != null || it.childCoverageRate > 0f }
    val totalCount = regions.size
    val coveragePercent = if (totalCount > 0) visitedCount * 100 / totalCount else 0

    // Single tap on region → pulse + show card
    mapController.setOnRegionTapListener { regionId ->
        mapController.pulseOverlay(regionId)
        viewModel.selectRegion(regionId)
        showRegionCard = true
    }
    mapController.setOnMarkerTapListener { attractionId ->
        navController.navigate(AttractionDetailScreen(attractionId))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen map
        PlatformMapView(
            controller = mapController,
            modifier = Modifier.fillMaxSize()
        )

        // Top breadcrumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            BreadcrumbNav(
                path = currentPath.map { BreadcrumbItem(it.id, it.name) },
                onNavigateUp = { viewModel.navigateUp() },
                onNavigateTo = { viewModel.navigateTo(it) }
            )
        }

        // Bottom coverage overlay — shifts up when RegionCard is visible
        CoverageOverlay(
            visitedCount = visitedCount,
            totalCount = totalCount,
            coveragePercent = coveragePercent,
            currentLevel = currentLevel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (showRegionCard && selectedRegion != null) 8.dp else 16.dp
                )
                .align(Alignment.BottomStart)
        )

        // Bottom RegionCard
        AnimatedVisibility(
            visible = showRegionCard && selectedRegion != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (selectedRegion != null) {
                RegionCard(
                    region = selectedRegion!!,
                    attractionCount = viewModel.getAttractionCountForRegion(selectedRegion!!.regionId),
                    canDrillDown = canDrillDown,
                    onMarkFootprint = { regionId, level ->
                        viewModel.markFootprint(regionId, level)
                    },
                    onDrillDown = {
                        showRegionCard = false
                        viewModel.drillIntoRegion(selectedRegion!!.regionId)
                    },
                    onShowAttractions = {
                        showAttractionsSheet = true
                    }
                )
            }
        }
    }

    // Onboarding overlay
    OnboardingOverlay(
        visible = showOnboarding,
        onDismiss = { showOnboarding = false }
    )

    // Attractions bottom sheet
    if (showAttractionsSheet && selectedRegion != null) {
        AttractionsBottomSheet(
            attractions = selectedRegionAttractions,
            currentRegionId = selectedRegion!!.regionId,
            onMarkVisit = { attractionId, regionId, level ->
                viewModel.markAttractionVisit(attractionId, regionId, level)
            },
            onRemoveVisit = { attractionId ->
                viewModel.removeAttractionVisit(attractionId)
            },
            onAttractionClick = { attractionId ->
                showAttractionsSheet = false
                navController.navigate(AttractionDetailScreen(attractionId))
            },
            onDismiss = {
                showAttractionsSheet = false
            }
        )
    }

    // Drill-down hint
    if (drillDownHint != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDrillDownHint() },
            title = { Text("继续探索", color = Color.White) },
            text = { Text("放大查看「$drillDownHint」的子区域？", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    val regionId = viewModel.selectedRegion.value?.regionId
                        ?: viewModel.currentPath.value.lastOrNull()?.id
                    if (regionId != null) {
                        viewModel.drillIntoRegion(regionId)
                    }
                    viewModel.dismissDrillDownHint()
                }) { Text("查看", color = MapChinaColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDrillDownHint() }) { Text("取消", color = Color.Gray) }
            },
            containerColor = Color(0xFF1A2C3D)
        )
    }

    // Achievement unlock dialog
    if (achievementResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAchievementUnlock() },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAchievementUnlock() }) {
                    Text("继续探索", color = MapChinaColors.Primary)
                }
            },
            title = { Text("成就解锁！", color = Color.White) },
            text = {
                Text(
                    "恭喜获得 ${achievementResult!!.newlyUnlocked.size} 个新成就，+${achievementResult!!.scoreAdded} 山河值",
                    color = Color.Gray
                )
            },
            containerColor = Color(0xFF1A2C3D)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttractionsBottomSheet(
    attractions: List<AttractionUi>,
    currentRegionId: String,
    onMarkVisit: (String, String, FootprintLevel) -> Unit,
    onRemoveVisit: (String) -> Unit,
    onAttractionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = Color(0xFF0F1923)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "4A/5A 景点",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (attractions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无景点数据", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(attractions, key = { it.id }) { attraction ->
                        AttractionSheetCard(
                            attraction = attraction,
                            regionId = currentRegionId,
                            onMarkVisit = onMarkVisit,
                            onRemoveVisit = onRemoveVisit,
                            onClick = { onAttractionClick(attraction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttractionSheetCard(
    attraction: AttractionUi,
    regionId: String,
    onMarkVisit: (String, String, FootprintLevel) -> Unit,
    onRemoveVisit: (String) -> Unit,
    onClick: () -> Unit
) {
    val isVisited = attraction.visitLevel != null
    val levelBadge = when (attraction.level) {
        "A5" -> "5A"
        "A4" -> "4A"
        else -> attraction.level
    }
    val bgColor = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep.copy(alpha = 0.2f)
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit.copy(alpha = 0.2f)
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy.copy(alpha = 0.2f)
        null -> Color(0xFF1A2C3D)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = levelBadge,
                    color = if (attraction.level == "A5") Color(0xFFFFD700) else Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") Color(0xFF332E00) else Color(0xFF0F3347),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attraction.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = if (isVisited) "已玩过" else "未到访",
                color = if (isVisited) MapChinaColors.FootprintDeep else Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isVisited) Color(0x332EC4B6) else Color(0x33888888))
                    .clickable {
                        if (isVisited) {
                            onRemoveVisit(attraction.id)
                        } else {
                            onMarkVisit(attraction.id, attraction.regionId, FootprintLevel.SHORT_VISIT)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CoverageOverlay(
    visitedCount: Int,
    totalCount: Int,
    coveragePercent: Int,
    currentLevel: MapZoomLevel,
    modifier: Modifier = Modifier
) {
    val levelLabel = when (currentLevel) {
        MapZoomLevel.NATIONAL -> "省"
        MapZoomLevel.PROVINCIAL -> "市"
        MapZoomLevel.CITY -> "区"
        MapZoomLevel.DISTRICT -> "区"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA0F1923))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${levelLabel}覆盖率",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$visitedCount/$totalCount",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFE94560),
            trackColor = Color(0xFF1A2C3D),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$coveragePercent%",
            color = Color(0xFFE94560),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
