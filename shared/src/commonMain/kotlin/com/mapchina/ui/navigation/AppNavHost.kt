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
        modifier = modifier
    ) {
        composable<MapScreen> {
            MapScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<AttractionsScreen> {
            AttractionsScreenComposable(navController = navController, viewModel = koinInject())
        }
        composable<AchievementScreen> {
            val vm: AchievementViewModel = koinInject()
            val statsVm: StatsViewModel = koinInject()
            AchievementScreen(
                viewModel = vm,
                statsViewModel = statsVm,
                onNavigateToBadgeWall = { navController.navigate(BadgeWallScreen) },
                onNavigateToProvinceConquest = { navController.navigate(ProvinceConquestScreen) },
                onNavigateToAtlas = { navController.navigate(com.mapchina.ui.navigation.AtlasScreen) }
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
        composable<ProfileScreen> {
            ProfileScreenComposable(
                viewModel = koinInject(),
                onNavigateToLogin = { navController.navigate(com.mapchina.ui.navigation.LoginScreen) },
                onNavigateToJournals = { navController.navigate(com.mapchina.ui.navigation.JournalListScreen) }
            )
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
        composable<AttractionDetailScreen> { backStackEntry ->
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
                },
                onRemoveVisit = {
                    attraction?.let { viewModel.removeVisit(it.id) }
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
            JournalCreateScreenComposable(
                viewModel = vm,
                regionId = regionId,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
