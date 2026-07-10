# Discover Recommendation Transition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn every “推荐去点亮” item into an image-backed destination card whose image expands from its list bounds into the attraction detail hero without a second zoom or white flash.

**Architecture:** `DiscoverRecommendationTransition.kt` owns pure transition geometry plus the image card and overlay composables. `DiscoverContent` owns one transition target and a `420ms` `Animatable`, then navigates exactly once; `AttractionDetailScreen` receives a route flag so the matching hero starts fully expanded while non-Discover entries retain their current animation.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10, Navigation3 1.1.1, Coil 3.1, Material 3, Robolectric Compose UI tests, Android ADB screenshots and screen recording

## Global Constraints

- Scope is the “推荐去点亮” list items; the existing 今日推荐 spotlight remains independently clickable.
- Recommendation cards use `148.dp` height and no more than `8.dp` corner radius.
- The transition duration is `420ms` with `FastOutSlowInEasing`.
- The destination hero is `320.dp` high with `0.dp` final corner radius.
- The list image URL is first in the detail image list so the handoff uses the same bitmap.
- Missing or failed images use the same deep-green mountain placeholder in the card and detail hero.
- Repeated taps are ignored while a transition is active.
- No new dependency is added.
- Preserve `docs/superpowers/plans/2026-06-14-haptic-feedback.md` unchanged and untracked.
- After UI/runtime edits, run `./gradlew installDebug`, launch via monkey, exercise the flow with ADB, and capture before/after evidence.

---

### Task 1: Testable Transition Geometry

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransitionTest.kt`

**Interfaces:**
- Consumes: `androidx.compose.ui.geometry.Rect`, a viewport width in pixels, a `320.dp` hero height converted to pixels, and normalized progress.
- Produces: `RecommendationTransitionFrame` and `recommendationTransitionFrame(start, viewportWidthPx, heroHeightPx, progress)`.

- [ ] **Step 1: Write failing geometry tests**

```kotlin
class DiscoverRecommendationTransitionTest {
    private val start = Rect(left = 40f, top = 600f, right = 1040f, bottom = 1044f)

    @Test
    fun frameAtStart_matchesCard() {
        val frame = recommendationTransitionFrame(start, 1080f, 960f, 0f)
        assertEquals(start, frame.bounds)
        assertEquals(8f, frame.cornerRadiusDp)
        assertEquals(0f, frame.backgroundProgress)
    }

    @Test
    fun frameAtEnd_matchesDetailHero() {
        val frame = recommendationTransitionFrame(start, 1080f, 960f, 1f)
        assertEquals(Rect(0f, 0f, 1080f, 960f), frame.bounds)
        assertEquals(0f, frame.cornerRadiusDp)
        assertEquals(1f, frame.backgroundProgress)
    }

