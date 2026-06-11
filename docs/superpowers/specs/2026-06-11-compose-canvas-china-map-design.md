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

## 范围外

- **地图旋转/朝向**：不支持 bearing 旋转，固定正北朝上
- **离线瓦片/卫星图**：纯矢量自绘，不加载瓦片图层

---

## §1 整体架构

```
commonMain/kotlin/com/mapchina/map/
├── ChinaMapView.kt          // 顶层 Composable，替代 PlatformMapView
├── MapRenderer.kt           // Canvas 绘制核心：投影 → Path → drawPath
├── GeoProjection.kt         // 经纬度 → 屏幕坐标投影（Web Mercator）
├── GeoPathCache.kt          // 坐标数据 → Compose Path 预构建 & 缓存
├── HitTester.kt             // 点击检测：屏幕坐标 → 省份/市县 ID
├── ViewportState.kt         // 视口状态：中心经纬度、缩放级别
├── MapGestureHandler.kt     // 手势处理：平移、缩放、点击、长按、双击缩放
├── MapAnimation.kt          // 脉冲、相机动画
└── BoundaryAssetLoader.kt   // expect/actual：从 assets 加载边界 JSON
```

核心思路：

- `ChinaMapView` 是唯一对外暴露的 Composable，内部组合 `ViewportState` + `MapGestureHandler` + `MapRenderer`
- `MapController` 从 `expect/actual` 改为普通 `class`（删除 expect 声明），全部实现放在 commonMain
- 所有绘制逻辑在 `commonMain`，无平台分支

与现有代码的关系：

- `PlatformMapView` expect/actual 删除，`MapScreen` 直接使用 `ChinaMapView`
- `MapController` 从 `expect class` 改为普通 `class`，删除 `AndroidMapController.kt` 和 `IosMapController.kt`
- `MapViewModel` 几乎不需要改动（它只通过 `MapController` 接口操作）
- `BoundaryAssetLoader` 保留 expect/actual 仅用于文件读取（Android assets / iOS bundle）

---

## §2 投影与路径构建

### GeoProjection — 经纬度到屏幕坐标

```kotlin
data class GeoProjection(
    val viewCenterLng: Double,
    val viewCenterLat: Double,
    val scale: Float,           // 像素/度，由 zoomLevel 派生
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

    fun unproject(x: Float, y: Float): Pair<Double, Double> {
        val lng = (x - canvasWidth / 2) / scale.toDouble() + viewCenterLng
        val mercY = -((y - canvasHeight / 2) / scale).toDouble() + centerMercY
        val lat = toDegrees(2 * atan(exp(mercY)) - PI / 2)
        return lng to lat
    }
}
```

- Web Mercator 投影：X 线性映射经度，Y 用墨卡托公式，保证省份形状不失真
- 以视口中心为原点，`scale` 控制缩放，天然支持平移/缩放动画
- 已知权衡：墨卡托在高纬度（黑龙江 ~53°N）放大面积，这是 Web 地图标准行为

### ViewportState.scale — zoomLevel 到 scale 的映射

```kotlin
// zoomLevel 3~15 与高德级别对齐
// scale = baseScale * 2^(zoomLevel - baseZoom)
// baseZoom = 3.5, baseScale 在 360dp 宽度下约 2.0 像素/度
// 具体数值在实现时通过实测校准，确保 zoom 3.5 时全国可见，zoom 15 时街道级别
val scale: Float get() = baseScale * 2f.pow(zoomLevel - BASE_ZOOM)
```

### GeoPathCache — 坐标数据 → Compose Path 预构建

```kotlin
class GeoPathCache {
    // regionId → 预构建的 Compose Path
    private val paths: Map<String, List<Path>>
    // regionId → 预计算的包围盒（用于视口裁剪和 hit-test 加速）
    private val bounds: Map<String, Rect>

    // 接受原始坐标数据（非 GeoJSON Feature），兼容现有 boundary_json 格式
    fun build(
        regionCoords: Map<String, List<List<Pair<Double, Double>>>>,
        projection: GeoProjection
    ) {
        // 每个区域可能有多个 ring（MultiPolygon 或带洞多边形）
        // 每个 ring → projection.project() → Path.moveTo/lineTo/close
    }

    fun getVisibleRegions(viewport: Rect): List<String> {
        // 只返回包围盒与视口相交的 region
    }
}
```

关键策略：

