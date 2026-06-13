package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaCard
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaMotion
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography
import kotlinx.coroutines.delay

@Composable
fun RegionCard(
    region: RegionFootprintUi,
    attractionCount: Int,
    canDrillDown: Boolean,
    onMarkFootprint: (String, FootprintLevel) -> Unit,
    onRemoveFootprint: (String) -> Unit = {},
    onDrillDown: () -> Unit,
    onShowAttractions: () -> Unit,
    onOpenCarving: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var footprintExpanded by remember { mutableStateOf(false) }
    var confirmMessage by remember { mutableStateOf<String?>(null) }
    var lastMarkedLevel by remember { mutableStateOf<FootprintLevel?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(confirmMessage) {
        if (confirmMessage != null) {
            delay(2500)
            confirmMessage = null
        }
    }

    val statusText = when (region.footprintLevel) {
        FootprintLevel.DEEP -> Copy.FOOTPRINT_DEEP
        FootprintLevel.SHORT_VISIT -> Copy.FOOTPRINT_SHORT
        FootprintLevel.PASS_BY -> Copy.FOOTPRINT_PASS
        null -> Copy.FOOTPRINT_NONE
    }
    val statusColor = when (region.footprintLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> MapChinaColors.TextTertiary
    }

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MapChinaColors.SurfaceOverlay,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
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
                    style = MapChinaTypography.Headline,
                    color = MapChinaColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MapChinaTypography.Body,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), MapChinaRadius.Small)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = { haptic.perform(HapticType.LIGHT); onClose() }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MapChinaColors.TextTertiary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = MapChinaColors.TextTertiary, modifier = Modifier.size(16.dp))
                }
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
                Text("覆盖率", color = MapChinaColors.TextTertiary, style = MapChinaTypography.Body)
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { region.childCoverageRate.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(MapChinaRadius.Small),
                    color = MapChinaColors.Primary,
                    trackColor = MapChinaColors.SurfaceElevated
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$coveragePercent%", color = MapChinaColors.Primary, style = MapChinaTypography.Body, fontWeight = FontWeight.Bold)
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
                onClick = { haptic.perform(HapticType.MEDIUM); footprintExpanded = !footprintExpanded }
            )

            // Drill down
            if (canDrillDown) {
                ActionChip(
                    label = "查看下级",
                    color = MapChinaColors.AccentBlue,
                    onClick = { haptic.perform(HapticType.HEAVY); onDrillDown() }
                )
            }

            // Attractions
            if (attractionCount > 0) {
                ActionChip(
                    label = "${attractionCount}个景点",
                    color = MapChinaColors.FootprintShortVisit,
                    onClick = { haptic.perform(HapticType.LIGHT); onShowAttractions() }
                )
            }

            // Carving
            ActionChip(
                label = "题刻",
                color = Color(0xFF8B7355),
                onClick = { haptic.perform(HapticType.LIGHT); onOpenCarving() }
            )
        }

        // Inline footprint selection
        AnimatedVisibility(
            visible = footprintExpanded && confirmMessage == null,
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
                    label = Copy.FOOTPRINT_PASS,
                    color = MapChinaColors.FootprintPassBy,
                    onClick = {
                        haptic.perform(HapticType.SUCCESS)
                        onMarkFootprint(region.regionId, FootprintLevel.PASS_BY)
                        lastMarkedLevel = FootprintLevel.PASS_BY
                        footprintExpanded = false
                        confirmMessage = Copy.MARKED_PASS
                    },
                    enabled = region.footprintLevel == null
                )
                FootprintButton(
                    label = Copy.FOOTPRINT_SHORT,
                    color = MapChinaColors.FootprintShortVisit,
                    onClick = {
                        haptic.perform(HapticType.SUCCESS)
                        onMarkFootprint(region.regionId, FootprintLevel.SHORT_VISIT)
                        lastMarkedLevel = FootprintLevel.SHORT_VISIT
                        footprintExpanded = false
                        confirmMessage = Copy.MARKED_SHORT
                    },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.SHORT_VISIT } ?: true
                )
                FootprintButton(
                    label = Copy.FOOTPRINT_DEEP,
                    color = MapChinaColors.FootprintDeep,
                    onClick = {
                        haptic.perform(HapticType.SUCCESS)
                        onMarkFootprint(region.regionId, FootprintLevel.DEEP)
                        lastMarkedLevel = FootprintLevel.DEEP
                        footprintExpanded = false
                        confirmMessage = Copy.MARKED_DEEP
                    },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.DEEP } ?: true
                )
            }
        }

        // Inline confirmation
        AnimatedVisibility(
            visible = confirmMessage != null,
            enter = expandVertically() + fadeIn(tween(200)),
            exit = shrinkVertically() + fadeOut(tween(150))
        ) {
            if (confirmMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MapChinaColors.Primary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MapChinaColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            confirmMessage!!,
                            color = MapChinaColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (lastMarkedLevel != null) {
                            Text(
                                "撤销",
                                color = MapChinaColors.Error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.perform(HapticType.WARNING)
                                        onRemoveFootprint(region.regionId)
                                        confirmMessage = null
                                        lastMarkedLevel = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
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
            .clip(MapChinaRadius.Medium)
            .background(color.copy(alpha = 0.15f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MapChinaTypography.Body,
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
    val targetAlpha = if (enabled) 1f else 0.4f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(200),
        label = "footprintAlpha"
    )
    Box(
        modifier = modifier
            .clip(MapChinaRadius.Medium)
            .background(color.copy(alpha = 0.15f * alpha))
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = color.copy(alpha = alpha),
            style = MapChinaTypography.Title,
            fontWeight = FontWeight.SemiBold
        )
    }
}
