# Compose 自绘中国地图组件 — 替代高德 SDK

> 日期：2026-06-11
> 状态：设计完成，待实现

## 目标

用 Compose Canvas 完全替换首页高德地图 SDK，实现一个跨平台（Android + iOS）、零第三方依赖的中国地图组件，支持多级钻取、着色、Marker、动画等现有全部功能。

## 约束

- 全部逻辑在 `commonMain` 实现，无平台分支
- 复用现有 `assets/boundaries/` 下的 509 个 GeoJSON 文件
- `MapController` 接口不变，`MapViewModel` 零改动
- 无新增第三方依赖

---

## §1 整体架构

```
commonMain/kotlin/com/mapchina/map/
├── ChinaMapView.kt          // 顶层 Composable，替代 PlatformMapView
├── MapRenderer.kt           // Canvas 绘制核心：投影 → Path → drawPath
├── GeoProjection.kt         // 经纬度 → 屏幕坐标投影（Web Mercator）
├── GeoPathCache.kt          // GeoJSON → Compose Path 预构建 & 缓存
├── HitTester.kt             // 点击检测：屏幕坐标 → 省份/市县 ID
├── ViewportState.kt         // 视口状态：中心经纬度、缩放级别、平移偏移
├── MapGestureHandler.kt     // 手势处理：平移、缩放、点击、长按
├── MapAnimation.kt          // 脉冲、相机动画
└── GeoJsonLoader.kt         // 从 assets 加载 & 解析 GeoJSON（重构到 commonMain）
```

核心思路：

- `ChinaMapView` 是唯一对外暴露的 Composable，内部组合 `ViewportState` + `MapGestureHandler` + `MapRenderer`
- `MapController` 的 expect/actual 接口保留，actual 实现改为驱动 `ViewportState` 而非高德 SDK
- 所有绘制逻辑在 `commonMain`，无平台分支

与现有代码的关系：

- `PlatformMapView` 改为直接渲染 `ChinaMapView`（不再包裹原生 AMap View）
- `MapController` 接口不变，actual 实现重写
- `MapViewModel` 几乎不需要改动（它只通过 `MapController` 接口操作）

---

## §2 投影与路径构建

### GeoProjection — 经纬度到屏幕坐标

```kotlin
data class GeoProjection(
    val viewCenterLng: Double,
    val viewCenterLat: Double,
    val scale: Float,
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    fun project(lng: Double, lat: Double): Offset {
        val x = (lng - viewCenterLng).toFloat() * scale + canvasWidth / 2
        val mercY = ln(tan(PI/4 + toRadians(lat)/2))
        val centerMercY = ln(tan(PI/4 + toRadians(viewCenterLat)/2))
        val y = -(mercY - centerMercY).toFloat() * scale + canvasHeight / 2
        return Offset(x, y)
    }
}
```

- Web Mercator 投影：X 线性映射经度，Y 用墨卡托公式，保证省份形状不失真
- 以视口中心为原点，`scale` 控制缩放，天然支持平移/缩放动画

### GeoPathCache — GeoJSON → Compose Path 预构建

```kotlin
class GeoPathCache {
    private val paths: Map<String, Path>
    private val bounds: Map<String, Rect>

    fun build(boundaryData: Map<String, GeoJsonFeature>, projection: GeoProjection) {
        // 坐标点 → projection.project() → Path.moveTo/lineTo/close
    }

    fun getVisibleRegions(viewport: Rect): List<String> {
        // 只返回包围盒与视口相交的 region
    }
}
```

关键策略：

- 每次视口变化时，用新 `GeoProjection` 重建所有 Path（509 个多边形 <1ms）
- `getVisibleRegions()` 做视口裁剪，市县级只绘制视口内的区域
- 市县级多边形在低缩放级别下用 Douglas-Peucker 简化（阈值随缩放动态调整），高缩放级别用原始精度

---

## §3 交互层 — 手势与点击检测

### ViewportState — 视口状态管理

```kotlin
class ViewportState {
    var centerLng: Double by mutableStateOf(104.0)
    var centerLat: Double by mutableStateOf(35.5)
    var zoomLevel: Float by mutableStateOf(3.5f)
    var offset: Offset by mutableStateOf(Offset.Zero)

    val scale: Float

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean)
    fun zoomBy(delta: Float, pivot: Offset)
    fun panBy(delta: Offset)
}
```

- 所有字段用 `mutableStateOf`，Compose 自动重组
- `moveTo(animated=true)` 用 `Animatable` 驱动平滑相机移动

### MapGestureHandler — 手势识别

