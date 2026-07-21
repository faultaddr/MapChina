# Home Map Digital Atlas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recompose the home map as a digital atlas with a correctly centered national view, an unframed map title, grouped low-frequency tools, and a single-piece first-footprint action.

**Architecture:** `ViewportState` owns a display-only national fit range while preserving complete map data and bounds. `HomeMapTitle` becomes a focused stateless composable, and the existing `MapFab` API is retained but internally rendered as a compact dock plus one grouped menu so `MapScreen` does not gain more responsibilities. Theme changes stay in `MapTheme`; transient panel and celebration ownership remains in `MapScreen`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3 icons, Navigation3, StateFlow, Kotlin test, Robolectric Compose UI tests, Android ADB.

## Global Constraints

- Preserve footprint persistence, map drill-down, sharing, random departure, photo-history experiment, and the approved first-footprint three-entry flow.
- National display fit uses longitude 73–136 and latitude 15–54; complete map bounds and South China Sea data remain unchanged.
- Do not add a map-theme selector or new dependency.
- All icon touch targets are at least 44dp and all text remains readable at font scale 1.5.
- Any UI/runtime change must pass `./gradlew installDebug`, Pixel 9 Pro ADB verification, an iOS Simulator build, and iPhone 17 Pro interactive verification with before/after screenshots.
- iOS verification must exercise real taps, scrolling, bottom tabs, map selection and return gestures; a successful `xcodebuild` or launch screenshot alone is insufficient.
- Preserve unrelated worktree changes, especially `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.

---

### Task 1: National Atlas Framing

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt`

**Interfaces:**
- Produces: `CHINA_DISPLAY_MIN_LAT = 15.0`
- Produces: `CHINA_DISPLAY_MAX_LAT = 54.0`
- Preserves: `CHINA_MIN_LAT = 14.0`, `CHINA_MAX_LAT = 57.0`
- Preserves: `fun computeChinaFitTarget(padding: Float = 1.02f): Triple<Double, Double, Float>`

- [ ] **Step 1: Add a failing display-center regression test**

```kotlin
@Test
fun chinaFit_usesMainlandDisplayRangeWithoutChangingFullBounds() {
    val viewport = ViewportState()
    viewport.canvasWidth = 1280f
    viewport.canvasHeight = 2600f

    val (_, centerLat, zoom) = viewport.computeChinaFitTarget()

    assertEquals(34.5, centerLat, 0.01)
    assertTrue(zoom >= ViewportState.BASE_ZOOM)
    assertEquals(14.0, ViewportState.CHINA_MIN_LAT)
}
```

- [ ] **Step 2: Run the common test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.ViewportStateTest.chinaFit_usesMainlandDisplayRangeWithoutChangingFullBounds --quiet`

Expected: FAIL because the current center latitude is `28.5`.

- [ ] **Step 3: Separate display fit constants from full bounds**

```kotlin
const val CHINA_DISPLAY_MIN_LNG = 73.0
const val CHINA_DISPLAY_MAX_LNG = 136.0
const val CHINA_DISPLAY_MIN_LAT = 15.0
const val CHINA_DISPLAY_MAX_LAT = 54.0

fun computeChinaFitTarget(padding: Float = 1.02f): Triple<Double, Double, Float> {
    val targetLng = (CHINA_DISPLAY_MIN_LNG + CHINA_DISPLAY_MAX_LNG) / 2.0
    val targetLat = (CHINA_DISPLAY_MIN_LAT + CHINA_DISPLAY_MAX_LAT) / 2.0
    // Existing scale calculation uses the DISPLAY constants only.
}
```

- [ ] **Step 4: Run viewport and full shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.ViewportStateTest --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

---

### Task 2: Default Atlas Surface

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeTest.kt`

**Interfaces:**
- Produces: `MapTheme.DEFAULT.backgroundRes = Res.drawable.bg_rice_paper`
- Produces: `MapTheme.DEFAULT.visualStyle.textureAlpha` in `0.04f..0.10f`
- Preserves: all non-default theme resources and `MapTheme.fromName`

- [ ] **Step 1: Write failing default-theme texture tests**

```kotlin
class MapThemeTest {
    @Test
    fun defaultTheme_usesSubtleExistingPaperTexture() {
        assertNotNull(MapTheme.DEFAULT.backgroundRes)
        assertTrue(MapTheme.DEFAULT.visualStyle.textureAlpha in 0.04f..0.10f)
    }

    @Test
    fun namedThemes_keepTheirExistingIdentity() {
        assertTrue(MapTheme.STARRY_NIGHT.visualStyle.isDark)
        assertTrue(MapTheme.VINTAGE_MAP.visualStyle.textureAlpha >= 0.20f)
    }
}
```

