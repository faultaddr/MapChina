package com.mapchina.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AchievementRarity
import com.mapchina.domain.model.UserAchievement
import com.mapchina.domain.service.AchievementUnlockResult
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun AchievementUnlockDialog(
    result: AchievementUnlockResult,
    achievementDefinitions: Map<String, String>,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D5C))
                    ) { Text("继续探索") }
                    if (onShare != null) {
                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                        ) { Text("分享") }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MapChinaColors.Primary.copy(alpha = 0.2f)),
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) { Text("继续探索") }
            }
        }
    }
}
