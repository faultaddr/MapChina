package com.mapchina.ui.map

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun MapFab(
    visitedCount: Int,
    totalCount: Int,
    coveragePercent: Int,
    currentLevel: String,
    photoMarkersVisible: Boolean,
    onTogglePhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val ringColor by animateColorAsState(
        targetValue = if (photoMarkersVisible) Color(0xFFE9C46A) else MapChinaColors.Primary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Box(modifier = modifier) {
        // Main circular FAB
        Surface(
            shape = CircleShape,
            color = Color(0xDD0F1923),
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background ring track
                Canvas(modifier = Modifier.size(76.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2 - 4.dp.toPx()
                    val arcSize = Size(radius * 2, radius * 2)
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )

                    drawCircle(
                        color = Color(0xFF1A2C3D),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$coveragePercent",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    )
                    Text(
                        "%",
                        color = ringColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                    Text(
                        "$visitedCount/$totalCount$currentLevel",
                        color = Color(0xFF7A909E),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        // Camera toggle badge
        Surface(
            shape = CircleShape,
            color = if (photoMarkersVisible) Color(0xFFE9C46A) else Color(0xFF1A2C3D),
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
                    tint = if (photoMarkersVisible) Color(0xFF0F1923) else Color(0xFF7A909E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
