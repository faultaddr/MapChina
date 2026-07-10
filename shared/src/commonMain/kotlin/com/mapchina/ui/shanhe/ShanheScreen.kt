package com.mapchina.ui.shanhe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.common.ExperiencePageHeader
import com.mapchina.ui.common.ExperienceSectionHeader
import com.mapchina.ui.navigation.AtlasScreen
import com.mapchina.ui.navigation.BadgeWallScreen
import com.mapchina.ui.navigation.CarvingListScreen
import com.mapchina.ui.navigation.DiscoverScreen
import com.mapchina.ui.navigation.ProvinceConquestScreen
import com.mapchina.ui.navigation.StatsScreen
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

private val DashboardShape = RoundedCornerShape(8.dp)

@Composable
fun ShanheScreen(
    viewModel: ShanheViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.ui.collectAsState()
    ShanheContent(ui = ui, onNavigate = onNavigate, modifier = modifier)
}

@Composable
fun ShanheContent(
    ui: ShanheUi,
    onNavigate: (NavKey) -> Unit,
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
            top = 14.dp,
            end = 16.dp,
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ExperiencePageHeader(Copy.SHANHE_TITLE, "把走过的地方，沉淀成自己的山河")
        }
        item { ShanheProgressHero(ui) }
        item {
            ShanheNextAction(ui = ui, onClick = { onNavigate(DiscoverScreen) })
        }
        item { ExperienceSectionHeader("成长图谱") }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GrowthEntryTile(
                    title = "勋章",
                    subtitle = "旅行荣誉",
                    metric = "${ui.unlockedCount}/${ui.totalAchievementCount}",
                    icon = Icons.Default.EmojiEvents,
                    accent = MapChinaColors.AccentGold,
                    onClick = { onNavigate(BadgeWallScreen) },
                    modifier = Modifier.weight(1f)
                )
                GrowthEntryTile(
                    title = "图鉴",
                    subtitle = "主题览胜",
                    metric = "去收集",
                    icon = Icons.Default.CollectionsBookmark,
                    accent = MapChinaColors.AccentBlue,
                    onClick = { onNavigate(AtlasScreen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GrowthEntryTile(
                    title = "征版",
                    subtitle = "点亮中国",
                    metric = "${ui.visitedProvinces}/${ui.totalProvinces}",
                    icon = Icons.Default.Map,
                    accent = MapChinaColors.Primary,
                    onClick = { onNavigate(ProvinceConquestScreen) },
                    modifier = Modifier.weight(1f)
                )
                GrowthEntryTile(
                    title = "碑刻",
                    subtitle = "石壁留名",
                    metric = "去题刻",
                    icon = Icons.Default.HistoryEdu,
                    accent = Color(0xFF8A6042),
                    onClick = { onNavigate(CarvingListScreen(showAll = "true")) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            ExperienceSectionHeader(
                title = "山河账本",
                actionLabel = "看统计",
                onAction = { onNavigate(StatsScreen) }
            )
        }
        item { CoverageLedger(ui = ui, onClick = { onNavigate(StatsScreen) }) }
        item { ExperienceSectionHeader("最近解锁") }
        item {
            RecentUnlocks(
                unlocks = ui.recentUnlocks,
                onClick = { onNavigate(BadgeWallScreen) }
            )
        }
    }
}

@Composable
private fun ShanheProgressHero(ui: ShanheUi) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DashboardShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF172422), Color(0xFF285D58), Color(0xFF406D66))
                )
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = DashboardShape, color = Color.White.copy(alpha = 0.12f)) {
                    Text(
                        "成长进度",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MapChinaTypography.Caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Landscape,
                    contentDescription = null,
                    tint = MapChinaColors.AccentGold,
                    modifier = Modifier.size(26.dp)
                )
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Lv.${ui.levelNumber}",
                    color = MapChinaColors.AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(ui.levelTitle, color = Color.White, style = MapChinaTypography.Headline)
                Spacer(Modifier.weight(1f))
                Text(
                    "${ui.currentScore} 山河值",
                    color = Color.White,
                    style = MapChinaTypography.Title
                )
            }
            LinearProgressIndicator(
                progress = { ui.levelProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MapChinaColors.AccentGold,
                trackColor = Color.White.copy(alpha = 0.16f)
            )
            Text(
                if (ui.remainingScore > 0) "距${ui.nextLevelTitle}还差 ${ui.remainingScore} 山河值" else "已抵达当前最高等级",
                color = Color.White.copy(alpha = 0.76f),
                style = MapChinaTypography.Caption
            )
        }
    }
}

@Composable
private fun ShanheNextAction(ui: ShanheUi, onClick: () -> Unit) {
    Surface(
        shape = DashboardShape,
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = DashboardShape, color = MapChinaColors.PrimaryLight) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = null,
                    tint = MapChinaColors.Primary,
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("下一步", color = MapChinaColors.Primary, style = MapChinaTypography.Caption, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(ui.targetProgressLabel, color = MapChinaColors.TextTertiary, style = MapChinaTypography.Caption)
                }
                Text(ui.targetTitle, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.targetBody, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { ui.targetProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = MapChinaColors.Primary,
                    trackColor = MapChinaColors.PrimaryLight
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MapChinaColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GrowthEntryTile(
    title: String,
    subtitle: String,
    metric: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = DashboardShape,
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = modifier.height(116.dp).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = DashboardShape, color = accent.copy(alpha = 0.11f)) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(7.dp).size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(metric, color = accent, style = MapChinaTypography.Caption, fontWeight = FontWeight.SemiBold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
                Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption)
            }
        }
    }
}

@Composable
private fun CoverageLedger(ui: ShanheUi, onClick: () -> Unit) {
    Surface(
        shape = DashboardShape,
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerMetric("省份", "${ui.visitedProvinces}/${ui.totalProvinces}", MapChinaColors.Primary, Modifier.weight(1f))
            LedgerDivider()
            LedgerMetric("城市", ui.visitedCities.toString(), MapChinaColors.AccentBlue, Modifier.weight(1f))
            LedgerDivider()
            LedgerMetric("区县", ui.visitedDistricts.toString(), MapChinaColors.AccentGold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LedgerMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption, maxLines = 1)
    }
}

@Composable
private fun LedgerDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MapChinaColors.BorderSubtle)
    )
}

@Composable
private fun RecentUnlocks(unlocks: List<String>, onClick: () -> Unit) {
    Surface(
        shape = DashboardShape,
        color = MapChinaColors.SurfaceElevated,
        border = BorderStroke(1.dp, MapChinaColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MilitaryTech,
                contentDescription = null,
                tint = MapChinaColors.AccentGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (unlocks.isEmpty()) {
                    Text("第一枚勋章正等你点亮", color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
                    Text("完成下一步，山河账本会留下第一笔", color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption)
                } else {
                    Text(unlocks.joinToString(" · "), color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("查看全部旅行荣誉", color = MapChinaColors.TextSecondary, style = MapChinaTypography.Caption)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MapChinaColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
