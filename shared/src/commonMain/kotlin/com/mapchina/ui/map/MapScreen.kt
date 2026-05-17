package com.mapchina.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mapchina.domain.model.FootprintLevel

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
                            viewModel.selectRegion(regionId)
                            showFootprintSheet = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val visited = regions.count { it.footprintLevel != null }
                val total = regions.size
                Text(
                    text = "覆盖率: $visited/$total",
                    color = Color.White
                )
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
