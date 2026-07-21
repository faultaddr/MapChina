# Home Map Context Focus Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved “晨雾青瓷” MapChina home screen, single-tap safe-area region focus, persistent national context layers, atomic child-layer loading, and restrained aurora floating action button.

**Architecture:** Keep one shared Compose map renderer, but make every overlay explicitly `CONTEXT` or `ACTIVE`; the renderer draws context first and hit testing only accepts active overlays. A request-gated camera controller and a small `RegionFocusCoordinator` make latest-tap-wins focus deterministic, while `MapViewModel` atomically commits drill-down data only after child boundaries are ready.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Kotlin coroutines and StateFlow, SQLDelight test database, Robolectric Compose UI tests, Gradle, ADB, Xcode Simulator.

## Global Constraints

- Preserve the unrelated untracked file `docs/superpowers/plans/2026-06-14-haptic-feedback.md`; never stage or edit it.
- Do not add a map SDK, screenshot-backed map layer, remote visual asset, or new runtime dependency.
- Default focus duration is exactly `650L` milliseconds.
- Context overlay opacity multiplier is exactly `0.25f`.
- Aurora ring rotation duration is exactly `7000` milliseconds.
- Single tap focuses and then opens the region card; it does not automatically drill down.
- Child overlays become visible only after their data and boundaries are ready; current map content must never be cleared first.
- Other `MapTheme` entries keep their existing visual identity; only `MapTheme.DEFAULT` becomes “晨雾青瓷”.
- Reduced-motion mode uses a `120L` millisecond focus transition and a static aurora ring.
- Every task follows TDD: failing test, observed failure, minimal implementation, passing focused tests, then commit.
- Every UI/runtime change requires Android installation, launch, ADB interaction, before/after screenshots, and screen recording when supported.
- Final verification also requires an iPhone 17 Pro Simulator build, launch, interaction check, and screenshots.

## File Structure

**Create**

- `shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionFocusState.kt` — focus state and latest-request coordinator.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerLoadState.kt` — atomic child-layer loading and error state.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerStatusPill.kt` — loading/error map overlay.
- `shared/src/commonMain/kotlin/com/mapchina/platform/MotionPreferences.kt` — common reduced-motion contract.
- `shared/src/androidMain/kotlin/com/mapchina/platform/MotionPreferences.android.kt` — Android animator-scale implementation.
- `shared/src/iosMain/kotlin/com/mapchina/platform/MotionPreferences.ios.kt` — iOS Reduce Motion implementation.
- `shared/src/commonTest/kotlin/com/mapchina/map/MapOverlayRoleTest.kt` — draw order and hit-test role coverage.
- `shared/src/commonTest/kotlin/com/mapchina/map/MapCameraFocusTest.kt` — safe-area fit and request-gate coverage.
- `shared/src/commonTest/kotlin/com/mapchina/ui/map/RegionFocusCoordinatorTest.kt` — focus transition and latest-request behavior.
- `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapFabMotionPolicyTest.kt` — aurora animation policy.
- `androidApp/src/test/kotlin/com/mapchina/ui/map/MapLayerStatusPillTest.kt` — loading, retry, and dismiss UI.
- `docs/qa/home-map-context-focus.md` — final cross-platform verification record.

**Modify**

- `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt` — overlay role and opacity multiplier.
- `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt` — role updates, active-only hit testing, focus animation requests.
- `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt` — context-first rendering and multiplied alpha.
- `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt` — safe-area camera target.
- `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt` — reusable smooth-step and request gate.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt` — focus state, atomic drill-down, layer roles, retry.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt` — single-tap focus, delayed card, status pill, reduced motion.
- `shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt` — “晨雾青瓷” default tokens.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/HomeMapTitle.kt` — lighter national title and segmented controls.
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt` — rotating sweep-gradient ring and pause policy.
- `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt` — reduced-motion-aware active-layer fade.
- `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt` — safe viewport regression cases.
- `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt` — focus and atomic drill-down behavior.
- `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeTest.kt` — exact default theme tokens.
- `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt` — focus-to-card and status integration.
- `androidApp/src/test/kotlin/com/mapchina/ui/map/HomeMapTitleTest.kt` — title and segmented action.
- `androidApp/src/test/kotlin/com/mapchina/ui/map/MapFabTest.kt` — existing accessibility/menu regression coverage.

## Execution Preflight

- [ ] Confirm the branch and preserve the unrelated file.

```bash
git status --short
git branch --show-current
```

Expected: branch `v1.0.1`; `docs/superpowers/plans/2026-06-14-haptic-feedback.md` may be untracked and must remain untouched.

- [ ] Start the existing Pixel 9 Pro emulator if `adb devices -l` has no device.

```bash
"$HOME/Library/Android/sdk/emulator/emulator" -avd Pixel_9_Pro -no-snapshot-load
adb wait-for-device
adb devices -l
```

Expected: one emulator reaches `device` state.

- [ ] Capture the pre-change Android baseline.

```bash
mkdir -p /tmp/mapchina-home-context-focus-qa
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell screencap -p /sdcard/home-map-before.png
adb pull /sdcard/home-map-before.png /tmp/mapchina-home-context-focus-qa/android-before.png
```

Expected: the current home map is visible in `/tmp/mapchina-home-context-focus-qa/android-before.png`.

---

### Task 1: Explicit Context and Active Overlay Roles

**Files:**

- Create: `shared/src/commonTest/kotlin/com/mapchina/map/MapOverlayRoleTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`

**Interfaces:**

- Produces: `enum class OverlayRole { CONTEXT, ACTIVE }`
- Produces: `OverlayData.role: OverlayRole`
- Produces: `OverlayData.opacityMultiplier: Float`
- Produces: `RenderState.orderedRegionOverlays(): List<Map.Entry<String, OverlayData>>`
- Produces: `MapController.updateOverlayRole(regionId: String, role: OverlayRole, opacityMultiplier: Float)`
- Produces: `MapController.hasOverlay(regionId: String): Boolean`
- Produces: `MapController.updateOverlayPresentation(regionId: String, style: OverlayStyle, isVisited: Boolean, role: OverlayRole, opacityMultiplier: Float)`
- Preserves: existing `addOverlay` callers through default `role = ACTIVE` and `opacityMultiplier = 1f`.

- [ ] **Step 1: Write failing role-order and hit-test tests**

Create `MapOverlayRoleTest.kt`:

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class MapOverlayRoleTest {
    private val square = "[[-1.0,-1.0],[1.0,-1.0],[1.0,1.0],[-1.0,1.0],[-1.0,-1.0]]"
    private val style = OverlayStyle(
        fillColor = 0xFFFFFFFF,
        strokeColor = 0xFF000000,
        strokeWidth = 1f,
        alpha = 1f
    )

    @Test
    fun orderedRegionOverlays_drawsContextBeforeActive() {
        val coords = listOf(listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0))
        val state = RenderState(
            overlays = linkedMapOf(
                "active" to OverlayData(coords, style, role = OverlayRole.ACTIVE),
                "context" to OverlayData(
                    coords,
                    style,
                    role = OverlayRole.CONTEXT,
                    opacityMultiplier = 0.25f
                )
            )
        )

        assertEquals(
            listOf("context", "active"),
            state.orderedRegionOverlays().map { it.key }
        )
    }

    @Test
    fun contextOverlay_isExcludedFromTapHitTesting() {
        val controller = MapController()
        controller.viewport.canvasWidth = 100f
        controller.viewport.canvasHeight = 100f
        controller.viewport.moveTo(0.0, 0.0, ViewportState.BASE_ZOOM)
        controller.addOverlay(
            regionId = "context",
            boundary = square,
            style = style,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )
        controller.updateHitTestBounds(mapOf("context" to Rect(35f, 35f, 65f, 65f)))
        var tapped: String? = null
        controller.setOnRegionTapListener { tapped = it }

        controller.handleTap(Offset(50f, 50f))

        assertNull(tapped)
    }

    @Test
    fun changingContextToActive_restoresTapHitTesting() {
        val controller = MapController()
        controller.viewport.canvasWidth = 100f
        controller.viewport.canvasHeight = 100f
        controller.viewport.moveTo(0.0, 0.0, ViewportState.BASE_ZOOM)
        controller.addOverlay(
            regionId = "region",
            boundary = square,
            style = style,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )
        controller.updateHitTestBounds(mapOf("region" to Rect(35f, 35f, 65f, 65f)))
        controller.updateOverlayRole("region", OverlayRole.ACTIVE, 1f)
        var tapped: String? = null
        controller.setOnRegionTapListener { tapped = it }

        controller.handleTap(Offset(50f, 50f))

        assertEquals("region", tapped)
    }

    @Test
    fun presentationUpdate_reusesParsedGeometry() {
        val controller = MapController()
        controller.addOverlay("region", square, style)
        val before = controller.renderState.value.overlays["region"]?.coords

        controller.updateOverlayPresentation(
            regionId = "region",
            style = style.copy(alpha = 0.5f),
            isVisited = true,
            role = OverlayRole.CONTEXT,
            opacityMultiplier = 0.25f
        )

        val after = controller.renderState.value.overlays["region"]?.coords
        assertSame(before, after)
    }

}
```

- [ ] **Step 2: Run the new test and observe the missing API failure**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapOverlayRoleTest --quiet
```

