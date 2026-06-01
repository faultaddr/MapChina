package com.mapchina.ui.map

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

data class RegionDetailUi(
    val regionId: String,
    val name: String,
    val footprintLevel: FootprintLevel?,
    val childCoverageRate: Float,
    val childRegions: List<RegionFootprintUi>,
    val attractionCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionDetailScreen(
    regionId: String,
    viewModel: MapViewModel,
    onBack: () -> Unit = {},
    onChildRegionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val regions by viewModel.regions.collectAsState()
    val region = regions.find { it.regionId == regionId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        TopAppBar(
            title = { Text(region?.name ?: "区域详情", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1923))
        )

        if (region == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("区域信息不可用", color = Color.Gray)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RegionSummaryCard(region)
            }

            if (region.childCoverageRate > 0f || region.footprintLevel != null) {
                item {
                    Text("下级区域", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            items(regions.filter { it.regionId != regionId }, key = { it.regionId }) { child ->
                ChildRegionCard(child, onClick = { onChildRegionClick(child.regionId) })
            }
        }
    }
}

@Composable
private fun RegionSummaryCard(region: RegionFootprintUi) {
    val statusText = when (region.footprintLevel) {
        FootprintLevel.DEEP -> "深度游览"
        FootprintLevel.SHORT_VISIT -> "短暂停留"
        FootprintLevel.PASS_BY -> "路过"
        null -> "未到访"
    }
    val statusColor = when (region.footprintLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(region.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            if (region.childCoverageRate > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                val coveragePercent = (region.childCoverageRate * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("覆盖率", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { region.childCoverageRate.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MapChinaColors.Primary,
                        trackColor = Color(0xFF0F1923)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$coveragePercent%", color = MapChinaColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChildRegionCard(region: RegionFootprintUi, onClick: () -> Unit) {
    val statusColor = when (region.footprintLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> Color(0xFF213647)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                region.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(region.childCoverageRate * 100).toInt()}%",
                color = if (region.childCoverageRate > 0f) MapChinaColors.Primary else Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