    @Test
    fun frameClampsProgress() {
        assertEquals(
            recommendationTransitionFrame(start, 1080f, 960f, 0f),
            recommendationTransitionFrame(start, 1080f, 960f, -1f)
        )
        assertEquals(
            recommendationTransitionFrame(start, 1080f, 960f, 1f),
            recommendationTransitionFrame(start, 1080f, 960f, 2f)
        )
    }
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.discover.DiscoverRecommendationTransitionTest`

Expected: compilation fails because the frame type and function do not exist.

- [ ] **Step 3: Implement clamped linear geometry**

```kotlin
data class RecommendationTransitionFrame(
    val bounds: Rect,
    val cornerRadiusDp: Float,
    val backgroundProgress: Float
)

fun recommendationTransitionFrame(
    start: Rect,
    viewportWidthPx: Float,
    heroHeightPx: Float,
    progress: Float
): RecommendationTransitionFrame {
    val fraction = progress.coerceIn(0f, 1f)
    return RecommendationTransitionFrame(
        bounds = Rect(
            left = lerp(start.left, 0f, fraction),
            top = lerp(start.top, 0f, fraction),
            right = lerp(start.right, viewportWidthPx, fraction),
            bottom = lerp(start.bottom, heroHeightPx, fraction)
        ),
        cornerRadiusDp = lerp(8f, 0f, fraction),
        backgroundProgress = fraction
    )
}
```

- [ ] **Step 4: Run the geometry tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.discover.DiscoverRecommendationTransitionTest`

Expected: PASS for start, end, and clamped progress.

- [ ] **Step 5: Commit geometry**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt shared/src/commonTest/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransitionTest.kt
git commit -m "test(discover): define recommendation transition geometry"
```

---

### Task 2: Image Cards And Expanding Overlay

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt`

**Interfaces:**
- Consumes: `DiscoverRecommendation`, rank, Coil `AsyncImage`, `boundsInRoot()`, `LocalScaffoldBottomPadding`.
- Produces: `RecommendationImageCard(recommendation, rank, onClick)` and `RecommendationTransitionOverlay(target, progress)`; navigation callback remains `(String) -> Unit`.

- [ ] **Step 1: Write failing UI and timing assertions**

Extend the existing Compose test with a nonblank image URL and these assertions:

```kotlin
onNodeWithContentDescription("黄山风景区头图").assertIsDisplayed()
onNodeWithContentDescription("打开景点详情：黄山风景区").performClick()
assertEquals("a1", clickedRecommendation)
```

Add a separate manual-clock test:

```kotlin
mainClock.autoAdvance = false
onNodeWithContentDescription("打开景点详情：黄山风景区").performClick()
assertEquals(null, clickedRecommendation)
mainClock.advanceTimeBy(419L)
assertEquals(null, clickedRecommendation)
mainClock.advanceTimeBy(2L)
waitForIdle()
assertEquals("a1", clickedRecommendation)
```

- [ ] **Step 2: Run DiscoverScreenTest and verify the new semantics fail**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.discover.DiscoverScreenTest`

Expected: FAIL because list items do not expose head-image or open-detail semantics and navigate immediately.

- [ ] **Step 3: Implement RecommendationImageCard**

The card must:

```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color.Transparent,
    modifier = modifier
        .fillMaxWidth()
        .height(148.dp)
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .semantics { contentDescription = "打开景点详情：${recommendation.title}" }
        .clickable { if (bounds != Rect.Zero) onClick(bounds) }
) {
    RecommendationImageLayer(
        recommendation = recommendation,
        rank = rank,
        modifier = Modifier.fillMaxSize()
    )
}
```

`RecommendationImageLayer` uses `AsyncImage` with `ContentScale.Crop`, a deep-green fallback, a vertical readability gradient, rank and level at the top, and title/subtitle/reason at the bottom. The image or fallback exposes `${recommendation.title}头图` semantics.

- [ ] **Step 4: Coordinate one active transition in DiscoverContent**

Wrap the LazyColumn in a root `Box`. Store:

```kotlin
var transitionTarget by remember { mutableStateOf<RecommendationTransitionTarget?>(null) }
val transitionProgress = remember { Animatable(0f) }
val scope = rememberCoroutineScope()
```

On a measured list-card click, set the target, animate `0f → 1f` with `tween(420, easing = FastOutSlowInEasing)`, then call `onRecommendationClick(id)` once. While a target exists, ignore new card clicks and draw `RecommendationTransitionOverlay` above the list.

The overlay uses `recommendationTransitionFrame`, draws a background circle from `target.startBounds.center` until it covers the viewport, renders the same cached image inside the animated rectangle, and interpolates corner radius from `8.dp` to `0.dp`.

- [ ] **Step 5: Run Discover UI tests**

Run: `./gradlew :androidApp:testDebugUnitTest --tests com.mapchina.ui.discover.DiscoverScreenTest`

Expected: PASS; recommendation semantics exist and navigation occurs after the transition duration.

- [ ] **Step 6: Commit list and overlay behavior**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverRecommendationTransition.kt shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt
git commit -m "feat(discover): expand image cards into detail"
```

---

### Task 3: Detail Hero Handoff

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionDetailScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionDetailImageTest.kt`

**Interfaces:**
- Consumes: `AttractionUi.imageUrl`, `AttractionDetail.imageUrls`, route `AttractionDetailScreen.fromDiscover`.
- Produces: `attractionDetailImageUrls(primaryImageUrl, detailImageUrls)` and `animateHeroEntrance: Boolean`.

- [ ] **Step 1: Write failing image-order tests**

```kotlin
class AttractionDetailImageTest {
    @Test
    fun recommendationImage_isFirstAndDeduplicated() {
        assertEquals(
            listOf("card.jpg", "second.jpg"),
            attractionDetailImageUrls("card.jpg", listOf("card.jpg", "second.jpg", ""))
        )
    }

