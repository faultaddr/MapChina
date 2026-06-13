package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.map.ChinaMapView
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.ui.achievement.AchievementUnlockDialog
import com.mapchina.ui.navigation.JournalDetailScreen
import com.mapchina.ui.navigation.CarvingListScreen
import com.mapchina.platform.DevicePhoto
import com.mapchina.ui.common.EmptyState
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star

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
            Text("足迹地图（初始化中）", color = MapChinaColors.TextPrimary)
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
    val achievementResult by viewModel.achievementUnlock.collectAsState()

    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val photoClusters by viewModel.photoClusters.collectAsState()
    val photoMarkersVisible by viewModel.photoMarkersVisible.collectAsState()
    val autoMarkMessage by viewModel.autoMarkMessage.collectAsState()

    LaunchedEffect(regions) {
        if (regions.isEmpty()) {
            kotlinx.coroutines.delay(500)
            viewModel.reloadData()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.autoMarkFromGps()
    }

    var showAttractionsSheet by remember { mutableStateOf(false) }
    var photoPreviewCluster by remember { mutableStateOf<PhotoCluster?>(null) }
    val scope = rememberCoroutineScope()
    var showDartTravel by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val bottomPanel by viewModel.bottomPanel.collectAsState()
    val previewAttraction by viewModel.previewAttraction.collectAsState()

    // Derive showRegionCard from bottomPanel for RegionCard visibility
    val showRegionPanel = bottomPanel is BottomPanel.Region && selectedRegion != null

    val canDrillDown = selectedRegion?.let { viewModel.canDrillIntoRegion(it.regionId) } ?: (currentLevel == MapZoomLevel.NATIONAL || currentLevel == MapZoomLevel.PROVINCIAL || currentLevel == MapZoomLevel.CITY)
    val levelLabel = when (currentLevel) {
        MapZoomLevel.NATIONAL -> "省"
        MapZoomLevel.PROVINCIAL -> "市"
        MapZoomLevel.CITY -> "县"
        MapZoomLevel.DISTRICT -> "县"
    }
    val visitedCount = regions.count { it.footprintLevel != null || it.childCoverageRate > 0f }
    val totalCount = regions.size
    val coveragePercent = if (totalCount > 0) visitedCount * 100 / totalCount else 0

    // Single tap on region → pulse + show card
    mapController.setOnRegionTapListener { regionId ->
        if (bottomPanel is BottomPanel.Region && selectedRegion?.regionId == regionId) return@setOnRegionTapListener
        mapController.pulseOverlay(regionId)
        viewModel.selectRegion(regionId)
        viewModel.showRegionPanel(regionId)
    }

    // Double tap on region → drill into region
    mapController.setOnRegionDoubleTapListener { regionId ->
        viewModel.drillIntoRegion(regionId)
    }

    // Viewport constraint: lock pan at national level, free at drill-down levels
    LaunchedEffect(currentLevel) {
        if (currentLevel == MapZoomLevel.NATIONAL) {
            mapController.viewport.panEnabled = false
            mapController.viewport.setChinaBounds()
        } else {
            mapController.viewport.panEnabled = true
            mapController.viewport.clearBounds()
        }
    }

    // Close region card → restore overlay
    LaunchedEffect(bottomPanel) {
        if (bottomPanel !is BottomPanel.Region) {
            mapController.restorePulsedOverlay()
        }
    }
    mapController.setOnMarkerTapListener { markerId ->
        val cluster = photoClusters.find { it.id == markerId }
        if (cluster != null) {
            photoPreviewCluster = cluster
        } else {
            viewModel.showAttractionPreview(markerId)
        }
    }

    // Re-sync map state when map view is recreated (e.g. after navigation back)
    mapController.setOnMapReadyListener {
        if (currentLevel == MapZoomLevel.NATIONAL) {
            mapController.fitChinaInView(false)
        } else {
            val (lat, lng, zoom) = viewModel.getSavedCameraState()
            mapController.setCamera(lat, lng, zoom, false)
        }
        viewModel.reloadData()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen map
        ChinaMapView(
            controller = mapController,
            modifier = Modifier.fillMaxSize()
        )

        // Top breadcrumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            BreadcrumbNav(
                path = listOf(BreadcrumbItem("", "中国")) + currentPath.map { BreadcrumbItem(it.id, it.name) },
                onNavigateUp = { viewModel.navigateUp() },
                onNavigateTo = { if (it.isNotEmpty()) viewModel.navigateTo(it) }
            )
        }

        // Scrim to dismiss FAB menu
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = { fabExpanded = false })
            )
        }

        // Top-right FAB: feature hub
        MapFab(
            visitedCount = visitedCount,
            totalCount = totalCount,
            coveragePercent = coveragePercent,
            currentLevel = levelLabel,
            photoMarkersVisible = photoMarkersVisible,
            isExpanded = fabExpanded,
            onExpandedChange = { fabExpanded = it },
            onTogglePhotos = { viewModel.togglePhotoMarkers() },
            onDepart = { showDartTravel = true },
            onNavigateToNational = { viewModel.navigateToNational() },
            onMyLocation = { viewModel.moveToCurrentLocation() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp)
        )

        // Bottom RegionCard
        val bottomBarOffset = com.mapchina.ui.LocalScaffoldBottomPadding.current
        AnimatedVisibility(
            visible = showRegionPanel,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(200)
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarOffset)
        ) {
            if (selectedRegion != null) {
                RegionCard(
                    region = selectedRegion!!,
                    attractionCount = viewModel.getAttractionCountForRegion(selectedRegion!!.regionId),
                    canDrillDown = canDrillDown,
                    onMarkFootprint = { regionId, level ->
                        viewModel.markFootprint(regionId, level)
                    },
                    onRemoveFootprint = { regionId ->
                        viewModel.removeFootprint(regionId)
                    },
                    onDrillDown = {
                        val regionId = selectedRegion!!.regionId
                        viewModel.clearBottomPanel()
                        scope.launch {
                            delay(200)
                            viewModel.drillIntoRegion(regionId)
                        }
                    },
                    onShowAttractions = {
                        showAttractionsSheet = true
                    },
                    onOpenCarving = {
                        val region = selectedRegion!!
                        viewModel.clearBottomPanel()
                        navController.navigate(CarvingListScreen(regionId = region.regionId, regionName = region.name))
                    },
                    onClose = {
                        viewModel.clearBottomPanel()
                        viewModel.clearSelection()
                    }
                )
            }
        }

        // Attraction preview card
        AnimatedVisibility(
            visible = bottomPanel is BottomPanel.AttractionPreview && previewAttraction != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(200)
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarOffset)
        ) {
            if (previewAttraction != null) {
                AttractionPreviewCard(
                    attraction = previewAttraction!!,
                    onViewDetail = {
                        navController.navigate(AttractionDetailScreen(previewAttraction!!.id))
                    },
                    onClose = { viewModel.clearBottomPanel() }
                )
            }
        }

        // Auto-mark snackbar
        if (autoMarkMessage != null) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MapChinaColors.SurfaceElevated,
                    border = MapChinaCard.border,
                    modifier = Modifier.clickable { viewModel.dismissAutoMarkMessage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            autoMarkMessage!!,
                            color = MapChinaColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "撤销",
                            color = MapChinaColors.Error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.undoLastAutoMark()
                                    viewModel.dismissAutoMarkMessage()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "✕",
                            color = MapChinaColors.TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.dismissAutoMarkMessage() }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Achievement unlock → top banner notification
        if (achievementResult != null) {
            val animatedScore by animateIntAsState(
                targetValue = achievementResult!!.scoreAdded,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "score"
            )

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(250)
                ) + fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MapChinaColors.SurfaceElevated,
                    shadowElevation = 8.dp,
                    border = MapChinaCard.border,
                    modifier = Modifier.clickable { viewModel.dismissAchievementUnlock() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MapChinaColors.AccentGold,
                                            MapChinaColors.AccentGold.copy(alpha = 0.7f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = MapChinaColors.SurfaceElevated,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "成就解锁",
                                    color = MapChinaColors.AccentGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("+", color = MapChinaColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("$animatedScore", color = MapChinaColors.AccentGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(" 山河值", color = MapChinaColors.TextTertiary, fontSize = 11.sp)
                                }
                            }
                            val names = achievementResult!!.newlyUnlocked.take(2).map {
                                viewModel.getAchievementName(it.achievementId)
                            }
                            val extraCount = achievementResult!!.newlyUnlocked.size - 2
                            Text(
                                if (extraCount > 0) "${names.joinToString("、")} 等${achievementResult!!.newlyUnlocked.size}个成就"
                                else names.joinToString("、"),
                                color = MapChinaColors.TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (achievementResult!!.levelChanged) {
                                Text(
                                    "等级提升！Lv${achievementResult!!.newLevel}",
                                    color = MapChinaColors.Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "✕",
                            color = MapChinaColors.TextTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { viewModel.dismissAchievementUnlock() }
                        )
                    }
                }
            }
        }
    }

    // Photo preview overlay
    if (photoPreviewCluster != null) {
        PhotoPreviewOverlay(
            cluster = photoPreviewCluster!!,
            onDismiss = { photoPreviewCluster = null }
        )
    }

    // Onboarding overlay
    OnboardingOverlay(
        visible = showOnboarding,
        onDismiss = { viewModel.dismissOnboarding() }
    )

    // Dart travel overlay
    var dartTravelKey by remember { mutableStateOf(0) }
    var dartTravelReady by remember { mutableStateOf(false) }
    LaunchedEffect(showDartTravel) {
        if (showDartTravel) {
            dartTravelReady = false
            dartTravelKey++
            mapController.fitChinaInView(true)
            kotlinx.coroutines.delay(800)
            dartTravelReady = true
        } else {
            dartTravelReady = false
        }
    }
    if (showDartTravel && dartTravelReady) {
        val cityDots = remember { viewModel.getCityDots() }
        DartTravelOverlay(
            key = dartTravelKey,
            cityDots = cityDots,
            mapController = mapController,
            onCitySelected = { cityId ->
                showDartTravel = false
                viewModel.navigateTo(cityId)
                viewModel.selectRegion(cityId)
                viewModel.showRegionPanel(cityId)
                showAttractionsSheet = true
            },
            onDismiss = { showDartTravel = false }
        )
    }

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
        containerColor = MapChinaColors.Background
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "4A/5A 景点",
                color = MapChinaColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (attractions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Attractions,
                    title = "暂无景点数据",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
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
        null -> MapChinaColors.SurfaceElevated
    }

    var showLevelMenu by remember { mutableStateOf(false) }

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
                    color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.2f) else MapChinaColors.AccentBlue.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attraction.name,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box {
                Text(
                    text = when (attraction.visitLevel) {
                        FootprintLevel.DEEP -> Copy.FOOTPRINT_DEEP
                        FootprintLevel.SHORT_VISIT -> Copy.FOOTPRINT_SHORT
                        FootprintLevel.PASS_BY -> Copy.FOOTPRINT_PASS
                        null -> Copy.FOOTPRINT_NONE
                    },
                    color = if (isVisited) MapChinaColors.FootprintDeep else MapChinaColors.TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVisited) MapChinaColors.Primary.copy(alpha = 0.2f) else MapChinaColors.BorderSubtle)
                        .clickable { showLevelMenu = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                DropdownMenu(
                    expanded = showLevelMenu,
                    onDismissRequest = { showLevelMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { LevelMenuItemContent(Copy.FOOTPRINT_DEEP, MapChinaColors.FootprintDeep, attraction.visitLevel == FootprintLevel.DEEP) },
                        onClick = {
                            showLevelMenu = false
                            onMarkVisit(attraction.id, attraction.regionId, FootprintLevel.DEEP)
                        }
                    )
                    DropdownMenuItem(
                        text = { LevelMenuItemContent(Copy.FOOTPRINT_SHORT, MapChinaColors.FootprintShortVisit, attraction.visitLevel == FootprintLevel.SHORT_VISIT) },
                        onClick = {
                            showLevelMenu = false
                            onMarkVisit(attraction.id, attraction.regionId, FootprintLevel.SHORT_VISIT)
                        }
                    )
                    DropdownMenuItem(
                        text = { LevelMenuItemContent(Copy.FOOTPRINT_PASS, MapChinaColors.FootprintPassBy, attraction.visitLevel == FootprintLevel.PASS_BY) },
                        onClick = {
                            showLevelMenu = false
                            onMarkVisit(attraction.id, attraction.regionId, FootprintLevel.PASS_BY)
                        }
                    )
                    if (isVisited) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MapChinaColors.Error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("取消标记", color = MapChinaColors.Error, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showLevelMenu = false
                                onRemoveVisit(attraction.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelMenuItemContent(label: String, color: Color, isSelected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = if (isSelected) color else MapChinaColors.TextPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        if (isSelected) {
            Spacer(modifier = Modifier.width(4.dp))
            Text("✓", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PhotoPreviewOverlay(
    cluster: PhotoCluster,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MapChinaColors.SurfaceOverlay)
            .clickable(onClick = onDismiss)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${cluster.count} 张照片",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("点击空白关闭", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${formatCoord(cluster.latitude)}, ${formatCoord(cluster.longitude)}",
                color = MapChinaColors.TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatCoord(value: Double): String {
    val rounded = kotlin.math.round(value * 10000) / 10000
    val s = rounded.toString()
    val dot = s.indexOf('.')
    if (dot == -1) return "$s.0000"
    val after = s.length - dot - 1
    return if (after >= 4) s else s + "0".repeat(4 - after)
}

