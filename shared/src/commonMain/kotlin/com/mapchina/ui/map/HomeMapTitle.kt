package com.mapchina.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.map.MapTheme
import com.mapchina.map.visualStyle
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun HomeMapTitle(
    path: List<BreadcrumbItem>,
    currentLevel: String,
    visitedCount: Int,
    totalCount: Int,
    coveragePercent: Int,
    onNavigateUp: () -> Unit,
    onNavigateToNational: () -> Unit,
    mapTheme: MapTheme,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val visualStyle = mapTheme.visualStyle
    val accent = if (visualStyle.isDark) Color(0xFF64FFDA) else MapChinaColors.Primary
    val secondary = visualStyle.chromeContentColor.copy(alpha = 0.64f)
    val currentName = if (path.size == 1) {
        "中国足迹"
    } else {
        path.lastOrNull()?.name ?: "中国足迹"
    }
    val progressText = if (totalCount > 0) "$visitedCount/$totalCount" else "加载中"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "地图铭牌" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (path.size > 1) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.perform(HapticType.MEDIUM)
                            onNavigateUp()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回上级",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = currentName,
                color = visualStyle.chromeContentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$progressText 已点亮",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(5.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = visualStyle.chromeColor.copy(alpha = 0.62f),
            shadowElevation = 2.dp,
            border = BorderStroke(
                1.dp,
                visualStyle.chromeContentColor.copy(alpha = 0.06f)
            )
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                Text(
                    text = "足迹",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF44C6AF), Color(0xFF1D9E92))
                            ),
                            RoundedCornerShape(11.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "全国",
                    color = visualStyle.chromeContentColor.copy(alpha = 0.64f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(onClick = onNavigateToNational)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${currentLevel}级地图 · $coveragePercent%",
                color = secondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(3.dp)
                    .background(visualStyle.chromeContentColor.copy(alpha = 0.10f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((coveragePercent / 100f).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(MapChinaColors.AccentGold, CircleShape)
                )
            }
        }
    }
}
