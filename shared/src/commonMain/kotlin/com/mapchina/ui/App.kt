package com.mapchina.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapchina.ui.navigation.AppNavHost
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.AttractionsScreen
import com.mapchina.ui.navigation.StatsScreen
import com.mapchina.ui.navigation.AchievementScreen
import com.mapchina.ui.navigation.ProfileScreen
import com.mapchina.ui.navigation.Screen
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTheme
import kotlinx.coroutines.delay

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(MapScreen, "足迹地图", Icons.Default.LocationOn),
    BottomNavItem(AttractionsScreen, "景点", Icons.Default.Attractions),
    BottomNavItem(StatsScreen, "统计", Icons.Default.BarChart),
    BottomNavItem(AchievementScreen, "成就", Icons.Default.EmojiEvents),
    BottomNavItem(ProfileScreen, "我的", Icons.Default.Person),
)

@Composable
fun MapChinaApp() {
    MapChinaTheme {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(onFinish = { showSplash = false })
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hasRoute(item.screen::class) == true
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    AppNavHost(navController = navController)
                }

                if (showBottomBar) {
                    ImmersiveBottomBar(currentDestination, navController)
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
        delay(1800)
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0F2838),
                        Color(0xFF1A5C4E)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            val mountainColor = Color(0xFF264653).copy(alpha = 0.7f)
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.65f)
                    cubicTo(w * 0.15f, h * 0.45f, w * 0.3f, h * 0.55f, w * 0.45f, h * 0.42f)
                    cubicTo(w * 0.55f, h * 0.35f, w * 0.65f, h * 0.50f, w * 0.8f, h * 0.38f)
                    cubicTo(w * 0.9f, h * 0.30f, w * 0.95f, h * 0.48f, w, h * 0.55f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = mountainColor,
                style = Fill
            )

            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.75f)
                    cubicTo(w * 0.2f, h * 0.60f, w * 0.4f, h * 0.70f, w * 0.6f, h * 0.58f)
                    cubicTo(w * 0.75f, h * 0.50f, w * 0.9f, h * 0.65f, w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = Color(0xFF2A9D8F).copy(alpha = 0.35f),
                style = Fill
            )

            val sunX = w * 0.72f
            val sunY = h * 0.22f
            val sunRadius = w * 0.06f
            drawCircle(
                color = Color(0xFFF4A261).copy(alpha = 0.9f),
                radius = sunRadius,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF4A261).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(sunX, sunY),
                    radius = sunRadius * 3
                ),
                radius = sunRadius * 3,
                center = Offset(sunX, sunY)
            )

            drawCircle(
                color = Color(0xFF2EC4B6).copy(alpha = 0.15f),
                radius = w * 0.25f,
                center = Offset(w * 0.35f, h * 0.8f)
            )

            for (i in 0..3) {
                val yBase = h * 0.82f + i * 12f
                val waveAlpha = 0.12f - i * 0.02f
                drawPath(
                    path = Path().apply {
                        moveTo(0f, yBase)
                        cubicTo(w * 0.25f, yBase - 8f, w * 0.5f, yBase + 8f, w, yBase)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    },
                    color = Color(0xFF2EC4B6).copy(alpha = waveAlpha),
                    style = Fill
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                "MapChina",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                "用脚步丈量中国",
                color = MapChinaColors.FootprintShortVisit,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val lineY = size.height * 0.85f
            val lineWidth = size.width * 0.6f * alpha.value
            val startX = (size.width - lineWidth) / 2
            drawLine(
                color = MapChinaColors.Primary.copy(alpha = 0.6f),
                start = Offset(startX, lineY),
                end = Offset(startX + lineWidth, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ImmersiveBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    navController: androidx.navigation.NavHostController
) {
    Surface(
        color = Color(0xDD0F1923),
    ) {
        Column {
            androidx.compose.material3.NavigationBar(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.screen::class) } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (selected) MapChinaColors.Primary else Color.Gray
                            )
                        },
                        label = {
                            androidx.compose.material3.Text(
                                item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MapChinaColors.Primary else Color.Gray
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MapChinaColors.Primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}
