# Place-Based Carving Creation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make new碑刻 creation feel tied to a real city or scenic site by adding place selection, a stronger摩崖现场 canvas, and empty-save protection.

**Architecture:** Keep the existing Android actual `CarvingListScreen` and `CarvingScreen` implementation. Add a small common `CarvingPlaceTarget` value object so the list can pass a selected location to navigation. Avoid persistence changes; saving still uses `CarvingViewModel.saveCarving`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Android actual UI, Robolectric Compose UI tests, SQLDelight-backed `CarvingViewModel`.

## Global Constraints

- Preserve unrelated dirty worktree files.
- Use TDD: write each behavior test and watch it fail before production edits.
- UI/runtime changes require `./gradlew installDebug` plus Android device/emulator verification with `adb shell monkey`, `adb shell input tap/swipe`, and `adb shell screencap`.
- Do not allow blank `regionId` or `regionName` for newly created carvings.
- Existing edit mode must keep working.

---

### Task 1: Place-Aware Create Callback

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Modify: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Test: `androidApp/src/test/kotlin/com/mapchina/ui/carving/CarvingListScreenTest.kt`

**Interfaces:**
- Produces: `data class CarvingPlaceTarget(val regionId: String, val regionName: String, val attractionId: String? = null, val attractionName: String? = null)`
- Changes: `CarvingListScreen(onCreateClick: (CarvingPlaceTarget) -> Unit, ...)`

- [ ] **Step 1: Write failing create-context test**

```kotlin
@Test fun allCarvingsCreateShowsPlacePickerInsteadOfBlankCanvas() = runComposeUiTest {
    val viewModel = createCarvingViewModel()
    var selected: CarvingPlaceTarget? = null

    setContent {
        CarvingListScreen(
            viewModel = viewModel,
            title = "我的碑刻",
            showAll = true,
            onCreateClick = { selected = it },
            onBack = {}
        )
    }

    onNodeWithContentDescription("新碑刻").performClick()
    onNodeWithText("选择留刻地点").assertIsDisplayed()
    onNodeWithText("杭州市杭州西湖风景区").performClick()

    assertEquals("330100", selected?.regionId)
    assertEquals("杭州市", selected?.regionName)
    assertEquals("mct_1033", selected?.attractionId)
    assertEquals("杭州市杭州西湖风景区", selected?.attractionName)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.carving.CarvingListScreenTest"`

Expected: FAIL because `CarvingPlaceTarget` or the new callback signature does not exist.

- [ ] **Step 3: Implement place target and picker**

Add `CarvingPlaceTarget`, update the expect/actual signature, and make the all-carvings FAB show preset site choices instead of immediately navigating.

- [ ] **Step 4: Update navigation**

In `AppNavHost`, navigate to `CarvingScreen` using the selected target values. For region-specific lists, pass the existing region context directly.

- [ ] **Step 5: Run test to verify it passes**

Run the same targeted Gradle command. Expected: PASS.

### Task 2: Field-Site Carving Studio

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Test: `androidApp/src/test/kotlin/com/mapchina/ui/carving/CarvingScreenTest.kt`

**Interfaces:**
- Consumes: existing `CarvingScreen(regionId, regionName, attractionId, attractionName, carvingId)`
- Produces UI text: `摩崖留刻`, selected place name, `在这面山石上刻下今日足迹`, `先刻下一笔，再落成碑刻`

- [ ] **Step 1: Write failing studio test**

```kotlin
@Test fun carvingStudioShowsPlaceContextAndBlocksEmptySave() = runComposeUiTest {
    val viewModel = createCarvingViewModel()

    setContent {
        CarvingScreen(
            regionId = "330100",
            regionName = "杭州市",
            attractionId = "mct_1033",
            attractionName = "杭州市杭州西湖风景区",
            viewModel = viewModel,
            onBack = {}
        )
    }

    onNodeWithText("摩崖留刻").assertIsDisplayed()
    onNodeWithText("杭州市杭州西湖风景区").assertIsDisplayed()
    onNodeWithText("在这面山石上刻下今日足迹").assertIsDisplayed()
    onNodeWithText("保存").performClick()
    onNodeWithText("先刻下一笔，再落成碑刻").assertIsDisplayed()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.carving.CarvingScreenTest"`

Expected: FAIL because the screen still says `题刻 · ...` and empty save is allowed.

- [ ] **Step 3: Implement studio copy and empty-save guard**

Change the top app bar/content overlay to field-site copy. If `finishedStrokes.isEmpty()`, set a local hint state instead of calling `viewModel.saveCarving`.

- [ ] **Step 4: Strengthen background realism**

Keep `cliff_face`, then add low-alpha crack/moss/light layers and a central carving-safe zone overlay. Keep controls compact at the bottom.

- [ ] **Step 5: Run test to verify it passes**

Run the same targeted Gradle command. Expected: PASS.

### Task 3: Full Verification And Commit

**Files:**
- Verify all changed files from Tasks 1-2.

- [ ] **Step 1: Run focused tests**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.carving.*"
```

Expected: all carving UI tests PASS.

- [ ] **Step 2: Run broader regression tests**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

Expected: both tasks PASS.

- [ ] **Step 3: Install and verify on device**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android 1
adb shell screencap -p /sdcard/mapchina-carving-before.png
adb pull /sdcard/mapchina-carving-before.png /tmp/mapchina-carving-before.png
```

Then navigate with `adb shell input tap/swipe` to 山河 → 碑刻 → `+`, choose a place, enter the studio, tap 保存 with no stroke, and capture `/tmp/mapchina-carving-after.png`.

- [ ] **Step 4: Commit code**

Stage only carving/navigation/test files touched by this plan and commit:

```bash
git commit -m "feat: add place-based carving creation"
```
