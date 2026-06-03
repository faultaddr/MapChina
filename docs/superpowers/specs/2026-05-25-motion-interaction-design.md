# MapChina 动效交互设计规范

**日期:** 2026-05-25
**版本:** v1.0
**设计方向:** 山水流动（流体动效系统）
**目标平台:** Android + iOS（Compose Multiplatform 同步实现）

---

## 1. 设计哲学

> 整个产品的动效语言围绕「水」的意象构建。地图下钻如潜水，页面转场如翻书卷，成就解锁如涟漪扩散。所有动效传递同一种质感——柔、连、不突兀。

### 1.1 三大空间隐喻

| 交互 | 隐喻 | 情感目标 |
|------|------|---------|
| 地图下钻 | 潜入水中 | 探索欲、好奇心 |
| 页面转场 | 翻开书卷 | 叙事感、连贯性 |
| 成就解锁 | 涟漪扩散 | 惊喜感、成就感 |

---

## 2. 核心运动曲线体系

### 2.1 Spring 曲线

| 名称 | dampingRatio | stiffness | 适用场景 | 质感 |
|------|-------------|-----------|---------|------|
| spring-gentle | 0.7 | 150 | 页面转场、卡片入场 | 水流般的柔和弹性 |
| spring-fluid | 0.85 | 90 | 地图下钻、列表错峰 | 潮水般的缓慢起伏 |
| spring-bouncy | 0.6 | 200 | 解锁弹窗弹出 | 轻微嬉戏感 |
| spring-heavy | 0.4 | 300 | 等级徽章弹跳 | 沉重的冲击感 |

### 2.2 Tween 曲线

| 名称 | duration | easing | 适用场景 | 质感 |
|------|----------|--------|---------|------|
| tween-slow-ease | 400ms | fastOutSlowIn | 进度条、覆盖层 | 云朵般舒缓 |
| tween-ripple | 600ms | decelerate | 涟漪扩散、徽章闪耀 | 水滴入水 |
| tween-quick | 200ms | fastOutSlowIn | 状态切换、颜色过渡 | 干脆利落 |

### 2.3 时序节奏体系

**双轨时序：**

- **快速反馈层（40-200ms）:** 按钮按压、触感反馈、状态切换 — 无需等待，即时响应
- **流畅过渡层（350-1200ms）:** 页面转场、地图下钻、解锁动画 — 需要感知，不打断流程

**列表错峰规则：**

- 列表项入场间隔：60ms
- Grid 入场间隔：50ms（从中心向四周涟漪扩散）
- 分段式内容（标题+列表）：标题先入（0ms），列表延迟 100ms 后开始错峰

---

## 3. 地图页面动效

### 3.1 区域下钻 —「潜水」效果（500ms）

**分步：**

| 时间 | 动效 | 曲线 |
|------|------|------|
| 0-150ms | 当前区域向上淡出 + 轻微放大 (alpha 1→0, scale 1→1.05) | spring-fluid |
| 150-350ms | 子区域从下方滑入 (translateY 40dp→0, alpha 0→1) | spring-fluid |
| 350-500ms | 面包屑导航渐变更新 + 覆盖率面板数值平滑过渡 | tween-slow-ease |

**实现要点：**
- 使用 `AnimatedContent` 包裹地图内容区域，配合 `slideInVertically` + `fadeIn`
- 透明度变化使用 `animateFloatAsState` 配合 `decelerate` easing
- 缩放使用 `animateFloatAsState` 配合 `spring(dampingRatio=0.85, stiffness=90)`
- **错误处理:** 下钻数据未就绪时不下钻，保持当前视图。详见 11.3

### 3.2 足迹标记反馈 —「水滴落水」效果（600ms）

**涟漪扩散：**
- 用户选择足迹等级后，点击位置产生圆形波纹
- 半径从 0dp 扩展到 60dp，alpha 从 0.6 衰减到 0
- 曲线: `decelerate(600ms)`

**颜色渐变：**
- 区域颜色从深灰平滑过渡到对应的足迹颜色
- 颜色插值使用 `animateColorAsState` 配合 `spring(0.7, 150)`
- 颜色路径: `#2D2D44 → #FFA502（路过）→ #FF6B6B（短玩）→ #E94560（深度）`
- 有轻微的弹性过冲，模拟染料渗透的质感

### 3.3 覆盖率面板 —「水位上升」效果（400ms）

- **数字滚动:** `animateIntAsState` 从旧值翻转到新值，类似码表
- **进度条:** 从左到右平滑填充，终点弹性 overshoot（过冲 3% 后回弹到目标值）
- **百分比文字:** 进度条到达终点的瞬间，文字放大闪烁 (scale 1→1.2→1, 200ms)

### 3.4 面包屑导航 —「浮标漂移」效果（300ms）

