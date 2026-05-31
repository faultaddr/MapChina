package com.mapchina.ui.profile

import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.animation.pressScale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val profile by (viewModel?.profile?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(ProfileUi("未登录", null, null)) })
    val isLoggedIn by (viewModel?.isLoggedIn?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(false) })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Text(
            "我的",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar entrance animation
            val avatarScale = remember { Animatable(0.8f) }
            val avatarRotation = remember { Animatable(-5f) }
            val avatarAlpha = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                launch { avatarAlpha.animateTo(1f, tween(400)) }
                launch { avatarScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 150f)) }
                avatarRotation.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 150f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isLoggedIn) MapChinaColors.Primary.copy(alpha = 0.2f) else Color(0xFF3D3D5C))
                            .graphicsLayer {
                                scaleX = avatarScale.value
                                scaleY = avatarScale.value
                                rotationZ = avatarRotation.value
                                alpha = avatarAlpha.value
                            },
                        tint = if (isLoggedIn) MapChinaColors.Primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(profile.nickname, fontSize = 20.sp, color = Color.White)
                    if (profile.phone != null) {
                        Text(profile.phone!!, fontSize = 14.sp, color = Color.Gray)
                    }

                    val levelInfo = profile.levelInfo
                    val animatedLevelProgress by animateFloatAsState(
                        targetValue = levelInfo?.progressToNext ?: 0f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label = "levelProgress"
                    )
                    if (isLoggedIn && levelInfo != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MapChinaColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Lv${levelInfo.currentLevel}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    levelInfo.currentTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!levelInfo.isMaxLevel) {
                                    LinearProgressIndicator(
                                        progress = { animatedLevelProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MapChinaColors.Primary,
                                        trackColor = Color(0xFF1A1A2E)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${levelInfo.currentScore}",
                                color = MapChinaColors.Primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoggedIn) {
                Button(
                    onClick = { viewModel?.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Error),
                    modifier = Modifier.fillMaxWidth().pressScale()
                ) { Text("退出登录") }
            } else {
                Button(
                    onClick = { onNavigateToLogin?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary),
                    modifier = Modifier.fillMaxWidth().pressScale()
                ) { Text("登录") }
            }
        }
    }
}
