package com.mapchina.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel? = null,
    modifier: Modifier = Modifier
) {
    val stats by (viewModel?.stats?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(StatsUi(0, 0, 0, 0, 0, 0, 0, 0)) })

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(title = { Text("统计") })

        Spacer(modifier = Modifier.height(16.dp))

        CoverageSection("省份", stats.visitedProvinces, stats.totalProvinces, stats.provincePercent)
        Spacer(modifier = Modifier.height(16.dp))
        CoverageSection("城市", stats.visitedCities, stats.totalCities, stats.cityPercent)
        Spacer(modifier = Modifier.height(16.dp))
        CoverageSection("区县", stats.visitedDistricts, stats.totalDistricts, stats.districtPercent)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("景点到访", fontWeight = FontWeight.Medium)
            Text(
                "${stats.visitedAttractions} / ${stats.totalAttractions}",
                color = MapChinaColors.FootprintDeep
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(
                "$visited / $total",
                color = MapChinaColors.Primary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth(),
            color = MapChinaColors.FootprintDeep,
            trackColor = Color.LightGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(percent * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