Expected: FAIL because `OverlayRole`, `role`, `opacityMultiplier`, `orderedRegionOverlays`, and `updateOverlayRole` do not exist.

- [ ] **Step 3: Add overlay roles and active-only hit-test rebuilding**

Add to `RenderState.kt`:

```kotlin
enum class OverlayRole {
    CONTEXT,
    ACTIVE
}

data class OverlayData(
    val coords: List<List<Pair<Double, Double>>>,
    val style: OverlayStyle,
    val isVisited: Boolean = false,
    val role: OverlayRole = OverlayRole.ACTIVE,
    val opacityMultiplier: Float = 1f
)

fun RenderState.orderedRegionOverlays(): List<Map.Entry<String, OverlayData>> =
    overlays.entries.sortedBy { entry ->
        when (entry.value.role) {
            OverlayRole.CONTEXT -> 0
            OverlayRole.ACTIVE -> 1
        }
    }
```

Update `MapController.kt` so it retains projected bounds and rebuilds interactive bounds from roles:

```kotlin
private val projectedOverlayBounds =
    mutableMapOf<String, androidx.compose.ui.geometry.Rect>()

fun addOverlay(
    regionId: String,
    boundary: String,
    style: OverlayStyle,
    isVisited: Boolean = false,
    role: OverlayRole = OverlayRole.ACTIVE,
    opacityMultiplier: Float = 1f
) {
    val coords = BoundaryParser.parseFlatCoords(boundary)
    hitTestCoords[regionId] = coords
    _renderState.update {
        it.copy(
            overlays = it.overlays + (
                regionId to OverlayData(
                    coords = coords,
                    style = style,
                    isVisited = isVisited,
                    role = role,
                    opacityMultiplier = opacityMultiplier
                )
            )
        )
    }
    rebuildInteractiveBounds()
}

fun updateOverlayRole(
    regionId: String,
    role: OverlayRole,
    opacityMultiplier: Float
) {
    val existing = _renderState.value.overlays[regionId] ?: return
    _renderState.update {
        it.copy(
            overlays = it.overlays + (
                regionId to existing.copy(
                    role = role,
                    opacityMultiplier = opacityMultiplier.coerceIn(0f, 1f)
                )
            )
        )
    }
    rebuildInteractiveBounds()
}

fun hasOverlay(regionId: String): Boolean =
    _renderState.value.overlays.containsKey(regionId)

fun updateOverlayPresentation(
    regionId: String,
    style: OverlayStyle,
    isVisited: Boolean,
    role: OverlayRole,
    opacityMultiplier: Float
) {
    val existing = _renderState.value.overlays[regionId] ?: return
    _renderState.update {
        it.copy(
            overlays = it.overlays + (
                regionId to existing.copy(
                    style = style,
                    isVisited = isVisited,
                    role = role,
                    opacityMultiplier = opacityMultiplier.coerceIn(0f, 1f)
                )
            )
        )
    }
    rebuildInteractiveBounds()
}

private fun rebuildInteractiveBounds() {
    val activeIds = _renderState.value.overlays
        .filterValues { it.role == OverlayRole.ACTIVE }
        .keys
    hitTestBounds.clear()
    hitTestBounds.putAll(projectedOverlayBounds.filterKeys { it in activeIds })
}

internal fun updateHitTestBounds(
    bounds: Map<String, androidx.compose.ui.geometry.Rect>
) {
    projectedOverlayBounds.clear()
    projectedOverlayBounds.putAll(bounds)
    rebuildInteractiveBounds()
}
```

Update the removal functions so projected bounds cannot outlive overlays:

```kotlin
fun removeOverlay(regionId: String) {
    hitTestCoords.remove(regionId)
    hitTestBounds.remove(regionId)
    projectedOverlayBounds.remove(regionId)
    _renderState.update { it.copy(overlays = it.overlays - regionId) }
}

fun clearOverlays() {
    hitTestCoords.clear()
    hitTestBounds.clear()
    projectedOverlayBounds.clear()
    _renderState.update { it.copy(overlays = emptyMap()) }
}

fun removeOverlaysExcept(regionIds: Set<String>) {
    hitTestCoords.keys.retainAll(regionIds)
    hitTestBounds.keys.retainAll(regionIds)
    projectedOverlayBounds.keys.retainAll(regionIds)
    _renderState.update {
        it.copy(overlays = it.overlays.filterKeys { key -> key in regionIds })
    }
}
```

Replace the region draw loop in `ChinaMapView.kt` with role-ordered alpha multiplication:

```kotlin
for ((regionId, data) in renderState.orderedRegionOverlays()) {
    val overlayPaths = pathCache.paths[regionId] ?: continue
    val opacity = data.opacityMultiplier.coerceIn(0f, 1f)
    val fillColor = if (visualStyle.isDark && !data.isVisited) {
        visualStyle.regionSurfaceColor.copy(alpha = 0.96f * opacity)
    } else {
        data.style.toFillColor().let { it.copy(alpha = it.alpha * opacity) }
    }
    val strokeColor = if (visualStyle.isDark && !data.isVisited) {
        visualStyle.labelColor.copy(alpha = 0.28f * opacity)
    } else {
        data.style.toStrokeColor().let { it.copy(alpha = it.alpha * opacity) }
    }
    val strokeWidth = if (zoom < 6f) 0.9.dp.toPx() else 0.75.dp.toPx()

    for (path in overlayPaths) {
        drawPath(
            path,
            color = visualStyle.regionSurfaceColor.copy(
                alpha = (if (visualStyle.isDark) 0.88f else 0.94f) * opacity
            )
        )
        drawPath(path, color = fillColor)
        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
    }
}
```

- [ ] **Step 4: Run focused and map regression tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapOverlayRoleTest --tests com.mapchina.map.GeoPathCacheTest --quiet
```

Expected: PASS.

- [ ] **Step 5: Commit the overlay-role unit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonTest/kotlin/com/mapchina/map/MapOverlayRoleTest.kt
git commit -m "refactor(map): model context and active overlays"
```

---

### Task 2: Safe-Area Camera Targets and Request-Gated Focus Animation

**Files:**

- Create: `shared/src/commonTest/kotlin/com/mapchina/map/MapCameraFocusTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt`

**Interfaces:**

- Produces: `data class ViewportInsets(leftPx: Float, topPx: Float, rightPx: Float, bottomPx: Float)`
- Produces: `data class CameraTarget(centerLng: Double, centerLat: Double, zoomLevel: Float)`
- Produces: `ViewportState.computeBoundsFitTarget(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double, insets: ViewportInsets = ViewportInsets(), paddingFraction: Float = 0.75f): CameraTarget`
- Produces: `MapController.focusBounds(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double, insets: ViewportInsets, durationMillis: Long = 650L, onComplete: (Long) -> Unit): Long`
- Produces: `MapController.focusCamera(lat: Double, lng: Double, zoomLevel: Float, insets: ViewportInsets, durationMillis: Long = 650L, onComplete: (Long) -> Unit): Long`
- Produces: `MapController.cancelCameraAnimation()`

- [ ] **Step 1: Write failing camera-fit and request-gate tests**

Create `MapCameraFocusTest.kt`:

```kotlin
package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.pow

class MapCameraFocusTest {
    @Test
    fun boundsFit_centersRegionInsideSafeViewport() {
        val viewport = ViewportState().apply {
            canvasWidth = 1000f
            canvasHeight = 2000f
        }
        val insets = ViewportInsets(
            leftPx = 40f,
            topPx = 200f,
            rightPx = 40f,
            bottomPx = 600f
        )

        val target = viewport.computeBoundsFitTarget(
            minLng = 100.0,
            maxLng = 110.0,
            minLat = 30.0,
            maxLat = 40.0,
            insets = insets
        )
        val projection = GeoProjection(
            viewCenterLng = target.centerLng,
            viewCenterLat = target.centerLat,
            scale = ViewportState.BASE_SCALE *
                2f.pow(target.zoomLevel - ViewportState.BASE_ZOOM),
            canvasWidth = 1000f,
            canvasHeight = 2000f
        )
        val projectedCenter = projection.project(105.0, 35.0)

        assertEquals(500f, projectedCenter.x, 1f)
        assertEquals(800f, projectedCenter.y, 12f)
    }

    @Test
    fun cameraRequestGate_acceptsOnlyLatestRequest() {
        val gate = CameraAnimationRequestGate()
        val first = gate.begin()
        val second = gate.begin()

        assertFalse(gate.isActive(first))
        assertTrue(gate.isActive(second))

        gate.cancel()
        assertFalse(gate.isActive(second))
    }

    @Test
    fun smoothStep_reachesStableEndpoints() {
        assertEquals(0f, smoothStep(0f))
        assertEquals(0.5f, smoothStep(0.5f))
        assertEquals(1f, smoothStep(1f))
    }
}
```

