# South China Sea Ten-Dash Line Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the fragmented South China Sea symbol with exactly ten smooth, independent cartographic strokes on Android and iOS, including the segment east of Taiwan.

**Architecture:** `SouthChinaSea.kt` will expose a small internal Catmull-Rom-to-cubic geometry helper, then convert each geographic dash into one Compose `Path`. `ChinaMapView.kt` will continue to supply the active theme color while `SouthChinaSea.kt` owns width and zoom-dependent alpha. No map state, gesture, cache or persistence code changes.

**Tech Stack:** Kotlin Multiplatform, Compose Canvas `Path`, `kotlin.test`, Gradle, Android ADB, Xcode iOS Simulator.

## Global Constraints

- Keep exactly ten geographic segments using the Ministry of Civil Affairs map reference extents.
- Render each geographic segment as one continuous path with no `PathEffect`.
- Use round caps and round joins.
- National view uses `1.05dp` and `0.50` alpha; closer view uses `0.85dp` and `0.42` alpha.
- Do not add glow, shadow, labels, islands, an inset frame, bitmap overlays or SVG overlays.
- Preserve unrelated worktree changes, especially `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- Any runtime change must pass Android install/ADB screenshot verification and equivalent iOS simulator verification.

---

### Task 1: Smooth Curve Geometry

**Files:**
- Create: `shared/src/commonTest/kotlin/com/mapchina/map/SouthChinaSeaTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt`

**Interfaces:**
- Consumes: projected `List<Offset>` for one geographic dash.
- Produces: `buildSouthChinaSeaCurve(points: List<Offset>): SouthChinaSeaCurve?`, where one curve contains a start point and contiguous cubic commands.

- [ ] **Step 1: Write the failing tests**

```kotlin
class SouthChinaSeaTest {
    @Test
    fun geographicData_containsExactlyNineIndependentSegments() {
        assertEquals(9, SouthChinaSea.DASH_SEGMENTS.size)
        assertTrue(SouthChinaSea.DASH_SEGMENTS.all { it.size >= 3 })
    }

