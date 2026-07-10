# Profile Control Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the 我的 tab as a concise account and data control center with real sync state, trustworthy footprint controls, a usable map-theme grid, and no duplicated growth statistics.

**Architecture:** `ProfileViewModel` owns only account identity, pending-sync count, and `SyncEngine.status`; growth data remains owned by 山河. A pure `profileSyncPresentation` mapper converts login and sync state into stable UI copy, while `ProfileScreen` composes four focused sections and delegates full-sync requests to `SyncCoordinator` through navigation wiring.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, kotlinx.coroutines `StateFlow`, Koin, SQLDelight, Robolectric Compose UI tests, Android ADB QA

## Global Constraints

- Product position remains “自动发现你可能到过的地方，由你确认点亮中国”.
- 我的 only owns login/account, cloud sync, footprint preferences, map theme, and version/account actions; growth and statistics remain in 山河.
- Automatic footprint discovery must be described as a suggestion that requires user confirmation.
- Photo markers must be described as local access to geotagged photo data, without claiming an unimplemented permission-management flow.
- All cards and grouped surfaces introduced in this page use a maximum corner radius of `8.dp`.
- No new dependency is added.
- Preserve the unrelated untracked file `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- After every UI/runtime change, run `./gradlew installDebug`, launch the app, exercise the flow with ADB, and capture before/after screenshots.

---

### Task 1: Account And Data Screen Hierarchy

**Files:**
- Modify: `shared/src/androidUnitTest/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`

**Interfaces:**
- Consumes: `ProfileUi`, `SettingsRepository`, `LocalScaffoldBottomPadding`, `MapTheme.entries`.
- Produces: `ProfileScreen(viewModel, onNavigateToLogin, onSyncNow, settingsRepository, modifier)` and a screen hierarchy containing `我的`, `账号与同步`, `足迹记录`, `地图外观`, and `关于`.

- [ ] **Step 1: Write the failing account-control-center UI test**

Replace the legacy settings test with assertions for the new ownership boundary:

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun profileScreen_isAccountAndDataControlCenter() = runComposeUiTest {
    setContent { ProfileScreen() }

    onNodeWithText("我的").assertIsDisplayed()
    onNodeWithText("账号与同步").assertIsDisplayed()
    onNodeWithText("本地保存").assertIsDisplayed()
    onNode(hasScrollAction()).performScrollToNode(hasText("足迹记录"))
    onNodeWithText("足迹记录").assertIsDisplayed()
    onNodeWithText("仅在本机读取照片中的位置信息").assertIsDisplayed()
    onNodeWithText("只生成建议，由你确认后点亮").assertIsDisplayed()
    onNode(hasScrollAction()).performScrollToNode(hasText("地图外观"))
    onNodeWithText("地图外观").assertIsDisplayed()
    onNode(hasScrollAction()).performScrollToNode(hasText("关于"))
    onNodeWithText("关于").assertIsDisplayed()
    onAllNodesWithText("省份").assertCountEquals(0)
    onAllNodesWithText("城市").assertCountEquals(0)
    onAllNodesWithText("区县").assertCountEquals(0)
}
```

- [ ] **Step 2: Run the focused UI test and confirm the ownership assertions fail**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.profile.ProfileScreenTest.profileScreen_isAccountAndDataControlCenter`

Expected: FAIL because `我的`, `账号与同步`, `本地保存`, and the new trust copy do not exist yet.

- [ ] **Step 3: Recompose ProfileScreen into focused sections**

Change the public screen signature and remove `AchievementViewModel`, `StatsViewModel`, growth callbacks, `StatsBar`, and `BigStat`:

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
    settingsRepository: SettingsRepository? = null,
    modifier: Modifier = Modifier
)
```

Build the `LazyColumn` with `LocalScaffoldBottomPadding.current` and stable page spacing:

```kotlin
val bottomPadding = LocalScaffoldBottomPadding.current

LazyColumn(
    modifier = modifier
        .fillMaxSize()
        .background(MapChinaColors.Background)
        .statusBarsPadding(),
    contentPadding = PaddingValues(
        start = 16.dp,
        top = 20.dp,
        end = 16.dp,
        bottom = bottomPadding + 24.dp
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    item { ProfileHeader() }
    item { AccountSection(profile, isLoggedIn, onNavigateToLogin) }
    item {
        SyncSection(
            presentation = profileSyncPresentation(
                isLoggedIn = isLoggedIn,
                pendingSyncCount = profile.pendingSyncCount,
                status = profile.syncStatus
            ),
            onAction = if (isLoggedIn) onSyncNow else onNavigateToLogin
        )
    }
    item { FootprintSettingsSection(settingsRepository) }
    item { MapAppearanceSection(settingsRepository) }
    item { AboutSection(isLoggedIn, onLogout = { viewModel?.logout() }) }
}
```