Add a portrait regression to `ViewportStateTest.kt`:

```kotlin
import kotlin.math.pow

@Test
fun boundsFit_withBottomCard_movesContentAbovePhysicalCenter() {
    val viewport = ViewportState().apply {
        canvasWidth = 1080f
        canvasHeight = 2400f
    }
    val target = viewport.computeBoundsFitTarget(
        minLng = 118.0,
        maxLng = 123.0,
        minLat = 28.0,
        maxLat = 35.0,
        insets = ViewportInsets(topPx = 180f, bottomPx = 760f)
    )
    val projection = viewport.toProjection(1080f, 2400f).copy(
        viewCenterLng = target.centerLng,
        viewCenterLat = target.centerLat,
        scale = ViewportState.BASE_SCALE *
            2f.pow(target.zoomLevel - ViewportState.BASE_ZOOM)
    )

    assertTrue(projection.project(120.5, 31.5).y < 1200f)
}
```

- [ ] **Step 2: Run the tests and observe unresolved target APIs**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapCameraFocusTest --tests com.mapchina.map.ViewportStateTest --quiet
```

Expected: FAIL because the inset, target, request gate, and smooth-step APIs do not exist.

- [ ] **Step 3: Implement safe-area fit math**

Add to `ViewportState.kt`:

```kotlin
data class ViewportInsets(
    val leftPx: Float = 0f,
    val topPx: Float = 0f,
    val rightPx: Float = 0f,
    val bottomPx: Float = 0f
)

data class CameraTarget(
    val centerLng: Double,
    val centerLat: Double,
    val zoomLevel: Float
)

fun computeBoundsFitTarget(
    minLng: Double,
    maxLng: Double,
    minLat: Double,
    maxLat: Double,
    insets: ViewportInsets = ViewportInsets(),
    paddingFraction: Float = 0.75f
): CameraTarget {
    val visibleWidth = (canvasWidth - insets.leftPx - insets.rightPx).coerceAtLeast(1f)
    val visibleHeight = (canvasHeight - insets.topPx - insets.bottomPx).coerceAtLeast(1f)
    val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)
    val mercMin = ln(tan(PI / 4 + minLat * PI / 360))
    val mercMax = ln(tan(PI / 4 + maxLat * PI / 360))
    val mercSpanDegrees = ((mercMax - mercMin) * 180.0 / PI).coerceAtLeast(0.0001)
    val scale = minOf(
        visibleWidth * paddingFraction / lngSpan.toFloat(),
        visibleHeight * paddingFraction / mercSpanDegrees.toFloat()
    )
    val zoom = (
        BASE_ZOOM + log2((scale / BASE_SCALE).toDouble()).toFloat()
    ).coerceIn(MIN_ZOOM, MAX_ZOOM)
    val resolvedScale = BASE_SCALE * 2f.pow(zoom - BASE_ZOOM)
    val resolvedMercScale = resolvedScale * (180.0 / PI).toFloat()
    val desiredX = insets.leftPx + visibleWidth / 2f
    val desiredY = insets.topPx + visibleHeight / 2f
    val regionCenterLng = (minLng + maxLng) / 2.0
    val regionCenterMerc = (mercMin + mercMax) / 2.0
    val cameraLng = regionCenterLng -
        (desiredX - canvasWidth / 2f) / resolvedScale
    val cameraMerc = regionCenterMerc +
        (desiredY - canvasHeight / 2f) / resolvedMercScale
    val cameraLat = (2 * atan(exp(cameraMerc)) - PI / 2) * 180 / PI

    return CameraTarget(cameraLng, cameraLat, zoom)
}
```

Keep `MapController.computeZoomForBounds` as this compatibility wrapper:

```kotlin
fun computeZoomForBounds(
    minLng: Double,
    maxLng: Double,
    minLat: Double,
    maxLat: Double
): Triple<Double, Double, Float> {
    val target = viewport.computeBoundsFitTarget(
        minLng = minLng,
        maxLng = maxLng,
        minLat = minLat,
        maxLat = maxLat
    )
    return Triple(target.centerLng, target.centerLat, target.zoomLevel)
}
```

- [ ] **Step 4: Implement the request gate and 650ms focus APIs**

Add to `MapAnimation.kt`:

```kotlin
internal fun smoothStep(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal class CameraAnimationRequestGate {
    private var sequence = 0L
    private var activeRequest = 0L

    fun begin(): Long {
        sequence += 1L
        activeRequest = sequence
        return activeRequest
    }

    fun isActive(requestId: Long): Boolean = requestId == activeRequest

    fun cancel() {
        activeRequest = 0L
    }
}
```

Use the gate in `MapController.kt`:

```kotlin
private val cameraRequestGate = CameraAnimationRequestGate()

fun focusBounds(
    minLng: Double,
    maxLng: Double,
    minLat: Double,
    maxLat: Double,
    insets: ViewportInsets,
    durationMillis: Long = 650L,
    onComplete: (Long) -> Unit
): Long {
    val target = viewport.computeBoundsFitTarget(
        minLng,
        maxLng,
        minLat,
        maxLat,
        insets
    )
    return animateCamera(
        targetLng = target.centerLng,
        targetLat = target.centerLat,
        targetZoom = target.zoomLevel,
        durationMillis = durationMillis,
        onComplete = onComplete
    )
}

fun focusCamera(
    lat: Double,
    lng: Double,
    zoomLevel: Float,
    insets: ViewportInsets,
    durationMillis: Long = 650L,
    onComplete: (Long) -> Unit
): Long {
    val target = viewport.offsetCameraTarget(lng, lat, zoomLevel, insets)
    return animateCamera(
        targetLng = target.centerLng,
        targetLat = target.centerLat,
        targetZoom = target.zoomLevel,
        durationMillis = durationMillis,
        onComplete = onComplete
    )
}

fun cancelCameraAnimation() {
    animJob?.cancel()
    animJob = null
    cameraRequestGate.cancel()
}

private fun animateCamera(
    targetLng: Double,
    targetLat: Double,
    targetZoom: Float,
    durationMillis: Long = 400L,
    onComplete: ((Long) -> Unit)? = null
): Long {
    animJob?.cancel()
    val requestId = cameraRequestGate.begin()
    val startLng = viewport.centerLng
    val startLat = viewport.centerLat
    val startZoom = viewport.zoomLevel

    if (startLng == targetLng && startLat == targetLat && startZoom == targetZoom) {
        if (cameraRequestGate.isActive(requestId)) {
            onComplete?.invoke(requestId)
            cameraAnimCompleteListener?.invoke()
        }
        return requestId
    }

    animJob = animationScope.launch {
        val startTime = TimeSource.Monotonic.markNow()
        while (cameraRequestGate.isActive(requestId)) {
            val elapsed = startTime.elapsedNow().inWholeMilliseconds
            val progress = (elapsed.toFloat() / durationMillis).coerceIn(0f, 1f)
            val eased = smoothStep(progress)
            viewport.updateCamera(
                lng = startLng + (targetLng - startLng) * eased,
                lat = startLat + (targetLat - startLat) * eased,
                zoom = startZoom + (targetZoom - startZoom) * eased
            )
            if (progress >= 1f) break
            delay(16L)
        }
        if (cameraRequestGate.isActive(requestId)) {
            onComplete?.invoke(requestId)
            cameraAnimCompleteListener?.invoke()
        }
    }
    return requestId
}
```

Add `ViewportState.offsetCameraTarget` with explicit fixed-zoom safe-area math:

```kotlin
fun offsetCameraTarget(
    targetLng: Double,
    targetLat: Double,
    zoomLevel: Float,
    insets: ViewportInsets
): CameraTarget {
    val zoom = zoomLevel.coerceIn(MIN_ZOOM, MAX_ZOOM)
    val scale = BASE_SCALE * 2f.pow(zoom - BASE_ZOOM)
    val mercScale = scale * (180.0 / PI).toFloat()
    val visibleWidth =
        (canvasWidth - insets.leftPx - insets.rightPx).coerceAtLeast(1f)
    val visibleHeight =
        (canvasHeight - insets.topPx - insets.bottomPx).coerceAtLeast(1f)
    val desiredX = insets.leftPx + visibleWidth / 2f
    val desiredY = insets.topPx + visibleHeight / 2f
    val centerLng = targetLng - (desiredX - canvasWidth / 2f) / scale
    val targetMerc = ln(tan(PI / 4 + targetLat * PI / 360))
    val centerMerc = targetMerc + (desiredY - canvasHeight / 2f) / mercScale
    val centerLat = (2 * atan(exp(centerMerc)) - PI / 2) * 180 / PI
    return CameraTarget(centerLng, centerLat, zoom)
}
```

Update `detachFromComposition` exactly:

```kotlin
fun detachFromComposition() {
    cancelCameraAnimation()
    pulseJob?.cancel()
}
```

- [ ] **Step 5: Run focused and full common map tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapCameraFocusTest --tests com.mapchina.map.ViewportStateTest --tests com.mapchina.map.MapAnimationTest --quiet
```

Expected: PASS.

- [ ] **Step 6: Commit camera focusing**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt shared/src/commonTest/kotlin/com/mapchina/map/MapCameraFocusTest.kt shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt
git commit -m "feat(map): add safe-area focus animation"
```

---

### Task 3: Latest-Tap Region Focus and Delayed Region Card

**Files:**

- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionFocusState.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/map/RegionFocusCoordinatorTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**

- Produces: `sealed interface RegionFocusState`
- Produces: `MapViewModel.regionFocusState: StateFlow<RegionFocusState>`
- Produces: `MapViewModel.focusRegion(regionId: String, insets: ViewportInsets, reducedMotion: Boolean): Long?`
- Produces: `MapViewModel.cancelRegionFocus()`
- Consumes: Task 2 `MapController.focusBounds`, `focusCamera`, and `cancelCameraAnimation`.

- [ ] **Step 1: Write failing coordinator and ViewModel focus tests**

Create `RegionFocusCoordinatorTest.kt`:

```kotlin
package com.mapchina.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionFocusCoordinatorTest {
    @Test
    fun focus_staysAnimatingUntilMatchingRequestCompletes() {
        val coordinator = RegionFocusCoordinator()
        val request = coordinator.begin("330000")

        assertEquals(
            RegionFocusState.Animating("330000", request),
            coordinator.state.value
        )
        assertTrue(coordinator.complete(request))
        assertEquals(
            RegionFocusState.Focused("330000"),
            coordinator.state.value
        )
    }

    @Test
    fun staleCompletion_cannotReplaceLatestFocus() {
        val coordinator = RegionFocusCoordinator()
        val first = coordinator.begin("330000")
        val second = coordinator.begin("610000")

        assertFalse(coordinator.complete(first))
        assertEquals(
            RegionFocusState.Animating("610000", second),
            coordinator.state.value
        )
        assertTrue(coordinator.complete(second))
        assertEquals(
            RegionFocusState.Focused("610000"),
            coordinator.state.value
        )
    }
}
```

Add to `MapViewModelTest.kt`:

```kotlin
@Test
fun focusRegion_withoutSpatialData_finishesAndSelectsRegion() {
    regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))

    viewModel.focusRegion(
        regionId = "330000",
        insets = com.mapchina.map.ViewportInsets(),
        reducedMotion = false
    )

    assertEquals("330000", viewModel.selectedRegion.value?.regionId)
    assertEquals(
        RegionFocusState.Focused("330000"),
        viewModel.regionFocusState.value
    )
}
```

- [ ] **Step 2: Run tests and observe missing focus state**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.map.RegionFocusCoordinatorTest --tests 'com.mapchina.ui.map.MapViewModelTest.focusRegion*' --quiet
```

