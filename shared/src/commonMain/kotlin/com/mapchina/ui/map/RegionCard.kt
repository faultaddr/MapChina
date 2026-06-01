package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun RegionCard(
    region: RegionFootprintUi,
    attractionCount: Int,
    canDrillDown: Boolean,
    onMarkFootprint: (String, FootprintLevel) -> Unit,
    onDrillDown: () -> Unit,
    onShowAttractions: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var footprintExpanded by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xDD0F1923))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Region name + status + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = region.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Coverage progress
        if (region.childCoverageRate > 0f || region.footprintLevel != null) {
            val coveragePercent = (region.childCoverageRate * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("覆盖率", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { region.childCoverageRate.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MapChinaColors.Primary,
                    trackColor = Color(0xFF1A2C3D)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$coveragePercent%", color = MapChinaColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mark footprint
            ActionChip(
                label = "标记足迹",
                color = MapChinaColors.Primary,
                onClick = { footprintExpanded = !footprintExpanded }
            )

            // Drill down
            if (canDrillDown) {
                ActionChip(
                    label = "查看下级",
                    color = Color(0xFF4A90D9),
                    onClick = onDrillDown
                )
            }

            // Attractions
            if (attractionCount > 0) {
                ActionChip(
                    label = "${attractionCount}个景点",
                    color = Color(0xFFFFA502),
                    onClick = onShowAttractions
                )
            }
        }

        // Inline footprint selection
        AnimatedVisibility(
            visible = footprintExpanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMedium)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FootprintButton(
                    label = "路过",
                    color = MapChinaColors.FootprintPassBy,
                    onClick = {
                        onMarkFootprint(region.regionId, FootprintLevel.PASS_BY)
                        footprintExpanded = false
                    },
                    enabled = region.footprintLevel == null
                )
                FootprintButton(
                    label = "短玩",
                    color = MapChinaColors.FootprintShortVisit,
                    onClick = {
                        onMarkFootprint(region.regionId, FootprintLevel.SHORT_VISIT)
                        footprintExpanded = false
                    },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.SHORT_VISIT } ?: true
                )
                FootprintButton(
                    label = "深度",
                    color = MapChinaColors.FootprintDeep,
                    onClick = {
                        onMarkFootprint(region.regionId, FootprintLevel.DEEP)
                        footprintExpanded = false
                    },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.DEEP } ?: true
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FootprintButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
    ) {
        Text(label)
    }
}