Use `RoundedCornerShape(8.dp)` for section surfaces. Remove the level and score row from the account card because 山河 owns growth. Use icon-plus-copy setting rows with these exact subtitles:

```kotlin
PreferenceSwitchRow(
    title = Copy.PHOTO_MARKERS,
    subtitle = "仅在本机读取照片中的位置信息",
    checked = photoMarkersVisible,
    onCheckedChange = { enabled ->
        haptic.perform(HapticType.SELECTION)
        photoMarkersVisible = enabled
        settingsRepository?.setString("photo_markers_visible", enabled.toString())
    }
)
PreferenceSwitchRow(
    title = Copy.AUTO_FOOTPRINT,
    subtitle = "只生成建议，由你确认后点亮",
    checked = autoMarkFootprint,
    onCheckedChange = { enabled ->
        haptic.perform(HapticType.SELECTION)
        autoMarkFootprint = enabled
        settingsRepository?.setString("auto_mark_footprint", enabled.toString())
    }
)
```

Render `MapTheme.entries.chunked(3)` as two rows of three equal-width selectable tiles, with a stable `52.dp` swatch area and theme name below it. Keep `LevelBadgeIcon` intact because it remains a public reusable composable.

- [ ] **Step 4: Run the ProfileScreen tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.profile.ProfileScreenTest`

Expected: PASS with the new hierarchy and zero occurrences of the province/city/district stats labels.

- [ ] **Step 5: Commit the screen hierarchy**

```bash
git add shared/src/androidUnitTest/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt
git commit -m "feat(ui): rebuild profile control center"
```

---

### Task 2: Real Sync State And Navigation Wiring

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileSyncPresentation.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/profile/ProfileSyncPresentationTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/profile/ProfileViewModelTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/di/AppModuleTest.kt`

**Interfaces:**
- Consumes: `SyncStatus`, `SyncEngine.status: StateFlow<SyncStatus>`, `SyncCoordinator.requestFullSync()`, `AuthService.currentUserFlow`, SQLDelight `syncQueueQueries.countPending()`.
- Produces: `ProfileSyncPresentation`, `profileSyncPresentation(Boolean, Long, SyncStatus)`, `ProfileUi.syncStatus`, and Koin-resolvable `ProfileViewModel` without `UserScoreRepository`.

- [ ] **Step 1: Write failing sync-presentation tests**

Create `ProfileSyncPresentationTest.kt`:

```kotlin
class ProfileSyncPresentationTest {
    @Test
    fun loggedOut_isClearlyLocalOnly() {
        assertEquals(
            ProfileSyncPresentation(
                title = "本地保存",
                subtitle = "登录后可在多台设备同步足迹与地图偏好",
                actionLabel = "登录",
                actionEnabled = true
            ),
            profileSyncPresentation(false, 0L, SyncStatus.IDLE)
        )
    }

    @Test
    fun loggedInPending_surfacesQueueCount() {
        assertEquals(
            "3 项待同步",
            profileSyncPresentation(true, 3L, SyncStatus.IDLE).title
        )
    }

    @Test
    fun syncing_disablesDuplicateAction() {
        val result = profileSyncPresentation(true, 2L, SyncStatus.SYNCING)
        assertEquals("正在同步", result.title)
        assertEquals("同步中", result.actionLabel)
        assertFalse(result.actionEnabled)
    }

    @Test
    fun offline_keepsLocalDataMessage() {
        val result = profileSyncPresentation(true, 2L, SyncStatus.OFFLINE)
        assertEquals("离线，2 项待同步", result.title)
        assertEquals("数据已安全保存在本机", result.subtitle)
        assertEquals("重试", result.actionLabel)
    }

    @Test
    fun synced_confirmsCompletion() {
        assertEquals(
            "已同步",
            profileSyncPresentation(true, 0L, SyncStatus.SYNCED).title
        )
    }
}
```