- [ ] **Step 2: Run the theme test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapThemeTest --quiet`

Expected: FAIL because `DEFAULT.backgroundRes` is null and texture alpha is zero.

- [ ] **Step 3: Apply the low-alpha default texture and neutral atlas colors**

```kotlin
DEFAULT(
    displayName = "经典",
    oceanColor = Color(0xFFF2F5F3),
    backgroundRes = Res.drawable.bg_rice_paper
)

MapTheme.DEFAULT -> MapVisualStyle(
    canvasTopColor = Color(0xFFF0F4F2),
    canvasBottomColor = Color(0xFFF7F8F5),
    regionSurfaceColor = Color(0xFFFAFCF9),
    labelColor = Color(0xFF394844),
    chromeColor = Color(0xFFF9FBF8),
    chromeContentColor = Color(0xFF17201E),
    textureAlpha = 0.07f
)
```

- [ ] **Step 4: Run theme and shared tests**

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

---

### Task 3: Unframed Home Map Title

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/HomeMapTitle.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/map/HomeMapTitleTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**
- Produces: `@Composable fun HomeMapTitle(path, currentLevel, visitedCount, totalCount, coveragePercent, onNavigateUp, mapTheme, modifier)`
- Removes: private `HomeMapHud` from `MapScreen.kt`
- Preserves: copy `中国`, `{visited}/{total} 已点亮`, `{level}级地图 · {percent}%`

- [ ] **Step 1: Write failing title layout and navigation tests**

```kotlin
@Test
fun title_displaysNationalProgressWithoutCardChrome() = runComposeUiTest {
    setContent {
        HomeMapTitle(
            path = listOf(BreadcrumbItem("", "中国")),
            currentLevel = "省",
            visitedCount = 1,
            totalCount = 34,
            coveragePercent = 2,
            onNavigateUp = {},
            mapTheme = MapTheme.DEFAULT
        )
    }
    onNodeWithContentDescription("地图铭牌").assertIsDisplayed()
    onNodeWithText("中国").assertIsDisplayed()
    onNodeWithText("1/34 已点亮").assertIsDisplayed()
    onNodeWithText("省级地图 · 2%").assertIsDisplayed()
    onAllNodesWithContentDescription("返回上级").assertCountEquals(0)
}

@Test
fun title_showsBackActionForDrilledPath() = runComposeUiTest {
    var backCount = 0
    setContent {
        HomeMapTitle(
            path = listOf(BreadcrumbItem("", "中国"), BreadcrumbItem("330000", "浙江省")),
            currentLevel = "市",
            visitedCount = 3,
            totalCount = 11,
            coveragePercent = 27,
            onNavigateUp = { backCount++ },
            mapTheme = MapTheme.DEFAULT
        )
    }
    onNodeWithContentDescription("返回上级").performClick()
    assertEquals(1, backCount)
}
```

- [ ] **Step 2: Run the title tests and verify red**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.HomeMapTitleTest --quiet`

Expected: FAIL because `HomeMapTitle` does not exist.

- [ ] **Step 3: Implement the unframed title**

```kotlin
@Composable
fun HomeMapTitle(...) {
    val visualStyle = mapTheme.visualStyle
    Column(
        modifier = modifier.semantics { contentDescription = "地图铭牌" },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (path.size > 1) MapTitleBackButton(onNavigateUp)
            Text(currentName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text("$progressText 已点亮", fontSize = 13.sp, color = accent)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${currentLevel}级地图 · $coveragePercent%", fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            AtlasProgressLine(coveragePercent, visualStyle)
        }
    }
}
```

Do not wrap the column in `Surface`, `Card`, background, border, or shadow.

- [ ] **Step 4: Replace `HomeMapHud` in `MapScreen`**

```kotlin
HomeMapTitle(
    path = listOf(BreadcrumbItem("", "中国")) + currentPath.map { BreadcrumbItem(it.id, it.name) },
    currentLevel = levelLabel,
    visitedCount = visitedCount,
    totalCount = totalCount,
    coveragePercent = coveragePercent,
    onNavigateUp = viewModel::navigateUp,
    mapTheme = currentMapTheme,
    modifier = Modifier
        .align(Alignment.TopStart)
        .statusBarsPadding()
        .padding(top = 14.dp, start = 18.dp, end = 18.dp)
)
```

- [ ] **Step 5: Run title and map screen tests**

Run: `./gradlew :androidApp:testDebugUnitTest --tests 'com.mapchina.ui.map.*TitleTest' --tests com.mapchina.ui.map.MapScreenTest --quiet`

