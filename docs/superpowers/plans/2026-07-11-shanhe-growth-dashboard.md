# Shanhe Growth Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static Shanhe entry list with a compact, data-backed growth dashboard that shows level progress, the next achievable target, four primary cultural-growth entries, footprint statistics, and recent unlocks.

**Architecture:** Keep `ShanheScreen` as the single navigation surface and make `ShanheViewModel` a presentation facade over the existing `AchievementViewModel` and `StatsViewModel` state flows. A pure `buildShanheUi` mapper isolates copy and metric decisions from Compose, while `ShanheContent` renders the resulting immutable state and remains independently testable.

**Tech Stack:** Kotlin Multiplatform 2.3.20, Compose Multiplatform 1.10.0, Navigation3 1.1.1, Koin 4.0.4, kotlinx.coroutines 1.10.2, Android compileSdk 36.

## Global Constraints

- Preserve the approved product position: MapChina is a half-automatic China footprint recorder, and Shanhe exists to motivate the next footprint action.
- Keep the bottom navigation as `足迹 / 发现 / 山河 / 我的`.
- The first viewport must answer current level, next action, and where growth capabilities live.
- Use real achievement and coverage data already exposed by `AchievementViewModel` and `StatsViewModel`; do not add a parallel persistence path.
- Keep cards at 8dp radius in the new dashboard, avoid nested cards, and use Material icons for recognizable entry actions.
- Preserve the unrelated untracked file `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- Any UI/runtime change requires `./gradlew installDebug`, adb launch/tap/swipe, UI-tree inspection, logcat checks, and before/after screenshots.

---

### Task 1: Dashboard Visual Hierarchy

**Files:**
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`

**Interfaces:**
- Consumes: `ShanheUi` with level, target, achievement, coverage, and recent-unlock fields.
- Produces: `ShanheContent(ui: ShanheUi, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier)` and the dashboard semantics used by device and Compose tests.

- [ ] **Step 1: Write the failing dashboard hierarchy test**

Replace the existing vertical-list assertions with a test that still constructs the current no-argument `ShanheViewModel`, then expects the new information hierarchy:

```kotlin
onNodeWithText("成长进度").assertIsDisplayed()
onNodeWithText("Lv.1").assertIsDisplayed()
onNodeWithText("下一步").assertIsDisplayed()
onNodeWithText("成长图谱").assertIsDisplayed()
onNodeWithText("勋章").assertIsDisplayed()
onNodeWithText("图鉴").assertIsDisplayed()
onNodeWithText("征版").assertIsDisplayed()
onNodeWithText("碑刻").assertIsDisplayed()
onNode(hasScrollAction()).performScrollToNode(hasText("山河账本"))
onNodeWithText("山河账本").assertIsDisplayed()
onNodeWithText("最近解锁").assertIsDisplayed()
```

- [ ] **Step 2: Run the test to verify the current list fails the new contract**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest.shanheScreenShowsGrowthDashboard"
```

Expected: FAIL because `成长进度`, `下一步`, `成长图谱`, and `最近解锁` are not rendered.

- [ ] **Step 3: Expand the immutable presentation model**

Replace the static four-string `ShanheUi` with:

```kotlin
data class ShanheUi(
    val levelNumber: Int = 1,
    val levelTitle: String = "初行者",
    val currentScore: Int = 0,
    val nextLevelTitle: String = "识途者",
    val remainingScore: Int = 100,
    val levelProgress: Float = 0f,
    val targetTitle: String = "点亮第一块版图",
    val targetBody: String = "从足迹页确认一个去过的地方",
    val targetProgressLabel: String = "0 / 1",
    val targetProgress: Float = 0f,
    val unlockedCount: Int = 0,
    val totalAchievementCount: Int = 0,
    val visitedProvinces: Int = 0,
    val totalProvinces: Int = 34,
    val visitedCities: Int = 0,
    val visitedDistricts: Int = 0,
    val recentUnlocks: List<String> = emptyList()
)
```

- [ ] **Step 4: Implement the dashboard layout**

Extract `ShanheContent` and render these sections in order:

```kotlin
ExperiencePageHeader(Copy.SHANHE_TITLE, "把走过的地方，沉淀成自己的山河")
ShanheProgressHero(ui)
ShanheNextAction(ui, onClick = { onNavigate(DiscoverScreen) })
ExperienceSectionHeader("成长图谱")
Row { GrowthEntryTile("勋章", "旅行荣誉", "${ui.unlockedCount}/${ui.totalAchievementCount}", Icons.Default.EmojiEvents, ...) }
Row { GrowthEntryTile("图鉴", "主题览胜", "去收集", Icons.Default.CollectionsBookmark, ...) }
Row { GrowthEntryTile("征版", "点亮中国", "${ui.visitedProvinces}/${ui.totalProvinces}", Icons.Default.Map, ...) }
Row { GrowthEntryTile("碑刻", "石壁留名", "去题刻", Icons.Default.HistoryEdu, ...) }
ExperienceSectionHeader("山河账本", actionLabel = "看统计", onAction = { onNavigate(StatsScreen) })
CoverageLedger(ui, onClick = { onNavigate(StatsScreen) })
ExperienceSectionHeader("最近解锁")
RecentUnlocks(ui.recentUnlocks, onClick = { onNavigate(BadgeWallScreen) })
```

Use stable 8dp surfaces, two equal-width columns, icon blocks instead of decorative text pills, a dark ink-green progress hero with a gold progress accent, and blue/teal/stone accents across the four entries.

- [ ] **Step 5: Run the focused UI test**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest.shanheScreenShowsGrowthDashboard"
```

