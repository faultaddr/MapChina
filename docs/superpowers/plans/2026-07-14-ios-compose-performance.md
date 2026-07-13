# iOS Compose Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Measure and reduce proven iOS Compose stalls in the home map, Discover transition, and carving flow without changing product behavior or regressing Android.

**Architecture:** Add opt-in, invisible cross-platform performance probes and repeatable xctrace capture first. Then remove the known duplicate map render subscription, stabilize listener lifecycles, move volatile state collection into leaf layers, and cache map label measurements; compare the same scripted scenarios before and after using raw trace artifacts and recomposition logs.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, StateFlow, xcrun xctrace, Animation Hitches, Time Profiler, iOS Simulator, shell, Kotlin test, Android ADB, Maestro.

## Global Constraints

- Treat root recomposition as a hypothesis until baseline traces and probe logs show correlation with long frames.
- Measure home map, Discover list/detail, and carving input with the same data and scripted interactions.
- Capture Debug and Release; simulator numbers are relative evidence and must not be described as physical-device absolute performance.
- Do not remove map gestures, map glow, Discover hero continuity, carving layers, or necessary animation to improve a number.
- `MapScreen` must not recompose continuously during a pure camera drag.
- Confirmed invalid root recompositions must fall by at least 80%.
- When baseline P95 exceeds one frame budget, target P95 must improve by at least 20%; no core scenario may regress more than 10%.
- Preserve unrelated worktree changes, especially `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- Every UI/runtime change requires Android install/ADB before-and-after proof and iOS build/install/interactive before-and-after proof.

---

### Task 1: Opt-In Performance Probes And Capture Harness

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/performance/PerformanceTrace.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/performance/PerformanceTrace.android.kt`
- Create: `shared/src/iosMain/kotlin/com/mapchina/performance/PerformanceTrace.ios.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/performance/PerformanceTraceTest.kt`
- Create: `scripts/qa/ios_performance_capture.sh`

**Interfaces:**
- Produces: `@Composable fun RecompositionProbe(name: String)`
- Produces: `expect fun performanceTracingEnabled(): Boolean`
- Produces: `expect fun performanceLog(message: String)`
- Enables tracing only when environment variable `MAPCHINA_PERF_TRACE=1`

- [ ] **Step 1: Write a failing formatting and disabled-path test**

```kotlin
class PerformanceTraceTest {
    @Test
    fun recompositionEvent_hasMachineReadableFormat() {
        assertEquals(
            "MAPCHINA_PERF|RECOMPOSE|MapScreen|42",
            formatRecompositionEvent("MapScreen", 42)
        )
    }
}
```

- [ ] **Step 2: Run the test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.performance.PerformanceTraceTest --quiet`

Expected: FAIL because performance tracing helpers do not exist.

- [ ] **Step 3: Implement an invisible opt-in probe**

```kotlin
@Composable
fun RecompositionProbe(name: String) {
    if (!performanceTracingEnabled()) return
    var count by remember(name) { mutableIntStateOf(0) }
    SideEffect {
        count += 1
        performanceLog(formatRecompositionEvent(name, count))
    }
}
```

Android actual reads `System.getenv("MAPCHINA_PERF_TRACE")` and logs with `Log.d`. iOS actual reads `NSProcessInfo.processInfo.environment` and logs with `NSLog`. Default execution performs no `SideEffect` or logging.

- [ ] **Step 4: Add a repeatable xctrace script**

The script accepts `DEVICE_UDID`, `APP_PATH`, `OUTPUT_DIR`, and `CONFIGURATION`, installs the app, launches with `SIMCTL_CHILD_MAPCHINA_PERF_TRACE=1`, captures logs, then records both templates:

```bash
xcrun xctrace record --template 'Animation Hitches' --device "$DEVICE_UDID" --attach iosApp --time-limit 30s --output "$OUTPUT_DIR/animation-hitches.trace" --no-prompt
xcrun xctrace record --template 'Time Profiler' --device "$DEVICE_UDID" --attach iosApp --time-limit 30s --output "$OUTPUT_DIR/time-profiler.trace" --no-prompt
```

Use `set -euo pipefail`, reject missing arguments, create the output directory, and preserve raw logs and command metadata.

- [ ] **Step 5: Run tests and shell syntax validation**

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

Run: `bash -n scripts/qa/ios_performance_capture.sh`

Expected: exit 0.

- [ ] **Step 6: Commit the measurement harness**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/performance shared/src/androidMain/kotlin/com/mapchina/performance shared/src/iosMain/kotlin/com/mapchina/performance scripts/qa/ios_performance_capture.sh
git commit -m "test(ios): add Compose performance tracing"
```