- **手势期间用仿射变换替代全量重建**：连续拖动/缩放时，对已缓存的 Path 应用 `graphicsLayer` 的 `scale`/`translation` 变换，避免每帧重建 ~42,000 点的 Path
- **手势结束后全量重建**：手势结束时用最新 `GeoProjection` 重建 Path，消除累积浮点误差
- `getVisibleRegions()` 做视口裁剪，市县级只绘制视口内的区域
- 市县级多边形在低缩放级别下用 Douglas-Peucker 简化，高缩放级别用原始精度
- **MultiPolygon 支持**：每个 regionId 映射到 `List<Path>`（而非单个 Path），支持港、澳等多环多边形，使用 `EVEN_ODD` 填充规则处理带洞区域

---

## §3 交互层 — 手势与点击检测

### ViewportState — 视口状态管理

```kotlin
class ViewportState {
    var centerLng: Double by mutableStateOf(104.0)
    var centerLat: Double by mutableStateOf(35.5)
    var zoomLevel: Float by mutableStateOf(3.5f)   // 3~15
    val scale: Float                                 // 由 zoomLevel 派生

    fun moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean)
    fun zoomBy(delta: Float, pivot: Offset)
    fun panBy(delta: Offset)
}
```

- 所有字段用 `mutableStateOf`，Compose 自动重组
- 删除了之前的 `offset` 字段 — 平移直接更新 `centerLng/Lat`，不引入中间偏移

### MapGestureHandler — 手势识别

```kotlin
fun Modifier.mapGestures(
    viewport: ViewportState,
    onTap: (Offset) -> Unit,
    onLongPress: ((Offset) -> Unit)? = null
): Modifier =
    pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom ->
            viewport.panBy(pan)
            viewport.zoomBy(zoom, centroid)
        }
    }.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { offset -> viewport.zoomBy(1f, offset) },  // 双击放大
            onTap = onTap
        )
    }
```

- 平移/缩放：`detectTransformGestures`，单指拖动 + 双指捏合
- 双击缩放：`onDoubleTap` → 以点击位置为锚点放大一级
- 点击：`onTap`，触发 hit-test
- 长按：预留 `onLongPress` 参数（用于 Marker 长按交互）

### HitTester — 屏幕坐标到区域 ID

```kotlin
class HitTester(private val pathCache: GeoPathCache) {
    fun hitTest(screenOffset: Offset): String? {
        // 1. 遍历 pathCache.bounds，快速排除包围盒外的区域
        // 2. 对包围盒命中的区域，用射线法（ray casting）精确判定
        //    通常只剩 1~3 个候选区域
        return matchedRegionId
    }
}
```

- 第一轮包围盒过滤：509 个 Rect 相交检测几乎零开销
- 第二轮精确检测：射线法纯 Kotlin 实现，不依赖 `android.graphics.Region`
- 高缩放级别下视口内区域少（通常 <10），射线法开销可忽略

---

## §4 数据管线 — 加载、解析、简化

### 边界数据格式

现有系统中，边界数据有两条路径：

1. **assets 原始文件**：509 个 JSON 文件，分两种格式
   - 省级文件（34 个）：`{"type":"Feature","properties":{...},"geometry":{...}}`
   - 市县级文件（475 个）：`{"type":"FeatureCollection","features":[{...}]}`
2. **SQLite boundary_json 列**：启动时从 assets 导入，存储为扁平坐标数组 `[lng,lat,lng,lat,...]`

**设计决策：复用 SQLite boundary_json 格式**，与现有 `MapViewModel` 调用 `addOverlay(regionId, boundary, style)` 的契约一致。`MapController.addOverlay()` 的 `boundary` 参数已经是扁平坐标数组字符串，无需重新解析 GeoJSON Feature。

`BoundaryAssetLoader`（expect/actual）仅在首次启动数据库种子时使用，运行时数据全部来自 SQLite。

```kotlin
// commonMain
expect class BoundaryAssetLoader() {
    suspend fun loadBoundary(adcode: String): String
}

// androidMain
actual class BoundaryAssetLoader {
    actual suspend fun loadBoundary(adcode: String): String {
        context.assets.open("boundaries/$adcode.json").bufferedReader().readText()
    }
}

// iosMain
actual class BoundaryAssetLoader {
    actual suspend fun loadBoundary(adcode: String): String {
        // NSBundle main bundle 读取同名文件
    }
}
```

### 边界解析 — 统一处理两种 GeoJSON 格式