Expected: PASS.

---

### Task 4: Compact Tool Dock And Grouped Menu

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapFabTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**
- Preserves: public `MapFab(...)` signature and all callbacks
- Produces: independent semantics `当前定位` and `地图工具` in regular mode
- Produces: single semantics `添加第一处足迹` in activation mode
- Removes: progress ring and regular-menu row `当前定位`

- [ ] **Step 1: Write failing regular dock tests**

```kotlin
@Test
fun regularMode_exposesDirectLocationAndGroupedToolsWithoutDuplicateLocation() = runComposeUiTest {
    var locationCount = 0
    setContent {
        MapFab(
            coveragePercent = 17,
            photoMarkersVisible = false,
            isExpanded = false,
            onExpandedChange = {},
            onTogglePhotos = {},
            onMyLocation = { locationCount++ }
        )
    }
    onNodeWithContentDescription("当前定位").performClick()
    assertEquals(1, locationCount)
    onNodeWithContentDescription("地图工具").assertIsDisplayed()
}

@Test
fun expandedRegularMode_doesNotRepeatCurrentLocation() = runComposeUiTest {
    setContent {
        MapFab(
            coveragePercent = 17,
            photoMarkersVisible = false,
            isExpanded = true,
            onExpandedChange = {},
            onTogglePhotos = {},
            onDepart = {},
            onShare = {},
            onMyLocation = {}
        )
    }
    onAllNodesWithText("当前定位").assertCountEquals(0)
    onNodeWithText("随机出发").assertIsDisplayed()
    onNodeWithText("照片回溯 · 实验").assertIsDisplayed()
    onNodeWithText("分享").assertIsDisplayed()
}
```

- [ ] **Step 2: Write failing first-footprint single-action test**

```kotlin
@Test
fun firstFootprintMode_usesOneCombinedAction() = runComposeUiTest {
    setContent {
        MapFab(
            coveragePercent = 0,
            photoMarkersVisible = false,
            isExpanded = false,
            onExpandedChange = {},
            onTogglePhotos = {},
            firstFootprintActivation = true,
            onChooseMap = {},
            onSearchAttraction = {},
            onUseCurrentLocation = {}
        )
    }
    onAllNodesWithContentDescription("添加第一处足迹").assertCountEquals(1)
    onNodeWithText("添加第一处足迹").assertIsDisplayed()
    onAllNodesWithContentDescription("地图工具").assertCountEquals(0)
}
```

- [ ] **Step 3: Run dock tests and verify red**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.MapFabTest --quiet`

Expected: FAIL because location is only a menu row and activation is split into two clickable pieces.

- [ ] **Step 4: Replace the progress ball with regular dock buttons**

```kotlin
Column(horizontalAlignment = Alignment.End) {
    AnimatedVisibility(isExpanded) { MapToolPanel(menuItems, themeColors) }
    Spacer(Modifier.height(10.dp))
    if (firstFootprintActivation) {
        FirstFootprintMapAction(mapSelectionActive, isExpanded, onExpandedChange)
    } else {
        MapDockIconButton("当前定位", Icons.Default.MyLocation, 44.dp, onMyLocation)
        Spacer(Modifier.height(8.dp))
        MapDockIconButton("地图工具", Icons.Default.Tune, 48.dp) {
            onExpandedChange(!isExpanded)
        }
    }
}
```

`coveragePercent` stays in the signature for source compatibility during this iteration but is no longer rendered by the dock.

- [ ] **Step 5: Render one grouped tool panel**

```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = surfaceColor,
    shadowElevation = 4.dp,
    border = BorderStroke(1.dp, textColor.copy(alpha = 0.08f)),
    modifier = Modifier.widthIn(min = 180.dp, max = 240.dp)
) {
    Column {
        menuItems.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = textColor.copy(alpha = 0.07f))
            MapToolPanelRow(item, textColor)
        }
    }
}
```

Build the regular `menuItems` without `当前定位`; keep the first-footprint menu exactly `在地图上选择 / 搜索景点 / 使用当前位置`.

- [ ] **Step 6: Run dock and map integration tests**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.MapFabTest --tests com.mapchina.ui.map.MapScreenTest --quiet`

Expected: PASS.

---