Expected: FAIL because the focus coordinator and ViewModel APIs do not exist.

- [ ] **Step 3: Implement the focus coordinator**

Create `RegionFocusState.kt`:

```kotlin
package com.mapchina.ui.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RegionFocusState {
    data object Idle : RegionFocusState
    data class Animating(
        val regionId: String,
        val requestId: Long
    ) : RegionFocusState
    data class Focused(val regionId: String) : RegionFocusState
}

internal class RegionFocusCoordinator {
    private var sequence = 0L
    private val mutableState =
        MutableStateFlow<RegionFocusState>(RegionFocusState.Idle)
    val state: StateFlow<RegionFocusState> = mutableState.asStateFlow()

    fun begin(regionId: String): Long {
        sequence += 1L
        mutableState.value = RegionFocusState.Animating(regionId, sequence)
        return sequence
    }

    fun complete(requestId: Long): Boolean {
        val current = mutableState.value as? RegionFocusState.Animating
            ?: return false
        if (current.requestId != requestId) return false
        mutableState.value = RegionFocusState.Focused(current.regionId)
        return true
    }

    fun cancel() {
        mutableState.value = RegionFocusState.Idle
    }
}
```

- [ ] **Step 4: Wire `focusRegion` through MapViewModel**

Add the coordinator and method to `MapViewModel.kt`:

```kotlin
private val regionFocusCoordinator = RegionFocusCoordinator()
val regionFocusState: StateFlow<RegionFocusState> =
    regionFocusCoordinator.state

fun focusRegion(
    regionId: String,
    insets: ViewportInsets,
    reducedMotion: Boolean
): Long? {
    val region = regionRepository.getRegion(regionId) ?: return null
    selectRegion(regionId)
    clearBottomPanel()
    _mapController?.pulseOverlay(regionId)
    val focusRequest = regionFocusCoordinator.begin(regionId)
    val controller = _mapController
    if (controller == null) {
        regionFocusCoordinator.complete(focusRequest)
        return focusRequest
    }
    val duration = if (reducedMotion) 120L else 650L
    val bounds = regionRepository.getRegionBounds(regionId)
    val complete: (Long) -> Unit = {
        regionFocusCoordinator.complete(focusRequest)
    }
    if (bounds != null) {
        controller.focusBounds(
            minLng = bounds.minLng,
            maxLng = bounds.maxLng,
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            insets = insets,
            durationMillis = duration,
            onComplete = complete
        )
    } else {
        val center = regionRepository.getRegionCenter(regionId)
        if (center == null) {
            regionFocusCoordinator.complete(focusRequest)
        } else {
            val zoom = when (region.level) {
                RegionLevel.PROVINCE -> 7f
                RegionLevel.CITY -> 9f
                RegionLevel.DISTRICT -> 11f
            }
            controller.focusCamera(
                lat = center.first,
                lng = center.second,
                zoomLevel = zoom,
                insets = insets,
                durationMillis = duration,
                onComplete = complete
            )
        }
    }
    return focusRequest
}

fun cancelRegionFocus() {
    regionFocusCoordinator.cancel()
    _mapController?.cancelCameraAnimation()
}
```

Call `cancelRegionFocus()` from `navigateUp`, `navigateToNational`, `clearSelection`, `onCleared`, and when replacing the controller.

- [ ] **Step 5: Wire single tap and delayed card in MapScreen**

Add exact focus insets and observe focus state:

```kotlin
val density = LocalDensity.current
val bottomBarOffset = com.mapchina.ui.LocalScaffoldBottomPadding.current
val reducedMotion = rememberReducedMotionEnabled()
val regionFocusState by viewModel.regionFocusState.collectAsState()
val focusInsets = ViewportInsets(
    leftPx = with(density) { 20.dp.toPx() },
    topPx = with(density) { 104.dp.toPx() },
    rightPx = with(density) { 20.dp.toPx() },
    bottomPx = with(density) {
        (bottomBarOffset + 244.dp).toPx()
    }
)

mapController.setOnRegionTapListener { regionId ->
    haptic.perform(HapticType.MEDIUM)
    mapSelectionActive = false
    viewModel.focusRegion(regionId, focusInsets, reducedMotion)
}
mapController.setOnRegionDoubleTapListener(null)

LaunchedEffect(regionFocusState) {
    val focused = regionFocusState as? RegionFocusState.Focused
        ?: return@LaunchedEffect
    viewModel.showRegionPanel(focused.regionId)
}
```

Remove the old single-tap `selectRegion/showRegionPanel` sequence and the double-tap drill listener. Remove the duplicate later declaration of `bottomBarOffset`. On card close, call `cancelRegionFocus()` before `clearSelection()`.

- [ ] **Step 6: Add the focus-to-card UI regression**

Add to `MapScreenTest.kt`:

```kotlin
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@Test
fun focusedRegion_opensCardOnlyFromFocusedState() = runComposeUiTest {
    val fixture = createSuggestionFixture(
        offerSuggestion = false,
        existingFootprint = true
    )

    setContent {
        MapScreen(
            onNavigate = {},
            onBack = {},
            viewModel = fixture.viewModel
        )
    }

    fixture.viewModel.focusRegion(
        regionId = "330000",
        insets = com.mapchina.map.ViewportInsets(),
        reducedMotion = true
    )
    waitForIdle()

    onNodeWithText("浙江省").assertIsDisplayed()
    onNodeWithText("这次停留有多深？").assertIsDisplayed()
    onAllNodesWithContentDescription("地图工具").assertCountEquals(0)
}
```