```kotlin
object BoundaryParser {
    fun parse(rawJson: String): List<List<Pair<Double, Double>>> {
        val root = Json.parseToJsonElement(rawJson).jsonObject
        return when (root["type"]?.jsonPrimitive?.content) {
            "Feature" -> parseFeatureGeometry(root)
            "FeatureCollection" -> {
                // 取 features 数组的第一个 Feature 的 geometry
                val feature = root["features"]!!.jsonArray.first().jsonObject
                parseFeatureGeometry(feature)
            }
            else -> emptyList()
        }
    }

    private fun parseFeatureGeometry(feature: JsonObject): List<List<Pair<Double, Double>>> {
        val geom = feature["geometry"]!!.jsonObject
        return when (geom["type"]!!.jsonPrimitive.content) {
            "Polygon" -> parsePolygonRings(geom["coordinates"]!!.jsonArray)
            "MultiPolygon" -> parseMultiPolygonRings(geom["coordinates"]!!.jsonArray)
            else -> emptyList()
        }
    }
}
```

- 统一处理 Feature 和 FeatureCollection 两种格式
- 支持 Polygon（单环 + 可选洞）和 MultiPolygon（多环）
- 输出 `List<List<Pair<Double, Double>>>`：外层 List 是各 ring，内层是坐标点

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
- 简化结果按 `(regionId, epsilon)` 缓存，同缩放级别不重复计算

---

## §5 MapController 接口适配

`MapController` 从 `expect class` 改为普通 `class`，全部实现放在 commonMain：

```kotlin
// commonMain — 删除 expect 声明，直接定义为普通 class
class MapController() {
    internal val viewport = ViewportState()
    private val _renderState = MutableStateFlow(RenderState())

    // ---- Overlay 操作 ----
    fun addOverlay(regionId: String, boundary: String, style: OverlayStyle) {
        val coords = parseBoundaryCoords(boundary)  // 复用现有扁平坐标解析
        _renderState.update { it.copy(overlays = it.overlays + (regionId to OverlayData(coords, style))) }
    }
    fun removeOverlay(regionId: String) {
        _renderState.update { it.copy(overlays = it.overlays - regionId) }
    }
    fun clearOverlays() {
        _renderState.update { it.copy(overlays = emptyMap()) }
    }
    fun removeOverlaysExcept(regionIds: Set<String>) {
        _renderState.update { it.copy(overlays = it.overlays.filterKeys { it in regionIds }) }
    }
    fun pulseOverlay(regionId: String) { ... }
    fun restorePulsedOverlay() { ... }

    // ---- Marker 操作 ----
    fun addMarker(attractionId: String, name: String, lat: Double, lng: Double, visited: Boolean) { ... }
    fun addAttractionMarker(attractionId: String, name: String, lat: Double, lng: Double, imageUrl: String?, visited: Boolean) { ... }
    fun removeMarker(attractionId: String) { ... }
    fun clearMarkers() { ... }

    // ---- 图片 Marker 操作 ----
    fun addImageMarker(id: String, lat: Double, lng: Double, imagePath: String, count: Int) { ... }
    fun removeImageMarker(id: String) { ... }
    fun clearImageMarkers() { ... }

    // ---- Polyline 操作 ----
    fun addPolyline(id: String, points: List<Pair<Double, Double>>, color: Long, width: Float) { ... }
    fun removePolyline(id: String) { ... }
    fun clearPolylines() { ... }

    // ---- 相机操作 ----
    fun setCamera(lat: Double, lng: Double, zoomLevel: Float, animated: Boolean) {
        viewport.moveTo(lng, lat, zoomLevel, animated)
    }
    fun toScreenLocation(lat: Double, lng: Double): Pair<Float, Float>? {
        return viewport.currentProjection?.project(lng, lat)?.let { it.x to it.y }
    }

    // ---- 回调 ----
    fun setOnRegionTapListener(listener: ((String) -> Unit)?) { regionTapListener = listener }
    fun setOnMarkerTapListener(listener: ((String) -> Unit)?) { markerTapListener = listener }
    fun setOnCameraZoomChangeListener(listener: ((Float) -> Unit)?) { ... }
    fun setOnCameraPositionListener(listener: ((Double, Double, Float) -> Unit)?) { ... }
    fun setOnMapReadyListener(listener: (() -> Unit)?) { ... }

    // ---- 生命周期 ----
    fun dispose() { animationScope.cancel() }
}
```

### RenderState — 渲染状态

```kotlin
data class RenderState(
    val overlays: Map<String, OverlayData> = emptyMap(),
    val markers: Map<String, MarkerData> = emptyMap(),
    val attractionMarkers: Map<String, AttractionMarkerData> = emptyMap(),
    val imageMarkers: Map<String, ImageMarkerData> = emptyMap(),
    val polylines: Map<String, PolylineData> = emptyMap(),
    val pulseTarget: String? = null
)
```

### Marker 图片处理