---

### Task 2: Baseline The Three Product Scenarios

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionDetailScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingArtwork.kt`
- Create: `docs/qa/ios-performance-baseline.md`
- Runtime artifacts: `.superpowers/sdd/ios-performance/before/`

**Interfaces:**
- Adds probes named exactly `MapScreen`, `ChinaMapView`, `RecommendationImageCard`, `AttractionDetailScreen`, `CarvingScreen`, and `CarvingArtwork`
- Produces raw before traces and machine-countable log lines

- [ ] **Step 1: Add probes at the named composition boundaries**

```kotlin
RecompositionProbe("MapScreen")
```

Place that statement as the first statement in the existing `MapScreen` body. Add the corresponding exact-name statement to each of the other named composable bodies. Do not add counters inside lazy-list item loops beyond `RecommendationImageCard` itself.

- [ ] **Step 2: Build Debug and Release simulator apps**

Run Debug and Release `xcodebuild` for `iPhone 17 Pro`; preserve the exact derived-data app paths in the baseline report.

Expected: both builds succeed.

- [ ] **Step 3: Capture fixed before scenarios**

For each configuration, run five trials per scenario and discard the first warm-up:

- Home: wait for stable map, drag/zoom 10 seconds, open/close a province sheet, expand/collapse tools.
- Discover: wait for first image, scroll the recommendation list, open detail, return, repeat once.
- Carving: open a multi-stroke document, write for 10 seconds, undo three times, save, return and scroll the gallery.

Use Maestro for taps and swipes while `ios_performance_capture.sh` records. Save screenshots at the start and end of each flow.

- [ ] **Step 4: Record the baseline without interpretation inflation**

In `docs/qa/ios-performance-baseline.md`, include build type, simulator model/OS, trial count, median/P95 frame duration when exported by the trace, hitch count, top main-thread symbols, and recomposition counts. Label any unavailable xctrace field as “not exported by template”; do not estimate it.

- [ ] **Step 5: Commit only the baseline report**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionDetailScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingArtwork.kt docs/qa/ios-performance-baseline.md
git commit -m "test(ios): capture Compose performance baseline"
```

---

### Task 3: Remove Duplicate Map Render Subscription

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/MapControllerThemeStateTest.kt`

**Interfaces:**
- Produces: `MapController.backgroundTheme: StateFlow<MapTheme>`
- Preserves: `renderState` for `ChinaMapView`
- Removes: `MapScreen` collection of the complete `renderState`

- [ ] **Step 1: Write failing theme-state tests**

```kotlin
@Test
fun backgroundTheme_changesOnlyWhenThemeChanges() {
    val controller = MapController()
    val initial = controller.backgroundTheme.value
    controller.addMarker("m", "marker", 30.0, 120.0, false)
    assertEquals(initial, controller.backgroundTheme.value)
    controller.setBackgroundTheme(MapTheme.STARRY_NIGHT)
    assertEquals(MapTheme.STARRY_NIGHT, controller.backgroundTheme.value)
}
```

- [ ] **Step 2: Run the test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapControllerThemeStateTest --quiet`

Expected: FAIL because `backgroundTheme` does not exist.

- [ ] **Step 3: Add dedicated theme state and remove full-state collection**

```kotlin
private val _backgroundTheme = MutableStateFlow(MapTheme.DEFAULT)
val backgroundTheme: StateFlow<MapTheme> = _backgroundTheme.asStateFlow()

fun setBackgroundTheme(theme: MapTheme) {
    if (_backgroundTheme.value == theme) return
    _backgroundTheme.value = theme
    _renderState.update { it.copy(backgroundTheme = theme, oceanColor = theme.oceanColor) }
}
```

In `MapScreen`, collect `mapController.backgroundTheme`; leave `ChinaMapView` as the sole collector of complete render state.

- [ ] **Step 4: Run map and full tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.*' --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the isolated theme flow**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonTest/kotlin/com/mapchina/map/MapControllerThemeStateTest.kt
git commit -m "perf(map): isolate theme state from render updates"
```

---

### Task 4: Stabilize Map Listener Lifetimes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/MapAnimationTest.kt`

**Interfaces:**
- Produces: `clearInteractionListeners()`
- Registers all map callbacks once per `(mapController, viewModel)` composition lifetime
- Uses `rememberUpdatedState` for changing callback dependencies