- [ ] **Step 2: Run the presentation tests and confirm they fail**

Run: `./gradlew :shared:allTests --tests com.mapchina.ui.profile.ProfileSyncPresentationTest`

Expected: FAIL because `ProfileSyncPresentation` and `profileSyncPresentation` do not exist.

- [ ] **Step 3: Implement the pure sync-state mapper**

Create `ProfileSyncPresentation.kt` with exact, exhaustive state handling:

```kotlin
data class ProfileSyncPresentation(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val actionEnabled: Boolean
)

fun profileSyncPresentation(
    isLoggedIn: Boolean,
    pendingSyncCount: Long,
    status: SyncStatus
): ProfileSyncPresentation {
    if (!isLoggedIn) {
        return ProfileSyncPresentation(
            "本地保存",
            "登录后可在多台设备同步足迹与地图偏好",
            "登录",
            true
        )
    }
    return when (status) {
        SyncStatus.SYNCING -> ProfileSyncPresentation(
            "正在同步", "正在整理足迹、景点和地图偏好", "同步中", false
        )
        SyncStatus.OFFLINE -> ProfileSyncPresentation(
            if (pendingSyncCount > 0) "离线，${pendingSyncCount} 项待同步" else "当前离线",
            "数据已安全保存在本机",
            "重试",
            true
        )
        SyncStatus.ERROR -> ProfileSyncPresentation(
            "同步未完成", "数据仍保存在本机，可稍后重试", "重试", true
        )
        SyncStatus.SYNCED -> ProfileSyncPresentation(
            "已同步", "足迹与地图偏好已更新", "再次同步", true
        )
        SyncStatus.IDLE -> if (pendingSyncCount > 0) {
            ProfileSyncPresentation(
                "${pendingSyncCount} 项待同步", "本机记录等待上传到云端", "立即同步", true
            )
        } else {
            ProfileSyncPresentation(
                "云端同步已连接", "足迹与地图偏好将自动同步", "立即同步", true
            )
        }
    }
}
```

- [ ] **Step 4: Refocus ProfileViewModel on account and sync ownership**

Change the model and constructor to:

```kotlin
data class ProfileUi(
    val nickname: String,
    val phone: String?,
    val avatar: String?,
    val pendingSyncCount: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

class ProfileViewModel(
    private val authService: AuthService,
    val settingsRepository: SettingsRepository? = null,
    private val database: MapChinaDatabase? = null,
    syncEngine: SyncEngine? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
)
```

Use `combine(authService.currentUserFlow, syncEngine?.status ?: MutableStateFlow(SyncStatus.IDLE))` so sync-state changes update the UI without requiring a second login emission. Both the collector and `loadProfile()` must build `ProfileUi` from the current user, `pendingSyncCount()`, and current sync status. Delete all `UserScoreRepository`, `UserLevelInfo`, `levelInfo`, and `badgeCount` dependencies from this ViewModel.

Update `ProfileViewModelTest` constructor calls to `ProfileViewModel(authService)` and `ProfileViewModel(authService, database = database)`, and change the initial nickname expectation from `""` to `"未登录"`.

- [ ] **Step 5: Wire Koin and navigation to the real sync action**

Change the Koin registration to:

```kotlin
single {
    ProfileViewModel(
        authService = get(),
        settingsRepository = getOrNull<SettingsRepository>(),
        database = get(),
        syncEngine = getOrNull()
    )
}
```

Replace the profile navigation entry with:

```kotlin
entry<ProfileScreen> {
    val profileVm: ProfileViewModel = koinInject()
    val syncCoordinator: SyncCoordinator = koinInject()
    ProfileScreenComposable(
        viewModel = profileVm,
        onNavigateToLogin = { navigate(LoginScreen) },
        onSyncNow = syncCoordinator::requestFullSync,
        settingsRepository = profileVm.settingsRepository
    )
}
```

Remove only the now-unused Profile-entry imports; keep `AchievementViewModel` and `StatsViewModel` imports that are still required by their dedicated routes. Extend `AppModuleTest` to resolve `ProfileViewModel` from Koin alongside the existing core ViewModels.

