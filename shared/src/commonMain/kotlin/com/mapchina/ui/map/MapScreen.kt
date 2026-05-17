package com.mapchina.ui.map

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.mapchina.map.MapZoomLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("足迹地图（初始化中）")
        }
        return
    }

    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val regions by viewModel.regions.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()

    var showFootprintSheet by remember { mutableStateOf(false) }

    val canDrillDown = currentLevel.nextDrillDown() != null
    val visitedCount = regions.count { it.footprintLevel != null }
    val totalCount = regions.size
    val coveragePercent = if (totalCount > 0) visitedCount * 100 / totalCount else 0

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                BreadcrumbNav(
                    path = currentPath.map { BreadcrumbItem(it.id, it.name) },
                    onNavigateUp = { viewModel.navigateUp() },
                    onNavigateTo = { viewModel.navigateTo(it) }
                )
            },
            actions = {
                FilledIconButton(
                    onClick = { viewModel.toggleViewMode() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (viewMode == ViewMode.BLOCK) Color(0xFFE94560) else Color.Gray
                    )
                ) {
                    Icon(
                        if (viewMode == ViewMode.MAP) Icons.Default.ViewModule else Icons.Default.Map,
                        contentDescription = "切换视图"
                    )
                }
            }
        )

        ZoomLevelIndicator(currentLevel = currentLevel)

        Box(modifier = Modifier.fillMaxSize()) {
            when (viewMode) {
                ViewMode.MAP -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("地图视图（待集成原生 SDK）")
                    }
                }
                ViewMode.BLOCK -> {
                    ColorBlockView(
                        regions = regions,
                        onRegionTap = { regionId ->
                            if (canDrillDown) {
                                viewModel.drillIntoRegion(regionId)
                            } else {
                                viewModel.selectRegion(regionId)
                                showFootprintSheet = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            CoverageOverlay(
                visitedCount = visitedCount,
                totalCount = totalCount,
                coveragePercent = coveragePercent,
                currentLevel = currentLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            )
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
private fun ZoomLevelIndicator(currentLevel: MapZoomLevel) {
    val label = when (currentLevel) {
        MapZoomLevel.NATIONAL -> "全国"
        MapZoomLevel.PROVINCIAL -> "省级"
        MapZoomLevel.CITY -> "市级"
        MapZoomLevel.DISTRICT -> "区县级"
    }
    val color = when (currentLevel) {
        MapZoomLevel.NATIONAL -> Color(0xFF2D2D44)
        MapZoomLevel.PROVINCIAL -> Color(0xFF3D3D5C)
        MapZoomLevel.CITY -> Color(0xFF4D4D6C)
        MapZoomLevel.DISTRICT -> Color(0xFF5D5D7C)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "缩放: $label",
            color = Color.White,
            fontSize = 12.sp
        )
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