- 新增路径项从左侧滑入 (translateX -20dp → 0)
- 当前层级高亮为红色并轻微放大 (scale 1→1.05)
- 分隔符 `>` 依次出现，间隔 200ms 延迟

### 3.5 右侧景点面板

- 使用 `AnimatedVisibility` 控制展开/收起
- 展开: `slideInHorizontally` + `fadeIn` (300ms)
- 收起: `slideOutHorizontally` + `fadeOut` (250ms)
- 切换按钮旋转动画: chevron 箭头 180° 旋转

---

## 4. 导航转场动效

### 4.1 底部 Tab 切换（300ms）

- **图标过渡:** 选中时图标弹跳缩放 (scale 1→1.3→1)，`spring(0.7, 150)`
- **指示器:** 选中指示条从当前 Tab 平滑滑动到目标 Tab，`spring(0.85, 200)`
- **页面内容:** 使用 `fadeThrough` 交叉淡变，不涉及方向性滑动

### 4.2 页面 Push/Pop 转场（350ms）

**Push（前进 — 潜入更深）：**
- 当前页: 向左滑出 30% + 缩小到 0.95x + alpha 渐出
- 新页面: 从右侧 40% 位置滑入到 0 + alpha 渐入
- 曲线: `spring(0.7, 150)`

**Pop（返回 — 浮出水面）：**
- 当前页: 向右完全滑出 (translateX 100%)
- 返回页: 从 0.95x 放大到 1x + alpha 渐入
- 曲线: `spring(0.7, 150)`

**实现方案：**
- 使用 Compose Navigation 的 `AnimatedNavHost` 和 `composable` 的 `enterTransition`/`exitTransition` 参数
- 定义 `slideIn + fadeIn` / `slideOut + fadeOut` 组合
- 使用 `targetStateBy` 判断 push 还是 pop 以决定动画方向

---

## 5. 成就系统动效

### 5.1 解锁弹窗 —「涟漪涌现」效果（1200ms）

**三段式动画：**

| 时间 | 动效 | 曲线 |
|------|------|------|
| 0-200ms | 屏幕遮罩淡入 (alpha 0→0.7) | tween-quick |
| 200-500ms | 光晕爆发：圆形波纹从中央扩散 (scale 0→3, alpha 0.6→0) + 径向渐变闪光 | tween-ripple |
| 500-800ms | 卡片从中心弹性弹出 (scale 0→1.05→1) | spring-bouncy |
| 800-1200ms | 内容逐行滑入：名称从上方滑落 / 山河值从右侧飘入 / 按钮渐显 | 每行间隔 100ms |

**实现要点：**
- 使用 `AnimatedVisibility` 或自定义 `Animatable` 实现多阶段动画
- 涟漪效果: 使用 `Canvas` 绘制圆形，radius 用 `Animatable` 驱动
- 卡片弹出: `animateFloatAsState(scale, spring(dampingRatio=0.6, stiffness=200))`
- 内容行: 使用 `Modifier.animateContentSize()` 或单独 `animate*AsState`

### 5.2 等级升级 —「破浪而出」效果（1500ms）

| 时间 | 动效 |
|------|------|
| 0-100ms | 全屏白色闪烁（像闪电劈开水面） |
| 100-500ms | Lv 标识从上方落下弹跳 3 次 |
| 500-900ms | 新称号文字从中间展开 (scaleX 0→1)，模拟卷轴展开 |
| 900-1200ms | 山河值数字码表滚动 |
| 1200-1500ms | 按钮渐显 |

### 5.3 徽章墙 —「涟漪网格」效果

**入场动画：**
- Grid 格子以涟漪方式从中心向四周依次入场
- 每个格子间隔 50ms
- 入场动画: scale 0.8→1 + alpha 0→1
- 曲线: `spring(0.7, 150)`

**Tab 切换：**
- 当前内容淡出 (alpha 1→0, 200ms)
- 新内容从下方滑入 (translateY 30→0, alpha 0→1, 每行间隔 80ms)
- Tab 指示器滑动 200ms

### 5.4 成就列表 —「流水线入场」

- 每个成就卡片: translateX(-30→0) + alpha(0→1) + 间隔 60ms
- 已解锁的卡片左侧有红色光晕渐显
- 进度条在卡片完全入场后开始从 0 填充到目标值
- 使用 `LazyColumn` 配合 `Modifier.animateItemPlacement()` 实现排序动画

### 5.5 省份征服列表

- 省份行从左侧滑入，按完成度排序依次入场（完成度高的先入场）
- 进度条在行完全可见后开始填充动画（使用 `LaunchedEffect` 延迟触发）
- 已完成省份有金色粒子轻微闪烁效果（`infiniteTransition` 驱动 alpha 脉冲）

