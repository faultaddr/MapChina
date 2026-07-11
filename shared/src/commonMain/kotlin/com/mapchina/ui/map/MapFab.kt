package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180), initialOffsetY = { it / 5 }),
            exit = fadeOut(tween(160)) + slideOutVertically(tween(160), targetOffsetY = { it / 5 })
        ) {
            MapToolPanel(
                items = menuItems,
                surfaceColor = fabSurfaceColor,
                textColor = fabTextColor
            )
        }
        Spacer(Modifier.height(10.dp))
        if (firstFootprintActivation) {
            FirstFootprintMapAction(
                mapSelectionActive = mapSelectionActive,
                isExpanded = isExpanded,
                surfaceColor = fabSurfaceColor,
                textColor = fabTextColor,
                accentColor = fabPrimaryColor,
                onExpandedChange = onExpandedChange
            )
        } else {
            onMyLocation?.let { locate ->
                MapDockIconButton(
                    contentDescription = "当前定位",
                    icon = Icons.Default.MyLocation,
                    size = 44.dp,
                    surfaceColor = fabSurfaceColor,
                    iconColor = MapChinaColors.AccentBlue,
                    onClick = {
                        haptic.perform(HapticType.MEDIUM)
                        locate()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            MapDockIconButton(
                contentDescription = "地图工具",
                icon = Icons.Default.Tune,
                size = 48.dp,
                surfaceColor = fabSurfaceColor,
                iconColor = fabPrimaryColor,
                onClick = {
                    haptic.perform(HapticType.MEDIUM)
                    onExpandedChange(!isExpanded)
                }
            )
        }
    }
}

@Composable
private fun MapToolPanel(items: List<MenuItem>, surfaceColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = surfaceColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f)),
        modifier = Modifier
            .widthIn(min = 180.dp, max = 240.dp)
            .semantics { contentDescription = "地图工具菜单" }
    ) {
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = textColor.copy(alpha = 0.07f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.tint,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MapDockIconButton(
    contentDescription: String,
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp,
    surfaceColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = surfaceColor,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.10f)),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(if (size > 44.dp) 21.dp else 19.dp)
            )
        }
    }
}

@Composable
private fun FirstFootprintMapAction(
    mapSelectionActive: Boolean,
    isExpanded: Boolean,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    onExpandedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val label = if (mapSelectionActive) "轻点地图选择" else "添加第一处足迹"
    val icon = if (mapSelectionActive) Icons.Default.Explore else Icons.Default.AddLocationAlt

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = surfaceColor,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.12f)),
        modifier = Modifier
            .heightIn(min = if (mapSelectionActive) 42.dp else 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics(mergeDescendants = true) { contentDescription = label }
            .clickable {
                haptic.perform(HapticType.MEDIUM)
                onExpandedChange(!isExpanded)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
