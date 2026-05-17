package com.mapchina.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapchina.ui.map.MapScreen
import com.mapchina.ui.attraction.AttractionsScreen
import com.mapchina.ui.stats.StatsScreen
import com.mapchina.ui.profile.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            MapScreen(navController = navController)
        }
        composable(Screen.Attractions.route) {
            AttractionsScreen(navController = navController)
        }
        composable(Screen.Stats.route) {
            StatsScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        composable(Screen.Login.route) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("登录页（待实现）")
            }
        }
        composable(Screen.RegionDetail.route) { backStackEntry ->
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("区域详情: $regionId")
            }
        }
        composable(Screen.AttractionDetail.route) { backStackEntry ->
            val attractionId = backStackEntry.arguments?.getString("attractionId") ?: ""
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("景点详情: $attractionId")
            }
        }
    }
}
