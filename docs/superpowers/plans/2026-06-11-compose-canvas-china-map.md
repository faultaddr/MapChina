# Compose Canvas 中国地图组件 实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Compose Canvas 替换高德地图 SDK，实现跨平台自绘中国地图组件。

**Architecture:** 所有逻辑在 commonMain，MapController 从 expect/actual 改为普通 class，Canvas 直接绘制 GeoJSON 边界多边形，手势期间用 graphicsLayer 仿射变换避免 Path 重建。

**Tech Stack:** Kotlin Multiplatform, Compose Canvas, kotlinx-serialization-json, Coil 3

**Spec:** `docs/superpowers/specs/2026-06-11-compose-canvas-china-map-design.md`

---

## File Structure

### New files (commonMain)

| File | Responsibility |
|------|---------------|
| `shared/src/commonMain/kotlin/com/mapchina/map/GeoProjection.kt` | Web Mercator 投影：经纬度 ↔ 屏幕坐标 |
| `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt` | 视口状态：中心、缩放、scale 派生、moveTo/panBy/zoomBy |
| `shared/src/commonMain/kotlin/com/mapchina/map/GeoPathCache.kt` | 坐标数据 → Compose Path 构建 & 缓存 & 视口裁剪 |
| `shared/src/commonMain/kotlin/com/mapchina/map/DouglasPeucker.kt` | Douglas-Peucker 多边形简化 |
| `shared/src/commonMain/kotlin/com/mapchina/map/HitTester.kt` | 屏幕坐标 → 区域 ID 射线法检测 |
| `shared/src/commonMain/kotlin/com/mapchina/map/BoundaryParser.kt` | GeoJSON Feature/FeatureCollection → 坐标列表 |
| `shared/src/commonMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt` | expect class：从 assets 读取边界 JSON |
| `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt` | 渲染状态数据类：OverlayData, MarkerData 等 |
| `shared/src/commonMain/kotlin/com/mapchina/map/MapGestureHandler.kt` | 手势 Modifier：平移、缩放、点击、双击 |
| `shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt` | 南海诸岛插图常量 & 绘制 |
| `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt` | 顶层 Composable：Canvas 绘制入口 |
| `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt` | 脉冲 & 相机动画 |

### Modified files (commonMain)

| File | Change |
|------|--------|
| `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt` | 从 `expect class` 改为普通 `class`，全部实现在此 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/map/PlatformMapView.kt` | 删除 expect 函数，改为直接调用 ChinaMapView |
| `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt` | 改用 ChinaMapView 替代 PlatformMapView |

### Deleted files

| File | Reason |
|------|--------|
| `shared/src/androidMain/kotlin/com/mapchina/map/AndroidMapController.kt` | MapController 改为 commonMain 实现 |
| `shared/src/iosMain/kotlin/com/mapchina/map/IosMapController.kt` | 同上 |
| `shared/src/androidMain/kotlin/com/mapchina/ui/map/PlatformMapView.android.kt` | 不再需要 AndroidView 互操作 |
| `shared/src/iosMain/kotlin/com/mapchina/ui/map/PlatformMapView.ios.kt` | 不再需要 UIKit 互操作 |
| `shared/src/androidMain/kotlin/com/mapchina/map/AttractionMarkerRenderer.kt` | 高德 Marker 渲染器 |
| `shared/src/androidMain/kotlin/com/mapchina/map/PhotoMarkerRenderer.kt` | 高德照片 Marker 渲染器 |

### New files (platform actual)

| File | Responsibility |
|------|---------------|
| `shared/src/androidMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt` | Android assets 文件读取 |
| `shared/src/iosMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt` | iOS NSBundle 文件读取 |

### New test files

| File | Responsibility |
|------|---------------|
| `shared/src/commonTest/kotlin/com/mapchina/map/GeoProjectionTest.kt` | 投影正反算测试 |
| `shared/src/commonTest/kotlin/com/mapchina/map/DouglasPeuckerTest.kt` | 简化算法测试 |
| `shared/src/commonTest/kotlin/com/mapchina/map/HitTesterTest.kt` | 点击检测测试 |
| `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt` | 视口状态测试 |
| `shared/src/commonTest/kotlin/com/mapchina/map/BoundaryParserTest.kt` | 边界解析测试 |

---

## Chunk 1: Core Geometry & Projection

### Task 1: GeoProjection

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/GeoProjection.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/GeoProjectionTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.mapchina.map

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class GeoProjectionTest {

    @Test
    fun `project center returns canvas center`() {
        val proj = GeoProjection(
            viewCenterLng = 104.0,
            viewCenterLat = 35.5,
            scale = 10f,
            canvasWidth = 400f,
            canvasHeight = 800f
        )
        val result = proj.project(104.0, 35.5)
        assertEquals(200f, result.x, 0.1f)
        assertEquals(400f, result.y, 0.1f)
    }

    @Test
    fun `project offset east moves right`() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val result = proj.project(105.0, 35.5)
        assert(result.x > 200f)
    }

    @Test
    fun `project offset north moves up`() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val result = proj.project(104.0, 36.5)
        assert(result.y < 400f)
    }

    @Test
    fun `unproject roundtrip at center`() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val (lng, lat) = proj.unproject(200f, 400f)
        assertEquals(104.0, lng, 0.001)
        assertEquals(35.5, lat, 0.001)
    }

    @Test
    fun `unproject roundtrip at offset`() {
        val proj = GeoProjection(104.0, 35.5, 10f, 400f, 800f)
        val (lng, lat) = proj.unproject(300f, 300f)
        val (x, y) = proj.project(lng, lat)
        assertEquals(300f, x, 0.5f)
        assertEquals(300f, y, 0.5f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.GeoProjectionTest" 2>&1 | tail -5`
