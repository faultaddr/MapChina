package com.mapchina.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaMotion
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback

@Composable
fun OnboardingOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MapChinaColors.SurfaceOverlay)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MapChinaColors.Primary,
                                    MapChinaColors.PrimaryVariant
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    Copy.SLOGAN,
                    style = MapChinaTypography.Display,
                    color = MapChinaColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "点击地图上的省份或城市\n标记你的到访足迹",
                    style = MapChinaTypography.Title,
                    color = MapChinaColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = MapChinaTypography.Title.lineHeight * 1.5f
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MapChinaRadius.Large)
                        .background(MapChinaColors.SurfaceElevated)
                        .padding(20.dp)
                ) {
                    OnboardingStep(1, "点击省份查看详情")
                    Spacer(modifier = Modifier.height(12.dp))
                    OnboardingStep(2, "标记足迹：${Copy.FOOTPRINT_PASS} / ${Copy.FOOTPRINT_SHORT} / ${Copy.FOOTPRINT_DEEP}")
                    Spacer(modifier = Modifier.height(12.dp))
                    OnboardingStep(3, "钻入下级查看城市和区县")
                    Spacer(modifier = Modifier.height(12.dp))
                    OnboardingStep(4, "收集成就，点亮版图")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { haptic.perform(HapticType.MEDIUM); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MapChinaRadius.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) {
                    Text(Copy.ACTION_EXPLORE, style = MapChinaTypography.Title, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MapChinaColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = MapChinaColors.Primary,
                style = MapChinaTypography.Caption,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MapChinaTypography.Title, color = MapChinaColors.TextPrimary)
    }
}
