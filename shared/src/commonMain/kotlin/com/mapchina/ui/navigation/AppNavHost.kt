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
import com.mapchina.ui.map.MapScreen as MapScreenComposable
import com.mapchina.ui.attraction.AttractionsScreen as AttractionsScreenComposable
import com.mapchina.ui.stats.StatsScreen as StatsScreenComposable
import com.mapchina.ui.profile.ProfileScreen as ProfileScreenComposable

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MapScreen,
        modifier = modifier
    ) {
        composable<MapScreen> {
            MapScreenComposable(navController = navController)
        }
        composable<AttractionsScreen> {
            AttractionsScreenComposable(navController = navController)
        }
        composable<StatsScreen> {
            StatsScreenComposable()
        }
        composable<ProfileScreen> {
            ProfileScreenComposable()
        }
        composable<LoginScreen> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("登录页（待实现）")
            }
        }
        composable<RegionDetailScreen> { backStackEntry ->
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("区域详情: $regionId")
            }
        }
        composable<AttractionDetailScreen> { backStackEntry ->
            val attractionId = backStackEntry.arguments?.getString("attractionId") ?: ""
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("景点详情: $attractionId")
            }
        }
    }
}
