package com.mapchina.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapchina.ui.achievement.AchievementScreen
import com.mapchina.ui.achievement.AchievementViewModel
import com.mapchina.ui.achievement.AtlasScreen as AtlasScreenComposable
import com.mapchina.ui.achievement.AtlasDetailScreen as AtlasDetailScreenComposable
import com.mapchina.ui.achievement.AtlasViewModel
import com.mapchina.ui.achievement.BadgeDetailScreen
import com.mapchina.ui.achievement.BadgeWallScreen
import com.mapchina.ui.achievement.ProvinceConquestScreen as ProvinceConquestScreenComposable
import com.mapchina.ui.achievement.ProvinceDetailScreen as ProvinceDetailScreenComposable
import com.mapchina.ui.achievement.ProvinceConquestViewModel
import com.mapchina.ui.attraction.AttractionDetailScreen
import com.mapchina.ui.attraction.AttractionViewModel
import com.mapchina.ui.map.MapScreen as MapScreenComposable
import com.mapchina.ui.attraction.AttractionsScreen as AttractionsScreenComposable
import com.mapchina.ui.stats.StatsScreen as StatsScreenComposable
import com.mapchina.ui.profile.ProfileScreen as ProfileScreenComposable
import org.koin.compose.koinInject

// Page transitions (slide from right for push, slide from left for pop)
private val AnimatedContentTransitionScope<*>.enterPageTransition: EnterTransition
    get() = slideInHorizontally(
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) { (it * 0.3f).toInt() } + fadeIn(tween(350))

private val AnimatedContentTransitionScope<*>.exitPageTransition: ExitTransition
    get() = slideOutHorizontally(
        animationSpec = tween(250, easing = FastOutSlowInEasing)
    ) { (it * 0.3f).toInt() } + fadeOut(tween(250))

private val AnimatedContentTransitionScope<*>.popEnterPageTransition: EnterTransition
    get() = slideInHorizontally(
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) { -(it * 0.3f).toInt() } + fadeIn(tween(350))

private val AnimatedContentTransitionScope<*>.popExitPageTransition: ExitTransition
    get() = slideOutHorizontally(
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) { it } + fadeOut(tween(350))

// Bottom tab transition (fade through, no slide)
private val AnimatedContentTransitionScope<*>.tabEnterTransition: EnterTransition
    get() = fadeIn(tween(200))

private val AnimatedContentTransitionScope<*>.tabExitTransition: ExitTransition
    get() = fadeOut(tween(200))

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MapScreen,
        modifier = modifier
    ) {
        composable<MapScreen>(
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            MapScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<AttractionsScreen>(
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            AttractionsScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<StatsScreen>(
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            StatsScreenComposable(viewModel = koinInject())
        }
        composable<AchievementScreen>(
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            val vm: AchievementViewModel = koinInject()
            AchievementScreen(
                viewModel = vm,
                onNavigateToBadgeWall = { navController.navigate(BadgeWallScreen) },
                onNavigateToProvinceConquest = { navController.navigate(ProvinceConquestScreen) },
                onNavigateToAtlas = { navController.navigate(com.mapchina.ui.navigation.AtlasScreen) }
            )
        }
        composable<BadgeWallScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) {
            val vm: AchievementViewModel = koinInject()
            BadgeWallScreen(
                viewModel = vm,
                onBadgeClick = { id -> navController.navigate(BadgeDetailScreen(id)) }
            )
        }
        composable<BadgeDetailScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) { backStackEntry ->
            val vm: AchievementViewModel = koinInject()
            val ui by vm.ui.collectAsState()
            val achievementId = backStackEntry.arguments?.getString("achievementId") ?: ""
            val item = ui.allAchievements.find { it.definition.id == achievementId }
            BadgeDetailScreen(item = item)
        }
        composable<ProfileScreen>(
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition }
        ) {
            ProfileScreenComposable(viewModel = koinInject())
        }
        composable<LoginScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("登录页（待实现）")
            }
        }
        composable<RegionDetailScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) { backStackEntry ->
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("区域详情: $regionId")
            }
        }
        composable<ProvinceConquestScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) {
            val vm: ProvinceConquestViewModel = koinInject()
            ProvinceConquestScreenComposable(
                viewModel = vm,
                onProvinceClick = { code -> navController.navigate(ProvinceDetailScreen(code)) }
            )
        }
        composable<ProvinceDetailScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) { backStackEntry ->
            val vm: ProvinceConquestViewModel = koinInject()
            val provinceCode = backStackEntry.arguments?.getString("provinceCode") ?: ""
            ProvinceDetailScreenComposable(
                viewModel = vm,
                provinceCode = provinceCode
            )
        }
        composable<com.mapchina.ui.navigation.AtlasScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) {
            val vm: AtlasViewModel = koinInject()
            AtlasScreenComposable(
                viewModel = vm,
                onAtlasClick = { atlasId -> navController.navigate(com.mapchina.ui.navigation.AtlasDetailScreen(atlasId)) }
            )
        }
        composable<com.mapchina.ui.navigation.AtlasDetailScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) { backStackEntry ->
            val vm: AtlasViewModel = koinInject()
            val atlasId = backStackEntry.arguments?.getString("atlasId") ?: ""
            AtlasDetailScreenComposable(
                viewModel = vm,
                atlasId = atlasId
            )
        }
        composable<AttractionDetailScreen>(
            enterTransition = { enterPageTransition },
            exitTransition = { exitPageTransition },
            popEnterTransition = { popEnterPageTransition },
            popExitTransition = { popExitPageTransition }
        ) { backStackEntry ->
            val attractionId = backStackEntry.arguments?.getString("attractionId") ?: ""
            val viewModel: AttractionViewModel = koinInject()
            val attraction = remember(attractionId) { viewModel.getAttractionById(attractionId) }
            val detail = remember(attractionId) { viewModel.getAttractionDetail(attractionId) }
            AttractionDetailScreen(
                navController = navController,
                attraction = attraction,
                detail = detail,
                onMarkVisit = { level ->
                    attraction?.let { viewModel.markVisit(it.id, it.regionId, level) }
                }
            )
        }
    }
}