---

## 6. 统计页面动效

### 6.1 数字码表动画

- 从 0 滚动到最终值，使用 `animateIntAsState` 整数插值
- 错峰开始：第一个数字立即开始，后续每个延迟 100ms
- 动态时长：小数字 (≤10) 用 300ms，大数字用 800ms
- 曲线: `decelerate(dynamicDuration)`

### 6.2 图表入场

**环形图/饼图：**
- 从 -90° 开始旋转填充，每个扇区依次出现，间隔 150ms
- 曲线: `decelerate(600ms)`
- 实现: `Canvas` + `Animatable<Float>` 驱动 sweepAngle

**柱状图：**
- 柱子从底部向上生长，按数值降序排列出场
- 柱子顶端数值在柱子到达顶部后弹出 (scale 0→1, 100ms)

### 6.3 景点列表

- 列表项从右侧滑入，间隔 50ms 错峰

---

## 7. 个人主页动效

### 7.1 头像入场（600ms）

- 头像从圆形剪影透明状态呈现
- 缩放 + 旋转: scale 0.8→1, rotate -5°→0, alpha 0→1
- 模拟拍立得照片显影效果

### 7.2 等级卡片（400ms）

- 从右侧滑入 (translateX 50→0)
- 经验条在卡片到位后从 0 填充
- Lv 徽章在经验条填充完成后弹跳一下 (scale 1→1.2→1)

### 7.3 按钮入场（500ms 延迟）

- 页面入场后 500ms 淡入
- alpha 0→1, translateY 10→0

---

## 8. 主题图鉴动效

### 8.1 卡片入场

- 图鉴卡片从左侧依次滑入，每张间隔 80ms
- 封面轻微放大 (scale 1→1.02) 再恢复

### 8.2 翻页效果（进入详情）

- 使用 scaleX 动画模拟 3D 翻转：scaleX 1→0→1
- 在 scaleX=0 的瞬间切换内容
- 耗时 400ms

### 8.3 收集完成反馈

- 进度达到 100% 时，卡片边框闪烁金色光晕 1 秒（alpha 脉冲 2 次）
- 最后 10% 进度条进入缓慢脉冲状态（color alpha 0.6→1 循环），提示用户即将完成

---

## 9. 微交互系统

### 9.1 按钮按压反馈

- 所有可点击元素按压时: scale 0.97
- 释放后: `spring` 弹回
- 触感: 80ms 压缩 + 200ms 回弹

### 9.2 加载状态

- 加载中: 内容保持 alpha 0.6 + translateY 4dp（轻微上移）
- 加载完成: `slideDown` 归位 + alpha 恢复 (200ms)

### 9.3 Toast/提示条

- 从顶部滑入: translateY -50dp → 0 (300ms)
- 停留 2 秒
- 滑出: translateY 0 → -50dp + alpha 1→0 (250ms)
- 带轻微的红色左边框指示

### 9.4 BottomSheet 动效

- 从底部弹性弹出: `spring(0.7, 300)`
- 背景遮罩同步淡入: alpha 0→0.7 (300ms)
- 三个足迹按钮依次滑入: 每个间隔 80ms
- 按钮选择瞬间: 背景色透明→填充色 (200ms)

### 9.5 Card 点击态

- 可点击卡片按压时: scale 0.98 + 亮度略微提升
- 释放后: `spring(0.7, 150)` 回弹
- 卡片间的间距在按压时略微缩小 (4dp→2dp) 以增强物理感

---

## 10. 无障碍与可达性（Accessibility）

### 10.1 减动画支持（Reduced Motion）

必须尊重系统级减动画偏好设置，对所有动效进行全局控制：

```kotlin
// 全局动画控制器
@Composable
fun rememberAnimationScale(): Float {
    val accessibilityManager = LocalAccessibilityManager.current
    // 系统开启了减动画 → 0f（无动画），否则 → 1f（完整动画）
    return if (accessibilityManager?.isReduceMotionEnabled == true) 0f else 1f
}
```

**减动画模式下的行为替换：**

| 完整动画 | 减动画替代 |
|---------|-----------|
| 弹性弹出 (spring) | 直接显示 (duration=1ms) |
| 涟漪扩散 (600ms) | 直接显示 |
| 错峰列表入场 (60ms间隔) | 所有项同时淡入 (100ms) |
| 翻页效果 (400ms) | 淡变过渡 (100ms) |
| 数值码表动画 | 直接显示最终值 |
| 无限脉冲闪烁 | 禁用 |

所有动画函数接收一个 `animationScale: Float` 参数，将 duration 乘以该值（0 则跳过动画直接跳到终态）。

### 10.2 动画中断与取消策略

**原则：用户操作始终优先于正在播放的动画。**

