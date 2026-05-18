package com.mapchina.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mapchina.ui.navigation.ProfileScreen
import com.mapchina.ui.navigation.Screen
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTheme

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(MapScreen, "足迹地图", Icons.Default.LocationOn),
    BottomNavItem(AttractionsScreen, "景点", Icons.Default.Attractions),
    BottomNavItem(StatsScreen, "统计", Icons.Default.BarChart),
    BottomNavItem(ProfileScreen, "我的", Icons.Default.Person),
)

@Composable
fun MapChinaApp() {
    MapChinaTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val showBottomBar = bottomNavItems.any { item ->
            currentDestination?.hasRoute(item.screen::class) == true
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // 内容区域占满剩余空间
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f)
            ) {
                AppNavHost(navController = navController)
            }

            // 底部导航栏 + 透明沉浸式
            if (showBottomBar) {
                ImmersiveBottomBar(currentDestination, navController)
            }
        }
    }
}

@Composable
private fun ImmersiveBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    navController: androidx.navigation.NavHostController
) {
    Surface(
        color = Color(0xDD1A1A2E),
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
            // 系统导航栏区域延伸
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
