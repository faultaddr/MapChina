package com.mapchina.ui.attraction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.common.EmptyState
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.navigation.CustomAttractionScreen
import com.mapchina.ui.theme.MapChinaColors

enum class AttractionFilter(val label: String) {
    ALL("全部"),
    A5("5A"),
    A4("4A"),
    CUSTOM("自定义"),
    VISITED("已到访"),
    UNVISITED("未到访")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttractionsScreen(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    viewModel: AttractionViewModel? = null,
    modifier: Modifier = Modifier
) {
    val attractions by (viewModel?.attractions?.collectAsState() ?: remember {
        mutableStateOf(
            emptyList<AttractionUi>()
        )
    })
    val searchQuery by (viewModel?.searchQuery?.collectAsState() ?: remember { mutableStateOf("") })
    var selectedFilter by remember { mutableStateOf(AttractionFilter.ALL) }
    var searchExpanded by remember { mutableStateOf(false) }
    var showMoreFilters by remember { mutableStateOf(false) }

    val primaryFilters = listOf(AttractionFilter.ALL, AttractionFilter.A5, AttractionFilter.A4, AttractionFilter.VISITED)
    val moreFilters = listOf(AttractionFilter.CUSTOM, AttractionFilter.UNVISITED)

    val filteredAttractions = remember(attractions, selectedFilter) {
        when (selectedFilter) {
            AttractionFilter.ALL -> attractions
            AttractionFilter.A5 -> attractions.filter { it.level == "A5" }
            AttractionFilter.A4 -> attractions.filter { it.level == "A4" }
            AttractionFilter.CUSTOM -> attractions.filter { it.isCustom }
            AttractionFilter.VISITED -> attractions.filter { it.visitLevel != null }
            AttractionFilter.UNVISITED -> attractions.filter { it.visitLevel == null }
        }
    }

    val visitedCount = attractions.count { it.visitLevel != null }
    val totalCount = attractions.size
    val bottomPadding = LocalScaffoldBottomPadding.current

    Box(modifier = modifier.fillMaxSize().background(MapChinaColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Hero header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MapChinaColors.Primary.copy(alpha = 0.08f),
                                MapChinaColors.Background
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (totalCount > 0) {
                            Text(
                                "$visitedCount/$totalCount 已探索",
                                color = MapChinaColors.TextTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (totalCount > 0) {
                        val progress by animateFloatAsState(
                            targetValue = visitedCount.toFloat() / totalCount,
                            animationSpec = tween(600),
                            label = "headerProgress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MapChinaColors.BorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MapChinaColors.Primary,
                                                MapChinaColors.PrimaryVariant
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Search bar (collapsible)
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically(tween(200)),
                exit = shrinkVertically(tween(150))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel?.searchAttractions(it) },
                    label = { Text("搜索景点", color = MapChinaColors.TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MapChinaColors.Primary.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel?.searchAttractions("") }) {
                                Text("✕", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MapChinaColors.TextPrimary,
                        unfocusedTextColor = MapChinaColors.TextSecondary,
                        focusedBorderColor = MapChinaColors.Primary,
                        unfocusedBorderColor = MapChinaColors.BorderMedium,
                        focusedContainerColor = MapChinaColors.SurfaceElevated,
                        unfocusedContainerColor = MapChinaColors.SurfaceElevated,
                        cursorColor = MapChinaColors.Primary
                    )
                )
            }

            // Filter chips row with search toggle
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search icon toggle
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (searchExpanded) MapChinaColors.Primary.copy(alpha = 0.12f) else MapChinaColors.SurfaceElevated,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 20.dp),
                        onClick = { searchExpanded = !searchExpanded }
                    )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = if (searchExpanded) MapChinaColors.Primary else MapChinaColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp).size(16.dp)
                    )
                }

                primaryFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val chipColor by animateColorAsState(
                        targetValue = if (isSelected) MapChinaColors.Primary else MapChinaColors.SurfaceElevated,
                        animationSpec = tween(200),
                        label = "chip"
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = chipColor,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 20.dp),
                            onClick = { selectedFilter = filter }
                        )
                    ) {
                        Text(
                            filter.label,
                            color = if (isSelected) MapChinaColors.SurfaceElevated else MapChinaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // "更多" dropdown
                Box {
                    val isMoreSelected = selectedFilter in moreFilters
                    val moreChipColor by animateColorAsState(
                        targetValue = if (isMoreSelected) MapChinaColors.Primary else MapChinaColors.SurfaceElevated,
                        animationSpec = tween(200),
                        label = "moreChip"
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = moreChipColor,
                        shadowElevation = if (isMoreSelected) 2.dp else 0.dp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 20.dp),
                            onClick = { showMoreFilters = true }
                        )
                    ) {
                        Text(
                            if (isMoreSelected) selectedFilter.label else "更多",
                            color = if (isMoreSelected) MapChinaColors.SurfaceElevated else MapChinaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreFilters,
                        onDismissRequest = { showMoreFilters = false }
                    ) {
                        moreFilters.forEach { filter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        filter.label,
                                        color = if (selectedFilter == filter) MapChinaColors.Primary else MapChinaColors.TextPrimary,
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedFilter = filter
                                    showMoreFilters = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (attractions.isEmpty()) {
                // Skeleton loading state
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(4) {
                        SkeletonAttractionCard()
                    }
                }
            } else if (filteredAttractions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = if (searchQuery.isBlank() && selectedFilter == AttractionFilter.ALL) "搜索景点" else "未找到匹配",
                    subtitle = if (searchQuery.isBlank() && selectedFilter == AttractionFilter.ALL) "输入关键词开始搜索" else "试试其他筛选条件",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAttractions, key = { it.id }) { attraction ->
                        AttractionCard(
                            attraction = attraction,
                            onClick = {
                                onNavigate(AttractionDetailScreen(attraction.id))
                            }
                        )
                    }
                }
            }
        }

        // Premium FAB
        FloatingActionButton(
            onClick = { onNavigate(CustomAttractionScreen(regionId = "")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
            containerColor = MapChinaColors.Primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加景点",
                tint = MapChinaColors.SurfaceElevated
            )
        }
    }
}

@Composable
private fun AttractionCard(
    attraction: AttractionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelBadge = when (attraction.level) {
        "A5" -> "5A"
        "A4" -> "4A"
        "CUSTOM" -> "自定义"
        else -> attraction.level
    }
    val levelColor = when (attraction.level) {
        "A5" -> MapChinaColors.AccentGold
        "A4" -> MapChinaColors.AccentBlue
        else -> MapChinaColors.TextTertiary
    }

    val visitColor = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> null
    }

    val visitLabel = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> "深度游览"
        FootprintLevel.SHORT_VISIT -> "短暂停留"
        FootprintLevel.PASS_BY -> "路过"
        null -> null
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image or initial placeholder (enlarged)
            Box {
                if (attraction.imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = attraction.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 100.dp, height = 84.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    ) {
                        val state = painter.state.collectAsState().value
                        when (state) {
                            is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            else -> InitialPlaceholder(attraction.name, levelColor, widthDp = 100, heightDp = 84)
                        }
                    }
                } else {
                    InitialPlaceholder(attraction.name, levelColor, widthDp = 100, heightDp = 84)
                }

                // Level badge overlaid on image
                if (attraction.level == "A5" || attraction.level == "A4") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            levelBadge,
                            color = MapChinaColors.SurfaceElevated,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Visit indicator on image
                if (visitColor != null && visitLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = visitColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            visitLabel,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attraction.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MapChinaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (attraction.description != null) {
                        Text(
                            text = attraction.description,
                            fontSize = 12.sp,
                            color = MapChinaColors.TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    // Unvisited tag (visited shown on image)
                    if (visitColor == null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MapChinaColors.BorderSubtle.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                "未到访",
                                fontSize = 11.sp,
                                color = MapChinaColors.TextTertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
private fun InitialPlaceholder(name: String, tintColor: Color, widthDp: Int = 68, heightDp: Int = 68) {
    Box(
        Modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.12f),
                        tintColor.copy(alpha = 0.06f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.take(1),
            color = tintColor.copy(alpha = 0.6f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SkeletonAttractionCard(modifier: Modifier = Modifier) {
    val shimmerColor = MapChinaColors.BorderSubtle.copy(alpha = 0.3f)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 84.dp)
                    .background(shimmerColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(shimmerColor, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .background(shimmerColor, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(20.dp)
                        .background(shimmerColor, RoundedCornerShape(6.dp))
                )
            }
        }
    }
}