| 场景 | 行为 |
|------|------|
| 用户点击导航时动画正在播放 | 立即跳转到目标页面，动画强制完成 (snap to end)|
| 用户快速切换 Tab | 跳过当前 Tab 的入场动画，立即渲染目标 Tab 内容 |
| 列表滚动中 | 暂停所有列表项入场动画，滚动停止后继续 |
| 解锁弹窗播放中用户返回 | 弹窗立即关闭，遮罩 snap 消失 |
| 地图下钻中反向操作 | 取消当前动画，反向动画立即开始 |

**实现：** 使用 `Animatable.snapTo()` 方法在中断时将动画直接跳到终态。所有动画在 `LaunchedEffect` 中运行，通过取消协程来终止。

### 10.3 动画测试策略

- **Compose Animation Test:** 使用 `awaitAnimation()` 等待动画完成，验证终态值
- **视觉回归:** 使用截图测试捕获关键动画帧（入场前、入场后、完成时）
- **性能测试:** 通过 `FrameMetricsAggregator` 监控动画帧率，纳入 CI 门禁
- **无障碍测试:** 在系统开启 Reduced Motion 时验证所有页面可正常交互
- **平台差异:** `LocalAccessibilityManager` 是 Android 特有 API，iOS 端通过 `expect/actual` 接入平台原生减动画设置

---

## 11. 实现指导

### 11.1 Compose Multiplatform API 映射

| 效果 | API | 备注 |
|------|-----|------|
| 基本动画 | `animateFloatAsState`, `animateColorAsState`, `animateIntAsState` | 声明式，自动驱动 |
| 弹性动画 | `spring(dampingRatio, stiffness)` | Compose Native 支持 |
| 页面转场 | `AnimatedNavHost`, `enterTransition`, `exitTransition` | Navigation 2.8+ 支持 |
| 列表入场 | `AnimatedVisibility` + `Modifier.animateItemPlacement()` | 配合 LazyColumn |
| 涟漪绘制 | `Canvas` + `Animatable` | 自定义绘制 |
| 无限循环 | `rememberInfiniteTransition` | 脉冲/闪烁效果 |
| 手势驱动 | `Modifier.pointerInput` + `animateDecay` | 拖拽/滑动阻尼 |

### 11.2 性能预算（Performance Budget）

**帧率目标：**
- 所有动画必须维持 60fps（16.7ms/帧）
- 列表滚动 + 动画并发时：维持 55fps 以上
- 性能未达标时：跳过非关键动画（列表入场、装饰性脉冲）

**预算限制：**
- Canvas 绘制的粒子/涟漪效果 ≤ 20 个/帧
- 同时播放的独立动画 ≤ 8 个（超出则进入降级队列）
- 无限循环动画（脉冲/闪烁）≤ 3 个同时运行

**性能监测：**
- 列表滚动时自动暂停入场动画（`LaunchedEffect` 检查 `isScrollInProgress`）
- `Modifier.graphicsLayer` 优先于 `Modifier.offset/alpha/scale`（避免重组）
- 颜色插值使用 `animateColorAsState` 而非自定义 interpolator
- 关键动画路径（地图下钻、解锁弹窗）纳入性能回归测试

### 11.3 错误状态与动画失败处理

**数据加载失败：**
- 地图下钻时若数据未就绪，内容保持当前状态不下钻，不显示空动画
- 解锁弹窗涉及异步检查时，先显示加载态（无动画），数据就绪后再播放弹窗
- 数据获取出错时，显示错误提示（使用 Toast 顶部滑入动画）

**动画中途中断：**
- 用户导航离开 → 协程取消，`Animatable.snapTo()` 将动画跳到终态
- 用户快速操作 → 放弃等待中的动画，执行最新操作
- 详情见 10.2 中断策略

**边界情况：**
- 页面首次进入时，跳过入场动画直接显示（减少首帧等待时间）
- 恢复后台状态时，内容直接显示终态不再播放入场动画
- 冷启动时 App 初始渲染无动画（首帧直接显示）

### 11.4 双平台一致性

- Compose Multiplatform 的 `spring`, `tween`, `decelerate` 等在 Android 和 iOS 上行为一致
- 平台差异点：iOS 的 scroll bounce 行为不同，Pull-to-refresh 需平台适配
- Canvas 绘制 API 在双平台行为一致

---

## 12. 动效检查清单

每个页面交付前检查：

- [ ] 页面转场动画已配置 (enter/exit transition)
- [ ] 列表项错峰入场已实现
- [ ] 按钮按压缩放已添加
- [ ] 加载/空状态有过渡动画
- [ ] 数据更新有过渡（非突变）
- [ ] 弹窗/Sheet 有弹性出入动画
- [ ] 颜色/进度变化有插值动画
- [ ] 导航返回手势有对应动画
