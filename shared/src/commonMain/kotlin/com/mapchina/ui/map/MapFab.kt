package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.map.MapTheme
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors

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
    val menuItems = buildList {
        if (onNavigateToNational != null) {
            add(MenuItem("回到全国", Icons.Default.Explore, fabPrimaryColor) {
                onExpandedChange(false)
                onNavigateToNational()
            })
        }
        if (onDepart != null) {
            add(MenuItem("随机出发", Icons.Default.Navigation, fabPrimaryVariant) {
                onExpandedChange(false)
                onDepart()
            })
        }
        add(MenuItem(
            if (photoMarkersVisible) "隐藏照片" else "照片标记",
            if (photoMarkersVisible) Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera,
            if (photoMarkersVisible) MapChinaColors.FootprintPassBy else fabTextTertiary
        ) {
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
                onExpandedChange(false)
                onMyLocation()
            })
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        // FAB disc
        val progress by animateFloatAsState(
            targetValue = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
        val ringColor by animateColorAsState(
            targetValue = if (photoMarkersVisible) MapChinaColors.FootprintPassBy else fabPrimaryColor,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )

        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onExpandedChange(false)
                        onNavigateToNational?.invoke()
                    },
                    onTap = {
                        onExpandedChange(!isExpanded)
                    }
                )
            }
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerR = 36.dp.toPx()
                val ringR = outerR - 6.dp.toPx()
                val strokeWidth = 3.5.dp.toPx()

                if (visitedCount == 0) {
                    // ── Depart: polished jade stone ──
                    // Main body: 4-stop jade gradient with light source top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF20CFD4),
                                Color(0xFF14A3A8),
                                Color(0xFF0D7377),
                                Color(0xFF085456),
                            ),
                            center = Offset(center.x - outerR * 0.35f, center.y - outerR * 0.35f),
                            radius = outerR
                        ),
                        radius = outerR,
                        center = center
                    )
                    // Specular: oval highlight
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(center.x - outerR * 0.22f, center.y - outerR * 0.28f),
                            radius = outerR * 0.38f
                        ),
                        topLeft = Offset(center.x - outerR * 0.7f, center.y - outerR * 0.58f),
                        size = Size(outerR * 1.1f, outerR * 0.65f)
                    )
                    // Crescent shine
                    drawArc(
                        color = Color.White.copy(alpha = 0.16f),
                        startAngle = 195f, sweepAngle = 150f, useCenter = false,
                        topLeft = Offset(center.x - ringR, center.y - ringR),
                        size = Size(ringR * 2, ringR * 2),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Bottom depth shadow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f)),
                            center = Offset(center.x, center.y + outerR * 0.55f),
                            radius = outerR
                        ),
                        radius = outerR, center = center
                    )
                    // Rim light
                    drawArc(
                        color = Color.White.copy(alpha = 0.30f),
                        startAngle = 120f, sweepAngle = 80f, useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    // ── Coverage: ceramic dial ──
                    // Base: warm white radial with light source
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFBF9F4), Color(0xFFF0ECE3)),
                            center = Offset(center.x - outerR * 0.2f, center.y - outerR * 0.2f),
                            radius = outerR
                        ),
                        radius = outerR, center = center
                    )
                    // Character tint (jade bleed from center)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(MapChinaColors.Primary.copy(alpha = 0.05f), Color.Transparent),
                            center = center,
                            radius = outerR * 0.6f
                        ),
                        radius = outerR * 0.6f, center = center
                    )
                    // Specular
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(center.x - outerR * 0.20f, center.y - outerR * 0.26f),
                            radius = outerR * 0.35f
                        ),
                        topLeft = Offset(center.x - outerR * 0.65f, center.y - outerR * 0.52f),
                        size = Size(outerR * 1.0f, outerR * 0.6f)
                    )
                    // Bottom depth
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.05f)),
                            center = Offset(center.x, center.y + outerR * 0.5f),
                            radius = outerR
                        ),
                        radius = outerR, center = center
                    )
                    // Rim: bright inner + subtle outer
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = outerR - 0.5.dp.toPx(), center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = MapChinaColors.BorderMedium.copy(alpha = 0.22f),
                        radius = outerR, center = center,
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                    // Progress track
                    drawCircle(
                        color = MapChinaColors.BorderSubtle,
                        radius = ringR, center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    if (progress > 0.005f) {
                        val arcSize = Size(ringR * 2, ringR * 2)
                        val topLeft = Offset(center.x - ringR, center.y - ringR)
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(ringColor, MapChinaColors.PrimaryVariant, ringColor),
                                center = center
                            ),
                            startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Center content (same regardless of expanded state)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (visitedCount == 0) {
                    Text(Copy.FAB_DEPART, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(Copy.FAB_DEPART_SUB, color = Color.White.copy(alpha = 0.75f), fontSize = 8.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$coveragePercent", color = fabTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("%", color = fabPrimaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(
                        when {
                            coveragePercent < 5 -> Copy.FAB_COVERAGE_EARLY
                            coveragePercent < 20 -> Copy.FAB_COVERAGE_MID
                            else -> "$visitedCount/$totalCount$currentLevel"
                        },
                        color = fabTextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                    )
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
