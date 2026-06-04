package com.mapchina.ui.map

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun MapFab(
    visitedCount: Int,
    totalCount: Int,
    coveragePercent: Int,
    currentLevel: String,
    photoMarkersVisible: Boolean,
    onTogglePhotos: () -> Unit,
    onDepart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val ringColor by animateColorAsState(
        targetValue = if (photoMarkersVisible) MapChinaColors.FootprintPassBy else MapChinaColors.Primary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (visitedCount == 0) 0.25f else 0.12f,
        animationSpec = tween(600),
        label = "glowAlpha"
    )

    Box(modifier = modifier) {
        // Outer glow halo
        Canvas(modifier = Modifier.size(88.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = glowAlpha),
                        ringColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2
                ),
                radius = size.minDimension / 2,
                center = center
            )
        }

        // Main jade disc
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .offset(x = 6.dp, y = 6.dp)
                .then(
                    if (visitedCount == 0 && onDepart != null) Modifier.clickable(onClick = onDepart)
                    else Modifier
                )
        ) {
            Canvas(modifier = Modifier.size(76.dp)) {
                val strokeWidth = 4.5.dp.toPx()
                val outerRadius = size.minDimension / 2 - 2.dp.toPx()
                val innerRingRadius = outerRadius - 8.dp.toPx()

                // Jade disc body — gradient fill
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MapChinaColors.SurfaceOverlay,
                            MapChinaColors.SurfaceElevated.copy(alpha = 0.95f),
                        ),
                        center = Offset(size.width * 0.35f, size.height * 0.35f),
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Inner shadow ring (subtle depth)
                drawCircle(
                    color = MapChinaColors.BorderSubtle,
                    radius = innerRingRadius + 1.dp.toPx(),
                    center = center,
                    style = Stroke(width = 0.5.dp.toPx())
                )

                // Progress track
                drawCircle(
                    color = MapChinaColors.SurfaceElevated.copy(alpha = 0.4f),
                    radius = innerRingRadius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                val arcSize = Size(innerRingRadius * 2, innerRingRadius * 2)
                val topLeft = Offset(
                    (size.width - innerRingRadius * 2) / 2,
                    (size.height - innerRingRadius * 2) / 2
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            MapChinaColors.Primary,
                            MapChinaColors.PrimaryVariant,
                            MapChinaColors.Primary,
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Arc endpoint glow dot
                if (progress > 0.01f) {
                    val angleRad = Math.toRadians((-90.0 + 360.0 * progress))
                    val dotX = center.x + innerRingRadius * kotlin.math.cos(angleRad).toFloat()
                    val dotY = center.y + innerRingRadius * kotlin.math.sin(angleRad).toFloat()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MapChinaColors.PrimaryVariant.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            center = Offset(dotX, dotY),
                            radius = 6.dp.toPx()
                        ),
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // Center content
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-1).dp)
                ) {
                    if (visitedCount == 0) {
                        androidx.compose.material3.Text(
                            "出发",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MapChinaColors.Primary,
                                        MapChinaColors.PrimaryVariant
                                    )
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = MapChinaColors.Primary.copy(alpha = 0.2f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 4f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        androidx.compose.material3.Text(
                            "$coveragePercent",
                            color = MapChinaColors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        androidx.compose.material3.Text(
                            "%",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MapChinaColors.Primary,
                                        MapChinaColors.PrimaryVariant
                                    )
                                ),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                    androidx.compose.material3.Text(
                        when {
                            visitedCount == 0 -> "开始旅程"
                            coveragePercent < 5 -> "探索起步"
                            coveragePercent < 20 -> "渐入佳境"
                            else -> "$visitedCount/$totalCount$currentLevel"
                        },
                        color = MapChinaColors.TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Camera toggle badge
        Surface(
            shape = CircleShape,
            color = if (photoMarkersVisible) MapChinaColors.FootprintPassBy else MapChinaColors.SurfaceElevated,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onTogglePhotos)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (photoMarkersVisible) Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera,
                    contentDescription = if (photoMarkersVisible) "隐藏照片" else "显示照片",
                    tint = if (photoMarkersVisible) MapChinaColors.Background else MapChinaColors.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