```kotlin
fun Modifier.mapGestures(viewport: ViewportState, onTap: (Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom ->
            viewport.panBy(pan)
            viewport.zoomBy(zoom, centroid)
        }
    }.pointerInput(Unit) {
        detectTapGestures { offset -> onTap(offset) }
    }
```

- 平移/缩放：`detectTransformGestures`，单指拖动 + 双指捏合
- 点击：`detectTapGestures`，触发 hit-test
- 长按：预留 `detectDragGestures` 的长按回调（用于 Marker 长按交互）

### HitTester — 屏幕坐标到区域 ID

```kotlin
class HitTester(private val pathCache: GeoPathCache) {
    fun hitTest(screenOffset: Offset): String? {
        // 1. 遍历 pathCache.bounds，快速排除包围盒外的区域
        // 2. 对包围盒命中的区域，用射线法（ray casting）精确判定
        return matchedRegionId
    }
}
```

- 第一轮包围盒过滤：509 个 Rect 相交检测几乎零开销
- 第二轮精确检测：通常只剩 1~3 个候选区域，射线法纯 Kotlin 实现
- 不依赖 `android.graphics.Region`

---

## §4 数据管线 — 加载、解析、简化

### GeoJSON 加载（commonMain）

```kotlin
expect class BoundaryAssetLoader() {
    suspend fun loadBoundary(adcode: String): String
}

// androidMain: context.assets.open("boundaries/$adcode.json")
// iosMain: NSBundle main bundle 读取同名文件
```

### GeoJSON 解析

用已有 `kotlinx-serialization-json`：

```kotlin
@Serializable
data class GeoJsonFeature(
    val properties: GeoJsonProperties,
    val geometry: GeoJsonGeometry
)

@Serializable
data class GeoJsonProperties(
    val adcode: Int,
    val name: String,
    val center: List<Double>
)

@Serializable
data class GeoJsonGeometry(
    val type: String,           // "Polygon" or "MultiPolygon"
    val coordinates: JsonArray
)
```

### Douglas-Peucker 简化

```kotlin
object DouglasPeucker {
    fun simplify(points: List<Pair<Double, Double>>, epsilon: Double): List<Pair<Double, Double>>
}
```

| 缩放级别 | epsilon | 效果 |
|----------|---------|------|
| 3~5 | 0.05 | 省级轮廓约 50 点/省，总 ~1700 点 |
| 6~9 | 0.01 | 市县级约 100 点/区 |
| 10+ | 0 | 原始精度 |

加载策略：

- 启动时加载 34 个省级边界（~7000 点，瞬间完成）
- 钻取到市级时按需加载该省的市县级边界（~15 个文件，lazy load）
- 已加载的边界缓存在内存，不重复读

---

## §5 MapController 接口适配

`MapController` 从平台绑定类改为纯状态驱动类，actual 实现全部在 commonMain：

```kotlin
actual class MapController actual constructor() {
    private val viewport = ViewportState()
    private val renderCommands = MutableStateFlow<List<RenderCommand>>(emptyList())

    actual fun addOverlay(regionId: String, boundary: String, style: OverlayStyle) {
        renderCommands.update { it + RenderCommand.AddOverlay(regionId, parsedCoords, style) }
    }
    actual fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        viewport.moveTo(lng, lat, zoomLevel, animated)
    }
    // ... 其余方法同理
}
```

### RenderCommand 密封类

```kotlin
sealed class RenderCommand {
    data class AddOverlay(val regionId: String, val coords: List<List<Pair<Double, Double>>>, val style: OverlayStyle)
    data class RemoveOverlay(val regionId: String)
    data class AddMarker(val id: String, val lng: Double, val lat: Double, val label: String, val visited: Boolean)
    data class AddPolyline(val id: String, val points: List<Pair<Double, Double>>, val color: Long, val width: Float)
    data class PulseRegion(val regionId: String)
}
```

### ChinaMapView — 顶层 Composable

```kotlin
@Composable
fun ChinaMapView(controller: MapController, modifier: Modifier = Modifier) {
    val viewport = controller.viewport
    val commands by controller.renderCommands.collectAsState()

    Box(modifier = modifier.mapGestures(viewport, onTap = { offset ->
        controller.hitTest(offset)?.let { controller.onRegionTap(it) }
    })) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val projection = viewport.toProjection(size)
            val pathCache = buildPaths(commands, projection)

            // L0: 海洋背景
            drawRect(MapChinaColors.Background)
            // L1/L2: 省份/市县多边形
            pathCache.visibleRegions.forEach { regionId ->
                drawPath(pathCache[regionId], color = styleFor(regionId).fillColor)
                drawPath(pathCache[regionId], color = styleFor(regionId).strokeColor, style = Stroke(1f))
            }
            // L4: Marker
            // L5: Polyline
            // L6: 脉冲动画
        }
    }
}
```

