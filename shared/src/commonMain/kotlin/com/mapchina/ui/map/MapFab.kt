package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.map.MapTheme
import com.mapchina.map.visualStyle
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback

private data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
fun MapFab(
    coveragePercent: Int,
    photoMarkersVisible: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTogglePhotos: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDepart: (() -> Unit)? = null,
    onNavigateToNational: (() -> Unit)? = null,
    onMyLocation: (() -> Unit)? = null,
    firstFootprintActivation: Boolean = false,
    mapSelectionActive: Boolean = false,
    onChooseMap: (() -> Unit)? = null,
    onSearchAttraction: (() -> Unit)? = null,
    onUseCurrentLocation: (() -> Unit)? = null,
    mapTheme: MapTheme = MapTheme.DEFAULT,
    modifier: Modifier = Modifier
) {
    val visualStyle = mapTheme.visualStyle
    val fabPrimaryColor = if (visualStyle.isDark) Color(0xFF64FFDA) else MapChinaColors.Primary
    val fabPrimaryVariant = if (visualStyle.isDark) Color(0xFF00BFA5) else MapChinaColors.PrimaryVariant
    val fabSurfaceColor = visualStyle.chromeColor.copy(alpha = 0.94f)
    val fabTextColor = visualStyle.chromeContentColor
    val fabTextTertiary = visualStyle.chromeContentColor.copy(alpha = 0.62f)
    val haptic = LocalHapticFeedback.current
    val menuItems = if (firstFootprintActivation) {
        buildList {
            onChooseMap?.let { chooseMap ->
                add(MenuItem("在地图上选择", Icons.Default.Explore, fabPrimaryColor) {
                    haptic.perform(HapticType.LIGHT)
                    onExpandedChange(false)
                    chooseMap()
                })
            }
            onSearchAttraction?.let { searchAttraction ->
                add(MenuItem("搜索景点", Icons.Default.Search, MapChinaColors.AccentGold) {
                    haptic.perform(HapticType.LIGHT)
                    onExpandedChange(false)
                    searchAttraction()
                })
            }
            onUseCurrentLocation?.let { useCurrentLocation ->
                add(MenuItem("使用当前位置", Icons.Default.MyLocation, MapChinaColors.AccentBlue) {
                    haptic.perform(HapticType.LIGHT)
                    onExpandedChange(false)
                    useCurrentLocation()
                })
            }
        }
    } else buildList {
        if (onNavigateToNational != null) {
            add(MenuItem("回到全国", Icons.Default.Explore, fabPrimaryColor) {
                haptic.perform(HapticType.LIGHT)
                onExpandedChange(false)
                onNavigateToNational()
            })
        }
        if (onDepart != null) {
            add(MenuItem("随机出发", Icons.Default.Navigation, fabPrimaryVariant) {
                haptic.perform(HapticType.LIGHT)
                onExpandedChange(false)
                onDepart()
            })
        }
        add(MenuItem(
            if (photoMarkersVisible) "隐藏照片回溯" else "照片回溯 · 实验",
            if (photoMarkersVisible) Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera,
            if (photoMarkersVisible) MapChinaColors.FootprintPassBy else fabTextTertiary
        ) {
            haptic.perform(HapticType.LIGHT)
            onTogglePhotos()
        })
        if (onShare != null) {
            add(MenuItem(Copy.SHARE_MAP, Icons.Default.Share, MapChinaColors.AccentGold) {
                onExpandedChange(false)
                onShare()
            })
        }
        if (onMyLocation != null) {
            add(MenuItem("当前定位", Icons.Default.MyLocation, MapChinaColors.AccentBlue) {
                haptic.perform(HapticType.LIGHT)
                onExpandedChange(false)
                onMyLocation()
            })
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                menuItems.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    MapToolMenuItem(
                        item = item,
                        surfaceColor = fabSurfaceColor,
                        textColor = fabTextColor
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (firstFootprintActivation) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = fabSurfaceColor,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.perform(HapticType.MEDIUM)
                            onExpandedChange(!isExpanded)
                        }
                ) {
                    Text(
                        text = if (mapSelectionActive) "轻点地图选择" else "添加第一处足迹",
                        color = fabTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.size(62.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    drawCircle(
                        color = fabSurfaceColor.copy(alpha = 0.78f),
                        radius = size.minDimension / 2f - strokeWidth,
                        style = Stroke(strokeWidth)
                    )
                    if (coveragePercent > 0) {
                        drawArc(
                            color = MapChinaColors.AccentGold,
                            startAngle = -90f,
                            sweepAngle = 360f * (coveragePercent / 100f).coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(strokeWidth)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = fabPrimaryColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .semantics {
                            contentDescription = if (firstFootprintActivation) {
                                "添加第一处足迹"
                            } else {
                                "地图工具"
                            }
                        }
                        .clickable {
                            haptic.perform(HapticType.MEDIUM)
                            onExpandedChange(!isExpanded)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (visualStyle.isDark) Color(0xFF0F1428) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapToolMenuItem(item: MenuItem, surfaceColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = surfaceColor,
        shadowElevation = 4.dp,
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = item.onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = item.tint,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                item.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