网络图片 Marker（`addAttractionMarker` 的 `imageUrl`）：

- 使用已有 Coil 3 依赖（`coil-compose` + `coil-network-ktor3`，已在 commonMain）加载网络图片
- 在 `ChinaMapView` 中用 `AsyncImage` 预加载到 `ImageBitmap`，再传入 Canvas 绘制
- 加载中显示占位圆形，加载失败显示默认图标

### ChinaMapView — 顶层 Composable

```kotlin
@Composable
fun ChinaMapView(controller: MapController, modifier: Modifier = Modifier) {
    val renderState by controller.renderState.collectAsState()

    Box(modifier = modifier.mapGestures(
        viewport = controller.viewport,
        onTap = { offset -> controller.handleTap(offset) },
        onLongPress = { offset -> controller.handleLongPress(offset) }
    )) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val projection = controller.viewport.toProjection(size)

            // L0: 海洋背景
            drawRect(MapChinaColors.Background)

            // L1/L2: 省份/市县多边形
            val pathCache = geoPathCache.buildIfChanged(renderState.overlays, projection)
            pathCache.visibleRegions.forEach { regionId ->
                val data = renderState.overlays[regionId] ?: return@forEach
                data.paths.forEach { path ->
                    drawPath(path, color = data.style.fillColor)
                    drawPath(path, color = data.style.strokeColor, style = Stroke(data.style.strokeWidth))
                }
            }

            // L4: Marker（圆形 + 文字标签）
            renderState.markers.values.forEach { marker ->
                val pos = projection.project(marker.lng, marker.lat)
                drawCircle(markerColor(marker.visited), radius = 6.dp.toPx(), center = pos)
                // drawText label...
            }

            // L5: Polyline
            renderState.polylines.values.forEach { polyline ->
                val path = polyline.toPath(projection)
                drawPath(path, color = Color(polyline.color), style = Stroke(polyline.width))
            }

            // L6: 脉冲动画
            controller.pulseTarget?.let { targetId ->
                pathCache[targetId]?.forEach { path ->
                    drawPath(path, color = pulseColor.copy(alpha = controller.pulseAlpha))
                }
            }

            // 南海插图（低缩放时）
            drawSouthChinaSeaInset(projection, controller.viewport.zoomLevel)
        }
    }
}
```

---

## §6 动画与视觉效果

### 脉冲动画

用 `Animatable` 替代高德的 `ValueAnimator`：

```kotlin
// 在 MapController 中
private val pulseAnimatable = Animatable(0f)

fun pulseOverlay(regionId: String) {
    pulseTarget = regionId
    animationScope.launch {
        pulseAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }
}

fun restorePulsedOverlay() {
    pulseTarget = null
    animationScope.launch { pulseAnimatable.snapTo(0f) }
}
```

- `animationScope` 是 `CoroutineScope(SupervisorJob() + Dispatchers.Main)`，随 `dispose()` 取消
- 脉冲 alpha 值通过 `pulseAnimatable.value` 在 Canvas 绘制帧中读取

### 着色方案

| 状态 | 填充色 | 说明 |
|------|--------|------|
| 未到访 | `SurfaceElevated` 低透明度 | 灰淡底色 |
| 路过 | `FootprintPassBy` 40% alpha | 浅蓝 |
| 短暂停留 | `FootprintShortVisit` 50% alpha | 中蓝 |
| 深度游 | `FootprintDeep` 60% alpha | 深蓝 |
| 选中/脉冲 | 叠加动画层 | 金色脉冲 |

- 颜色取自 `MapChinaColors`，已适配深色模式
- 描边宽度随缩放动态调整：zoom 3~5 → 0.5dp，zoom 6+ → 1dp

### 相机动画

```kotlin
fun ViewportState.moveTo(lng: Double, lat: Double, zoom: Float, animated: Boolean) {
    if (!animated) { centerLng = lng; centerLat = lat; zoomLevel = zoom; return }
    // 使用调用方提供的 scope，随 MapController.dispose() 取消
    animationScope.launch {
        launch { Animatable(centerLng).animateTo(lng, tween(400)) { centerLng = value } }
        launch { Animatable(centerLat).animateTo(lat, tween(400)) { centerLat = value } }
        launch { Animatable(zoomLevel).animateTo(zoom, tween(400)) { zoomLevel = value } }
    }
}
```

- 三个维度并行动画
- `animationScope` 生命周期绑定 `MapController`，不会泄漏

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

### 手势期间仿射变换策略

连续手势（拖动/缩放）期间，避免每帧重建 Path：

