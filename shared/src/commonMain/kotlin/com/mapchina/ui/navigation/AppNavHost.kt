package com.mapchina.ui.navigation

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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.mapchina.ui.attraction.CustomAttractionScreen as CustomAttractionScreenComposable
import com.mapchina.ui.community.CommunityScreen as CommunityScreenComposable
import com.mapchina.ui.community.PostDetailScreen as PostDetailScreenComposable
import com.mapchina.ui.community.CommunityViewModel
import com.mapchina.ui.journal.JournalViewModel
import com.mapchina.ui.journal.JournalListScreen as JournalListScreenComposable
import com.mapchina.ui.journal.JournalDetailScreen as JournalDetailScreenComposable
import com.mapchina.ui.journal.JournalCreateScreen as JournalCreateScreenComposable
import com.mapchina.ui.map.MapScreen as MapScreenComposable
import com.mapchina.ui.map.RegionDetailScreen as RegionDetailScreenComposable
import com.mapchina.ui.attraction.AttractionsScreen as AttractionsScreenComposable
import com.mapchina.ui.profile.ProfileScreen as ProfileScreenComposable
import com.mapchina.ui.profile.LoginScreen as LoginScreenComposable
import com.mapchina.ui.stats.StatsViewModel
import com.mapchina.domain.service.AuthService
import org.koin.compose.koinInject

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MapScreen,
        modifier = modifier,
        enterTransition = {
            slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(300))
        },
        exitTransition = {
            fadeOut(tween(150))
        },
        popEnterTransition = {
            fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(tween(300))
        }
    ) {
        composable<MapScreen> {
            MapScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<AttractionsScreen> {
            AttractionsScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<ProfileScreen> {
            val profileVm: com.mapchina.ui.profile.ProfileViewModel = koinInject()
            val achievementVm: AchievementViewModel = koinInject()
            val statsVm: StatsViewModel = koinInject()
            ProfileScreenComposable(
                viewModel = profileVm,
                achievementViewModel = achievementVm,
                statsViewModel = statsVm,
                onNavigateToLogin = { navController.navigate(com.mapchina.ui.navigation.LoginScreen) },
                onNavigateToJournals = { navController.navigate(com.mapchina.ui.navigation.JournalListScreen) },
                onNavigateToBadgeWall = { navController.navigate(BadgeWallScreen) },
                onNavigateToProvinceConquest = { navController.navigate(ProvinceConquestScreen) },
                onNavigateToAtlas = { navController.navigate(com.mapchina.ui.navigation.AtlasScreen) },
                onNavigateToCarvings = { navController.navigate(com.mapchina.ui.navigation.CarvingListScreen(showAll = true)) }
            )
        }
        composable<BadgeWallScreen> {
            val vm: AchievementViewModel = koinInject()
            BadgeWallScreen(
                viewModel = vm,
                onBadgeClick = { id -> navController.navigate(BadgeDetailScreen(id)) }
            )
        }
        composable<BadgeDetailScreen> { backStackEntry ->
            val vm: AchievementViewModel = koinInject()
            val ui by vm.ui.collectAsState()
            val achievementId = backStackEntry.arguments?.getString("achievementId") ?: ""
            val item = ui.allAchievements.find { it.definition.id == achievementId }
            BadgeDetailScreen(item = item)
        }
        composable<LoginScreen> {
            val authService: AuthService = koinInject()
            LoginScreenComposable(
                onLoginSuccess = { navController.popBackStack() },
                onQuickStart = { nickname ->
                    authService.quickStart(nickname)
                    navController.popBackStack()
                }
            )
        }
        composable<RegionDetailScreen> { backStackEntry ->
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            val mapViewModel: com.mapchina.ui.map.MapViewModel = koinInject()
            RegionDetailScreenComposable(
                regionId = regionId,
                viewModel = mapViewModel,
                onBack = { navController.popBackStack() },
                onChildRegionClick = { id -> navController.navigate(RegionDetailScreen(id)) }
            )
        }
        composable<ProvinceConquestScreen> {
            val vm: ProvinceConquestViewModel = koinInject()
            ProvinceConquestScreenComposable(
                viewModel = vm,
                onProvinceClick = { code -> navController.navigate(ProvinceDetailScreen(code)) }
            )
        }
        composable<ProvinceDetailScreen> { backStackEntry ->
            val vm: ProvinceConquestViewModel = koinInject()
            val provinceCode = backStackEntry.arguments?.getString("provinceCode") ?: ""
            ProvinceDetailScreenComposable(
                viewModel = vm,
                provinceCode = provinceCode
            )
        }
        composable<com.mapchina.ui.navigation.AtlasScreen> {
            val vm: AtlasViewModel = koinInject()
            AtlasScreenComposable(
                viewModel = vm,
                onAtlasClick = { atlasId -> navController.navigate(com.mapchina.ui.navigation.AtlasDetailScreen(atlasId)) }
            )
        }
        composable<com.mapchina.ui.navigation.AtlasDetailScreen> { backStackEntry ->
            val vm: AtlasViewModel = koinInject()
            val atlasId = backStackEntry.arguments?.getString("atlasId") ?: ""
            AtlasDetailScreenComposable(
                viewModel = vm,
                atlasId = atlasId
            )
        }
        composable<AttractionDetailScreen>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(250))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) { backStackEntry ->
            val attractionId = backStackEntry.arguments?.getString("attractionId") ?: ""
            val viewModel: AttractionViewModel = koinInject()
            val attraction = remember(attractionId) { viewModel.getAttractionById(attractionId) }
            val detail = remember(attractionId) { viewModel.getAttractionDetail(attractionId) }
            val journalVm: JournalViewModel = koinInject()
            val journals = remember(attractionId) { journalVm.getJournalsByAttraction(attractionId) }
            AttractionDetailScreen(
                navController = navController,
                attraction = attraction,
                detail = detail,
                journals = journals,
                onMarkVisit = { level ->
                    attraction?.let { viewModel.markVisit(it.id, it.regionId, level) }
                },
                onRemoveVisit = {
                    attraction?.let { viewModel.removeVisit(it.id) }
                },
                onWriteJournal = {
                    attraction?.let { navController.navigate(com.mapchina.ui.navigation.JournalCreateScreen(attractionId = it.id)) }
                },
                onJournalClick = { id -> navController.navigate(com.mapchina.ui.navigation.JournalDetailScreen(id)) },
                onOpenCarving = {
                    attraction?.let {
                        navController.navigate(com.mapchina.ui.navigation.CarvingScreen(regionId = it.regionId, regionName = "", attractionId = it.id, attractionName = it.name))
                    }
                }
            )
        }
        composable<com.mapchina.ui.navigation.JournalListScreen> {
            val vm: JournalViewModel = koinInject()
            JournalListScreenComposable(
                viewModel = vm,
                onJournalClick = { id -> navController.navigate(com.mapchina.ui.navigation.JournalDetailScreen(id)) },
                onCreateClick = { navController.navigate(com.mapchina.ui.navigation.JournalCreateScreen()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<com.mapchina.ui.navigation.JournalDetailScreen> { backStackEntry ->
            val vm: JournalViewModel = koinInject()
            val journalId = backStackEntry.arguments?.getString("journalId") ?: ""
            JournalDetailScreenComposable(
                journalId = journalId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onDelete = { navController.popBackStack() }
            )
        }
        composable<com.mapchina.ui.navigation.JournalCreateScreen> { backStackEntry ->
            val vm: JournalViewModel = koinInject()
            val regionId = backStackEntry.arguments?.getString("regionId")
            val attractionId = backStackEntry.arguments?.getString("attractionId")
            JournalCreateScreenComposable(
                viewModel = vm,
                regionId = regionId,
                attractionId = attractionId,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<com.mapchina.ui.navigation.CarvingScreen> { backStackEntry ->
            val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            val regionName = backStackEntry.arguments?.getString("regionName") ?: ""
            val attractionId = backStackEntry.arguments?.getString("attractionId")
            val attractionName = backStackEntry.arguments?.getString("attractionName")
            com.mapchina.ui.carving.CarvingScreen(
                regionId = regionId,
                regionName = regionName,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                attractionId = attractionId,
                attractionName = attractionName
            )
        }
        composable<com.mapchina.ui.navigation.CarvingListScreen> { backStackEntry ->
            val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
            val regionId = backStackEntry.arguments?.getString("regionId")
            val regionName = backStackEntry.arguments?.getString("regionName")
            val attractionId = backStackEntry.arguments?.getString("attractionId")
            val showAll = backStackEntry.arguments?.getString("showAll") == "true"
            com.mapchina.ui.carving.CarvingListScreen(
                viewModel = vm,
                title = if (showAll) "我的碑刻" else "碑刻 · ${regionName ?: ""}",
                regionId = regionId,
                attractionId = attractionId,
                showAll = showAll,
                onCreateClick = {
                    val rId = regionId ?: ""
                    val rName = regionName ?: ""
                    navController.navigate(com.mapchina.ui.navigation.CarvingScreen(regionId = rId, regionName = rName, attractionId = attractionId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<CustomAttractionScreen> { backStackEntry ->
            val vm: AttractionViewModel = koinInject()
            val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            CustomAttractionScreenComposable(
                regionId = regionId,
                latitude = latitude,
                longitude = longitude,
                viewModel = vm,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<CommunityScreen> {
            val vm: CommunityViewModel = koinInject()
            CommunityScreenComposable(
                viewModel = vm,
                onPostClick = { postId -> navController.navigate(PostDetailScreen(postId)) }
            )
        }
        composable<PostDetailScreen> { backStackEntry ->
            val vm: CommunityViewModel = koinInject()
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreenComposable(
                postId = postId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