- [ ] **Step 7: Run focus and UI tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.map.RegionFocusCoordinatorTest --tests 'com.mapchina.ui.map.MapViewModelTest.focusRegion*' :androidApp:testDebugUnitTest --tests 'com.mapchina.ui.map.MapScreenTest.*focus*' --quiet
```

Expected: PASS.

- [ ] **Step 8: Commit single-tap focus**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionFocusState.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/RegionFocusCoordinatorTest.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt
git commit -m "feat(map): focus tapped regions before opening cards"
```

---

### Task 4: Atomic Child-Layer Loading and Persistent National Context

**Files:**

- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerLoadState.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerStatusPill.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapLayerStatusPillTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`

**Interfaces:**

- Produces: `sealed interface MapLayerLoadState`
- Produces: `MapViewModel.mapLayerLoadState: StateFlow<MapLayerLoadState>`
- Produces: `MapViewModel.retryLayerLoad()`
- Produces: `MapViewModel.dismissLayerLoadError()`
- Consumes: Task 1 overlay roles and exact context opacity `0.25f`.

- [ ] **Step 1: Write failing atomic-load and role tests**

Add to `MapViewModelTest.kt`:

```kotlin
private val provinceBoundary =
    "[[100.0,30.0],[110.0,30.0],[110.0,40.0],[100.0,40.0],[100.0,30.0]]"
private val cityBoundary =
    "[[103.0,32.0],[107.0,32.0],[107.0,36.0],[103.0,36.0],[103.0,32.0]]"
private val districtBoundary =
    "[[104.0,33.0],[106.0,33.0],[106.0,35.0],[104.0,35.0],[104.0,33.0]]"

@Test
fun drillWithoutChildBoundaries_keepsCurrentLevelAndExposesRetry() {
    regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
    regionRepo.updateBoundary("510000", provinceBoundary)

    viewModel.drillIntoRegion("510000")

    assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
    assertTrue(viewModel.currentPath.value.isEmpty())
    assertEquals(
        MapLayerLoadState.Error(
            regionId = "510000",
            message = "市级地图暂时无法展开"
        ),
        viewModel.mapLayerLoadState.value
    )
}

@Test
fun drillWithReadyChildren_commitsPathAndKeepsProvinceAsContext() {
    regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
    regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
    regionRepo.updateBoundary("510000", provinceBoundary)
    regionRepo.updateBoundary("510100", cityBoundary)
    val controller = MapController()
    viewModel.mapController = controller
    viewModel.reloadData()

    viewModel.drillIntoRegion("510000")

    assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
    assertEquals("510000", viewModel.currentPath.value.single().id)
    assertEquals(OverlayRole.CONTEXT, controller.renderState.value.overlays["510000"]?.role)
    assertEquals(0.25f, controller.renderState.value.overlays["510000"]?.opacityMultiplier)
    assertEquals(OverlayRole.ACTIVE, controller.renderState.value.overlays["510100"]?.role)
    assertEquals(MapLayerLoadState.Idle, viewModel.mapLayerLoadState.value)
}
```

Update the existing drill tests to seed the next layer before expecting a committed path:

```kotlin
private fun seedProvinceCityAndDistrict() {
    regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
    regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
    regionRepo.insertRegion(Region("510104", "锦江区", RegionLevel.DISTRICT, "510100"))
    regionRepo.updateBoundary("510000", provinceBoundary)
    regionRepo.updateBoundary("510100", cityBoundary)
    regionRepo.updateBoundary("510104", districtBoundary)
}

@Test
fun drillIntoProvince_updatesCurrentLevel() {
    seedProvinceCityAndDistrict()
    viewModel.drillIntoRegion("510000")
    assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
    assertEquals("510000", viewModel.currentPath.value.single().id)
}

@Test
fun drillIntoCity_thenNavigateUp() {
    seedProvinceCityAndDistrict()
    viewModel.drillIntoRegion("510000")
    viewModel.drillIntoRegion("510100")
    assertEquals(MapZoomLevel.CITY, viewModel.currentLevel.value)

    viewModel.navigateUp()
    assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
    assertEquals("510000", viewModel.currentPath.value.single().id)
}
```

- [ ] **Step 2: Run the tests and observe premature path mutation**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.MapViewModelTest.drill*' --quiet
```

Expected: FAIL because current code updates `currentPath/currentLevel` before child data is ready and has no load-state API.

- [ ] **Step 3: Add explicit child-layer load states**

Create `MapLayerLoadState.kt`:

```kotlin
package com.mapchina.ui.map

sealed interface MapLayerLoadState {
    data object Idle : MapLayerLoadState
    data class Loading(
        val regionId: String,
        val label: String
    ) : MapLayerLoadState
    data class Error(
        val regionId: String,
        val message: String
    ) : MapLayerLoadState
}
```

Add to `MapViewModel.kt`:

```kotlin
private val _mapLayerLoadState =
    MutableStateFlow<MapLayerLoadState>(MapLayerLoadState.Idle)
val mapLayerLoadState: StateFlow<MapLayerLoadState> =
    _mapLayerLoadState.asStateFlow()
private var pendingDrillRegionId: String? = null

private data class PreparedChildLayer(
    val parent: Region,
    val children: List<Region>,
    val boundaries: Map<String, String>
)
```

- [ ] **Step 4: Replace eager drill-down with prepare-then-commit**

Replace `drillIntoRegion` and split child loading:

```kotlin
fun drillIntoRegion(regionId: String) {
    if (_mapLayerLoadState.value is MapLayerLoadState.Loading) return
    if (_currentPath.value.any { it.id == regionId }) return
    val region = regionRepository.getRegion(regionId) ?: return
    val label = when (region.level) {
        RegionLevel.PROVINCE -> "正在展开市级地图"
        RegionLevel.CITY -> "正在展开区级地图"
        RegionLevel.DISTRICT -> return
    }
    pendingDrillRegionId = regionId
    _mapLayerLoadState.value = MapLayerLoadState.Loading(regionId, label)
    vmScope.launch {
        val prepared = prepareChildLayer(region)
        if (pendingDrillRegionId != regionId) return@launch
        if (prepared == null) {
            val targetLabel = if (region.level == RegionLevel.PROVINCE) "市级" else "区级"
            _mapLayerLoadState.value = MapLayerLoadState.Error(
                regionId,
                "${targetLabel}地图暂时无法展开"
            )
            return@launch
        }
        commitChildLayer(prepared)
        pendingDrillRegionId = null
        _mapLayerLoadState.value = MapLayerLoadState.Idle
    }
}

private fun prepareChildLayer(parent: Region): PreparedChildLayer? {
    var children = regionRepository.getChildRegions(parent.id)
    var boundaries = regionRepository.getBoundariesByParentId(parent.id)
    if ((children.isEmpty() || boundaries.isEmpty()) && boundaryLoader != null) {
        val loaded = boundaryLoader.loadChildRegions(parent.id).orEmpty()
        if (loaded.isNotEmpty()) {
            val level = when (parent.level) {
                RegionLevel.PROVINCE -> RegionLevel.CITY
                RegionLevel.CITY -> RegionLevel.DISTRICT
                RegionLevel.DISTRICT -> return null
            }
            regionRepository.insertRegionsInTransaction(
                loaded.map { Region(it.id, it.name, level, parent.id) }
            )
            regionRepository.updateBoundariesInTransaction(
                loaded.map { it.id to it.boundary }
            )
            children = regionRepository.getChildRegions(parent.id)
            boundaries = regionRepository.getBoundariesByParentId(parent.id)
            rebuildChildrenIndex()
        }
    }
    if (children.isEmpty() || boundaries.isEmpty()) return null
    return PreparedChildLayer(parent, children, boundaries)
}

private fun commitChildLayer(prepared: PreparedChildLayer) {
    _currentPath.value = _currentPath.value + prepared.parent
    _currentLevel.value = when (prepared.parent.level) {
        RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
        RegionLevel.CITY -> MapZoomLevel.CITY
        RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
    }
    applyPreparedRegions(prepared.children, prepared.boundaries)
    _selectedRegion.value = null
    _selectedRegionAttractions.value = emptyList()
    _bottomPanel.value = BottomPanel.None
    cancelRegionFocus()
    vmScope.launch { loadAttractionsForRegion(prepared.parent.id) }
}

fun retryLayerLoad() {
    val regionId = (mapLayerLoadState.value as? MapLayerLoadState.Error)?.regionId
        ?: return
    drillIntoRegion(regionId)
}

fun dismissLayerLoadError() {
    pendingDrillRegionId = null
    _mapLayerLoadState.value = MapLayerLoadState.Idle
}
```

Implement `applyPreparedRegions` so `_regions` is complete before the renderer changes:

```kotlin
private fun applyPreparedRegions(
    children: List<Region>,
    boundaries: Map<String, String>
) {
    val footprints = getFootprintCache()
    val coverage = if (childrenIndexReady) {
        computeCoverageBatch(children.map { it.id }, footprints)
    } else {
        emptyMap()
    }
    lastBoundaries = boundaries
    _regions.value = children.map { region ->
        RegionFootprintUi(
            regionId = region.id,
            name = region.name,
            footprintLevel = footprints[region.id],
            normalizedPath = emptyList(),
            bounds = RegionBounds(0f, 0f, 0f, 0f),
            childCoverageRate = coverage[region.id] ?: 0f
        )
    }
    syncOverlaysToMap(boundaries)
}
```

- [ ] **Step 5: Assign explicit roles in `syncOverlaysToMap`**

Add this local helper inside `syncOverlaysToMap` so role/style changes reuse parsed geometry:

```kotlin
fun syncOverlay(
    regionId: String,
    boundary: String,
    style: OverlayStyle,
    isVisited: Boolean,
    role: OverlayRole,
    opacityMultiplier: Float
) {
    if (controller.hasOverlay(regionId)) {
        controller.updateOverlayPresentation(
            regionId = regionId,
            style = style,
            isVisited = isVisited,
            role = role,
            opacityMultiplier = opacityMultiplier
        )
    } else {
        controller.addOverlay(
            regionId = regionId,
            boundary = boundary,
            style = style,
            isVisited = isVisited,
            role = role,
            opacityMultiplier = opacityMultiplier
        )
    }
}
```

For cached provinces outside the national level:

```kotlin
syncOverlay(
    regionId = id,
    boundary = boundary,
    style = style,
    isVisited = fp != null || coverage > 0f,
    role = OverlayRole.CONTEXT,
    opacityMultiplier = 0.25f
)
```

For current `_regions`:

```kotlin
syncOverlay(
    regionId = region.regionId,
    boundary = boundary,
    style = style,
    isVisited = region.footprintLevel != null ||
        region.childCoverageRate > 0f,
    role = OverlayRole.ACTIVE,
    opacityMultiplier = 1f
)
```

At national level, provinces are added only through the current-region loop and remain `ACTIVE`. Remove the old `provinceIds` and `removeHitTestFor(provinceIds)` workaround.

- [ ] **Step 6: Add the loading/error pill and UI tests**

Create `MapLayerStatusPill.kt`:

```kotlin
package com.mapchina.ui.map

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun MapLayerStatusPill(
    state: MapLayerLoadState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        MapLayerLoadState.Idle -> Unit
        is MapLayerLoadState.Loading -> Surface(
            modifier = modifier.semantics {
                contentDescription = "地图层级加载中"
            },
            shape = RoundedCornerShape(18.dp),
            color = MapChinaColors.SurfaceOverlay.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = state.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                color = MapChinaColors.TextPrimary,
                fontSize = 13.sp
            )
        }
        is MapLayerLoadState.Error -> Surface(
            modifier = modifier.semantics {
                contentDescription = "地图层级加载失败"
            },
            shape = RoundedCornerShape(18.dp),
            color = MapChinaColors.SurfaceOverlay.copy(alpha = 0.96f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.message, color = MapChinaColors.TextPrimary, fontSize = 13.sp)
                TextButton(onClick = onRetry) { Text("重试") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    }
}
```

Create `MapLayerStatusPillTest.kt` with two tests:

```kotlin
package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class MapLayerStatusPillTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loading_showsLevelSpecificCopy() = runComposeUiTest {
        setContent {
            MapLayerStatusPill(
                state = MapLayerLoadState.Loading("510000", "正在展开市级地图"),
                onRetry = {},
                onDismiss = {}
            )
        }
        onNodeWithContentDescription("地图层级加载中").assertIsDisplayed()
        onNodeWithText("正在展开市级地图").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun error_exposesRetryAndDismiss() = runComposeUiTest {
        var retry = 0
        var dismiss = 0
        setContent {
            MapLayerStatusPill(
                state = MapLayerLoadState.Error("510000", "市级地图暂时无法展开"),
                onRetry = { retry += 1 },
                onDismiss = { dismiss += 1 }
            )
        }
        onNodeWithText("重试").performClick()
        onNodeWithText("关闭").performClick()
        assertEquals(1, retry)
        assertEquals(1, dismiss)
    }
}
```

Place the pill in `MapScreen.kt` below the HUD, above the map:

```kotlin
MapLayerStatusPill(
    state = mapLayerLoadState,
    onRetry = viewModel::retryLayerLoad,
    onDismiss = viewModel::dismissLayerLoadError,
    modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 108.dp)
        .zIndex(2f)
)
```

Keep the region card visible while loading; its drill action calls `viewModel.drillIntoRegion(regionId)` directly without first clearing the panel or delaying 200ms.

- [ ] **Step 7: Run drill-down and status tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.map.MapViewModelTest.drill*' :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.MapLayerStatusPillTest --tests com.mapchina.ui.map.MapScreenTest --quiet
```

Expected: PASS.

- [ ] **Step 8: Commit atomic child-layer loading**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerLoadState.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapLayerStatusPill.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt androidApp/src/test/kotlin/com/mapchina/ui/map/MapLayerStatusPillTest.kt
git commit -m "feat(map): keep context visible during drilldown"
```

---

### Task 5: “晨雾青瓷” Default Theme and Lightweight HUD

**Files:**

- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/HomeMapTitle.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/MapThemeTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/HomeMapTitleTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`

**Interfaces:**

- Produces: exact default theme colors from the approved visual direction.
- Changes: `HomeMapTitle` gains `onNavigateToNational: () -> Unit`.
- Preserves: region-level back action and progress semantics.

- [ ] **Step 1: Write failing exact-token and HUD tests**

Add to `MapThemeTest.kt`:

```kotlin
import androidx.compose.ui.graphics.Color
import kotlin.test.assertEquals

@Test
fun defaultTheme_usesMorningJadePalette() {
    val style = MapTheme.DEFAULT.visualStyle
    assertEquals(Color(0xFFF8FBF6), style.canvasTopColor)
    assertEquals(Color(0xFFDFF0EC), style.canvasBottomColor)
    assertEquals(Color(0xFFF8FCF8), style.regionSurfaceColor)
    assertEquals(Color(0xFF173D36), style.chromeContentColor)
    assertEquals(0.05f, style.textureAlpha)
}
```

Add `onNavigateToNational = {}` to every pre-existing `HomeMapTitle` construction in `HomeMapTitleTest.kt`, and add `onNavigateToNational = viewModel::navigateToNational` to the `HomeMapTitle` call in `MapScreen.kt`.

Replace exact national-title assertions in both `HomeMapTitleTest.kt` and `MapScreenTest.kt`:

```kotlin
onNodeWithText("中国足迹").assertIsDisplayed()
```

Keep drilled-level assertions on the real region name, such as `"浙江省"`.

Update `HomeMapTitleTest.kt`:

```kotlin
@Test
fun title_displaysFreshNationalHudAndNationalAction() = runComposeUiTest {
    var nationalCount = 0
    setContent {
        HomeMapTitle(
            path = listOf(BreadcrumbItem("", "中国")),
            currentLevel = "省",
            visitedCount = 8,
            totalCount = 34,
            coveragePercent = 23,
            onNavigateUp = {},
            onNavigateToNational = { nationalCount += 1 },
            mapTheme = MapTheme.DEFAULT
        )
    }

    onNodeWithText("中国足迹").assertIsDisplayed()
    onNodeWithText("8/34 已点亮").assertIsDisplayed()
    onNodeWithText("足迹").assertIsDisplayed()
    onNodeWithText("全国").assertIsDisplayed().performClick()
    assertEquals(1, nationalCount)
}
```

- [ ] **Step 2: Run focused tests and observe palette/signature failures**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapThemeTest :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.HomeMapTitleTest --quiet
```

Expected: FAIL on old colors, old national title, and missing `onNavigateToNational`.

- [ ] **Step 3: Apply exact “晨雾青瓷” default tokens**

Replace the `MapTheme.DEFAULT` visual style:

```kotlin
MapTheme.DEFAULT -> MapVisualStyle(
    canvasTopColor = Color(0xFFF8FBF6),
    canvasBottomColor = Color(0xFFDFF0EC),
    regionSurfaceColor = Color(0xFFF8FCF8),
    labelColor = Color(0xFF294943),
    chromeColor = Color(0xFFFCFFFC),
    chromeContentColor = Color(0xFF173D36),
    textureAlpha = 0.05f
)
```

Change the default ocean color to `Color(0xFFEAF6F0)`. Do not edit any other theme branch.

- [ ] **Step 4: Implement the lightweight title and segmented controls**

Change the title calculation:

```kotlin
val currentName = if (path.size == 1) {
    "中国足迹"
} else {
    path.lastOrNull()?.name ?: "中国足迹"
}
```

Add `onNavigateToNational` to the parameters and render this compact control beneath the title row:

```kotlin
Surface(
    shape = RoundedCornerShape(14.dp),
    color = visualStyle.chromeColor.copy(alpha = 0.62f),
    shadowElevation = 2.dp,
    border = BorderStroke(
        1.dp,
        visualStyle.chromeContentColor.copy(alpha = 0.06f)
    )
) {
    Row(modifier = Modifier.padding(3.dp)) {
        Text(
            text = "足迹",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF44C6AF), Color(0xFF1D9E92))
                    ),
                    RoundedCornerShape(11.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Text(
            text = "全国",
            color = visualStyle.chromeContentColor.copy(alpha = 0.64f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .clickable(onClick = onNavigateToNational)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
```

Keep `省级地图 · 23%` and the slim progress bar in the same HUD, with no full-width card background. Pass `viewModel::navigateToNational` from `MapScreen`.

- [ ] **Step 5: Run theme, HUD, and screen regression tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.map.MapThemeTest :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.HomeMapTitleTest --tests com.mapchina.ui.map.MapScreenTest --quiet
```

Expected: PASS.

- [ ] **Step 6: Commit the default visual refresh**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/HomeMapTitle.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonTest/kotlin/com/mapchina/map/MapThemeTest.kt androidApp/src/test/kotlin/com/mapchina/ui/map/HomeMapTitleTest.kt androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt
git commit -m "feat(map): apply morning jade home theme"
```

---

### Task 6: Reduced-Motion-Aware Aurora Floating Action Button

**Files:**

- Create: `shared/src/commonMain/kotlin/com/mapchina/platform/MotionPreferences.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/platform/MotionPreferences.android.kt`
- Create: `shared/src/iosMain/kotlin/com/mapchina/platform/MotionPreferences.ios.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapFabMotionPolicyTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapFabTest.kt`

**Interfaces:**

- Produces: `@Composable expect fun rememberReducedMotionEnabled(): Boolean`
- Produces: `auroraMotionEnabled(isExpanded: Boolean, reducedMotion: Boolean, isScreenActive: Boolean): Boolean`
- Produces: `mapLayerTransitionDurationMillis(reducedMotion: Boolean): Int`
- Changes: `MapFab` gains `reducedMotion: Boolean = false` and `isScreenActive: Boolean = true`.
- Preserves: `"当前定位"`, `"地图工具"`, first-footprint action, menu copy, click sizes, and haptics.

- [ ] **Step 1: Write the failing aurora motion policy test**

Create `MapFabMotionPolicyTest.kt`:

```kotlin
package com.mapchina.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.mapchina.map.focusOverlayOpacity
import com.mapchina.map.mapLayerTransitionDurationMillis

class MapFabMotionPolicyTest {
    @Test
    fun aurora_runsOnlyWhenCollapsedAndMotionAllowed() {
        assertTrue(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = false,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = true,
                reducedMotion = false,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = true,
                isScreenActive = true
            )
        )
        assertFalse(
            auroraMotionEnabled(
                isExpanded = false,
                reducedMotion = false,
                isScreenActive = false
            )
        )
    }

    @Test
    fun activeLayerFade_isRemovedForReducedMotion() {
        assertEquals(220, mapLayerTransitionDurationMillis(false))
        assertEquals(0, mapLayerTransitionDurationMillis(true))
    }

    @Test
    fun focusedMap_dimsOnlyNonSelectedActiveRegions() {
        assertEquals(
            1f,
            focusOverlayOpacity(isSelected = true, hasFocus = true, progress = 1f)
        )
        assertEquals(
            0.25f,
            focusOverlayOpacity(isSelected = false, hasFocus = true, progress = 1f)
        )
        assertEquals(
            1f,
            focusOverlayOpacity(isSelected = false, hasFocus = false, progress = 1f)
        )
    }
}
```

- [ ] **Step 2: Run the policy test and observe the missing function**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.map.MapFabMotionPolicyTest --quiet
```

Expected: FAIL because `auroraMotionEnabled` does not exist.

- [ ] **Step 3: Add cross-platform reduced-motion readers**

Create common `MotionPreferences.kt`:

```kotlin
package com.mapchina.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberReducedMotionEnabled(): Boolean
```

Create Android `MotionPreferences.android.kt`:

```kotlin
package com.mapchina.platform

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f
}
```

Create iOS `MotionPreferences.ios.kt`:

```kotlin
package com.mapchina.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReducedMotionEnabled(): Boolean =
    UIAccessibilityIsReduceMotionEnabled()
```

- [ ] **Step 4: Implement the 7000ms sweep-gradient aurora ring**

Add the policy and update `MapFab`:

```kotlin
internal fun auroraMotionEnabled(
    isExpanded: Boolean,
    reducedMotion: Boolean,
    isScreenActive: Boolean
): Boolean = !isExpanded && !reducedMotion && isScreenActive
```

Add these parameters to `MapFab` immediately before `mapTheme`:

```kotlin
reducedMotion: Boolean = false,
isScreenActive: Boolean = true,
```

Add the layer transition policy to `MapAnimation.kt`:

```kotlin
internal fun mapLayerTransitionDurationMillis(
    reducedMotion: Boolean
): Int = if (reducedMotion) 0 else 220