- [ ] **Step 1: Write a failing listener cleanup test**

```kotlin
@Test
fun clearInteractionListeners_removesRegisteredCallbacks() {
    val controller = MapController()
    var taps = 0
    controller.setOnRegionTapListener { taps++ }
    controller.clearInteractionListeners()
    controller.dispatchRegionTapForTest("330000")
    assertEquals(0, taps)
}
```

Add these exact methods to `MapController`; production tap handling remains unchanged:

```kotlin
fun clearInteractionListeners() {
    regionTapListener = null
    regionDoubleTapListener = null
    markerTapListener = null
    cameraZoomChangeListener = null
    cameraPositionListener = null
    mapReadyListener = null
    cameraAnimCompleteListener = null
}

internal fun dispatchRegionTapForTest(regionId: String) {
    regionTapListener?.invoke(regionId)
}
```

- [ ] **Step 2: Run the test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.map.MapAnimationTest.clearInteractionListeners*' --quiet`

Expected: FAIL because cleanup does not exist.

- [ ] **Step 3: Move composition-body registrations into `DisposableEffect`**

```kotlin
val latestHaptic by rememberUpdatedState(haptic)

DisposableEffect(mapController, viewModel) {
    mapController.setOnRegionTapListener { regionId ->
        if (viewModel.bottomPanel.value is BottomPanel.Region &&
            viewModel.selectedRegion.value?.regionId == regionId
        ) return@setOnRegionTapListener
        latestHaptic.perform(HapticType.MEDIUM)
        viewModel.selectRegion(regionId)
        viewModel.showRegionPanel(regionId)
    }
    mapController.setOnMarkerTapListener { markerId ->
        viewModel.photoClusters.value.find { it.id == markerId }
            ?.let { photoPreviewCluster = it }
            ?: viewModel.showAttractionPreview(markerId)
    }
    onDispose { mapController.clearInteractionListeners() }
}
```

Include region tap/double-tap, marker tap, map ready, camera position, zoom change, and animation-complete callbacks in the same lifecycle owner or in focused effects keyed only by the controller and owner.

- [ ] **Step 4: Run shared tests and both target compilers**

Run: `./gradlew :shared:allTests :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: PASS.

- [ ] **Step 5: Commit listener lifecycle fixes**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonTest/kotlin/com/mapchina/map/MapAnimationTest.kt
git commit -m "perf(map): stabilize interaction callbacks"
```

---

### Task 5: Move Volatile State Collection Into Leaf Layers

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreenLayers.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapScreenLayerPolicyTest.kt`

**Interfaces:**
- Produces: `MapHeaderLayer`, `MapBottomPanelLayer`, `MapTransientOverlayLayer`, and `MapPhotoPreviewLayer`
- Leaves root `MapScreen` subscribed only to `currentLevel` and dedicated `backgroundTheme`; all other StateFlows are collected in their owning leaf
- Preserves current UI order and navigation callbacks

- [ ] **Step 1: Write failing pure visibility policy tests**

```kotlin
@Test
fun regionPanel_requiresRegionBottomPanelAndSelection() {
    assertTrue(shouldShowRegionPanel(BottomPanel.Region("330000"), selectedRegion = region))
    assertFalse(shouldShowRegionPanel(BottomPanel.None, selectedRegion = region))
    assertFalse(shouldShowRegionPanel(BottomPanel.Region("330000"), selectedRegion = null))
}
```

- [ ] **Step 2: Run the policy test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.map.MapScreenLayerPolicyTest --quiet`

Expected: FAIL because layer policies do not exist.

- [ ] **Step 3: Extract state-owning leaf composables**

```kotlin
@Composable
internal fun MapBottomPanelLayer(
    viewModel: MapViewModel,
    onNavigate: (NavKey) -> Unit,
    onShowAttractions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panel by viewModel.bottomPanel.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val attractions by viewModel.selectedRegionAttractions.collectAsState()
    // Existing RegionCard / attraction preview content unchanged.
}
```

Create focused leaves for header/progress, bottom panels, achievements/first-footprint messages, photo preview, and suggestion banners. Pass stable event lambdas and avoid passing the entire `MapViewModel` to pure visual children below each leaf.

- [ ] **Step 4: Reduce root subscriptions and retain effects**

Keep only states required by root lifecycle effects. Move each effect next to its owning leaf when it depends only on leaf state. Add `RecompositionProbe` to each extracted layer while tracing is enabled.