---

## §6 动画与视觉效果

### 脉冲动画

用 `Animatable` 替代高德的 `ValueAnimator`：

```kotlin
val pulseAlpha by controller.pulseAnimatable.animateAsState(
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

### 着色方案

| 状态 | 填充色 | 说明 |
|------|--------|------|
| 未到访 | `SurfaceElevated` 低透明度 | 灰淡底色 |
| 路过 | `FootprintPassBy` 40% alpha | 浅蓝 |
| 短暂停留 | `FootprintShortVisit` 50% alpha | 中蓝 |
| 深度游 | `FootprintDeep` 60% alpha | 深蓝 |
| 选中/脉冲 | 叠加动画层 | 金色脉冲 |

描边宽度随缩放动态调整：zoom 3~5 → 0.5dp，zoom 6+ → 1dp。

### 相机动画

```kotlin
fun ViewportState.moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean) {
    if (!animated) { centerLng = lng; centerLat = lat; zoomLevel = zoom; return }
    CoroutineScope(Dispatchers.Main).launch {
        Animatable(centerLng).animateTo(lng, tween(400)) { centerLng = value }
    }
}
```

### Marker 绘制

Canvas 绘制图标 + 文字标签，图片 Marker 用 `drawImage` 绘制预解码的 `ImageBitmap`。

---

## §7 性能策略

### 分层渲染

| 层 | 内容 | 重绘条件 |
|----|------|----------|
| L0 | 海洋/底图背景 | 从不 |
| L1 | 省级多边形（34 个） | 视口变化时 |
| L2 | 市县级多边形（按需） | 视口变化且 zoom ≥ 6 时 |
| L3 | 描边 | 与 L1/L2 同步 |
| L4 | Marker | 视口变化或 Marker 增删时 |
| L5 | Polyline | 视口变化或 Polyline 增删时 |
| L6 | 脉冲/选中高亮 | 动画帧 |

- 低缩放（zoom 3~5）只绘制 L0 + L1 + L3
- 高缩放（zoom 6+）启用 L2，且用 `getVisibleRegions()` 视口裁剪

### 路径缓存与增量更新

```kotlin
private var lastProjection: GeoProjection? = null

fun buildIfChanged(newProjection: GeoProjection): Map<String, Path> {
    if (newProjection == lastProjection) return cachedPaths
    // 否则重建
}
```

### 帧率保障

- 目标 60fps
- 若 iOS 实测帧率不足，引入 `graphicsLayer` 将 L1~L3 绘制到离屏缓冲，手势期间只重绘变换矩阵

---

## §8 南海诸岛与测试

### 南海诸岛

```kotlin
fun DrawScope.drawSouthChinaSeaInset(projection: GeoProjection, zoom: Float) {
    if (zoom > 8f) return  // 高缩放时用户可平移到南海
    // 右下角固定位置插图框：
    // - 九段线折线（~18 个点，硬编码常量）
    // - 主要岛屿点
    // - 与主图相同的着色逻辑
}
```

- 九段线坐标作为常量硬编码在 `commonMain`
- 插图框约 80dp × 100dp，右下角固定
- 高缩放级别下隐藏插图

### 测试策略

| 层级 | 测试内容 | 方式 |
|------|----------|------|
| 单元 | `GeoProjection.project()` 正反算 | commonTest |
| 单元 | `DouglasPeucker.simplify()` 保端点、保精度 | commonTest |
| 单元 | `HitTester.hitTest()` 包含/排除边界 | commonTest |
| 单元 | `ViewportState.zoomBy()` 锚点缩放正确性 | commonTest |
| 单元 | `GeoJsonFeature` 反序列化 | commonTest + fixture |
| 集成 | 34 省级边界全部加载并构建 Path | androidUnitTest |
| 集成 | 钻取→加载市县→渲染→返回→恢复省级 | androidUnitTest |
| E2E | 手势平移/缩放/点击省份 | Compose UI 测试 |
| E2E | 着色随到访状态变化 | screenshot 验证 |

---

## 移除清单

实现完成后可移除的依赖和文件：

- `com.amap.api:3dmap:10.0.600` — build.gradle.kts androidMain 依赖
- `AndroidMapController.kt` — 高德 actual 实现（替换为 commonMain 实现）
- `PlatformMapView.android.kt` 中 AMap View 创建逻辑
- `AttractionMarkerRenderer.kt` / `PhotoMarkerRenderer.kt` — 高德 Marker 渲染器
- AndroidManifest 中高德 API Key 等 meta-data
