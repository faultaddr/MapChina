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
import com.mapchina.map.MapController
import com.mapchina.map.MapZoomLevel
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.ui.achievement.AchievementUnlockDialog
import com.mapchina.ui.navigation.JournalDetailScreen
import com.mapchina.ui.navigation.CarvingScreen
import com.mapchina.platform.DevicePhoto
import com.mapchina.ui.common.EmptyState
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
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

    var showRegionCard by remember { mutableStateOf(false) }
    var showAttractionsSheet by remember { mutableStateOf(false) }
    var photoPreviewCluster by remember { mutableStateOf<PhotoCluster?>(null) }
    val scope = rememberCoroutineScope()
    var showDartTravel by remember { mutableStateOf(false) }

    val canDrillDown = selectedRegion?.let { viewModel.canDrillIntoRegion(it.regionId) } ?: (currentLevel == MapZoomLevel.NATIONAL || currentLevel == MapZoomLevel.PROVINCIAL || currentLevel == MapZoomLevel.CITY)
    val levelLabel = when (currentLevel) {
        MapZoomLevel.NATIONAL -> "国家"
        MapZoomLevel.PROVINCIAL -> "省"
        MapZoomLevel.CITY -> "市"
        MapZoomLevel.DISTRICT -> "县"
    }
    val visitedCount = regions.count { it.footprintLevel != null || it.childCoverageRate > 0f }
    val totalCount = regions.size
    val coveragePercent = if (totalCount > 0) visitedCount * 100 / totalCount else 0

    // Single tap on region → pulse + show card
    mapController.setOnRegionTapListener { regionId ->
        if (showRegionCard && selectedRegion?.regionId == regionId) return@setOnRegionTapListener
        mapController.pulseOverlay(regionId)
        viewModel.selectRegion(regionId)
        showRegionCard = true
    }

    // Close region card → restore overlay
    LaunchedEffect(showRegionCard) {
        if (!showRegionCard) {
            mapController.restorePulsedOverlay()
        }
    }
    mapController.setOnMarkerTapListener { markerId ->
        val cluster = photoClusters.find { it.id == markerId }
        if (cluster != null) {
            photoPreviewCluster = cluster
        } else {
            navController.navigate(AttractionDetailScreen(markerId))
        }
    }

    // Re-sync map state when AMap view is recreated (e.g. after navigation back)
    mapController.setOnMapReadyListener {
        val (lat, lng, zoom) = viewModel.getSavedCameraState()
        mapController.setCamera(lat, lng, zoom, false)
        viewModel.reloadData()
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
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            BreadcrumbNav(
                path = listOf(BreadcrumbItem("", "中国")) + currentPath.map { BreadcrumbItem(it.id, it.name) },
                onNavigateUp = { viewModel.navigateUp() },
                onNavigateTo = { if (it.isNotEmpty()) viewModel.navigateTo(it) }
            )
        }

        // Top-right FAB: coverage + photo toggle
        MapFab(
            visitedCount = visitedCount,
            totalCount = totalCount,
            coveragePercent = coveragePercent,
            currentLevel = levelLabel,
            photoMarkersVisible = photoMarkersVisible,
            onTogglePhotos = { viewModel.togglePhotoMarkers() },
            onDepart = { showDartTravel = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp)
        )

        // Bottom RegionCard
        val bottomBarOffset = com.mapchina.ui.LocalScaffoldBottomPadding.current
        AnimatedVisibility(
            visible = showRegionCard && selectedRegion != null,
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
                    onDrillDown = {
                        val regionId = selectedRegion!!.regionId
                        showRegionCard = false
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
                        showRegionCard = false
                        navController.navigate(CarvingScreen(region.regionId, region.name))
                    },
                    onClose = {
                        showRegionCard = false
                        viewModel.clearSelection()
                    }
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
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("点击关闭", color = MapChinaColors.TextTertiary, fontSize = 11.sp)
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
            mapController.setCamera(34.5, 106.0, 3.8f, true)
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
                showRegionCard = true
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

    // Achievement unlock dialog with animation
    if (achievementResult != null) {
        val animatedScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "achievementScale"
        )
        val animatedScore by animateIntAsState(
            targetValue = achievementResult!!.scoreAdded,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "score"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MapChinaColors.TextPrimary.copy(alpha = 0.6f))
                .clickable { viewModel.dismissAchievementUnlock() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(enabled = false) { },
                colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glow badge
                    val glowAlpha by animateFloatAsState(
                        targetValue = 0.6f,
                        animationSpec = tween(800),
                        label = "glow"
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MapChinaColors.AccentGold.copy(alpha = glowAlpha * 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MapChinaColors.SurfaceElevated, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "成就解锁",
                        color = MapChinaColors.AccentGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Score counter
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("+", color = MapChinaColors.AccentGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "$animatedScore",
                            color = MapChinaColors.AccentGold,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("山河值", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Achievement list
                    achievementResult!!.newlyUnlocked.take(3).forEach { achievement ->
                        val rarity = viewModel.getAchievementRarity(achievement.achievementId)
                        val name = viewModel.getAchievementName(achievement.achievementId)
                        val desc = viewModel.getAchievementDescription(achievement.achievementId)
                        val rarityColor = when (rarity) {
                            "LEGENDARY" -> MapChinaColors.RarityLegendary
                            "EPIC" -> MapChinaColors.RarityEpic
                            "RARE" -> MapChinaColors.RarityRare
                            else -> MapChinaColors.RarityCommon
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(rarityColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = MapChinaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (desc.isNotEmpty()) {
                                    Text(desc, color = MapChinaColors.TextTertiary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    if (achievementResult!!.newlyUnlocked.size > 3) {
                        Text(
                            "…等 ${achievementResult!!.newlyUnlocked.size} 个成就",
                            color = MapChinaColors.TextTertiary,
                            fontSize = 12.sp
                        )
                    }

                    if (achievementResult!!.levelChanged) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "等级提升！Lv${achievementResult!!.newLevel}",
                            color = MapChinaColors.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "继续探索",
                        color = MapChinaColors.TextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.dismissAchievementUnlock() }
                    )
                }
            }
        }
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

            Text(
                text = if (isVisited) "已玩过" else "未到访",
                color = if (isVisited) MapChinaColors.FootprintDeep else MapChinaColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isVisited) MapChinaColors.Primary.copy(alpha = 0.2f) else MapChinaColors.BorderSubtle)
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
                "%.4f, %.4f".format(cluster.latitude, cluster.longitude),
                color = MapChinaColors.TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

