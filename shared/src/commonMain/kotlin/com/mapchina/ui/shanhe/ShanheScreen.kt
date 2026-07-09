package com.mapchina.ui.shanhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.LocalScaffoldBottomPadding
import com.mapchina.ui.common.ExperienceActionTile
import com.mapchina.ui.common.ExperiencePageHeader
import com.mapchina.ui.common.ExperienceSectionHeader
import com.mapchina.ui.common.ExperienceSoftCard
import com.mapchina.ui.navigation.AtlasScreen
import com.mapchina.ui.navigation.BadgeWallScreen
import com.mapchina.ui.navigation.CarvingListScreen
import com.mapchina.ui.navigation.DiscoverScreen
import com.mapchina.ui.navigation.ProvinceConquestScreen
import com.mapchina.ui.navigation.StatsScreen
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun ShanheScreen(
    viewModel: ShanheViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalScaffoldBottomPadding.current
    val ui by viewModel.ui.collectAsState()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExperiencePageHeader(Copy.SHANHE_TITLE, Copy.SHANHE_SUBTITLE)
        }
        item {
            ExperienceSoftCard {
                Text(ui.levelTitle, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Headline)
                Spacer(Modifier.height(6.dp))
                Text(ui.scoreLabel, color = MapChinaColors.AccentGold, style = MapChinaTypography.Title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "从确认可能足迹开始，把旅行慢慢沉淀成自己的山河账本",
                    color = MapChinaColors.TextSecondary,
                    style = MapChinaTypography.Body
                )
            }
        }
        item {
            ExperienceSoftCard {
                Text(ui.targetTitle, color = MapChinaColors.Primary, style = MapChinaTypography.Title)
                Spacer(Modifier.height(6.dp))
                Text(ui.targetBody, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
            }
        }
        item { ExperienceSectionHeader("成长入口") }
        item { ShanheEntry("勋章", "旅行荣誉", MapChinaColors.AccentGold, onClick = { onNavigate(BadgeWallScreen) }) }
        item { ShanheEntry("图鉴", "主题览胜", MapChinaColors.AccentBlue, onClick = { onNavigate(AtlasScreen) }) }
        item { ShanheEntry("征版", "点亮中国版图", MapChinaColors.Primary, onClick = { onNavigate(ProvinceConquestScreen) }) }
        item { ShanheEntry("碑刻", "石壁留名", Color(0xFF8B7355), onClick = { onNavigate(CarvingListScreen(showAll = "true")) }) }
        item { ShanheEntry("统计", "足迹总览", MapChinaColors.FootprintShortVisit, onClick = { onNavigate(StatsScreen) }) }
        item { ShanheEntry("目标", "下一块版图", MapChinaColors.FootprintDeep, onClick = { onNavigate(DiscoverScreen) }) }
    }
}

@Composable
private fun ShanheEntry(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    ExperienceActionTile(
        title = title,
        subtitle = subtitle,
        accent = accent,
        onClick = onClick
    )
}