Expected: FAIL — `GeoProjection` not found

- [ ] **Step 3: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan
import kotlin.math.toDegrees
import kotlin.math.toRadians

data class GeoProjection(
    val viewCenterLng: Double,
    val viewCenterLat: Double,
    val scale: Float,
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    private val centerMercY: Double = ln(tan(PI / 4 + toRadians(viewCenterLat) / 2))

    fun project(lng: Double, lat: Double): Offset {
        val x = (lng - viewCenterLng).toFloat() * scale + canvasWidth / 2
        val mercY = ln(tan(PI / 4 + toRadians(lat) / 2))
        val y = -(mercY - centerMercY).toFloat() * scale + canvasHeight / 2
        return Offset(x, y)
    }

    fun unproject(x: Float, y: Float): Pair<Double, Double> {
        val lng = (x - canvasWidth / 2) / scale.toDouble() + viewCenterLng
        val mercY = -((y - canvasHeight / 2) / scale).toDouble() + centerMercY
        val lat = toDegrees(2 * atan(exp(mercY)) - PI / 2)
        return lng to lat
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.GeoProjectionTest" 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/GeoProjection.kt shared/src/commonTest/kotlin/com/mapchina/map/GeoProjectionTest.kt
git commit -m "feat: add GeoProjection with Web Mercator project/unproject"
```

---

### Task 2: ViewportState

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewportStateTest {

    @Test
    fun `default center is china center`() {
        val vp = ViewportState()
        assertEquals(104.0, vp.centerLng)
        assertEquals(35.5, vp.centerLat)
        assertEquals(3.5f, vp.zoomLevel)
    }

    @Test
    fun `panBy updates center`() {
        val vp = ViewportState(scale = 10f, canvasWidth = 400f, canvasHeight = 800f)
        vp.panBy(androidx.compose.ui.geometry.Offset(10f, 0f))
        // pan right 10px at scale=10 → lng increases by 1 degree
        assertTrue(vp.centerLng > 104.0)
    }

    @Test
    fun `zoomBy increases zoom level`() {
        val vp = ViewportState()
        vp.zoomBy(1f, androidx.compose.ui.geometry.Offset(200f, 400f))
        assertTrue(vp.zoomLevel > 3.5f)
    }

    @Test
    fun `zoom level clamped to valid range`() {
        val vp = ViewportState()
        vp.zoomBy(-100f, androidx.compose.ui.geometry.Offset(200f, 400f))
        assertEquals(3f, vp.zoomLevel)
        vp.zoomBy(100f, androidx.compose.ui.geometry.Offset(200f, 400f))
        assertEquals(15f, vp.zoomLevel)
    }

    @Test
    fun `toProjection returns valid projection`() {
        val vp = ViewportState()
        val proj = vp.toProjection(400f, 800f)
        assertEquals(104.0, proj.viewCenterLng)
        assertEquals(35.5, proj.viewCenterLat)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.ViewportStateTest" 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.pow

class ViewportState(
    initialCenterLng: Double = 104.0,
    initialCenterLat: Double = 35.5,
    initialZoomLevel: Float = 3.5f,
    internal var scale: Float = 2.0f,
    internal var canvasWidth: Float = 400f,
    internal var canvasHeight: Float = 800f
) {
    companion object {
        const val BASE_ZOOM = 3.5f
        const val BASE_SCALE = 2.0f
        const val MIN_ZOOM = 3f
        const val MAX_ZOOM = 15f
    }

    var centerLng by mutableStateOf(initialCenterLng)
    var centerLat by mutableStateOf(initialCenterLat)
    var zoomLevel by mutableFloatStateOf(initialZoomLevel)

    val derivedScale: Float get() = BASE_SCALE * 2f.pow(zoomLevel - BASE_ZOOM)

    fun panBy(delta: Offset) {
        val s = derivedScale
        centerLng -= delta.x / s
        val mercShift = -delta.y / s
        val currentMerc = ln(tan(kotlin.math.PI / 4 + kotlin.math.toRadians(centerLat) / 2))
        val newMerc = currentMerc + mercShift
        centerLat = kotlin.math.toDegrees(2 * kotlin.math.atan(kotlin.math.exp(newMerc)) - kotlin.math.PI / 2)
            .coerceIn(-85.0, 85.0)
    }

    fun zoomBy(delta: Float, pivot: Offset) {
        val oldZoom = zoomLevel
        zoomLevel = (zoomLevel + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val zoomRatio = 2f.pow(zoomLevel - oldZoom)
        val s = derivedScale
        val pivotLng = centerLng + (pivot.x - canvasWidth / 2) / s
        val pivotMerc = ln(tan(kotlin.math.PI / 4 + kotlin.math.toRadians(centerLat) / 2)) -
            (pivot.y - canvasHeight / 2) / s
        val pivotLat = kotlin.math.toDegrees(2 * kotlin.math.atan(kotlin.math.exp(pivotMerc)) - kotlin.math.PI / 2)
        centerLng = pivotLng - (pivot.x - canvasWidth / 2) / derivedScale
        val newPivotMerc = ln(tan(kotlin.math.PI / 4 + kotlin.math.toRadians(pivotLat) / 2))
        centerLat = kotlin.math.toDegrees(
            2 * kotlin.math.atan(kotlin.math.exp(newPivotMerc + (pivot.y - canvasHeight / 2) / derivedScale)) - kotlin.math.PI / 2
        ).coerceIn(-85.0, 85.0)
    }

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean = false) {
        centerLng = lng
        centerLat = lat.coerceIn(-85.0, 85.0)
        zoomLevel = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun toProjection(width: Float, height: Float): GeoProjection {
        canvasWidth = width
        canvasHeight = height
        return GeoProjection(
            viewCenterLng = centerLng,
            viewCenterLat = centerLat,
            scale = derivedScale,
            canvasWidth = width,
            canvasHeight = height
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.ViewportStateTest" 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt
git commit -m "feat: add ViewportState with pan/zoom/moveTo"
```

---

### Task 3: DouglasPeucker

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/DouglasPeucker.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/DouglasPeuckerTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DouglasPeuckerTest {

    @Test
    fun `simplify preserves endpoints`() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 0.0, 3.0 to 1.0, 4.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun `simplify with zero epsilon returns original`() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.0)
        assertEquals(points.size, result.size)
    }

    @Test
    fun `simplify removes collinear points`() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 2.0, 3.0 to 3.0)
        val result = DouglasPeucker.simplify(points, 0.1)
        assertEquals(2, result.size)
    }

    @Test
    fun `simplify keeps points far from line`() {
        val points = listOf(0.0 to 0.0, 1.0 to 5.0, 2.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 1.0)
        assertEquals(3, result.size)
    }

    @Test
    fun `simplify handles two points`() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(2, result.size)
    }

    @Test
    fun `simplify handles single point`() {
        val points = listOf(0.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(1, result.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.DouglasPeuckerTest" 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package com.mapchina.map

import kotlin.math.abs

object DouglasPeucker {

    fun simplify(points: List<Pair<Double, Double>>, epsilon: Double): List<Pair<Double, Double>> {
        if (points.size <= 2) return points.toList()
        if (epsilon <= 0.0) return points.toList()

        var maxDist = 0.0
        var maxIndex = 0

        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = simplify(points.subList(0, maxIndex + 1), epsilon)
            val right = simplify(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(
        point: Pair<Double, Double>,
        lineStart: Pair<Double, Double>,
        lineEnd: Pair<Double, Double>
    ): Double {
        val dx = lineEnd.first - lineStart.first
        val dy = lineEnd.second - lineStart.second
        if (dx == 0.0 && dy == 0.0) {
            val ddx = point.first - lineStart.first
            val ddy = point.second - lineStart.second
            return kotlin.math.sqrt(ddx * ddx + ddy * ddy)
        }
        val numerator = abs(dy * point.first - dx * point.second + lineEnd.first * lineStart.second - lineEnd.second * lineStart.first)
        val denominator = kotlin.math.sqrt(dx * dx + dy * dy)
        return numerator / denominator
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.DouglasPeuckerTest" 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/DouglasPeucker.kt shared/src/commonTest/kotlin/com/mapchina/map/DouglasPeuckerTest.kt
git commit -m "feat: add DouglasPeucker polygon simplification"
```

---

### Task 4: HitTester

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/HitTester.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/HitTesterTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HitTesterTest {

    private val squareCoords = listOf(
        listOf(
            0.0 to 0.0, 10.0 to 0.0, 10.0 to 10.0, 0.0 to 10.0, 0.0 to 0.0
        )
    )
    private val squareBounds = Rect(0f, 0f, 10f, 10f)

    @Test
    fun `hit inside square returns region id`() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        assertEquals("region_a", tester.hitTest(5f, 5f))
    }

    @Test
    fun `hit outside square returns null`() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        assertNull(tester.hitTest(15f, 15f))
    }

    @Test
    fun `hit on edge returns null or id`() {
        val tester = HitTester(
            bounds = mapOf("region_a" to squareBounds),
            coords = mapOf("region_a" to squareCoords)
        )
        // On edge — behavior is implementation-defined, just shouldn't crash
        tester.hitTest(10f, 5f)
    }

    @Test
    fun `multiple regions returns topmost hit`() {
        val smallSquare = listOf(listOf(2.0 to 2.0, 8.0 to 2.0, 8.0 to 8.0, 2.0 to 8.0, 2.0 to 2.0))
        val tester = HitTester(
            bounds = mapOf("big" to squareBounds, "small" to Rect(2f, 2f, 8f, 8f)),
            coords = mapOf("big" to squareCoords, "small" to smallSquare)
        )
        val result = tester.hitTest(5f, 5f)
        // Both contain the point; return one of them
        assertEquals(true, result == "big" || result == "small")
    }

    @Test
    fun `empty bounds returns null`() {
        val tester = HitTester(bounds = emptyMap(), coords = emptyMap())
        assertNull(tester.hitTest(5f, 5f))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.HitTesterTest" 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Rect

class HitTester(
    private val bounds: Map<String, Rect>,
    private val coords: Map<String, List<List<Pair<Double, Double>>>>
) {
    fun hitTest(x: Float, y: Float): String? {
        val candidates = bounds.filter { (_, rect) ->
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        }
        for ((id, _) in candidates) {
            val rings = coords[id] ?: continue
            for (ring in rings) {
                if (pointInPolygon(x.toDouble(), y.toDouble(), ring)) {
                    return id
                }
            }
        }
        return null
    }

    private fun pointInPolygon(x: Double, y: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var inside = false
        var i = 0
        var j = polygon.size - 1
        while (i < polygon.size) {
            val xi = polygon[i].first; val yi = polygon[i].second
            val xj = polygon[j].first; val yj = polygon[j].second
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i++
        }
        return inside
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.HitTesterTest" 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/HitTester.kt shared/src/commonTest/kotlin/com/mapchina/map/HitTesterTest.kt
git commit -m "feat: add HitTester with ray casting point-in-polygon"
```

---

### Task 5: BoundaryParser

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/BoundaryParser.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/BoundaryParserTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoundaryParserTest {

    @Test
    fun `parse Feature type with Polygon`() {
        val json = """{"type":"Feature","properties":{"adcode":110000,"name":"北京市","center":[116.405,39.905]},"geometry":{"type":"Polygon","coordinates":[[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]]}}"""
        val result = BoundaryParser.parse(json)
        assertEquals(1, result.size)
        assertEquals(5, result[0].size)
        assertEquals(116.0, result[0][0].first)
        assertEquals(39.0, result[0][0].second)
    }

    @Test
    fun `parse FeatureCollection type`() {
        val json = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"adcode":110101,"name":"东城区"},"geometry":{"type":"Polygon","coordinates":[[[116.4,39.9],[116.5,39.9],[116.5,40.0],[116.4,40.0],[116.4,39.9]]]}}]}"""
        val result = BoundaryParser.parse(json)
        assertEquals(1, result.size)
    }

    @Test
    fun `parse MultiPolygon geometry`() {
        val json = """{"type":"Feature","properties":{"adcode":810000,"name":"香港"},"geometry":{"type":"MultiPolygon","coordinates":[[[[114.0,22.0],[114.1,22.0],[114.1,22.1],[114.0,22.1],[114.0,22.0]]],[[[114.2,22.2],[114.3,22.2],[114.3,22.3],[114.2,22.3],[114.2,22.2]]]]}}"""
        val result = BoundaryParser.parse(json)
        assertEquals(2, result.size)
    }

    @Test
    fun `parse flat coordinate array format`() {
        // Existing boundary_json format: [[lng,lat],[lng,lat],...]
        val json = """[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]"""
        val result = BoundaryParser.parseFlatCoords(json)
        assertEquals(1, result.size)
        assertEquals(5, result[0].size)
    }

    @Test
    fun `parse empty json returns empty list`() {
        val json = """{"type":"Unknown"}"""
        val result = BoundaryParser.parse(json)
        assertTrue(result.isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.BoundaryParserTest" 2>&1 | tail -5`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package com.mapchina.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BoundaryParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): List<List<Pair<Double, Double>>> {
        val element = json.parseToJsonElement(rawJson)
        if (element !is JsonObject) return emptyList()
        val root = element.jsonObject
        return when (root["type"]?.jsonPrimitive?.content) {
            "Feature" -> parseFeatureGeometry(root)
            "FeatureCollection" -> {
                val features = root["features"]?.jsonArray ?: return emptyList()
                if (features.isEmpty()) return emptyList()
                parseFeatureGeometry(features[0].jsonObject)
            }
            else -> emptyList()
        }
    }

    fun parseFlatCoords(boundaryJson: String): List<List<Pair<Double, Double>>> {
        val element = json.parseToJsonElement(boundaryJson)
        if (element !is JsonArray) return emptyList()
        val coords = mutableListOf<Pair<Double, Double>>()
        for (item in element.jsonArray) {
            val arr = item.jsonArray
            if (arr.size >= 2) {
                coords.add(arr[0].jsonPrimitive.double to arr[1].jsonPrimitive.double)
            }
        }
        return if (coords.isNotEmpty()) listOf(coords) else emptyList()
    }

    private fun parseFeatureGeometry(feature: JsonObject): List<List<Pair<Double, Double>>> {
        val geom = feature["geometry"]?.jsonObject ?: return emptyList()
        return when (geom["type"]?.jsonPrimitive?.content) {
            "Polygon" -> parsePolygonRings(geom["coordinates"]?.jsonArray ?: return emptyList())
            "MultiPolygon" -> parseMultiPolygonRings(geom["coordinates"]?.jsonArray ?: return emptyList())
            else -> emptyList()
        }
    }

    private fun parsePolygonRings(coordinates: JsonArray): List<List<Pair<Double, Double>>> {
        val rings = mutableListOf<List<Pair<Double, Double>>>()
        for (ring in coordinates) {
            rings.add(parseCoordinateRing(ring.jsonArray))
        }
        return rings
    }

    private fun parseMultiPolygonRings(coordinates: JsonArray): List<List<Pair<Double, Double>>> {
        val allRings = mutableListOf<List<Pair<Double, Double>>>()
        for (polygon in coordinates) {
            for (ring in polygon.jsonArray) {
                allRings.add(parseCoordinateRing(ring.jsonArray))
            }
        }
        return allRings
    }

    private fun parseCoordinateRing(ring: JsonArray): List<Pair<Double, Double>> {
        return ring.map { coord ->
            val arr = coord.jsonArray
            arr[0].jsonPrimitive.double to arr[1].jsonPrimitive.double
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest --tests "com.mapchina.map.BoundaryParserTest" 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/BoundaryParser.kt shared/src/commonTest/kotlin/com/mapchina/map/BoundaryParserTest.kt
git commit -m "feat: add BoundaryParser for Feature/FeatureCollection/flat coords"
```

---

## Chunk 2: Rendering Layer

### Task 6: RenderState & Data Classes

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt`

- [ ] **Step 1: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.graphics.Color

data class RenderState(
    val overlays: Map<String, OverlayData> = emptyMap(),
    val markers: Map<String, MarkerData> = emptyMap(),
    val attractionMarkers: Map<String, AttractionMarkerData> = emptyMap(),
    val imageMarkers: Map<String, ImageMarkerData> = emptyMap(),
    val polylines: Map<String, PolylineData> = emptyMap(),
    val pulseTarget: String? = null
)

data class OverlayData(
    val coords: List<List<Pair<Double, Double>>>,
    val style: OverlayStyle
)

data class MarkerData(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val visited: Boolean
)

data class AttractionMarkerData(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val imageUrl: String?,
    val visited: Boolean
)

data class ImageMarkerData(
    val id: String,
    val lat: Double,
    val lng: Double,
    val imagePath: String,
    val count: Int
)

data class PolylineData(
    val id: String,
    val points: List<Pair<Double, Double>>,
    val color: Long,
    val width: Float
)

fun OverlayStyle.toFillColor(): Color {
    val a = ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
    val rgb = (fillColor and 0xFFFFFF).toInt()
    return Color(a or rgb)
}

fun OverlayStyle.toStrokeColor(): Color {
    val a = 0xFF000000.toInt()
    val rgb = (strokeColor and 0xFFFFFF).toInt()
    return Color(a or rgb)
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/RenderState.kt
git commit -m "feat: add RenderState and overlay/marker/polyline data classes"
```

---

### Task 7: GeoPathCache

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/GeoPathCache.kt`

- [ ] **Step 1: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType

class GeoPathCache {
    private var cachedPaths: Map<String, List<Path>> = emptyMap()
    private var cachedBounds: Map<String, Rect> = emptyMap()
    private var lastProjection: GeoProjection? = null
    private var lastOverlayKeys: Set<String>? = null

    val paths: Map<String, List<Path>> get() = cachedPaths
    val bounds: Map<String, Rect> get() = cachedBounds

    fun buildIfChanged(
        overlays: Map<String, OverlayData>,
        projection: GeoProjection,
        zoomLevel: Float
    ): GeoPathCache {
        val currentKeys = overlays.keys
        if (projection == lastProjection && currentKeys == lastOverlayKeys) return this

        val epsilon = when {
            zoomLevel < 6 -> 0.05
            zoomLevel < 10 -> 0.01
            else -> 0.0
        }

        val newPaths = mutableMapOf<String, List<Path>>()
        val newBounds = mutableMapOf<String, Rect>()

        for ((id, data) in overlays) {
            val paths = mutableListOf<Path>()
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (ring in data.coords) {
                val simplified = if (epsilon > 0) DouglasPeucker.simplify(ring, epsilon) else ring
                val path = Path()
                path.fillType = PathFillType.EvenOdd
                for ((i, point) in simplified.withIndex()) {
                    val offset = projection.project(point.first, point.second)
                    if (offset.x < minX) minX = offset.x
                    if (offset.y < minY) minY = offset.y
                    if (offset.x > maxX) maxX = offset.x
                    if (offset.y > maxY) maxY = offset.y
                    if (i == 0) path.moveTo(offset.x, offset.y)
                    else path.lineTo(offset.x, offset.y)
                }
                path.close()
                paths.add(path)
            }

            newPaths[id] = paths
            if (minX < Float.MAX_VALUE) {
                newBounds[id] = Rect(minX, minY, maxX, maxY)
            }
        }

        cachedPaths = newPaths
        cachedBounds = newBounds
        lastProjection = projection
        lastOverlayKeys = currentKeys
        return this
    }

    fun getVisibleRegions(viewport: Rect): List<String> {
        return cachedBounds.filter { (_, rect) ->
            rect.overlaps(viewport)
        }.keys.toList()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/GeoPathCache.kt
git commit -m "feat: add GeoPathCache with Douglas-Peucker and viewport culling"
```

---

### Task 8: MapGestureHandler

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/MapGestureHandler.kt`

- [ ] **Step 1: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.mapGestures(
    viewport: ViewportState,
    onTap: (Offset) -> Unit,
    onLongPress: ((Offset) -> Unit)? = null
): Modifier = this
    .pointerInput(viewport) {
        detectTransformGestures { centroid, pan, zoom ->
            if (pan != Offset.Zero) viewport.panBy(pan)
            if (zoom != 1f) viewport.zoomBy(zoom - 1f, centroid)
        }
    }
    .pointerInput(viewport) {
        detectTapGestures(
            onDoubleTap = { offset -> viewport.zoomBy(1f, offset) },
            onTap = onTap,
            onLongPress = onLongPress
        )
    }
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapGestureHandler.kt
git commit -m "feat: add mapGestures Modifier with pan/zoom/double-tap"
```

---

### Task 9: SouthChinaSea Inset

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt`

- [ ] **Step 1: Write implementation**

```kotlin
package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

object SouthChinaSea {
    val NINE_DASH_LINE = listOf(
        109.5 to 18.5, 111.0 to 16.5, 112.5 to 14.5, 114.0 to 12.0,
        115.5 to 10.5, 117.0 to 8.5, 117.5 to 7.0, 116.0 to 6.0,
        114.0 to 5.5, 112.0 to 5.0, 110.0 to 6.0, 109.0 to 8.0,
        108.5 to 11.0, 108.0 to 13.0, 108.5 to 15.0, 109.0 to 17.0,
        109.5 to 18.5
    )

    val ISLANDS = mapOf(
        "西沙" to (112.0 to 16.5),
        "南沙" to (114.0 to 10.0),
        "中沙" to (115.0 to 15.0),
        "东沙" to (117.0 to 20.5)
    )
}

fun DrawScope.drawSouthChinaSeaInset(
    projection: GeoProjection,
    zoomLevel: Float,
    strokeColor: Color,
    islandColor: Color
) {
    if (zoomLevel > 8f) return

    val insetWidth = 80.dp.toPx()
    val insetHeight = 100.dp.toPx()
    val margin = 12.dp.toPx()
    val insetLeft = size.width - insetWidth - margin
    val insetTop = size.height - insetHeight - margin

    val minLng = 107.0
    val maxLng = 118.0
    val minLat = 4.0
    val maxLat = 22.0
    val lngRange = maxLng - minLng
    val latRange = maxLat - minLat

    fun insetProject(lng: Double, lat: Double): Offset {
        val x = insetLeft + ((lng - minLng) / lngRange * insetWidth).toFloat()
        val y = insetTop + ((maxLat - lat) / latRange * insetHeight).toFloat()
        return Offset(x, y)
    }

    val borderPath = Path().apply {
        moveTo(insetLeft, insetTop)
        lineTo(insetLeft + insetWidth, insetTop)
        lineTo(insetLeft + insetWidth, insetTop + insetHeight)
        lineTo(insetLeft, insetTop + insetHeight)
        close()
    }
    drawPath(borderPath, color = Color.White.copy(alpha = 0.9f))
    drawPath(borderPath, color = strokeColor, style = Stroke(1.dp.toPx()))

    val dashPath = Path().apply {
        val points = SouthChinaSea.NINE_DASH_LINE.map { insetProject(it.first, it.second) }
        if (points.isNotEmpty()) {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
    }
    drawPath(dashPath, color = strokeColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

    for ((_, coords) in SouthChinaSea.ISLANDS) {
        val center = insetProject(coords.first, coords.second)
        drawCircle(islandColor, radius = 2.dp.toPx(), center = center)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/SouthChinaSea.kt
git commit -m "feat: add SouthChinaSea inset with nine-dash line and islands"
```

---

## Chunk 3: MapController Rewrite & Integration

### Task 10: Rewrite MapController as commonMain class

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/map/AndroidMapController.kt`
- Delete: `shared/src/iosMain/kotlin/com/mapchina/map/IosMapController.kt`

- [ ] **Step 1: Rewrite MapController**

Replace the existing `expect class MapController()` with a full commonMain implementation. The new class must implement all 27 methods from the current expect interface. Key internals:
- `viewport: ViewportState` for camera state
- `_renderState: MutableStateFlow<RenderState>` for render data
- `hitTester` for tap detection (rebuilt from renderState + viewport)
- `animationScope: CoroutineScope` for animations
- Callback listeners stored as fields

The `addOverlay` method parses `boundary` string using `BoundaryParser.parseFlatCoords(boundary)` (same format as current Android `parseBoundary`: flat `[[lng,lat],...]` array).

The `setCamera` method delegates to `viewport.moveTo()`.

The `pulseOverlay` / `restorePulsedOverlay` use `Animatable` with `infiniteRepeatable(tween(600))`.

The `toScreenLocation` method uses `viewport.toProjection()` + `project()`.

The `dispose` method cancels `animationScope`.

- [ ] **Step 2: Delete AndroidMapController.kt**

Remove `shared/src/androidMain/kotlin/com/mapchina/map/AndroidMapController.kt` entirely.

- [ ] **Step 3: Delete IosMapController.kt**

Remove `shared/src/iosMain/kotlin/com/mapchina/map/IosMapController.kt` entirely.

- [ ] **Step 4: Build to verify compilation**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:compileKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL (may need to fix downstream references first — see next task)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: rewrite MapController as commonMain class, remove platform actuals"
```

---

### Task 11: Replace PlatformMapView with ChinaMapView

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Delete: `shared/src/commonMain/kotlin/com/mapchina/ui/map/PlatformMapView.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/ui/map/PlatformMapView.android.kt`
- Delete: `shared/src/iosMain/kotlin/com/mapchina/ui/map/PlatformMapView.ios.kt`

- [ ] **Step 1: Create ChinaMapView Composable**

The `ChinaMapView` composable:
- Collects `renderState` from `controller.renderState`
- Uses `mapGestures` modifier for pan/zoom/tap
- Uses `Canvas` to draw layers L0-L6 per spec
- Maintains a `remember`ed `GeoPathCache` instance
- Calls `drawSouthChinaSeaInset` at the end
- Uses `graphicsLayer` for affine transform during gestures

- [ ] **Step 2: Update MapScreen to use ChinaMapView directly**

Replace `PlatformMapView(controller = mapController, modifier = ...)` with `ChinaMapView(controller = mapController, modifier = ...)`.

- [ ] **Step 3: Delete PlatformMapView files**

Remove:
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/PlatformMapView.kt`
- `shared/src/androidMain/kotlin/com/mapchina/ui/map/PlatformMapView.android.kt`
- `shared/src/iosMain/kotlin/com/mapchina/ui/map/PlatformMapView.ios.kt`

- [ ] **Step 4: Build to verify**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:compileKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add ChinaMapView, replace PlatformMapView"
```

---

### Task 12: MapAnimation — pulse & camera animations

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt`

- [ ] **Step 1: Write implementation**

Contains:
- `animatePulse(animatable: Animatable<Float, AnimationVector1D>, scope: CoroutineScope)` — starts the infinite pulse animation
- `animateCameraMove(viewport: ViewportState, targetLng: Double, targetLat: Double, targetZoom: Float, scope: CoroutineScope)` — parallel animation of center/zoom using `Animatable`

- [ ] **Step 2: Integrate into MapController**

Wire `pulseOverlay` → `animatePulse`, `restorePulsedOverlay` → `snapTo(0f)`, `setCamera(animated=true)` → `animateCameraMove`.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/MapAnimation.kt shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt
git commit -m "feat: add MapAnimation with pulse and camera move"
```

---

### Task 13: BoundaryAssetLoader expect/actual

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt`
- Create: `shared/src/iosMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt`

- [ ] **Step 1: Write expect declaration**

```kotlin
package com.mapchina.map

expect class BoundaryAssetLoader() {
    suspend fun loadBoundary(adcode: String): String
}
```

- [ ] **Step 2: Write Android actual**

Reads from `context.assets.open("boundaries/$adcode.json")`. Uses application context from `MapChinaApplication`.

- [ ] **Step 3: Write iOS actual**

Reads from `NSBundle.mainBundle.pathForResource("boundaries/$adcode", "json")`.

- [ ] **Step 4: Build to verify**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:compileKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt shared/src/androidMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt shared/src/iosMain/kotlin/com/mapchina/map/BoundaryAssetLoader.kt
git commit -m "feat: add BoundaryAssetLoader expect/actual for asset reading"
```

---

## Chunk 4: Cleanup & Migration

### Task 14: Remove AMap SDK dependency and files

**Files:**
- Modify: `shared/build.gradle.kts` — remove `com.amap.api:3dmap:10.0.600`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/map/AttractionMarkerRenderer.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/map/PhotoMarkerRenderer.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml` — remove AMap API key meta-data

- [ ] **Step 1: Remove AMap dependency from build.gradle.kts**

Remove line: `implementation("com.amap.api:3dmap:10.0.600")`

- [ ] **Step 2: Delete AMap renderer files**

```bash
rm shared/src/androidMain/kotlin/com/mapchina/map/AttractionMarkerRenderer.kt
rm shared/src/androidMain/kotlin/com/mapchina/map/PhotoMarkerRenderer.kt
```

- [ ] **Step 3: Remove AMap meta-data from AndroidManifest**

Remove any `<meta-data>` entries with `com.amap.api.v2.apikey` or similar.

- [ ] **Step 4: Build to verify**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :androidApp:assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: remove AMap SDK dependency and renderer files"
```

---

### Task 15: Smoke test — run app and verify map renders

- [ ] **Step 1: Build and install debug APK**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :androidApp:installDebug 2>&1 | tail -5`

- [ ] **Step 2: Launch app and verify**

Manually verify:
- [ ] Map shows 34 province outlines
- [ ] Province colors match footprint level
- [ ] Tapping a province shows RegionCard
- [ ] Drill-down loads city/district boundaries
- [ ] Pan and zoom work smoothly
- [ ] Double-tap zooms in
- [ ] Pulse animation works on region select
- [ ] South China Sea inset visible at low zoom
- [ ] Markers render at correct positions

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix: adjust map rendering based on smoke test"
```

---

### Task 16: Final integration test — ensure MapViewModel works unchanged

- [ ] **Step 1: Run existing tests**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:commonTest 2>&1 | tail -5`
Expected: All tests pass

- [ ] **Step 2: Run Android unit tests**

Run: `cd /Users/missy/PROJ/MapChina && ./gradlew :shared:testDebugUnitTest 2>&1 | tail -5`
Expected: All tests pass

- [ ] **Step 3: Commit final state**

```bash
git add -A
git commit -m "test: verify all existing tests pass with new map implementation"
```