1. **手势开始时**：保存当前 `GeoProjection` 和已构建的 Path 作为快照
2. **手势进行中**：用 `graphicsLayer` 的 `transformOrigin` + `scale`/`translation` 对快照做仿射变换，不触发 Path 重建
3. **手势结束时**：用最新 `GeoProjection` 全量重建 Path，消除累积误差，重置 `graphicsLayer` 变换

```kotlin
// ChinaMapView 中
val isGesturing by controller.isGesturing.collectAsState()
val gestureTransform by controller.gestureTransform.collectAsState()

Canvas(
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            if (isGesturing) {
                scaleX = gestureTransform.scale
                scaleY = gestureTransform.scale
                translationX = gestureTransform.offsetX
                translationY = gestureTransform.offsetY
                transformOrigin = TransformOrigin(gestureTransform.pivotX, gestureTransform.pivotY)
            }
        }
) { ... }
```

### 路径缓存与增量更新

```kotlin
private var lastProjection: GeoProjection? = null

fun buildIfChanged(
    overlays: Map<String, OverlayData>,
    newProjection: GeoProjection
): Map<String, List<Path>> {
    if (newProjection == lastProjection && overlays == lastOverlays) return cachedPaths
    lastProjection = newProjection
    lastOverlays = overlays
    // 全量重建
    return rebuildAll(overlays, newProjection)
}
```

### 帧率保障

- 目标 60fps
- 仿射变换策略确保手势期间零 Path 重建
- 手势结束后单次重建，~42,000 点的坐标变换 + Path 构建预估 10~30ms（单帧内可接受）
- 若 iOS 实测帧率不足，进一步优化为只重建视口内区域的 Path

---

## §8 南海诸岛与测试

### 南海诸岛

```kotlin
fun DrawScope.drawSouthChinaSeaInset(projection: GeoProjection, zoom: Float) {
    if (zoom > 8f) return  // 高缩放时用户可平移到南海
    // 右下角固定位置插图框（80dp × 100dp）：
    // - 九段线折线（~18 个经纬度点，硬编码常量 SouthChinaSea.NINE_DASH_LINE）
    // - 主要岛屿点（西沙、南沙、中沙、东沙坐标常量）
    // - 与主图相同的着色逻辑
}
```

- 九段线坐标和主要岛屿坐标作为常量硬编码在 `commonMain`
- 插图框约 80dp × 100dp，右下角固定
- 高缩放级别下隐藏插图，用户可直接平移到南海区域
- 509 个边界文件中不包含南海诸岛详细边界，仅通过插图表示

### 测试策略

| 层级 | 测试内容 | 方式 |
|------|----------|------|
| 单元 | `GeoProjection.project()` / `unproject()` 正反算 | commonTest |
| 单元 | `DouglasPeucker.simplify()` 保端点、保精度 | commonTest |
| 单元 | `HitTester.hitTest()` 包含/排除边界 | commonTest |
| 单元 | `ViewportState.zoomBy()` 锚点缩放正确性 | commonTest |
| 单元 | `BoundaryParser.parse()` Feature 和 FeatureCollection 两种格式 | commonTest + fixture |
| 单元 | `BoundaryParser.parse()` Polygon 和 MultiPolygon 几何 | commonTest + fixture |
| 单元 | `MapController` 全部 27 个方法的接口契约 | commonTest |
| 集成 | 34 省级边界全部加载并构建 Path | androidUnitTest |
| 集成 | 钻取→加载市县→渲染→返回→恢复省级 | androidUnitTest |
| 集成 | 网络图片 Marker 加载与渲染 | androidUnitTest + mock |
| E2E | 手势平移/缩放/点击/双击缩放 | Compose UI 测试 + 手势注入 |
| E2E | 着色随到访状态变化 | screenshot 验证 |

---

## 移除清单

实现完成后可移除的依赖和文件：

- `com.amap.api:3dmap:10.0.600` — build.gradle.kts androidMain 依赖
- `AndroidMapController.kt` — 高德 actual 实现（MapController 改为 commonMain 普通 class）
- `IosMapController.kt` — iOS actual 实现（同上）
- `PlatformMapView.android.kt` 中 AMap View 创建逻辑
- `PlatformMapView.ios.kt` 中 MKMapView 相关代码
- `AttractionMarkerRenderer.kt` / `PhotoMarkerRenderer.kt` — 高德 Marker 渲染器
- AndroidManifest 中高德 API Key 等 meta-data

保留不动的依赖：

- `androidx.ink:*` — 与地图无关，用于刻字功能
- `BoundaryAssetLoader` 的 expect/actual — 仍需平台特定文件读取
