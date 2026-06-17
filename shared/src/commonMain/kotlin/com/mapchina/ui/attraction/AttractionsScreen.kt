package com.mapchina.ui.attraction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
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
    var showMoreFilters by remember { mutableStateOf(false) }

    val primaryFilters = listOf(AttractionFilter.ALL, AttractionFilter.A5, AttractionFilter.A4, AttractionFilter.VISITED, AttractionFilter.UNVISITED, AttractionFilter.CUSTOM)

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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header - 大标题 + 进度
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    "探索中国",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MapChinaColors.TextPrimary
                )
                if (totalCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "$visitedCount",
                            color = MapChinaColors.Primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "/$totalCount 景点已探索",
                            color = MapChinaColors.TextTertiary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.weight(1f))
                        val percent = (visitedCount.toFloat() / totalCount * 100).toInt()
                        Text(
                            "$percent%",
                            color = MapChinaColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
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

            // 常驻搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel?.searchAttractions(it) },
                placeholder = { Text("搜索景点...", color = MapChinaColors.TextTertiary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MapChinaColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel?.searchAttractions("") }) {
                            Text("✕", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MapChinaColors.TextPrimary,
                    unfocusedTextColor = MapChinaColors.TextSecondary,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MapChinaColors.SurfaceElevated,
                    unfocusedContainerColor = MapChinaColors.SurfaceElevated,
                    cursorColor = MapChinaColors.Primary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            // 横向滚动筛选 chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                            color = if (isSelected) Color.White else MapChinaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (attractions.isEmpty()) {
                // Skeleton loading
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
                // 双列瀑布流
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAttractions, key = { it.id }) { attraction ->
                        WaterfallCard(
                            attraction = attraction,
                            onClick = {
                                onNavigate(AttractionDetailScreen(attraction.id))
                            }
                        )
                    }
                }
            }
        }

        // FAB - 碧玉渐变
        FloatingActionButton(
            onClick = {
                val resolved = viewModel?.resolveCurrentLocationForNewAttraction()
                if (resolved != null) {
                    onNavigate(CustomAttractionScreen(regionId = resolved.first, latitude = resolved.second.toString(), longitude = resolved.third.toString()))
                } else {
                    onNavigate(CustomAttractionScreen(regionId = ""))
                }
            },
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
                tint = Color.White
            )
        }
    }
}

@Composable
private fun WaterfallCard(
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
        FootprintLevel.DEEP -> "深游"
        FootprintLevel.SHORT_VISIT -> "小驻"
        FootprintLevel.PASS_BY -> "途经"
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
        Column {
            // 大图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                if (attraction.imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = attraction.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val state = painter.state.collectAsState().value
                        when (state) {
                            is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            else -> GridPlaceholder(attraction.name, levelColor)
                        }
                    }
                } else {
                    GridPlaceholder(attraction.name, levelColor)
                }

                // Level badge - 右上角
                if (attraction.level == "A5" || attraction.level == "A4") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            levelBadge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Visit badge - 左下角
                if (visitColor != null && visitLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = visitColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
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

            // 标题
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = attraction.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MapChinaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (visitColor == null) {
                    Text(
                        "未到访",
                        fontSize = 11.sp,
                        color = MapChinaColors.TextTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridPlaceholder(name: String, tintColor: Color) {
    Box(
        Modifier
            .fillMaxSize()
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
            color = tintColor.copy(alpha = 0.5f),
            fontSize = 36.sp,
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
