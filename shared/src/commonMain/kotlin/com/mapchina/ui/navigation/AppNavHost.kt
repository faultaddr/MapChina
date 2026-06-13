package com.mapchina.ui.navigation

import kotlin.time.Clock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
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
import com.mapchina.ui.stats.StatsScreen as StatsScreenComposable
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    backStack: MutableList<NavKey>,
    modifier: Modifier = Modifier
) {
    val onBack: () -> Unit = { backStack.removeLastOrNull() }
    val navigate: (NavKey) -> Unit = { backStack.add(it) }

    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        modifier = modifier,
        entryProvider = entryProvider {
            entry<MapScreen> {
                val vm: com.mapchina.ui.map.MapViewModel = koinInject()
                MapScreenComposable(
                    onNavigate = navigate,
                    onBack = onBack,
                    viewModel = vm,
                    mapController = vm.persistentMapController
                )
            }
            entry<AttractionsScreen> {
                AttractionsScreenComposable(
                    onNavigate = navigate,
                    onBack = onBack,
                    viewModel = koinInject()
                )
            }
            entry<ProfileScreen> {
                val profileVm: com.mapchina.ui.profile.ProfileViewModel = koinInject()
                val achievementVm: AchievementViewModel = koinInject()
                val statsVm: StatsViewModel = koinInject()
                ProfileScreenComposable(
                    viewModel = profileVm,
                    achievementViewModel = achievementVm,
                    statsViewModel = statsVm,
                    onNavigateToLogin = { navigate(LoginScreen) },
                    onNavigateToJournals = { navigate(JournalListScreen) },
                    onNavigateToBadgeWall = { navigate(BadgeWallScreen) },
                    onNavigateToProvinceConquest = { navigate(ProvinceConquestScreen) },
                    onNavigateToAtlas = { navigate(AtlasScreen) },
                    onNavigateToCarvings = { navigate(CarvingListScreen(showAll = "true")) },
                    onNavigateToStats = { navigate(StatsScreen) },
                    settingsRepository = profileVm.settingsRepository
                )
            }
            entry<BadgeWallScreen> {
                val vm: AchievementViewModel = koinInject()
                BadgeWallScreen(
                    viewModel = vm,
                    onBadgeClick = { id -> navigate(BadgeDetailScreen(id)) }
                )
            }
            entry<BadgeDetailScreen> { key ->
                val vm: AchievementViewModel = koinInject()
                val ui by vm.ui.collectAsState()
                val item = ui.allAchievements.find { it.definition.id == key.achievementId }
                BadgeDetailScreen(item = item)
            }
            entry<LoginScreen> {
                val authService: AuthService = koinInject()
                val apiClient: com.mapchina.data.remote.MapChinaApiClient = koinInject()
                val scope = rememberCoroutineScope()
                var loginError by remember { mutableStateOf("") }
                LoginScreenComposable(
                    onLoginSuccess = onBack,
                    onQuickStart = { nickname ->
                        authService.quickStart(nickname)
                        onBack()
                    },
                    onPhoneLogin = { phone, code ->
                        loginError = ""
                        scope.launch {
                            try {
                                val resp = apiClient.login(phone, code)
                                apiClient.accessToken = resp.accessToken
                                authService.onLogin(com.mapchina.data.model.UserDto(
                                    id = resp.userId,
                                    phone = phone,
                                    nickname = resp.nickname,
                                    avatar = null,
                                    createdAt = Clock.System.now().toEpochMilliseconds()
                                ))
                                onBack()
                            } catch (e: Exception) {
                                loginError = e.message ?: "登录失败"
                            }
                        }
                    },
                    onSendCode = { phone ->
                        scope.launch {
                            try { apiClient.sendLoginCode(phone) } catch (e: Exception) {  }
                        }
                    }
                )
            }
            entry<RegionDetailScreen> { key ->
                val mapViewModel: com.mapchina.ui.map.MapViewModel = koinInject()
                RegionDetailScreenComposable(
                    regionId = key.regionId,
                    viewModel = mapViewModel,
                    onBack = onBack,
                    onChildRegionClick = { id -> navigate(RegionDetailScreen(id)) }
                )
            }
            entry<ProvinceConquestScreen> {
                val vm: ProvinceConquestViewModel = koinInject()
                ProvinceConquestScreenComposable(
                    viewModel = vm,
                    onProvinceClick = { code -> navigate(ProvinceDetailScreen(code)) }
                )
            }
            entry<ProvinceDetailScreen> { key ->
                val vm: ProvinceConquestViewModel = koinInject()
                ProvinceDetailScreenComposable(
                    viewModel = vm,
                    provinceCode = key.provinceCode
                )
            }
            entry<AtlasScreen> {
                val vm: AtlasViewModel = koinInject()
                AtlasScreenComposable(
                    viewModel = vm,
                    onAtlasClick = { atlasId -> navigate(AtlasDetailScreen(atlasId)) }
                )
            }
            entry<AtlasDetailScreen> { key ->
                val vm: AtlasViewModel = koinInject()
                AtlasDetailScreenComposable(
                    viewModel = vm,
                    atlasId = key.atlasId
                )
            }
            entry<AttractionDetailScreen> { key ->
                val viewModel: AttractionViewModel = koinInject()
                var attraction by remember(key.attractionId) { mutableStateOf(viewModel.getAttractionById(key.attractionId)) }
                val detail = remember(key.attractionId) { viewModel.getAttractionDetail(key.attractionId) }
                val journalVm: JournalViewModel = koinInject()
                val journals = remember(key.attractionId) { journalVm.getJournalsByAttraction(key.attractionId) }
                AttractionDetailScreen(
                    onNavigate = navigate,
                    onBack = onBack,
                    attraction = attraction,
                    detail = detail,
                    journals = journals,
                    onMarkVisit = { level ->
                        attraction?.let { viewModel.markVisit(it.id, it.regionId, level) }
                        attraction = viewModel.getAttractionById(key.attractionId)
                    },
                    onRemoveVisit = {
                        attraction?.let { viewModel.removeVisit(it.id) }
                        attraction = viewModel.getAttractionById(key.attractionId)
                    },
                    onWriteJournal = {
                        attraction?.let { navigate(JournalCreateScreen(attractionId = it.id)) }
                    },
                    onJournalClick = { id -> navigate(JournalDetailScreen(id)) },
                    onOpenCarving = {
                        attraction?.let {
                            navigate(CarvingScreen(regionId = it.regionId, regionName = "", attractionId = it.id, attractionName = it.name))
                        }
                    }
                )
            }
            entry<JournalListScreen> {
                val vm: JournalViewModel = koinInject()
                JournalListScreenComposable(
                    viewModel = vm,
                    onJournalClick = { id -> navigate(JournalDetailScreen(id)) },
                    onCreateClick = { navigate(JournalCreateScreen()) },
                    onBack = onBack
                )
            }
            entry<JournalDetailScreen> { key ->
                val vm: JournalViewModel = koinInject()
                JournalDetailScreenComposable(
                    journalId = key.journalId,
                    viewModel = vm,
                    onBack = onBack,
                    onDelete = onBack
                )
            }
            entry<JournalCreateScreen> { key ->
                val vm: JournalViewModel = koinInject()
                JournalCreateScreenComposable(
                    viewModel = vm,
                    regionId = key.regionId,
                    attractionId = key.attractionId,
                    onSave = onBack,
                    onBack = onBack
                )
            }
            entry<CarvingScreen> { key ->
                val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
                com.mapchina.ui.carving.CarvingScreen(
                    regionId = key.regionId,
                    regionName = key.regionName,
                    viewModel = vm,
                    onBack = onBack,
                    attractionId = key.attractionId,
                    attractionName = key.attractionName,
                    carvingId = key.carvingId
                )
            }
            entry<CarvingListScreen> { key ->
                val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
                val showAll = key.showAll == "true"
                com.mapchina.ui.carving.CarvingListScreen(
                    viewModel = vm,
                    title = if (showAll) "我的碑刻" else "碑刻 · ${key.regionName ?: ""}",
                    regionId = key.regionId,
                    attractionId = key.attractionId,
                    showAll = showAll,
                    onCreateClick = {
                        val rId = key.regionId ?: ""
                        val rName = key.regionName ?: ""
                        navigate(CarvingScreen(regionId = rId, regionName = rName, attractionId = key.attractionId))
                    },
                    onEditClick = { carving ->
                        navigate(CarvingScreen(
                            regionId = carving.regionId,
                            regionName = carving.regionName,
                            attractionId = carving.attractionId,
                            attractionName = carving.attractionName,
                            carvingId = carving.id
                        ))
                    },
                    onBack = onBack
                )
            }
            entry<CustomAttractionScreen> { key ->
                val vm: AttractionViewModel = koinInject()
                CustomAttractionScreenComposable(
                    regionId = key.regionId,
                    latitude = key.latitude.toDoubleOrNull() ?: 0.0,
                    longitude = key.longitude.toDoubleOrNull() ?: 0.0,
                    viewModel = vm,
                    onSave = onBack,
                    onBack = onBack
                )
            }
            entry<StatsScreen> {
                val vm: StatsViewModel = koinInject()
                StatsScreenComposable(viewModel = vm)
            }
            entry<CommunityScreen> {
                val vm: CommunityViewModel = koinInject()
                CommunityScreenComposable(
                    viewModel = vm,
                    onPostClick = { postId -> navigate(PostDetailScreen(postId)) }
                )
            }
            entry<PostDetailScreen> { key ->
                val vm: CommunityViewModel = koinInject()
                PostDetailScreenComposable(
                    postId = key.postId,
                    viewModel = vm,
                    onBack = onBack
                )
            }
        }
    )
}