- [ ] **Step 5: Run policy, shared, and Android UI tests**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest --quiet`

Expected: PASS.

- [ ] **Step 6: Commit state partitioning**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreenLayers.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/MapScreenLayerPolicyTest.kt
git commit -m "perf(map): contain Compose state invalidation"
```

---

### Task 6: Cache Map Label Measurement By Zoom Bucket

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/MapLabelCachePolicy.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/MapLabelCachePolicyTest.kt`

**Interfaces:**
- Produces: `labelZoomBucket(zoom): MapLabelZoomBucket`
- Caches `TextLayoutResult` by labels, visual style, density, and zoom bucket
- Continues to recompute projected positions/collision rectangles while the camera moves

- [ ] **Step 1: Write failing zoom-bucket tests**

```kotlin
@Test
fun zoomBucket_changesOnlyAtFontThresholds() {
    assertEquals(MapLabelZoomBucket.NATIONAL, labelZoomBucket(3.5f))
    assertEquals(MapLabelZoomBucket.NATIONAL, labelZoomBucket(5.9f))
    assertEquals(MapLabelZoomBucket.REGIONAL, labelZoomBucket(7f))
    assertEquals(MapLabelZoomBucket.LOCAL, labelZoomBucket(10f))
}
```

- [ ] **Step 2: Run the policy test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapLabelCachePolicyTest --quiet`

Expected: FAIL because the cache policy does not exist.

- [ ] **Step 3: Cache text measurement outside the Canvas frame loop**

```kotlin
val zoomBucket = labelZoomBucket(zoom)
val measuredLabels = remember(renderState.labels, visualStyle, zoomBucket, density) {
    renderState.labels.values.associateWith { label ->
        textMeasurer.measure(label.name, labelTextStyle(visualStyle, zoomBucket, density))
    }
}
```

Inside Canvas, only project positions, build collision rectangles from cached sizes, and draw accepted layouts. Do not cache screen positions across camera movement.

- [ ] **Step 4: Run map and full tests**

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Commit label caching**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapLabelCachePolicy.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonTest/kotlin/com/mapchina/map/MapLabelCachePolicyTest.kt
git commit -m "perf(map): cache label measurement"
```

---

### Task 7: Reprofile, Verify Thresholds, And Document Evidence

**Files:**
- Create: `docs/qa/ios-performance-after.md`
- Modify: `docs/qa/cross-platform-ui-regression.md`
- Runtime artifacts: `.superpowers/sdd/ios-performance/after/`

**Interfaces:**
- Reuses: exact baseline scripts, data, build types, trial count, and scenarios
- Produces: before/after recomposition counts, trace metrics, screenshots, logs, and threshold verdicts

- [ ] **Step 1: Run all automated verification**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest installDebug --quiet`

Expected: PASS and Android debug APK installed.

Run both Debug and Release `xcodebuild` commands for `iPhone 17 Pro`.

Expected: `** BUILD SUCCEEDED **` for both.

- [ ] **Step 2: Complete Android Iron Law interaction checks**

Use `adb shell monkey`, `adb shell input tap/swipe`, and `adb shell screencap` for home map drag/zoom, province sheet, tool menu, Discover transition, and carving. Compare against before screenshots and inspect logcat for crashes.

- [ ] **Step 3: Repeat all iOS performance trials**

Run the exact five-trial home, Discover, and carving scripts with xctrace. Capture start/end screenshots and simulator error/fault logs. Do not change data volume, simulator model, OS, or build type between baseline and after runs.

- [ ] **Step 4: Calculate threshold verdicts**

For every scenario, report baseline and after median/P95, hitch count, top symbols, and named recomposition counts. Calculate percent change from raw exported values. Evaluate and state a boolean verdict for each rule: root invalid recompositions reduced by at least 80%; baseline P95 over one frame improved by at least 20%; no scenario regressed more than 10%; carving input has no missing or broken strokes; Discover layout remains stable after image load. Mark the P95 improvement rule as not applicable only when the measured baseline is already within one frame budget.

If a threshold fails, inspect the corresponding after trace and continue optimizing the top measured symbol before claiming completion.

- [ ] **Step 5: Write after report and regression matrix**

Include raw trace paths, script revisions, exact commands, screenshot paths, logs, numeric table, and limitations in `docs/qa/ios-performance-after.md`. Update the existing cross-platform matrix with the three performance scenarios.

- [ ] **Step 6: Commit performance verification**

```bash
git add docs/qa/ios-performance-after.md docs/qa/cross-platform-ui-regression.md
git commit -m "test(ios): verify Compose performance improvements"
```
