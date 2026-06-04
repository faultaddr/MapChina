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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
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

    Box(modifier = modifier) {
        // Single Canvas draws everything: disc + shadow + ring + arc
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .then(
                    if (visitedCount == 0 && onDepart != null) Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDepart)
                    else Modifier
                )
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerR = size.minDimension / 2 - 4.dp.toPx()
            val ringR = outerR - 5.dp.toPx()
            val strokeWidth = 4.dp.toPx()

            // Drop shadow (dark circle offset down-right)
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = outerR,
                center = Offset(center.x + 1.dp.toPx(), center.y + 2.dp.toPx())
            )

            // Main disc — warm white gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8F6F1),
                    ),
                    center = Offset(center.x - outerR * 0.25f, center.y - outerR * 0.25f),
                    radius = outerR
                ),
                radius = outerR,
                center = center
            )

            // Subtle border ring
            drawCircle(
                color = MapChinaColors.BorderMedium.copy(alpha = 0.5f),
                radius = outerR,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Progress track (background)
            drawCircle(
                color = MapChinaColors.BorderSubtle,
                radius = ringR,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc (foreground)
            if (progress > 0.005f) {
                val arcSize = Size(ringR * 2, ringR * 2)
                val topLeft = Offset(center.x - ringR, center.y - ringR)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor,
                            MapChinaColors.PrimaryVariant,
                            ringColor,
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
            }
        }

        // Center text overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                androidx.compose.material3.Text(
                    "开始旅程",
                    color = MapChinaColors.TextTertiary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                androidx.compose.material3.Text(
                    "$coveragePercent",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 20.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                androidx.compose.material3.Text(
                    when {
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