Expected: PASS.

- [ ] **Step 6: Commit the visual hierarchy**

```bash
git add androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt
git commit -m "feat(ui): rebuild shanhe growth dashboard"
```

---

### Task 2: Real Achievement And Coverage Data

**Files:**
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/shanhe/ShanheViewModelTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`

**Interfaces:**
- Consumes: `AchievementViewModel.ui: StateFlow<AchievementUi>` and `StatsViewModel.stats: StateFlow<StatsUi>`.
- Produces: `buildShanheUi(achievement: AchievementUi, stats: StatsUi): ShanheUi`, `ShanheViewModel.refresh()`, and `ShanheViewModel.ui: StateFlow<ShanheUi>`.

- [ ] **Step 1: Write the failing presentation mapping test**

Create test data with a level, one next target, one unlocked achievement, and footprint coverage, then assert:

```kotlin
val ui = buildShanheUi(achievementUi, statsUi)
assertEquals(3, ui.levelNumber)
assertEquals("远游者", ui.levelTitle)
assertEquals(320, ui.currentScore)
assertEquals("山河行者", ui.nextLevelTitle)
assertEquals(480, ui.remainingScore)
assertEquals("百县行者", ui.targetTitle)
assertEquals("7 / 10", ui.targetProgressLabel)
assertEquals(2, ui.visitedProvinces)
assertEquals(listOf("初见巴蜀"), ui.recentUnlocks)
```

- [ ] **Step 2: Run the mapper test and verify RED**

Run:

```bash
./gradlew :shared:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheViewModelTest.buildShanheUi_usesGrowthAndCoverageData"
```

Expected: FAIL because `buildShanheUi` and the expanded model behavior do not exist.

- [ ] **Step 3: Implement the pure mapper**

Map `AchievementUi.levelInfo`, `nextTarget`, counts, and `recentUnlocked` plus the province/city/district fields from `StatsUi`. Clamp progress to `0f..1f`, clamp remaining score to zero, and preserve the first-footprint fallback copy when no target is available.

```kotlin
fun buildShanheUi(achievement: AchievementUi, stats: StatsUi): ShanheUi {
    val level = achievement.levelInfo
    val target = achievement.nextTarget
    return ShanheUi(
        levelNumber = level?.currentLevel ?: 1,
        levelTitle = level?.currentTitle ?: "初行者",
        currentScore = level?.currentScore ?: 0,
        nextLevelTitle = level?.nextTitle ?: "识途者",
        remainingScore = ((level?.nextLevelScore ?: 100) - (level?.currentScore ?: 0)).coerceAtLeast(0),
        levelProgress = level?.progressToNext ?: 0f,
        targetTitle = target?.definition?.name ?: "点亮第一块版图",
        targetBody = target?.definition?.description ?: "从足迹页确认一个去过的地方",
        targetProgressLabel = target?.let { "${it.progressValue} / ${it.progressTarget}" } ?: "0 / 1",
        targetProgress = target?.progressPercent?.coerceIn(0f, 1f) ?: 0f,
        unlockedCount = achievement.unlockedCount,
        totalAchievementCount = achievement.totalCount,
        visitedProvinces = stats.visitedProvinces,
        totalProvinces = stats.totalProvinces.takeIf { it > 0 } ?: 34,
        visitedCities = stats.visitedCities,
        visitedDistricts = stats.visitedDistricts,
        recentUnlocks = achievement.recentUnlocked.map { it.definition.name }
    )
}
```

- [ ] **Step 4: Turn `ShanheViewModel` into a state-flow facade**

Keep a no-argument fallback for Compose tests, and when both source view models are provided combine them eagerly:

```kotlin
class ShanheViewModel(
    private val achievementViewModel: AchievementViewModel? = null,
    private val statsViewModel: StatsViewModel? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val ui: StateFlow<ShanheUi> = if (achievementViewModel != null && statsViewModel != null) {
        combine(achievementViewModel.ui, statsViewModel.stats, ::buildShanheUi)
            .stateIn(scope, SharingStarted.Eagerly, ShanheUi())
    } else {
        MutableStateFlow(ShanheUi()).asStateFlow()
    }

    fun refresh() {
        achievementViewModel?.refresh()
        statsViewModel?.refreshStats()
    }

    fun onCleared() = scope.cancel()
}
```

Call `viewModel.refresh()` from `ShanheScreen` in `LaunchedEffect(viewModel)`.

- [ ] **Step 5: Wire real source view models in Koin**

Change the module registration to:

```kotlin
single { ShanheViewModel(get(), get()) }
```

The two resolved dependencies are the existing `AchievementViewModel` and `StatsViewModel` singletons registered immediately above.

- [ ] **Step 6: Run focused and surrounding tests**

Run:

```bash
./gradlew :shared:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheViewModelTest" :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest"
```

Expected: PASS.

- [ ] **Step 7: Commit the data integration**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt shared/src/commonTest/kotlin/com/mapchina/ui/shanhe/ShanheViewModelTest.kt
git commit -m "feat(ui): drive shanhe from growth data"
```

