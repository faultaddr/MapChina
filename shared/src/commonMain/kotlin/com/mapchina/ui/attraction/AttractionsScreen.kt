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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mapchina.domain.model.FootprintLevel
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
    navController: NavHostController,
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

            // Search bar
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            // Filter chips
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttractionFilter.values().forEach { filter ->
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAttractions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = if (searchQuery.isBlank() && selectedFilter == AttractionFilter.ALL) "搜索景点" else "未找到匹配",
                    subtitle = if (searchQuery.isBlank() && selectedFilter == AttractionFilter.ALL) "输入关键词开始搜索" else "试试其他筛选条件",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAttractions, key = { it.id }) { attraction ->
                        AttractionCard(
                            attraction = attraction,
                            onClick = {
                                navController.navigate(AttractionDetailScreen(attraction.id))
                            }
                        )
                    }
                }
            }
        }

        // Premium FAB
        FloatingActionButton(
            onClick = { navController.navigate(CustomAttractionScreen(regionId = "")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
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
            // Left accent bar for visited status
            if (visitColor != null) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(68.dp)
                        .background(visitColor)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (visitColor != null) 10.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image or initial placeholder
                Box {
                    if (attraction.imageUrl != null) {
                        SubcomposeAsyncImage(
                            model = attraction.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            val state = painter.state.collectAsState().value
                            when (state) {
                                is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                else -> InitialPlaceholder(attraction.name, levelColor)
                            }
                        }
                    } else {
                        InitialPlaceholder(attraction.name, levelColor)
                    }

                    // Level badge overlaid on image
                    if (attraction.level == "A5" || attraction.level == "A4") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = levelColor.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                levelBadge,
                                color = MapChinaColors.SurfaceElevated,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Visit status or unvisited tag
                    if (visitColor != null && visitLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = visitColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(visitColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    visitLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = visitColor
                                )
                            }
                        }
                    }
                }

                // Right side: unvisited indicator or visit dot
                if (visitColor == null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MapChinaColors.BorderSubtle.copy(alpha = 0.6f)
                    ) {
                        Text(
                            "未到访",
                            fontSize = 11.sp,
                            color = MapChinaColors.TextTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialPlaceholder(name: String, tintColor: Color) {
    Box(
        Modifier
            .size(68.dp)
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
