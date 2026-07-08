package com.mapchina.ui.shanhe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.LocalScaffoldBottomPadding
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = Copy.SHANHE_TITLE,
                    color = MapChinaColors.TextPrimary,
                    style = MapChinaTypography.Display
                )
                Text(
                    text = Copy.SHANHE_SUBTITLE,
                    color = MapChinaColors.TextSecondary,
                    style = MapChinaTypography.Body
                )
            }
        }
        item { ShanheEntry("勋章", "旅行荣誉", onClick = { onNavigate(BadgeWallScreen) }) }
        item { ShanheEntry("图鉴", "主题览胜", onClick = { onNavigate(AtlasScreen) }) }
        item { ShanheEntry("征版", "点亮中国版图", onClick = { onNavigate(ProvinceConquestScreen) }) }
        item { ShanheEntry("碑刻", "石壁留名", onClick = { onNavigate(CarvingListScreen(showAll = "true")) }) }
        item { ShanheEntry("统计", "足迹总览", onClick = { onNavigate(StatsScreen) }) }
        item { ShanheEntry("目标", "下一块版图", onClick = { onNavigate(DiscoverScreen) }) }
    }
}

@Composable
private fun ShanheEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
            Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
        }
    }
}