---

### Task 3: Full Verification And Device Evidence

**Files:**
- Verify: `.superpowers/sdd/mapchina-ui-iteration-shanhe-before.png`
- Create: `.superpowers/sdd/mapchina-ui-iteration-shanhe-after.png`
- Create: `.superpowers/sdd/mapchina-ui-iteration-shanhe-after-scroll.png`
- Create: `.superpowers/sdd/mapchina-ui-iteration-shanhe-after.xml`

**Interfaces:**
- Consumes: built Android debug APK and `com.mapchina.android/.MainActivity`.
- Produces: fresh automated-test evidence plus device screenshots, UI-tree text, navigation taps, and crash-log checks.

- [ ] **Step 1: Run the full relevant test bundle**

```bash
./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 2: Build and install the current app**

```bash
./gradlew installDebug
```

Expected: APK installs on `emulator-5554`.

- [ ] **Step 3: Launch and navigate using UI-tree coordinates**

```bash
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb exec-out uiautomator dump /dev/tty > .superpowers/sdd/mapchina-ui-iteration-launch.xml
```

Use the `山河` node bounds from the dump to calculate its center, tap it with `adb shell input tap`, then dump the Shanhe tree again.

- [ ] **Step 4: Capture and inspect the first viewport**

```bash
adb exec-out screencap -p > .superpowers/sdd/mapchina-ui-iteration-shanhe-after.png
adb exec-out uiautomator dump /dev/tty > .superpowers/sdd/mapchina-ui-iteration-shanhe-after.xml
```

Expected visible text includes `山河`, `成长进度`, `下一步`, `成长图谱`, `勋章`, `图鉴`, `征版`, and `碑刻` without overlap.

- [ ] **Step 5: Exercise scrolling and a deep-link entry**

```bash
adb shell input swipe 640 2200 640 1300 300
adb exec-out screencap -p > .superpowers/sdd/mapchina-ui-iteration-shanhe-after-scroll.png
```

Use a fresh UI dump to locate and tap `看统计`, confirm the stats destination appears, then send Android back and confirm Shanhe remains stable.

- [ ] **Step 6: Check focus and crashes**

```bash
adb shell dumpsys window | rg 'mCurrentFocus|mFocusedApp'
adb logcat -d -t 600 | rg 'FATAL EXCEPTION|Process: com\.mapchina|AndroidRuntime.*com\.mapchina|Unable to start activity|Unable to resume activity|RuntimeException'
```

Expected: focus remains in `com.mapchina.android/.MainActivity` and the crash filter returns no output.

- [ ] **Step 7: Inspect the net diff**

```bash
git diff --check
git status --short
```

Expected: no whitespace errors and the unrelated haptic-feedback plan remains untouched.