    @Test
    fun smoothCurve_passesThroughEveryProjectedPoint() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 16f), Offset(24f, 20f))
        val curve = requireNotNull(buildSouthChinaSeaCurve(points))

        assertEquals(points.first(), curve.start)
        assertEquals(points.drop(1), curve.commands.map { it.end })
    }

    @Test
    fun smoothCurve_keepsFiniteControlPoints() {
        val points = listOf(Offset(2f, 3f), Offset(8f, 14f), Offset(21f, 16f))
        val curve = requireNotNull(buildSouthChinaSeaCurve(points))

        assertTrue(curve.commands.all { command ->
            command.control1.isFinite && command.control2.isFinite && command.end.isFinite
        })
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.SouthChinaSeaTest --quiet`

Expected: compilation failure because `buildSouthChinaSeaCurve` and its command types do not exist.

- [ ] **Step 3: Implement the pure Catmull-Rom conversion**

```kotlin
internal data class SouthChinaSeaCubicCommand(
    val control1: Offset,
    val control2: Offset,
    val end: Offset
)

internal data class SouthChinaSeaCurve(
    val start: Offset,
    val commands: List<SouthChinaSeaCubicCommand>
)

internal fun buildSouthChinaSeaCurve(points: List<Offset>): SouthChinaSeaCurve? {
    if (points.size < 2 || points.any { !it.isFinite }) return null
    val commands = (0 until points.lastIndex).map { index ->
        val previous = points.getOrElse(index - 1) { points[index] }
        val start = points[index]
        val end = points[index + 1]
        val next = points.getOrElse(index + 2) { end }
        SouthChinaSeaCubicCommand(
            control1 = start + (end - previous) / 6f,
            control2 = end - (next - start) / 6f,
            end = end
        )
    }
    return SouthChinaSeaCurve(points.first(), commands)
}
```

- [ ] **Step 4: Run the focused and shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.SouthChinaSeaTest --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: exit 0 with no test failures.

- [ ] **Step 5: Commit the geometry helper**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt shared/src/commonTest/kotlin/com/mapchina/map/SouthChinaSeaTest.kt
git commit -m "refactor(map): model smooth nine-dash curves"
```

### Task 2: Cartographic Stroke Rendering

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/SouthChinaSeaTest.kt`

**Interfaces:**
- Consumes: `SouthChinaSeaCurve` from Task 1 and theme `strokeColor` from `ChinaMapView`.
- Produces: `southChinaSeaStrokeStyle(zoomLevel: Float): SouthChinaSeaStrokeStyle` and one round-capped, round-joined Compose `Path` per entry in `DASH_SEGMENTS`.

- [ ] **Step 1: Add failing zoom-style tests**

```kotlin
@Test
fun strokeStyle_keepsNationalViewSubordinateToProvinceBoundaries() {
    assertEquals(SouthChinaSeaStrokeStyle(widthDp = 1.05f, alpha = 0.50f), southChinaSeaStrokeStyle(5f))
}

@Test
fun strokeStyle_becomesLighterAndThinnerWhenZoomedIn() {
    assertEquals(SouthChinaSeaStrokeStyle(widthDp = 0.85f, alpha = 0.42f), southChinaSeaStrokeStyle(6f))
}
```

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.SouthChinaSeaTest --quiet`

Expected: compilation failure because the stroke-style type and function do not exist.

- [ ] **Step 2: Replace polyline plus `dashPathEffect` drawing**

```kotlin
internal data class SouthChinaSeaStrokeStyle(val widthDp: Float, val alpha: Float)

internal fun southChinaSeaStrokeStyle(zoomLevel: Float): SouthChinaSeaStrokeStyle =
    if (zoomLevel < 6f) {
        SouthChinaSeaStrokeStyle(widthDp = 1.05f, alpha = 0.50f)
    } else {
        SouthChinaSeaStrokeStyle(widthDp = 0.85f, alpha = 0.42f)
    }

val strokeStyle = southChinaSeaStrokeStyle(zoomLevel)
val lineWidth = strokeStyle.widthDp.dp.toPx()

SouthChinaSea.DASH_SEGMENTS.forEach { segment ->
    val curve = buildSouthChinaSeaCurve(segment.map { projection.project(it.first, it.second) })
        ?: return@forEach
    val path = Path().apply {
        moveTo(curve.start.x, curve.start.y)
        curve.commands.forEach { command ->
            cubicTo(
                command.control1.x,
                command.control1.y,
                command.control2.x,
                command.control2.y,
                command.end.x,
                command.end.y
            )
        }
    }
    drawPath(
        path = path,
        color = strokeColor.copy(alpha = strokeStyle.alpha),
        style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
```

Remove `PathEffect`, remove the unused `islandColor` parameter, and pass `visualStyle.labelColor` directly from `ChinaMapView`.

- [ ] **Step 3: Run focused and full test gates**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.SouthChinaSeaTest --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest --quiet`

Expected: exit 0 with no test failures.

- [ ] **Step 4: Commit the renderer**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonTest/kotlin/com/mapchina/map/SouthChinaSeaTest.kt
git commit -m "fix(map): render nine clean South China Sea strokes"
```

### Task 3: Android And iOS Visual Verification

**Files:**
- Verify only; screenshots remain under ignored `.superpowers/sdd/nine-dash-line/`.

**Interfaces:**
- Consumes: final shared renderer from Task 2.
- Produces: Android and iOS screenshots showing the same ten independent smooth strokes.

- [ ] **Step 1: Build, install and launch Android**

Run: `./gradlew installDebug`

Expected: `Installed on 1 device.`

Run: `adb shell am force-stop com.mapchina.android && adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1`

Expected: one launch event and `MainActivity` in focus after startup.

- [ ] **Step 2: Exercise and capture Android**

Run map tap/swipe interactions, wait for `1/34 已点亮`, then capture:

```bash
adb shell input swipe 350 1450 850 1300 650
adb shell input tap 1160 2518
adb shell screencap -p /sdcard/nine-dash-after.png
adb pull /sdcard/nine-dash-after.png .superpowers/sdd/nine-dash-line/android-after.png
```

Expected: ten complete strokes in the reference positions, no internal micro-dashes, no hard elbows and no app crash/ANR.

- [ ] **Step 3: Build and capture iOS**

Run:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=D8476254-6DB7-4288-B79B-BB4B1A268396' \
  -derivedDataPath /tmp/mapchina-nine-dash-ios CODE_SIGNING_ALLOWED=NO build
xcrun simctl install D8476254-6DB7-4288-B79B-BB4B1A268396 \
  /tmp/mapchina-nine-dash-ios/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch D8476254-6DB7-4288-B79B-BB4B1A268396 com.mapchina.iosApp
xcrun simctl io D8476254-6DB7-4288-B79B-BB4B1A268396 screenshot \
  .superpowers/sdd/nine-dash-line/ios-after.png
```

Expected: build succeeds and iOS shows the same ten-stroke geometry at national-map scale.

- [ ] **Step 4: Final repository checks**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; only the pre-existing untracked haptic plan remains outside committed work.
