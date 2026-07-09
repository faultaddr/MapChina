package com.mapchina.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.common.ExperienceMetricPill
import com.mapchina.ui.common.ExperiencePageHeader
import com.mapchina.ui.common.ExperienceSectionHeader
import com.mapchina.ui.common.ExperienceSoftCard
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

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
        onRecommendationClick = { id -> onNavigate(AttractionDetailScreen(id)) },
        modifier = modifier
    )
}

@Composable
fun DiscoverContent(
    ui: DiscoverUi,
    onSearch: (String) -> Unit,
    onRecommendationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalScaffoldBottomPadding.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
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
            val primaryRecommendation = ui.recommendations.firstOrNull()
            ExperienceSoftCard {
                Text("下一块可点亮", color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
                Spacer(Modifier.height(4.dp))
                Text(
                    primaryRecommendation?.reason ?: "先确认一个足迹，系统会推荐下一站",
                    color = MapChinaColors.TextSecondary,
                    style = MapChinaTypography.Body
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ExperienceMetricPill(
                        label = "待确认",
                        value = ui.pendingSuggestions.size.toString(),
                        accent = MapChinaColors.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    ExperienceMetricPill(
                        label = "推荐",
                        value = ui.recommendations.size.toString(),
                        accent = MapChinaColors.AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("搜索去过或想去的景点") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { ExperienceSectionHeader("待补录") }
        if (ui.pendingSuggestions.isEmpty()) {
            item { DiscoverEmptyLine("暂无待确认足迹") }
        } else {
            items(ui.pendingSuggestions, key = { it.id }) { suggestion ->
                DiscoverInfoCard(
                    title = suggestion.parentPath,
                    subtitle = "${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}",
                    badge = "去足迹确认",
                    onClick = {}
                )
            }
        }
        item { ExperienceSectionHeader("补地图推荐") }
        if (ui.recommendations.isEmpty()) {
            item { DiscoverEmptyLine("暂无推荐") }
        } else {
            items(ui.recommendations, key = { it.id }) { recommendation ->
                DiscoverInfoCard(
                    title = recommendation.title,
                    subtitle = "${recommendation.subtitle} · ${recommendation.reason}",
                    badge = recommendation.levelLabel,
                    onClick = { onRecommendationClick(recommendation.id) }
                )
            }
        }
        item { ExperienceSectionHeader("附近可点亮") }
        item { DiscoverEmptyLine("开启定位后显示附近可点亮景点") }
        item { ExperienceSectionHeader("主题路线") }
        item { DiscoverEmptyLine("五岳、丝路、海岸线等主题路线将在推荐能力增强阶段补齐") }
    }
}

@Composable
private fun DiscoverEmptyLine(text: String) {
    Text(text, color = MapChinaColors.TextTertiary, style = MapChinaTypography.Body)
}

@Composable
private fun DiscoverInfoCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = MapChinaColors.TextPrimary,
                    style = MapChinaTypography.Title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Text(badge, color = MapChinaColors.AccentBlue, style = MapChinaTypography.Caption)
                }
            }
            Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
        }
    }
}