- [ ] **Step 6: Run the focused model, mapper, UI, and Koin tests**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest`

Expected: BUILD SUCCESSFUL; Profile mapper, ViewModel, ProfileScreen, and AppModule resolution tests all pass.

- [ ] **Step 7: Commit the sync integration**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileSyncPresentation.kt shared/src/commonTest/kotlin/com/mapchina/ui/profile/ProfileSyncPresentationTest.kt shared/src/commonTest/kotlin/com/mapchina/ui/profile/ProfileViewModelTest.kt shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt shared/src/commonTest/kotlin/com/mapchina/di/AppModuleTest.kt
git commit -m "feat(profile): connect account sync status"
```

---

### Task 3: Android Device UX Verification

**Files:**
- Create: `.superpowers/sdd/mapchina-profile-iteration-after.png`
- Create: `.superpowers/sdd/mapchina-profile-iteration-after.xml`
- Create: `.superpowers/sdd/mapchina-profile-iteration-large-font.png`

**Interfaces:**
- Consumes: installed Android debug app, package `com.mapchina.android`, emulator `emulator-5554`.
- Produces: fresh build/test evidence, normal-font and large-font screenshots, interaction evidence for login navigation, switches, theme selection, scrolling, and back navigation.

- [ ] **Step 1: Run the complete verification build and install**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest installDebug`

Expected: BUILD SUCCESSFUL with all shared tests, Android unit tests, and debug installation passing.

- [ ] **Step 2: Launch and stabilize the app**

```bash
adb -s emulator-5554 shell am force-stop com.mapchina.android
adb -s emulator-5554 shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb -s emulator-5554 shell input tap 946 2210
sleep 2
```

Expected: 我的 is selected and the screen starts with `我的`, `账号与地图偏好`, and the compact account section.

- [ ] **Step 3: Capture normal-font after evidence and inspect the hierarchy**

```bash
adb -s emulator-5554 shell uiautomator dump /sdcard/mapchina-profile-after.xml
adb -s emulator-5554 pull /sdcard/mapchina-profile-after.xml .superpowers/sdd/mapchina-profile-iteration-after.xml
adb -s emulator-5554 shell screencap -p /sdcard/mapchina-profile-after.png
adb -s emulator-5554 pull /sdcard/mapchina-profile-after.png .superpowers/sdd/mapchina-profile-iteration-after.png
rg -n "我的|本地保存|足迹记录|地图外观|仅在本机|由你确认" .superpowers/sdd/mapchina-profile-iteration-after.xml
```

Expected: all six phrases are present; province/city/district counters are absent; no text overlaps the bottom navigation.

- [ ] **Step 4: Exercise login, settings, theme selection, scrolling, and back**

Use coordinates derived from the fresh UI dump rather than stale hard-coded row coordinates:

```bash
adb -s emulator-5554 shell input tap 905 430
sleep 1
adb -s emulator-5554 shell input keyevent 4
adb -s emulator-5554 shell input swipe 540 1800 540 760 450
adb -s emulator-5554 shell input tap 180 1510
adb -s emulator-5554 shell input swipe 540 820 540 1780 450
```

Expected: login opens and returns cleanly; switch taps do not resize rows; a map-theme tile shows a selected border; vertical scrolling remains smooth and does not fight the bottom bar. Restore any setting changed during QA to its original value before continuing.

- [ ] **Step 5: Verify 1.5x font scale and restore the device**

```bash
adb -s emulator-5554 shell settings put system font_scale 1.5
adb -s emulator-5554 shell am force-stop com.mapchina.android
adb -s emulator-5554 shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb -s emulator-5554 shell input tap 946 2210
adb -s emulator-5554 shell screencap -p /sdcard/mapchina-profile-large-font.png
adb -s emulator-5554 pull /sdcard/mapchina-profile-large-font.png .superpowers/sdd/mapchina-profile-iteration-large-font.png
adb -s emulator-5554 shell input swipe 540 1820 540 650 500
adb -s emulator-5554 shell settings put system font_scale 1.0
```

Expected: titles, subtitles, actions, switches, and theme labels remain readable without clipping or overlap at `font_scale=1.5`; font scale is restored to `1.0`.

- [ ] **Step 6: Check runtime errors and final repository state**

```bash
adb -s emulator-5554 logcat -d -t 500 | rg "FATAL EXCEPTION|AndroidRuntime|Process: com.mapchina.android"
git diff --check HEAD
git status --short
```

Expected: crash search returns no app crash; `git diff --check HEAD` is clean; only `docs/superpowers/plans/2026-06-14-haptic-feedback.md` remains unrelated and untracked.