internal fun focusOverlayOpacity(
    isSelected: Boolean,
    hasFocus: Boolean,
    progress: Float
): Float {
    if (!hasFocus || isSelected) return 1f
    return 1f - progress.coerceIn(0f, 1f) * 0.75f
}
```

Create the ring wrapper in `MapFab.kt`:

```kotlin
@Composable
private fun AuroraMapDockButton(
    contentDescription: String,
    icon: ImageVector,
    size: Dp,
    surfaceColor: Color,
    iconColor: Color,
    motionEnabled: Boolean,
    onClick: () -> Unit
) {
    val rotation = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "auroraRing")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "auroraRotation"
        ).value
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .size(size)
            .drawWithCache {
                val ringWidth = 2.5.dp.toPx()
                val glowWidth = 7.dp.toPx()
                val aurora = Brush.sweepGradient(
                    listOf(
                        Color(0xFF64F3CB),
                        Color(0xFF2AA6D6),
                        Color(0xFF9B8CFF),
                        Color(0xFF64F3CB)
                    )
                )
                onDrawBehind {
                    rotate(rotation) {
                        drawCircle(
                            brush = aurora,
                            style = Stroke(width = glowWidth),
                            alpha = 0.14f
                        )
                        drawCircle(
                            brush = aurora,
                            style = Stroke(width = ringWidth)
                        )
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
    ) {
        Surface(
            shape = CircleShape,
            color = surfaceColor,
            shadowElevation = 5.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
```

Use `AuroraMapDockButton` for the main `"地图工具"` button only. Keep the location button understated. Pass:

```kotlin
motionEnabled = auroraMotionEnabled(
    isExpanded = isExpanded,
    reducedMotion = reducedMotion,
    isScreenActive = isScreenActive
)
```

When `MapFab` is not composed, its transition is disposed. When expanded or reduced-motion is active, the branch returns static rotation `0f`.

- [ ] **Step 5: Pause motion with lifecycle and fade in new active layers**

Read the preference and lifecycle near the top of `MapScreen`:

```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

val reducedMotion = rememberReducedMotionEnabled()
val lifecycleOwner = LocalLifecycleOwner.current
val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
val isScreenActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
```

Change `ChinaMapView` to accept:

```kotlin
@Composable
fun ChinaMapView(
    controller: MapController,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
)
```

Fade only the current active layer when its IDs change:

```kotlin
val activeLayerKey = remember(renderState.overlays) {
    renderState.overlays
        .filterValues { it.role == OverlayRole.ACTIVE }
        .keys
        .sorted()
        .joinToString("|")
}
val activeLayerAlpha = remember { Animatable(1f) }
LaunchedEffect(activeLayerKey, reducedMotion) {
    val duration = mapLayerTransitionDurationMillis(reducedMotion)
    if (duration == 0) {
        activeLayerAlpha.snapTo(1f)
    } else {
        activeLayerAlpha.snapTo(0f)
        activeLayerAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration)
        )
    }
}
val selectionDimProgress by animateFloatAsState(
    targetValue = if (renderState.pulseTarget == null) 0f else 1f,
    animationSpec = tween(if (reducedMotion) 0 else 180),
    label = "selectionDim"
)
```

In the region draw loop, calculate:

```kotlin
val selectionOpacity = focusOverlayOpacity(
    isSelected = regionId == renderState.pulseTarget,
    hasFocus = renderState.pulseTarget != null &&
        data.role == OverlayRole.ACTIVE,
    progress = selectionDimProgress
)
val opacity = (
    data.opacityMultiplier *
        selectionOpacity *
        if (data.role == OverlayRole.ACTIVE) activeLayerAlpha.value else 1f
).coerceIn(0f, 1f)
```

Pass lifecycle and reduced-motion state to the map and FAB:

```kotlin
ChinaMapView(
    controller = mapController,
    modifier = Modifier.fillMaxSize(),
    reducedMotion = reducedMotion
)

MapFab(
    coveragePercent = coveragePercent,
    photoMarkersVisible = photoMarkersVisible,
    isExpanded = fabExpanded,
    onExpandedChange = { fabExpanded = it },
    onTogglePhotos = { viewModel.togglePhotoMarkers() },
    reducedMotion = reducedMotion,
    isScreenActive = isScreenActive,
    onShare = { viewModel.enterShareMode() },
    onDepart = { showDartTravel = true },
    onNavigateToNational = if (currentLevel != MapZoomLevel.NATIONAL) {
        { viewModel.navigateToNational() }
    } else {
        null
    },
    onMyLocation = { viewModel.moveToCurrentLocation() },
    firstFootprintActivation = firstFootprintActivation,
    mapSelectionActive = mapSelectionActive,
    onChooseMap = { mapSelectionActive = true },
    onSearchAttraction = {
        mapSelectionActive = false
        onNavigate(AttractionsScreen(autoFocusSearch = true))
    },
    onUseCurrentLocation = {
        mapSelectionActive = false
        viewModel.activateCurrentLocation()
    },
    mapTheme = currentMapTheme,
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .zIndex(1f)
        .padding(end = 16.dp, bottom = bottomBarOffset + 16.dp)
)
```

Use the same `reducedMotion` value for `focusRegion`.

- [ ] **Step 6: Run common policy, Android UI, and iOS compile tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.map.MapFabMotionPolicyTest :androidApp:testDebugUnitTest --tests com.mapchina.ui.map.MapFabTest :shared:compileKotlinIosSimulatorArm64 --quiet
```

Expected: PASS; iOS actual motion preference compiles.

- [ ] **Step 7: Commit the aurora FAB**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/platform/MotionPreferences.kt shared/src/androidMain/kotlin/com/mapchina/platform/MotionPreferences.android.kt shared/src/iosMain/kotlin/com/mapchina/platform/MotionPreferences.ios.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/MapFabMotionPolicyTest.kt androidApp/src/test/kotlin/com/mapchina/ui/map/MapFabTest.kt
git commit -m "feat(map): add reduced-motion aurora fab"
```

---

### Task 7: Full Regression and Cross-Platform Runtime Proof

**Files:**

- Create: `docs/qa/home-map-context-focus.md`
- Verify: `/tmp/mapchina-home-context-focus-qa/android-before.png`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/android-national-after.png`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/android-focused-after.png`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/android-drilldown-after.png`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/android-interaction.mp4`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/ios-national-after.png`
- Create runtime artifact: `/tmp/mapchina-home-context-focus-qa/ios-focused-after.png`

**Interfaces:**

- Verifies all previous tasks as one user-visible flow.
- Produces committed QA record `docs/qa/home-map-context-focus.md`.

- [ ] **Step 1: Run the complete automated suite and platform compiles**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:allTests :androidApp:testDebugUnitTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --quiet
```

Expected: BUILD SUCCESSFUL with no failed tests.

- [ ] **Step 2: Install and relaunch the Android app**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell am force-stop com.mapchina.android
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell screencap -p /sdcard/home-national-after.png
adb pull /sdcard/home-national-after.png /tmp/mapchina-home-context-focus-qa/android-national-after.png
```

Expected: the national home screen shows “中国足迹”, the morning-jade background, lightweight HUD, and aurora map-tool button.

- [ ] **Step 3: Record real focus, rapid-tap, drill-down, and return interaction**

Start this command asynchronously:

```bash
adb shell screenrecord --time-limit 35 /sdcard/home-context-focus.mp4
```

While it records, run:

```bash
adb shell input tap 430 920
adb shell input tap 865 1180
adb shell input tap 430 920
adb shell input tap 640 2050
adb shell input swipe 620 1300 760 1150 350
adb shell input keyevent 4
```

Expected observations:

- Each tap immediately highlights a region.
- The latest tap wins when two taps are close together.
- The selected region reaches the unobstructed map center before its card appears.
- Drilling down keeps faint national context visible and never presents a blank map.
- Back returns to the previous layer without a context-layer flash.

Pull the recording and focused screenshots:

```bash
adb shell screencap -p /sdcard/home-focused-after.png
adb pull /sdcard/home-focused-after.png /tmp/mapchina-home-context-focus-qa/android-focused-after.png
adb shell input tap 640 2050
adb shell screencap -p /sdcard/home-drilldown-after.png
adb pull /sdcard/home-drilldown-after.png /tmp/mapchina-home-context-focus-qa/android-drilldown-after.png
adb pull /sdcard/home-context-focus.mp4 /tmp/mapchina-home-context-focus-qa/android-interaction.mp4
```

If `screenrecord` is unsupported, take screenshots after each ADB input and record that limitation in the QA document.

- [ ] **Step 4: Inspect Android artifacts before claiming success**

Open and inspect:

```text
/tmp/mapchina-home-context-focus-qa/android-before.png
/tmp/mapchina-home-context-focus-qa/android-national-after.png
/tmp/mapchina-home-context-focus-qa/android-focused-after.png
/tmp/mapchina-home-context-focus-qa/android-drilldown-after.png
/tmp/mapchina-home-context-focus-qa/android-interaction.mp4
```

Expected: visual evidence matches the observations in Step 3. If a coordinate tapped the wrong region, repeat the capture with coordinates chosen from the latest screenshot; do not weaken the acceptance criteria.

- [ ] **Step 5: Build, boot, install, and launch iOS**

```bash
xcrun simctl bootstatus "iPhone 17 Pro" -b
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' -derivedDataPath /tmp/MapChinaDerivedData build
xcrun simctl install booted /tmp/MapChinaDerivedData/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted com.mapchina.iosApp
xcrun simctl io booted screenshot /tmp/mapchina-home-context-focus-qa/ios-national-after.png
```

Expected: build succeeds and the iPhone 17 Pro Simulator shows the same morning-jade home map.

- [ ] **Step 6: Verify iOS interaction through the Simulator UI**

Use the `computer-use` skill to:

1. Click a visible province.
2. Confirm the map centers and zooms before the card appears.
3. Click the card’s child-map action.
4. Confirm national context remains visible behind child boundaries.
5. Expand the map tools and confirm the aurora ring stops rotating.
6. Return to the national map.

Then capture:

```bash
xcrun simctl io booted screenshot /tmp/mapchina-home-context-focus-qa/ios-focused-after.png
```

Expected: the focused state is visible and no blank canvas or stuck card occurs.

- [ ] **Step 7: Write the completed QA record**

Create `docs/qa/home-map-context-focus.md` only after all checks pass:

```markdown
# Home Map Context Focus QA

Date: 2026-07-19

## Automated

- PASS: shared allTests
- PASS: Android Robolectric UI tests
- PASS: Android shared compilation
- PASS: iOS Simulator shared compilation

## Android Pixel 9 Pro

- PASS: morning-jade national home
- PASS: single-tap 650ms safe-area focus
- PASS: latest rapid tap wins
- PASS: region card appears after camera completion
- PASS: child layer retains national context
- PASS: back navigation restores the previous layer
- PASS: aurora FAB rotates collapsed and pauses expanded
- PASS: no blank canvas during child-layer load

Artifacts: `/tmp/mapchina-home-context-focus-qa/android-before.png`, `/tmp/mapchina-home-context-focus-qa/android-national-after.png`, `/tmp/mapchina-home-context-focus-qa/android-focused-after.png`, `/tmp/mapchina-home-context-focus-qa/android-drilldown-after.png`, `/tmp/mapchina-home-context-focus-qa/android-interaction.mp4`

## iPhone 17 Pro Simulator

- PASS: shared morning-jade layout and safe areas
- PASS: focus/card ordering
- PASS: persistent national context during drill-down
- PASS: aurora FAB pause behavior
- PASS: return-to-national flow

Artifacts: `/tmp/mapchina-home-context-focus-qa/ios-national-after.png`, `/tmp/mapchina-home-context-focus-qa/ios-focused-after.png`
```

- [ ] **Step 8: Verify the final diff and commit the QA record**

```bash
git diff --check
git status --short
git add docs/qa/home-map-context-focus.md
git commit -m "test(map): record contextual focus qa"
```

Expected: the unrelated `docs/superpowers/plans/2026-06-14-haptic-feedback.md` remains untracked; no other unexpected files are staged.

## Final Completion Gate

- [ ] Confirm `git log --oneline -8` shows one focused commit per task.
- [ ] Confirm `git diff --check` is clean.
- [ ] Confirm all automated commands in Task 7 pass from the final source state.
- [ ] Confirm Android before/after screenshots and interaction recording are visually inspected.
- [ ] Confirm iOS national/focused screenshots and interactive flow are visually inspected.
- [ ] Confirm no requirement in `docs/superpowers/specs/2026-07-19-home-map-context-focus-design.md` is unimplemented.
