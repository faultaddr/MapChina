package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.map.MapTheme
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
    visitedCount: Int,
    totalCount: Int,
    coveragePercent: Int,
    currentLevel: String,
    photoMarkersVisible: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTogglePhotos: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDepart: (() -> Unit)? = null,
    onNavigateToNational: (() -> Unit)? = null,
    onMyLocation: (() -> Unit)? = null,
    mapTheme: MapTheme = MapTheme.DEFAULT,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = mapTheme == MapTheme.STARRY_NIGHT
    val fabPrimaryColor = if (isDarkTheme) Color(0xFF64FFDA) else MapChinaColors.Primary
    val fabPrimaryVariant = if (isDarkTheme) Color(0xFF00BFA5) else MapChinaColors.PrimaryVariant
    val fabSurfaceColor = if (isDarkTheme) Color(0xFF1A2332) else MapChinaColors.SurfaceElevated
    val fabTextColor = if (isDarkTheme) Color(0xFFE0E0E0) else MapChinaColors.TextPrimary
    val fabTextTertiary = if (isDarkTheme) Color(0xFF90A4AE) else MapChinaColors.TextTertiary
    val haptic = LocalHapticFeedback.current
    val menuItems = buildList {
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
            if (photoMarkersVisible) "隐藏照片" else "照片标记",
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
        Surface(
            shape = CircleShape,
            color = if (visitedCount == 0) fabPrimaryColor else fabSurfaceColor.copy(alpha = 0.92f),
            shadowElevation = 9.dp,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.perform(HapticType.HEAVY)
                            onExpandedChange(false)
                            onNavigateToNational?.invoke()
                        },
                        onTap = {
                            haptic.perform(HapticType.MEDIUM)
                            onExpandedChange(!isExpanded)
                        }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = if (visitedCount == 0) Color.White.copy(alpha = 0.18f) else fabPrimaryColor.copy(alpha = 0.10f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (visitedCount == 0) Icons.Default.Navigation else Icons.Default.Explore,
                            contentDescription = if (visitedCount == 0) Copy.FAB_DEPART else "$coveragePercent%",
                            tint = if (visitedCount == 0) Color.White else fabPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Menu items below the FAB
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)),
            exit = scaleOut(tween(120))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                menuItems.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    MenuItemButton(item, surfaceColor = fabSurfaceColor, textColor = fabTextTertiary)
                }
            }
        }
    }
}

@Composable
private fun MenuItemButton(item: MenuItem, surfaceColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = surfaceColor,
            shadowElevation = 6.dp,
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = item.onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor, textAlign = TextAlign.Center)
    }
}