    @Test
    fun blankPrimary_usesDetailImages() {
        assertEquals(
            listOf("detail.jpg"),
            attractionDetailImageUrls(" ", listOf("detail.jpg"))
        )
    }
}
```

- [ ] **Step 2: Run the image-order test and verify it fails**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.attraction.AttractionDetailImageTest`

Expected: compilation fails because `attractionDetailImageUrls` does not exist.

- [ ] **Step 3: Add the route flag and stable image order**

Change the route to:

```kotlin
@Serializable
data class AttractionDetailScreen(
    val attractionId: String,
    val fromDiscover: Boolean = false
) : Screen()
```

Discover navigates with `fromDiscover = true`. AppNavHost passes `animateHeroEntrance = !key.fromDiscover`.

Build detail images with:

```kotlin
fun attractionDetailImageUrls(primaryImageUrl: String?, detailImageUrls: List<String>): List<String> =
    buildList {
        primaryImageUrl?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(detailImageUrls.filter { it.isNotBlank() })
    }.distinct()
```

- [ ] **Step 4: Prevent a second hero zoom for Discover entries**

Add `animateHeroEntrance: Boolean = true` to `AttractionDetailScreen`. Initialize hero state to `1f / 1f / 0f` when false; in that branch animate only `backAlpha` and `contentAlpha`. Other callers retain the current `0.82f / 0f / 30f` hero animation.

- [ ] **Step 5: Run focused and full tests**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest`

Expected: BUILD SUCCESSFUL across Android shared tests, Android app UI tests, release tests, and iOS simulator tests.

- [ ] **Step 6: Commit the detail handoff**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionDetailScreen.kt shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionDetailImageTest.kt
git commit -m "feat(attraction): receive discover image transition"
```

---

### Task 4: Android Motion Verification

**Files:**
- Create: `.superpowers/sdd/mapchina-discover-transition-after.png`
- Create: `.superpowers/sdd/mapchina-discover-transition-detail.png`
- Create: `.superpowers/sdd/mapchina-discover-transition.mp4`
- Create: `.superpowers/sdd/mapchina-discover-transition-large-font.png`

**Interfaces:**
- Consumes: debug app on `emulator-5554`, package `com.mapchina.android`.
- Produces: build/install evidence, list/detail screenshots, motion recording, large-font screenshot, crash-free log evidence.

- [ ] **Step 1: Build, test, and install**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest installDebug`

Expected: BUILD SUCCESSFUL and APK installed on one device.

- [ ] **Step 2: Launch and navigate using fresh UI bounds**

Launch with monkey, wait for splash completion, dump the root UI, and derive the Discover tab center from the `发现` text bounds. Do not reuse stale fixed coordinates.

- [ ] **Step 3: Capture the image-card list state**

Dump UI and screenshot after scrolling `推荐去点亮` into view. Confirm the top three recommendation cards expose `打开景点详情：...` and `...头图` semantics.

- [ ] **Step 4: Record the click transition**

Start `adb shell screenrecord`, tap the first recommendation using its fresh bounds, wait one second, stop recording, and pull the MP4. Capture a detail screenshot. Verify frame-by-frame that the clicked image grows from the card bounds to the top hero with no white frame or second zoom.

- [ ] **Step 5: Verify back and 1.5x font**

Return to Discover, set `font_scale=1.5`, relaunch, open Discover, and capture the recommendation list. Confirm titles/reasons do not cover the level badge and cards remain scrollable. Restore `font_scale=1.0`.

- [ ] **Step 6: Final runtime and repository checks**

```bash
adb -s emulator-5554 logcat -d -t 800 | rg "FATAL EXCEPTION|Process: com.mapchina.android"
git diff --check HEAD
git status --short
```

Expected: no app crash lines; clean diff check; only the unrelated haptic plan remains untracked.
