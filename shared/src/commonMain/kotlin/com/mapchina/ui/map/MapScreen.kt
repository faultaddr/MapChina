package com.mapchina.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
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

    // seed 异步加载后刷新数据
    LaunchedEffect(regions) {
        if (regions.isEmpty()) {
            kotlinx.coroutines.delay(500)
            viewModel.reloadData()
        }
    }

    var showFootprintSheet by remember { mutableStateOf(false) }
    var attractionsPanelExpanded by remember { mutableStateOf(true) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (attractionsPanelExpanded) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
        label = "chevronRotation"
    )

    val canDrillDown = currentLevel.nextDrillDown() != null
    val visitedCount = regions.count { it.footprintLevel != null || it.childCoverageRate > 0f }
    val totalCount = regions.size
    val coveragePercent = if (totalCount > 0) visitedCount * 100 / totalCount else 0

    val currentParentId = currentPath.lastOrNull()?.id
    val showAttractionsPanel = currentLevel != MapZoomLevel.NATIONAL && attractions.isNotEmpty()

    mapController.setOnRegionLongPressListener { regionId ->
        viewModel.selectRegion(regionId)
        showFootprintSheet = true
    }
    mapController.setOnRegionDoubleTapListener { regionId ->
        if (canDrillDown) {
            viewModel.drillIntoRegion(regionId)
        }
    }
    mapController.setOnMarkerTapListener { attractionId ->
        navController.navigate(AttractionDetailScreen(attractionId))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 全屏地图
        PlatformMapView(
            controller = mapController,
            modifier = Modifier.fillMaxSize()
        )

        // 顶部面包屑（悬浮，不占空间）
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

        // 底部覆盖率
        CoverageOverlay(
            visitedCount = visitedCount,
            totalCount = totalCount,
            coveragePercent = coveragePercent,
            currentLevel = currentLevel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = if (showAttractionsPanel && attractionsPanelExpanded) 0.dp else 16.dp,
                    bottom = 16.dp
                )
                .align(Alignment.BottomStart)
        )

        // 右侧景点面板
        AnimatedVisibility(
            visible = showAttractionsPanel,
            enter = slideInHorizontally { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally { it } + fadeOut(tween(250))
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            ) {
                // 收起/展开按钮
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .background(Color(0xAA1A1A2E))
                        .clickable { attractionsPanelExpanded = !attractionsPanelExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (attractionsPanelExpanded) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = if (attractionsPanelExpanded) "收起" else "展开",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }

                if (attractionsPanelExpanded) {
                    AttractionsPanel(
                        attractions = attractions,
                        currentRegionId = currentParentId ?: "",
                        onMarkVisit = { attractionId, regionId, level ->
                            viewModel.markAttractionVisit(attractionId, regionId, level)
                        },
                        onRemoveVisit = { attractionId ->
                            viewModel.removeAttractionVisit(attractionId)
                        },
                        onAttractionClick = { attractionId ->
                            navController.navigate(AttractionDetailScreen(attractionId))
                        },
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    if (showFootprintSheet && selectedRegion != null) {
        FootprintSheet(
            region = selectedRegion!!,
            onMarkFootprint = { regionId, level ->
                viewModel.markFootprint(regionId, level)
                showFootprintSheet = false
                viewModel.clearSelection()
            },
            onDismiss = {
                showFootprintSheet = false
                viewModel.clearSelection()
            }
        )
    }
}

@Composable
private fun AttractionsPanel(
    attractions: List<AttractionUi>,
    currentRegionId: String,
    onMarkVisit: (String, String, FootprintLevel) -> Unit,
    onRemoveVisit: (String) -> Unit,
    onAttractionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xDD1A1A2E))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "4A/5A 景点",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(attractions, key = { it.id }) { attraction ->
                AttractionPanelCard(
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

@Composable
private fun AttractionPanelCard(
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
        null -> Color(0xFF2D2D44)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                if (attraction.level == "A5") Color(0xFF332200) else Color(0xFF0D2744),
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (isVisited) "已玩过" else "未到访",
                    color = if (isVisited) MapChinaColors.FootprintDeep else Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVisited) Color(0x33E94560) else Color(0x33888888))
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

    val animatedVisited by animateFloatAsState(
        targetValue = visitedCount.toFloat(),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "visitedCount"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA1A1A2E))
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
                text = "${animatedVisited.toInt()}/$totalCount",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFE94560),
            trackColor = Color(0xFF2D2D44),
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
