package com.mapchina.ui.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.mapchina.ui.common.ExperiencePageHeader
import com.mapchina.ui.common.ExperienceSectionHeader
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.ui.collectAsState()
    DiscoverContent(
        ui = ui,
        onSearch = viewModel::search,
        onRecommendationClick = { id -> onNavigate(AttractionDetailScreen(id, fromDiscover = true)) },
        onSpotlightClick = { id -> onNavigate(AttractionDetailScreen(id)) },
        onPendingSuggestionsClick = { onNavigate(MapScreen) },
        modifier = modifier
    )
}

@Composable
fun DiscoverContent(
    ui: DiscoverUi,
    onSearch: (String) -> Unit,
    onRecommendationClick: (String) -> Unit,
    onSpotlightClick: (String) -> Unit = onRecommendationClick,
    onPendingSuggestionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalScaffoldBottomPadding.current
    val primaryRecommendation = ui.recommendations.firstOrNull()
    val scope = rememberCoroutineScope()
    val transitionProgress = remember { Animatable(0f) }
    var transitionTarget by remember { mutableStateOf<RecommendationTransitionTarget?>(null) }

    val beginRecommendationTransition: (DiscoverRecommendation, Int, Rect) -> Unit = { recommendation, rank, bounds ->
        if (transitionTarget == null) {
            transitionTarget = RecommendationTransitionTarget(recommendation, rank, bounds)
            scope.launch {
                transitionProgress.snapTo(0f)
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
                onRecommendationClick(recommendation.id)
                delay(32L)
                transitionTarget = null
                transitionProgress.snapTo(0f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = bottomPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExperiencePageHeader(Copy.DISCOVER_TITLE, Copy.DISCOVER_SUBTITLE)
            }
            item {
                DiscoverSpotlightCard(
                    recommendation = primaryRecommendation,
                    pendingCount = ui.pendingSuggestions.size,
                    recommendationCount = ui.recommendations.size,
                    onRecommendationClick = onSpotlightClick
                )
            }
            item {
                DiscoverSearchField(
                    value = ui.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                PendingSuggestionBanner(
                    pendingCount = ui.pendingSuggestions.size,
                    topSuggestion = ui.pendingSuggestions.firstOrNull(),
                    onClick = onPendingSuggestionsClick
                )
            }
            item { ExperienceSectionHeader("推荐去点亮") }
            if (ui.recommendations.isEmpty()) {
                item { DiscoverEmptyLine("暂无推荐") }
            } else {
                itemsIndexed(ui.recommendations, key = { _, recommendation -> recommendation.id }) { index, recommendation ->
                    RecommendationImageCard(
                        recommendation = recommendation,
                        rank = index + 1,
                        onClick = { bounds ->
                            beginRecommendationTransition(recommendation, index + 1, bounds)
                        }
                    )
                }
            }
            item { ExperienceSectionHeader("继续探索") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DiscoveryMiniFeature(
                        title = "附近可点亮",
                        subtitle = "定位开启后优先显示",
                        accent = MapChinaColors.AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    DiscoveryMiniFeature(
                        title = "主题路线",
                        subtitle = "五岳 / 丝路 / 海岸线",
                        accent = MapChinaColors.AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        transitionTarget?.let { target ->
            RecommendationTransitionOverlay(
                target = target,
                progress = transitionProgress.value
            )
        }
    }
}

@Composable
private fun DiscoverEmptyLine(text: String) {
    Text(text, color = MapChinaColors.TextTertiary, style = MapChinaTypography.Body)
}

@Composable
private fun DiscoverSpotlightCard(
    recommendation: DiscoverRecommendation?,
    pendingCount: Int,
    recommendationCount: Int,
    onRecommendationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val hasRecommendation = recommendation != null
    Surface(
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF173B35),
                            Color(0xFF245F58),
                            Color(0xFFB9873A)
                        )
                    )
                )
                .clickable(enabled = hasRecommendation) {
                    recommendation?.let { onRecommendationClick(it.id) }
                }
        ) {
            if (shouldUseSpotlightImage(recommendation?.imageUrl)) {
                AsyncImage(
                    model = recommendation?.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66000000),
                                Color(0x22000000),
                                Color(0xD9000000)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MapChinaColors.Primary.copy(alpha = 0.46f),
                                Color.Transparent,
                                MapChinaColors.AccentGold.copy(alpha = 0.28f)
                            )
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.18f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("今日推荐", color = Color.White, style = MapChinaTypography.Caption, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${recommendationCount} 个候选", color = Color.White.copy(alpha = 0.82f), style = MapChinaTypography.Caption)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        recommendation?.title ?: "先确认足迹，生成下一站",
                        color = Color.White,
                        style = MapChinaTypography.Headline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        recommendation?.reason ?: "系统会根据你的足迹和未点亮版图推荐景点",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MapChinaTypography.Body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DiscoverHeroMetric(label = "待确认", value = pendingCount.toString(), modifier = Modifier.weight(1f))
                    DiscoverHeroMetric(label = "推荐池", value = recommendationCount.toString(), modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MapChinaColors.Primary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("优先点亮", color = MapChinaColors.Primary, style = MapChinaTypography.Caption, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

fun shouldUseSpotlightImage(imageUrl: String?): Boolean {
    return !imageUrl.isNullOrBlank()
}

@Composable
private fun DiscoverHeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.16f), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = Color.White.copy(alpha = 0.76f), style = MapChinaTypography.Caption, maxLines = 1)
        }
    }
}

@Composable
private fun DiscoverSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(18.dp))
        },
        placeholder = { Text("搜索去过或想去的景点") },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MapChinaColors.Primary,
            unfocusedBorderColor = MapChinaColors.BorderSubtle,
            focusedContainerColor = MapChinaColors.SurfaceElevated,
            unfocusedContainerColor = MapChinaColors.SurfaceElevated
        ),
        modifier = modifier
    )
}

@Composable
private fun PendingSuggestionBanner(
    pendingCount: Int,
    topSuggestion: com.mapchina.domain.model.FootprintSuggestion?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActionable = topSuggestion != null
    val title = if (isActionable) "有 $pendingCount 条足迹待确认" else "暂无待确认足迹"
    val subtitle = topSuggestion
        ?.parentPath
        ?: "发现页会优先展示推荐，补录只在需要时提醒"
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isActionable, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MapChinaColors.PrimaryLight) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MapChinaColors.Primary,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title, maxLines = 1)
                Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (isActionable) "去足迹确认" else "已同步",
                color = if (isActionable) MapChinaColors.Primary else MapChinaColors.TextTertiary,
                style = MapChinaTypography.Caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = isActionable, onClick = onClick)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DiscoveryMiniFeature(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.12f)) {
                Icon(Icons.Default.Explore, contentDescription = null, tint = accent, modifier = Modifier.padding(7.dp).size(16.dp))
            }
            Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title, maxLines = 1)
            Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