### Task 5: Overlay Integration And Responsive Regression

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/FirstFootprintSuccessBarTest.kt`

**Interfaces:**
- Preserves: `LocalScaffoldBottomPadding` positioning
- Preserves: success bar above region panel and dock
- Produces: dock bottom offset `bottomBarOffset + 16.dp`

- [ ] **Step 1: Add failing map-screen state assertions**

```kotlin
@Test
fun existingFootprint_showsDockButHidesItWhenRegionPanelOpens() = runComposeUiTest {
    val fixture = createSuggestionFixture(offerSuggestion = false, existingFootprint = true)
    setContent { MapScreen(onNavigate = {}, onBack = {}, viewModel = fixture.viewModel) }
    onNodeWithContentDescription("当前定位").assertIsDisplayed()
    onNodeWithContentDescription("地图工具").assertIsDisplayed()

    fixture.viewModel.selectRegion("330000")
    fixture.viewModel.showRegionPanel("330000")

    onAllNodesWithContentDescription("当前定位").assertCountEquals(0)
    onAllNodesWithContentDescription("地图工具").assertCountEquals(0)
}
```

- [ ] **Step 2: Run the state test and verify red if the old dock semantics remain**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.MapScreenTest --quiet`

Expected: FAIL before the new dock semantics are integrated; PASS only after Task 4 is wired.

- [ ] **Step 3: Normalize bottom offsets and z-order**

```kotlin
MapFab(
    ...,
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .zIndex(1f)
        .padding(end = 16.dp, bottom = bottomBarOffset + 16.dp)
)
```

Keep region panels above the dock and `FirstFootprintSuccessBar` at `zIndex(3f)`. The existing full-screen scrim remains below the menu and consumes the closing tap.

- [ ] **Step 4: Run full Android and shared tests**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest --quiet`

Expected: PASS.

Run: `git diff --check`

Expected: no output.

---

### Task 6: Pixel 9 Pro Android Verification

**Files:**
- Create QA evidence only under `.superpowers/sdd/`.

**Interfaces:**
- Consumes: the completed Tasks 1–5.
- Produces: Android before/after screenshots, UI trees, and crash-log evidence.

- [ ] **Step 1: Build and install the final app**

Run: `./gradlew installDebug`

Expected: `Installed on 1 device.`

- [ ] **Step 2: Capture clean zero-footprint before/after evidence**

Reset app data, grant photo permission, launch with `adb shell monkey`, then capture:

```bash
adb -s emulator-5554 shell screencap -p /sdcard/home-atlas-zero.png
adb -s emulator-5554 pull /sdcard/home-atlas-zero.png .superpowers/sdd/mapchina-home-atlas-zero-after.png
```

Verify: mainland is vertically centered, the title has no card surface, and the first-footprint action is one combined button.

- [ ] **Step 3: Verify activation menu, direct region levels, and success transition**

Use `adb shell input tap`, `uiautomator dump`, and screenshots to prove:

- exact three-entry activation menu;
- direct `途经 / 小驻 / 深游` region panel;
- immediate HUD and map update;
- success bar replaces the panel and ends at the regular dock.

- [ ] **Step 4: Verify regular dock and grouped menu**

Use the settled one-footprint state to prove:

- visible independent `当前定位` and `地图工具` buttons;
- grouped menu includes random, photo experiment, and share;
- grouped menu does not include duplicate `当前定位`;
- region panel hides the dock.

- [ ] **Step 5: Verify drill-down, 1.5 font scale, and restore device**

Set `font_scale=1.5`, verify title, grouped menu, activation button and region panel fit, then restore `font_scale=1.0`. Exercise return-to-national and confirm the national frame is stable without a startup zoom jump.

- [ ] **Step 6: Android gates**

Run:

```bash
adb -s emulator-5554 logcat -d -t 500 | rg 'FATAL EXCEPTION|ANR in com.mapchina.android|Process: com.mapchina.android'
./gradlew :shared:allTests :androidApp:testDebugUnitTest --quiet
git diff --check
```

Expected: no app fatal/ANR lines, tests exit 0, and diff check produces no output.

Do not commit yet; continue to the iOS gate.

---

### Task 7: iPhone 17 Pro iOS Verification And Shared QA Matrix

**Files:**
- Create: `docs/qa/cross-platform-ui-regression.md`
- Create QA evidence only under `.superpowers/sdd/`.

**Interfaces:**
- Consumes: the same completed Tasks 1–5 installed on iOS.
- Produces: iOS screenshots, interactive-flow notes, Dynamic Type and dark-mode evidence, process logs, and a reusable Android/iOS UI regression matrix.

- [ ] **Step 1: Write the cross-platform regression matrix**

Create `docs/qa/cross-platform-ui-regression.md` with one row per flow and columns `Android`, `iOS`, `Evidence`, `Notes`. Include these exact groups:

```markdown
## App Shell
- Splash to home
- Footprint / Discover / Shanhe / Profile tabs
- Safe-area and system-back behavior

