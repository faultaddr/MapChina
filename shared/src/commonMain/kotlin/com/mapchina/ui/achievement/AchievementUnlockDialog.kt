package com.mapchina.ui.achievement

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.UserAchievement
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.ui.animation.pressScale
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.coroutines.delay

@Composable
fun AchievementUnlockDialog(
    result: AchievementUnlockResult,
    achievementDefinitions: Map<String, String>,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    var phase by remember { mutableIntStateOf(0) }
    val overlayAlpha = remember { Animatable(0f) }
    val burstScale = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: 遮罩淡入 (0-200ms)
        phase = 1
        overlayAlpha.animateTo(0.7f, tween(200))

        // Phase 2: 光晕爆发 (200-500ms)
        phase = 2
        burstScale.snapTo(0f)
        burstScale.animateTo(3f, tween(300, easing = LinearOutSlowInEasing))

        // Phase 3: 卡片弹出 (500-800ms)
        phase = 3
        cardScale.animateTo(1.05f, spring(dampingRatio = 0.6f, stiffness = 200f))
        cardScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))

        // Phase 4: 内容渐显 (800-1200ms)
        phase = 4
        contentAlpha.animateTo(1f, tween(400))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha.value))
                .clickable(enabled = phase >= 4, onClick = onDismiss)
        )

        // 光晕爆发
        if (phase >= 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * burstScale.value / 6f
                val burstAlpha = (0.6f * (1f - burstScale.value / 3f)).coerceIn(0f, 0.6f)
                drawCircle(
                    color = MapChinaColors.Primary.copy(alpha = burstAlpha),
                    radius = radius,
                    center = center
                )
            }
        }

        // 卡片内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = if (phase >= 3) 1f else 0f
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .graphicsLayer { alpha = contentAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (result.newlyUnlocked.size == 1) {
                        SingleUnlockContent(result.newlyUnlocked.first(), achievementDefinitions)
                    } else {
                        MultiUnlockContent(result.newlyUnlocked, achievementDefinitions)
                    }

                    if (result.levelChanged) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "等级提升！",
                            color = MapChinaColors.Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "+${result.scoreAdded} 山河值",
                        color = MapChinaColors.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).pressScale(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D5C))
                        ) { Text("继续探索") }
                        if (onShare != null) {
                            Button(
                                onClick = onShare,
                                modifier = Modifier.weight(1f).pressScale(),
                                colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                            ) { Text("分享") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleUnlockContent(
    achievement: UserAchievement,
    definitions: Map<String, String>
) {
    val name = definitions[achievement.achievementId] ?: "新成就"
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MapChinaColors.Primary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MapChinaColors.Primary)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text("解锁新成就", color = Color.Gray, fontSize = 13.sp)
    Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun MultiUnlockContent(
    achievements: List<UserAchievement>,
    definitions: Map<String, String>
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MapChinaColors.Primary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MapChinaColors.Primary)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text("本次共解锁", color = Color.Gray, fontSize = 13.sp)
    Text("${achievements.size} 个成就", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    achievements.take(5).forEach { a ->
        val name = definitions[a.achievementId] ?: "成就"
        Text(name, color = Color.Gray, fontSize = 13.sp)
    }
    if (achievements.size > 5) {
        Text("...等 ${achievements.size} 个", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun LevelUpDialog(
    newLevel: Int,
    newTitle: String,
    score: Int,
    nextTitle: String,
    onDismiss: () -> Unit
) {
    val cardScale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val levelBounce = remember { Animatable(0.5f) }

    LaunchedEffect(Unit) {
        cardScale.animateTo(1.05f, spring(dampingRatio = 0.6f, stiffness = 200f))
        cardScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
        levelBounce.animateTo(1.2f, spring(dampingRatio = 0.4f, stiffness = 300f))
        levelBounce.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 200f))
        contentAlpha.animateTo(1f, tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .graphicsLayer { alpha = contentAlpha.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MapChinaColors.Primary.copy(alpha = 0.2f))
                        .graphicsLayer {
                            scaleX = levelBounce.value
                            scaleY = levelBounce.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lv$newLevel", color = MapChinaColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("恭喜升级为", color = Color.Gray, fontSize = 13.sp)
                Text("「$newTitle」", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("你已累计获得 $score 山河值", color = Color.Gray, fontSize = 13.sp)
                Text("下一站是「$nextTitle」", color = MapChinaColors.Primary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().pressScale(),
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) { Text("继续探索") }
            }
        }
    }
}
