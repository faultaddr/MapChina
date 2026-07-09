# Discover And Carving Visual Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Discover's top recommendation feel destination-led and make the carving studio default to a convincing large cliff-inscription style.

**Architecture:** Keep changes local to the existing Compose screens. Discover reuses `DiscoverRecommendation.imageUrl` with Coil's `AsyncImage`; carving adds small pure helper functions for default brush selection and brush sizing, then uses those helpers in the existing Android Ink rendering pipeline.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Android Ink, Coil 3 Compose, Robolectric Compose UI tests, Gradle.

## Global Constraints

- Preserve unrelated dirty worktree changes.
- Do not add a text-template system for carving in this pass.
- UI/runtime changes require Android device verification: `./gradlew installDebug`, launch with `adb shell monkey`, interact with `adb shell input`, and capture before/after screenshots.
- Robolectric should not instantiate the full Ink canvas; test pure helpers and small Compose surfaces instead.

---

### Task 1: Discover Spotlight Image Background

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt`

**Interfaces:**
- Consumes: `DiscoverRecommendation.imageUrl: String?`
- Produces: `internal fun shouldUseSpotlightImage(imageUrl: String?): Boolean`

- [ ] **Step 1: Write the failing test**

Add this test to `DiscoverScreenTest`:

```kotlin
@Test
fun spotlightUsesImageOnlyWhenRecommendationHasNonBlankImageUrl() {
    assertEquals(false, shouldUseSpotlightImage(null))
    assertEquals(false, shouldUseSpotlightImage(""))
    assertEquals(false, shouldUseSpotlightImage("   "))
    assertEquals(true, shouldUseSpotlightImage("https://example.com/huangshan.jpg"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest"
```

Expected: FAIL because `shouldUseSpotlightImage` is unresolved.

- [ ] **Step 3: Implement image-backed spotlight**

In `DiscoverScreen.kt`, add:

```kotlin
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
```

Add helper:

```kotlin
internal fun shouldUseSpotlightImage(imageUrl: String?): Boolean {
    return !imageUrl.isNullOrBlank()
}
```

Inside `DiscoverSpotlightCard`, before the text column, render:

```kotlin
if (shouldUseSpotlightImage(recommendation?.imageUrl)) {
    AsyncImage(
        model = recommendation?.imageUrl,
        contentDescription = null,
        modifier = Modifier.matchParentSize(),
        contentScale = ContentScale.Crop
    )
}
```

Replace the pure gradient background with a fallback background plus overlays:

```kotlin
.background(
    Brush.linearGradient(
        listOf(Color(0xFF173B35), Color(0xFF245F58), Color(0xFFB9873A))
    )
)
```

Add a vertical dark mask and a subtle brand tint:

```kotlin
Box(
    modifier = Modifier
        .matchParentSize()
        .background(
            Brush.verticalGradient(
                listOf(Color(0x55000000), Color(0x22000000), Color(0xCC000000))
            )
        )
)
Box(
    modifier = Modifier
        .matchParentSize()
        .background(
            Brush.linearGradient(
                listOf(MapChinaColors.Primary.copy(alpha = 0.48f), Color.Transparent, MapChinaColors.AccentGold.copy(alpha = 0.28f))
            )
        )
)
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest"
```

Expected: PASS.

---

### Task 2: Monumental Carving Default And Rendering

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/carving/CarvingScreenTest.kt`

**Interfaces:**
- Produces: `fun defaultCarvingBrushType(): CarvingBrushType`
- Produces: `fun adjustedCarvingBrushSize(brushType: CarvingBrushType, baseSize: Float): Float`
- Consumes: existing `CarvingBrushType.MONUMENTAL`

- [ ] **Step 1: Write the failing test**

Add this test to `CarvingScreenTest`:

```kotlin
@Test
fun monumentalBrushIsDefaultAndScalesLargerThanChisel() {
    assertEquals(CarvingBrushType.MONUMENTAL, defaultCarvingBrushType())
    assertTrue(adjustedCarvingBrushSize(CarvingBrushType.MONUMENTAL, 24f) > adjustedCarvingBrushSize(CarvingBrushType.IRON_CHISEL, 24f))
    assertTrue(adjustedCarvingBrushSize(CarvingBrushType.MONUMENTAL, 24f) >= 64f)
}
```

Add imports:

```kotlin
import org.junit.Assert.assertEquals
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.carving.CarvingScreenTest"
```

Expected: FAIL because `defaultCarvingBrushType` and `adjustedCarvingBrushSize` are unresolved.

- [ ] **Step 3: Implement default and sizing helpers**

In `CarvingScreen.kt`, add:

```kotlin
fun defaultCarvingBrushType(): CarvingBrushType = CarvingBrushType.MONUMENTAL

fun adjustedCarvingBrushSize(brushType: CarvingBrushType, baseSize: Float): Float {
    return when (brushType) {
        CarvingBrushType.IRON_CHISEL -> baseSize * 1.7f
        CarvingBrushType.MONUMENTAL -> baseSize * 2.8f
        CarvingBrushType.WEATHERED -> baseSize * 1.45f
    }
}
```

Use them in state and brush creation:

```kotlin
var brushType by remember { mutableStateOf(defaultCarvingBrushType()) }
var brushSize by remember { mutableStateOf(24f) }
```

```kotlin
val adjustedSize = adjustedCarvingBrushSize(brushType, size)
```

Change size options to:

```kotlin
listOf(14f to "寸", 24f to "尺", 36f to "丈")
```

- [ ] **Step 4: Strengthen monumental rendering**

In the finished-stroke render passes, branch on `brushType == CarvingBrushType.MONUMENTAL`:

```kotlin
val isMonumental = brushType == CarvingBrushType.MONUMENTAL
val haloExtra = if (isMonumental) 34f else 20f
val deepShadowExtra = if (isMonumental) 22f else 12f
val floorAlpha = if (isMonumental) 0.92f else spallAlpha
```

Use the values to make the halo, shadow, floor, highlight, crater, and debris passes visually heavier for large inscriptions.

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.carving.CarvingScreenTest"
```

Expected: PASS.

---

### Task 3: Full Verification And Device QA

**Files:**
- No new source files unless fixing verification failures.

**Interfaces:**
- Consumes: Task 1 and Task 2 code.
- Produces: screenshot evidence under `/tmp`.

- [ ] **Step 1: Run focused tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest" --tests "com.mapchina.ui.carving.CarvingScreenTest"
```

Expected: PASS.

- [ ] **Step 2: Run full regression**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 3: Install and launch**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
```

Expected: app installs and launches.

- [ ] **Step 4: Capture before/after UI evidence**

Use `adb shell input tap`, `adb shell input swipe`, and:

```bash
adb shell screencap -p /sdcard/mapchina-after-discover.png
adb pull /sdcard/mapchina-after-discover.png /tmp/mapchina-after-discover.png
adb shell screencap -p /sdcard/mapchina-after-carving.png
adb pull /sdcard/mapchina-after-carving.png /tmp/mapchina-after-carving.png
```

Expected: Discover spotlight shows a scenic image with a readable gradient mask; carving studio defaults to `榜书` and produces large cliff-inscription strokes.