## Footprint Map
- Zero-state combined action and exact three-entry menu
- Search autofocus and return
- Simulated current location
- Map region selection and direct visit depth
- First success, regular dock, grouped menu
- Drill-down and return to national

## Discover
- Daily recommendation hero
- Recommendation list imagery
- List image to detail hero handoff
- Detail scroll and return path

## Carving And Records
- Region carving entry
- Create carving background
- Large calligraphic title effect
- Journal and atlas entry smoke

## Growth And Account
- Shanhe first screen and reachable achievements
- Profile first screen and sync/account status
```

- [ ] **Step 2: Build the iOS app for the available iPhone 17 Pro simulator**

Run:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=D8476254-6DB7-4288-B79B-BB4B1A268396' \
  -derivedDataPath .superpowers/ios-derived \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Expected: `** BUILD SUCCEEDED **` and app at `.superpowers/ios-derived/Build/Products/Debug-iphonesimulator/iosApp.app`.

- [ ] **Step 3: Boot, reset, configure, install and launch iOS**

Run:

```bash
xcrun simctl boot D8476254-6DB7-4288-B79B-BB4B1A268396 || true
open -a Simulator
xcrun simctl uninstall D8476254-6DB7-4288-B79B-BB4B1A268396 com.mapchina.iosApp || true
xcrun simctl install D8476254-6DB7-4288-B79B-BB4B1A268396 .superpowers/ios-derived/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl privacy D8476254-6DB7-4288-B79B-BB4B1A268396 grant photos com.mapchina.iosApp
xcrun simctl privacy D8476254-6DB7-4288-B79B-BB4B1A268396 grant location com.mapchina.iosApp
xcrun simctl location D8476254-6DB7-4288-B79B-BB4B1A268396 set 31.2304,121.4737
xcrun simctl launch --terminate-running-process D8476254-6DB7-4288-B79B-BB4B1A268396 com.mapchina.iosApp
```

Expected: launch returns a process identifier and the app reaches the Footprint tab.

- [ ] **Step 4: Exercise the same recent high-risk flows interactively**

Use the Computer Use skill against the Simulator app. After every click or gesture, refresh app state before reusing an element index. Verify and capture at least:

1. Footprint zero state → exact three-entry menu → map select → direct visit depth → first success → regular dock.
2. Direct current-location action using the Shanghai simulated location; confirm a matched region or explicit unavailable feedback.
3. Drill into a region and return to national; exercise the iOS edge-back gesture on a pushed detail screen.
4. Discover → recommendation list → attraction detail → scroll → return, checking continuous image handoff and no content jump.
5. Region → carving list/create flow, checking real background and large calligraphic title.
6. All four bottom tabs, Shanhe first screen, Profile first screen, and reachable journal/atlas/achievement entries.

Capture settled frames with:

```bash
xcrun simctl io D8476254-6DB7-4288-B79B-BB4B1A268396 screenshot .superpowers/sdd/mapchina-ios-<flow>-after.png
```

Record each result and artifact path in `docs/qa/cross-platform-ui-regression.md`.

- [ ] **Step 5: Verify Dynamic Type and dark appearance**

Run:

```bash
xcrun simctl ui D8476254-6DB7-4288-B79B-BB4B1A268396 content_size extra-extra-extra-large
xcrun simctl ui D8476254-6DB7-4288-B79B-BB4B1A268396 appearance dark
xcrun simctl launch --terminate-running-process D8476254-6DB7-4288-B79B-BB4B1A268396 com.mapchina.iosApp
```

Use Computer Use to inspect the map title, dock, grouped menu, Discover list/detail and carving flow. Verify no overlap, clipped action labels, unsafe top inset, blank map or unreadable dark-theme controls. Capture screenshots, then restore:

```bash
xcrun simctl ui D8476254-6DB7-4288-B79B-BB4B1A268396 content_size large
xcrun simctl ui D8476254-6DB7-4288-B79B-BB4B1A268396 appearance light
```

- [ ] **Step 6: Collect iOS logs and run final dual-platform gates**

Run:

```bash
xcrun simctl spawn D8476254-6DB7-4288-B79B-BB4B1A268396 log show --last 10m --style compact --predicate 'process == "iosApp" AND (messageType == error OR messageType == fault)'
./gradlew :shared:allTests :androidApp:testDebugUnitTest --quiet
git diff --check
```

Expected: no app crash/fault lines, tests exit 0, and diff check produces no output.

- [ ] **Step 7: Stage only related files and commit**

Stage the home-map atlas implementation, tests, updated spec/plan, and `docs/qa/cross-platform-ui-regression.md`. Do not stage `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.

Commit with:

```bash
git commit -m "feat(map): refine home into digital atlas"
```
