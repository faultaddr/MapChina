package com.mapchina.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel? = null,
    modifier: Modifier = Modifier
) {
    val stats by (viewModel?.stats?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(StatsUi(0, 0, 0, 0, 0, 0, 0, 0)) })

    LaunchedEffect(viewModel) {
        viewModel?.refreshStats()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        Text(
            "统计",
            style = MapChinaTypography.Display,
            color = MapChinaColors.TextPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 覆盖率三卡片
            item { CoverageSection("省份", stats.visitedProvinces, stats.totalProvinces, stats.provincePercent) }
            item { CoverageSection("城市", stats.visitedCities, stats.totalCities, stats.cityPercent) }
            item { CoverageSection("区县", stats.visitedDistricts, stats.totalDistricts, stats.districtPercent) }

            // 4A/5A 饼状图
            item { LevelPieChart(stats.levelDistribution, stats.visitedAttractions) }

            // 到访级别环形图
            if (stats.visitLevelCounts.isNotEmpty()) {
                item { VisitLevelDonutChart(stats.visitLevelCounts) }
            }

            // 省份景点分布柱状图
            if (stats.provinceVisits.isNotEmpty()) {
                item { ProvinceBarChart(stats.provinceVisits) }
            }

            // 已到访景点列表
            if (stats.visitedAttractionList.isNotEmpty()) {
                item {
                    Text(
                        "已到访景点",
                        color = MapChinaColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                items(stats.visitedAttractionList, key = { it.id }) { attraction ->
                    VisitedAttractionCard(attraction)
                }
            }
        }
    }
}

@Composable
private fun LevelPieChart(dist: LevelDistribution, visitedTotal: Int) {
    var chartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartVisible = true }
    val chartProgress by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "pieProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MapChinaRadius.Medium)
            .background(MapChinaColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Text("景点等级分布", style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val measurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.size(160.dp)) {
                val total = (dist.a5Total + dist.a4Total).toFloat()
                if (total <= 0f) return@Canvas

                val strokeWidth = 28.dp.toPx()
                val diameter = min(size.width, size.height) - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // 背景环
                drawArc(
                    color = MapChinaColors.Background,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // 5A 已到访
                val a5Sweep = if (dist.a5Total > 0) (dist.a5Visited.toFloat() / dist.a5Total) * 180f * chartProgress else 0f
                drawArc(
                    color = MapChinaColors.AccentGold,
                    startAngle = -90f,
                    sweepAngle = a5Sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // 4A 已到访
                val a4Sweep = if (dist.a4Total > 0) (dist.a4Visited.toFloat() / dist.a4Total) * 180f * chartProgress else 0f
                drawArc(
                    color = MapChinaColors.AccentBlue,
                    startAngle = 90f,
                    sweepAngle = a4Sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // 中心数字 + 单位
                val centerText = "$visitedTotal"
                val unitText = "处"
                val textResult = measurer.measure(centerText)
                drawText(
                    textLayoutResult = textResult,
                    color = MapChinaColors.TextPrimary,
                    topLeft = Offset(
                        (size.width - textResult.size.width) / 2f,
                        (size.height - textResult.size.height) / 2f - 6.dp.toPx()
                    )
                )
                val unitResult = measurer.measure(unitText)
                drawText(
                    textLayoutResult = unitResult,
                    color = MapChinaColors.TextTertiary,
                    topLeft = Offset(
                        (size.width - unitResult.size.width) / 2f,
                        (size.height - textResult.size.height) / 2f + textResult.size.height - 6.dp.toPx()
                    )
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(MapChinaColors.AccentGold, "5A", dist.a5Visited, dist.a5Total)
                LegendItem(MapChinaColors.AccentBlue, "4A", dist.a4Visited, dist.a4Total)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, visited: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = MapChinaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$visited / $total", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VisitLevelDonutChart(levelCounts: Map<FootprintLevel, Int>) {
    val total = levelCounts.values.sum()
    if (total == 0) return

    var chartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartVisible = true }
    val chartProgress by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "donutProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MapChinaRadius.Medium)
            .background(MapChinaColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Text("到访级别分布", style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val measurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 24.dp.toPx()
                val diameter = min(size.width, size.height) - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                val segments = listOf(
                    FootprintLevel.DEEP to MapChinaColors.BadgeRed,
                    FootprintLevel.SHORT_VISIT to MapChinaColors.Error,
                    FootprintLevel.PASS_BY to MapChinaColors.FootprintShortVisit
                )
                for ((level, color) in segments) {
                    val count = levelCounts[level] ?: 0
                    if (count == 0) continue
                    val sweep = (count.toFloat() / total) * 360f * chartProgress
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize
                    )
                    startAngle += sweep
                }

                // 中心挖空
                drawCircle(
                    color = MapChinaColors.SurfaceElevated,
                    radius = diameter / 2f - strokeWidth
                )

                val centerText = "$total"
                val unitText = "次"
                val textResult = measurer.measure(centerText)
                drawText(
                    textLayoutResult = textResult,
                    color = MapChinaColors.TextPrimary,
                    topLeft = Offset(
                        (size.width - textResult.size.width) / 2f,
                        (size.height - textResult.size.height) / 2f - 6.dp.toPx()
                    )
                )
                val unitResult = measurer.measure(unitText)
                drawText(
                    textLayoutResult = unitResult,
                    color = MapChinaColors.TextTertiary,
                    topLeft = Offset(
                        (size.width - unitResult.size.width) / 2f,
                        (size.height - textResult.size.height) / 2f + textResult.size.height - 6.dp.toPx()
                    )
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LevelLegend(FootprintLevel.DEEP, Copy.FOOTPRINT_DEEP, MapChinaColors.BadgeRed, levelCounts[FootprintLevel.DEEP] ?: 0, total)
                LevelLegend(FootprintLevel.SHORT_VISIT, Copy.FOOTPRINT_SHORT, MapChinaColors.Error, levelCounts[FootprintLevel.SHORT_VISIT] ?: 0, total)
                LevelLegend(FootprintLevel.PASS_BY, Copy.FOOTPRINT_PASS, MapChinaColors.FootprintShortVisit, levelCounts[FootprintLevel.PASS_BY] ?: 0, total)
            }
        }
    }
}

@Composable
private fun LevelLegend(level: FootprintLevel, label: String, color: Color, count: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = MapChinaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            val pct = if (total > 0) count * 100 / total else 0
            Text("$count ($pct%)", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProvinceBarChart(provinceVisits: List<ProvinceVisitUi>) {
    val maxCount = provinceVisits.maxOfOrNull { it.attractionCount } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MapChinaRadius.Medium)
            .background(MapChinaColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Text("景点省份分布", style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("最多 ${maxCount} 个", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        provinceVisits.take(10).forEach { pv ->
            ProvinceBarRow(pv, maxCount)
        }
    }
}

@Composable
private fun ProvinceBarRow(pv: ProvinceVisitUi, maxCount: Int) {
    val animatedTotalWidth by animateFloatAsState(
        targetValue = pv.attractionCount.toFloat() / maxCount,
        animationSpec = tween(600),
        label = "barWidth"
    )
    val animatedVisitedRatio by animateFloatAsState(
        targetValue = if (pv.attractionCount > 0) pv.visitedCount.toFloat() / pv.attractionCount else 0f,
        animationSpec = tween(800),
        label = "visitedWidth"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pv.provinceName,
            color = MapChinaColors.TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.width(64.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MapChinaColors.Background)
        ) {
            // 总量背景条
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(animatedTotalWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MapChinaColors.CardBackgroundLight)
            )
            // 已到访前景条（渐变）
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(animatedTotalWidth * animatedVisitedRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MapChinaColors.Primary,
                                MapChinaColors.PrimaryVariant
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${pv.visitedCount}/${pv.attractionCount}",
            color = MapChinaColors.TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun VisitedAttractionCard(attraction: VisitedAttractionUi) {
    val levelBadge = when (attraction.level) {
        "A5" -> "5A"
        "A4" -> "4A"
        else -> attraction.level
    }
    val visitLabel = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> "深度"
        FootprintLevel.SHORT_VISIT -> "短玩"
        FootprintLevel.PASS_BY -> "路过"
    }
    val visitColor = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
                border = MapChinaCard.border,
                elevation = CardDefaults.cardElevation(defaultElevation = MapChinaCard.elevationDp.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = levelBadge,
                    color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.2f) else MapChinaColors.AccentBlue.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = attraction.name,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = visitLabel,
                color = visitColor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CoverageSection(
    label: String,
    visited: Int,
    total: Int,
    percent: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MapChinaRadius.Medium)
            .background(MapChinaColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(
                "$visited / $total",
                color = MapChinaColors.Primary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MapChinaColors.Primary,
            trackColor = MapChinaColors.Background
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(percent * 100).toInt()}%",
            fontSize = 12.sp,
            color = MapChinaColors.TextTertiary
        )
    }
}
