# First Footprint Map Activation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the full-screen onboarding with an in-map, three-entry first-footprint activation flow that ends in a finite map celebration and returns to the normal tool state.

**Architecture:** `MapViewModel` owns repository-derived first-footprint state, current-location resolution, and a one-shot celebration model. `MapFab` and `RegionCard` render explicit activation variants, while `MapScreen` coordinates the transient map-selection and success presentation. The existing attraction list remains the search surface, with an `autoFocusSearch` navigation flag.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Navigation3, StateFlow, SQLDelight, Robolectric Compose UI tests, Android ADB QA.

## Global Constraints

- Do not show the legacy full-screen onboarding.
- First activation menu contains exactly: `在地图上选择`, `搜索景点`, `使用当前位置`.
- First region selection displays `途经`, `小驻`, `深游` without an intermediate `标记足迹` click.
- First success updates map color and HUD, runs a finite celebration, then restores the regular map tool.
- Photo history remains outside first activation and is labeled `照片回溯 · 实验` in regular mode.
- Every UI/runtime change must pass `./gradlew installDebug` and ADB device verification with before/after screenshots.

---

### Task 1: Repository-Derived Activation State

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`

**Interfaces:**
- Produces: `StateFlow<Boolean> firstFootprintActivation`
- Produces: `StateFlow<FirstFootprintCelebration?> firstFootprintCelebration`
- Produces: `fun activateCurrentLocation()` and `fun dismissFirstFootprintCelebration()`

- [ ] **Step 1: Write failing first-state and celebration tests**

```kotlin
@Test fun emptyRepository_startsFirstFootprintActivation() {
    assertTrue(viewModel.firstFootprintActivation.value)
}

@Test fun firstMark_emitsCelebrationAndEndsActivation() {
    regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
    viewModel.markFootprint("510000", FootprintLevel.DEEP)
    assertFalse(viewModel.firstFootprintActivation.value)
    assertEquals("四川省", viewModel.firstFootprintCelebration.value?.regionName)
}
```

- [ ] **Step 2: Run `./gradlew :shared:allTests` and verify unresolved state references fail**

- [ ] **Step 3: Implement repository-derived state and one-shot celebration**

```kotlin
data class FirstFootprintCelebration(
    val regionId: String,
    val regionName: String,
    val level: FootprintLevel
)

private fun hasAnyFootprint(): Boolean =
    footprintRepository.getFootprintsByUser(userId).isNotEmpty() ||
        footprintRepository.getAttractionVisitsByUser(userId).isNotEmpty()
```

- [ ] **Step 4: Add and test current-location resolution to the most specific matched region**

```kotlin
val target = match.district ?: match.city ?: match.province
selectRegion(target.id)
showRegionPanel(target.id)
```

- [ ] **Step 5: Run `./gradlew :shared:allTests` and verify all tests pass**

### Task 2: Activation Tool And Direct Level Choice

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionCard.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**
- `MapFab(firstFootprintActivation, mapSelectionActive, onChooseMap, onSearchAttraction, onUseCurrentLocation)`
- `RegionCard(firstFootprintActivation: Boolean)`

- [ ] **Step 1: Replace the zero-state UI assertions with failing activation assertions**

```kotlin
onNodeWithText("添加第一处足迹").assertIsDisplayed()
onNodeWithContentDescription("添加第一处足迹").performClick()
onNodeWithText("在地图上选择").assertIsDisplayed()
onNodeWithText("搜索景点").assertIsDisplayed()
onNodeWithText("使用当前位置").assertIsDisplayed()
onAllNodesWithText("随机出发").assertCountEquals(0)
```

- [ ] **Step 2: Run `./gradlew :androidApp:testDebugUnitTest` and verify the new assertions fail**

- [ ] **Step 3: Implement the labeled activation tool and exact three-item menu**

- [ ] **Step 4: Add a failing region-card test proving `途经 / 小驻 / 深游` are visible while `标记足迹` is absent in activation mode**

- [ ] **Step 5: Implement the direct activation layout in `RegionCard` and run Android tests**

### Task 3: Search Focus, Map Coordination, And Success Feedback

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt`
- Delete: `shared/src/commonMain/kotlin/com/mapchina/ui/map/OnboardingOverlay.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/MapAnimationTest.kt`
- Test: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**
- `AttractionsScreen(autoFocusSearch: Boolean = false)`
- `MapController.celebrateOverlay(regionId: String)`
- `suspend fun animateCelebrationPulse(onAlpha: (Float) -> Unit)`

- [ ] **Step 1: Write a failing finite-animation test that verifies the pulse returns to zero and completes**

- [ ] **Step 2: Implement the finite pulse and controller cleanup**

- [ ] **Step 3: Change the attractions route to carry `autoFocusSearch`, then request focus only for the activation entry**

- [ ] **Step 4: Wire map-selection state, current-location activation, direct region confirmation, and the success bar in `MapScreen`**

- [ ] **Step 5: Remove `OnboardingOverlay` and all onboarding-count state**

- [ ] **Step 6: Run `./gradlew :shared:allTests :androidApp:testDebugUnitTest` and verify all tests pass**

### Task 4: Device Verification

**Files:**
- Create QA evidence under `.superpowers/sdd/` only.

- [ ] **Step 1: Run `./gradlew installDebug` and launch with `adb shell monkey`**
- [ ] **Step 2: Capture the zero-footprint map before interaction**
- [ ] **Step 3: Verify the exact three-item activation menu and map-selection prompt**
- [ ] **Step 4: Select a region, verify direct level buttons, mark it, and record the finite map celebration plus HUD update**
- [ ] **Step 5: Reopen the regular tool menu and verify `照片回溯 · 实验`, random, share, and location actions**
- [ ] **Step 6: Repeat layout checks at font scales 1.0 and 1.5, then restore 1.0**
- [ ] **Step 7: Filter app logcat for fatal exceptions, run `git diff --check`, and commit only related files**
