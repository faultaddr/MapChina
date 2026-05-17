# 中国足迹地图 App 设计文档

## 概述

一款跨平台移动应用，让用户标注到过的中国城市（省→市→区三级），直观感受足迹覆盖情况，同时以景点为锚点帮助用户发现值得去的地方。

## 核心决策

| 维度 | 选择 |
|------|------|
| 平台 | KMP Multiplatform |
| UI 框架 | Compose Multiplatform |
| 标注方式 | 手动 + GPS 辅助 |
| 景点范围 | V1 覆盖 5A/4A 核心景点（约 1000+），后续扩展 |
| 数据存储 | 账号 + 云同步（离线优先） |
| 社交属性 | 纯个人使用 |
| 地图视图 | 双视图切换（真实地图 + 色块填色） |
| 足迹粒度 | 分级标记（路过/短玩/深度） |
| 商业模式 | 免费 + 高级功能 |
| 后端 | Ktor 自建（JWT Auth + API + PostgreSQL） |

## 架构方案

**选定方案：KMP + Compose Multiplatform**

KMP 共享业务逻辑和 UI 层，地图组件通过 Expect/Actual 封装各平台原生 SDK（iOS MapKit / Android 高德地图），色块填色视图使用 Compose Canvas 统一绘制。

### 四层架构

```
┌─────────────────────────────────────────────┐
│  UI 层 — Compose Multiplatform              │
│  足迹地图页 | 景点列表页 | 统计页 | 设置页   │
│  地图组件: Expect/Actual 封装原生 SDK        │
└──────────────────────┬──────────────────────┘
                       ↕
┌──────────────────────┴──────────────────────┐
│  ViewModel 层 — KMP 共享                    │
│  MapViewModel | AttractionViewModel         │
│  StatsViewModel | ProfileViewModel          │
└──────────────────────┬──────────────────────┘
                       ↕
┌──────────────────────┴──────────────────────┐
│  Domain 层 — KMP 共享                       │
│  FootprintService | AttractionService       │
│  AuthService | SyncService                  │
└──────────────────────┬──────────────────────┘
                       ↕
┌──────────────────────┴──────────────────────┐
│  Data 层 — KMP 共享                         │
│  本地数据库 (SQLDelight) | 远程 API (Ktor)  │
│  GPS 定位 | Repository 模式封装              │
└─────────────────────────────────────────────┘
```

## 数据模型

### Region（区域）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 区域唯一标识 |
| name | String | 区域名称 |
| level | Enum | 省 / 市 / 区 |
| parentId | String? | 上级区域 id |
| boundary | GeoJSON | 地理边界数据 |

三级树状结构，全国约 3400+ 个区级行政区。

### Attraction（景点）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 景点唯一标识 |
| name | String | 景点名称 |
| regionId | String | 所属区域 id |
| level | Enum | 5A / 4A |
| coordinate | LatLng | 经纬度坐标 |
| description | String? | 简介 |

V1 覆盖约 1000+ 个 5A/4A 景点。

### Footprint（足迹记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | String | 用户 id |
| regionId | String | 区域 id |
| level | Enum | 路过 / 短玩 / 深度 |
| timestamp | Instant | 标记时间 |

### AttractionVisit（景点访问记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | String | 用户 id |
| attractionId | String | 景点 id |
| level | Enum | 路过 / 短玩 / 深度 |
| timestamp | Instant | 访问时间 |
| note | String? | 可选笔记（高级功能） |

### User（用户）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 用户唯一标识 |
| nickname | String | 昵称 |
| avatar | String? | 头像 URL |
| createdAt | Instant | 注册时间 |

### 足迹分级定义

- **路过**：短暂停留，如转车、经停
- **短玩**：1-2 天游玩
- **深度**：3 天以上深度体验

升级操作不可降级（深度 > 短玩 > 路过）。

### Footprint 与 AttractionVisit 的关系

标记 AttractionVisit 时，必须自动 upsert 对应区域的 Footprint：
- 景点所属的区级 Footprint 自动设置为不低于 AttractionVisit 的级别
- 区级 Footprint 变更时，其父级市、省 Footprint 自动设为"至少路过"
- 例如：用户标记九寨沟为"深度"，则九寨沟所在的阿坝州 Footprint 自动升级为"深度"，四川省 Footprint 自动设为"路过"（如果尚未标记）

## 数据合规

### 地图合规

- 使用 GCJ-02 坐标系（国测局坐标），符合《测绘法》要求
- 所有地图底图使用具备审图号的合规地图服务
- 区域边界数据来源：民政部行政区划数据 + 开放街景（OSM）补充，确认授权许可

### GeoJSON 数据策略

采用分层精度策略控制包体积：

| 层级 | 精度 | 预估大小 | 加载方式 |
|------|------|----------|----------|
| 省级 | 高度简化 | ~2 MB | 预置打包 |
| 市级 | 中等简化 | ~15 MB | 预置打包 |
| 区级 | 完整精度 | ~80 MB | 按省按需下载 |

目标安装包大小 < 50 MB（含省级 + 市级边界 + 景点数据），区级数据首次进入该省时下载并缓存本地。

## UI 导航结构

### 底部 Tab 导航（4 个主 Tab）

| Tab | 图标 | 功能 |
|-----|------|------|
| 足迹地图 | 🗺️ | 核心页面，双视图切换，逐级钻入 |
| 景点 | 🏛️ | 按区域浏览景点，搜索，详情，标记已访问 |
| 统计 | 📊 | 足迹覆盖率，景点打卡率（详细统计为付费功能） |
| 我的 | ⚙️ | 账号管理，云同步，导出足迹图（付费），设置 |

### 钻入式导航

全国总览 → 省份视图 → 城市视图 → 区县视图 → 景点列表

每一级都可标记足迹，地图/色块视图随层级自动聚焦。

## 地图交互设计

### 双视图切换

**真实地图视图：**
- 地理底图 + 区域半透明覆盖
- 已去区域用暖色覆盖，未去区域透明或淡色
- 景点以锚点形式显示在地图上
- 缩放到城市级别后显示景点锚点

**色块填色视图：**
- 深色背景 + 简化色块
- 分级色彩：深度 = 深红，短玩 = 红，路过 = 橙，未去 = 暗灰
- 左上角显示覆盖率统计（8/34 省 · 23/333 市）
- 全局概览一目了然

### 地图组件接口

```kotlin
interface MapController {
    fun addOverlay(region: Region, style: OverlayStyle)
    fun removeOverlay(regionId: String)
    fun addMarker(attraction: Attraction, visited: Boolean)
    fun removeMarker(attractionId: String)
    fun setCamera(region: Region, animated: Boolean)
    fun setOnRegionTapListener(listener: (Region) -> Unit)
    fun setOnMarkerTapListener(listener: (Attraction) -> Unit)
    fun setZoomLevel(level: MapZoomLevel)
    fun getZoomLevel(): MapZoomLevel
}

enum class MapZoomLevel { NATIONAL, PROVINCIAL, CITY, DISTRICT }
```

共享代码通过 `MapController` 接口操作地图，各平台 Actual 实现负责 SDK 对接。手势缩放和点击事件由共享代码处理层级逻辑，平台代码仅负责渲染和原始事件上报。

### 交互流程

1. **双视图切换** — 右上角切换按钮
2. **点击区域** — 弹出底部 Sheet，显示区域名称、足迹状态、景点数量，点击后钻入该区域
3. **标记足迹** — 在底部 Sheet 中选择级别（路过/短玩/深度），GPS 辅助确认位置
4. **层级切换** — 点击区域触发钻入（全国→省→市→区），面包屑导航 + 返回按钮回到上级；双指缩放在当前层级内自由缩放，超过阈值时提示"点击进入 XX 视图"
5. **景点锚点** — 进入城市级别后自动显示
6. **长按锚点** — 弹出景点详情卡片，可标记已访问

### 层级缩放阈值

| 层级 | 缩放范围 | 触发行为 |
|------|----------|----------|
| 全国 | zoom < 6 | 显示省级色块/覆盖 |
| 省份 | zoom 6-9 | 点击省份钻入，显示市级 |
| 城市 | zoom 9-12 | 点击城市钻入，显示区级 + 景点锚点 |
| 区县 | zoom 12+ | 显示景点锚点详情 |

## 技术选型

| 领域 | 技术 | 说明 |
|------|------|------|
| UI 框架 | Compose Multiplatform | 一套 UI 代码双端复用 |
| 地图组件 | Expect/Actual 封装 | iOS MapKit / Android 高德 |
| 本地数据库 | SQLDelight | 类型安全、KMP 原生支持 |
| 网络请求 | Ktor Client | Kotlin 生态、多平台支持 |
| 后端 | Ktor Server | JWT Auth + REST API + PostgreSQL |
| 依赖注入 | Koin | 轻量、KMP 友好 |
| 色块绘制 | Compose Canvas | 统一跨平台自定义绘制 |
| 区域数据 | GeoJSON 预置 | 全国省市县边界 + 景点坐标打包 |

## 数据同步策略

离线优先，联网后自动同步：

1. **本地写入优先** — 所有操作先写入本地数据库
2. **后台推送云端** — 联网后异步同步到 Ktor 后端
3. **冲突合并** — 同一区域的足迹冲突时，始终保留更高级别（深度 > 短玩 > 路过），不考虑时间戳；其他数据（用户信息等）采用 last-write-wins
4. **拉取远端更新** — 同步其他设备的变更

### 离线行为

- 所有本地数据完全可用，无需网络即可浏览和标记
- 同步队列持久化，跨 App 重启保留，采用指数退避重试
- 首次启动需网络完成注册/登录
- 同步状态 UI 指示（顶部同步图标：已同步 / 同步中 / 离线）
- 同步失败时显示 SnackBar 提示，可手动重试

## 认证方案

### V1 认证方式

- **手机号 + 短信验证码**（面向中国市场）
- 备选：邮箱 + 密码

### JWT 生命周期

| Token | TTL | 存储 |
|-------|-----|------|
| Access Token | 30 分钟 | 内存 |
| Refresh Token | 30 天 | iOS Keychain / Android EncryptedSharedPreferences |

Refresh Token 轮换策略：每次刷新时颁发新 Refresh Token，旧 Token 失效。

## GPS 辅助标注

- 打开标记 Sheet 时，如果 GPS 可用，高亮用户当前所在区域并显示"标记当前位置"快捷操作
- GPS 为可选辅助，用户可随时手动选择任意区域
- 首次使用 GPS 时请求定位权限，拒绝后不影响手动标注
- 定位权限使用说明：用于辅助标记您当前所在的城市/区域

## 色块视图点击检测

3400+ 个区级多边形需要高效点击检测：
- 使用 R-tree 空间索引加速 point-in-polygon 查询
- 第一阶段用边界框快速筛选候选区域，第二阶段精确多边形验证
- 索引数据随 GeoJSON 预置构建，无需运行时计算

## 无障碍

- 足迹分级除颜色外，增加纹理/图案区分（斜线、点阵、网格）
- 色块区域提供语义化标签供屏幕阅读器朗读
- 地图导航提供语音播报当前区域信息

## 错误处理与加载状态

| 场景 | 处理方式 |
|------|----------|
| GeoJSON 加载失败 | 全屏错误页 + 重试按钮 |
| 网络请求失败 | SnackBar 提示 + 自动重试 |
| 同步冲突 | 静默合并（高级别优先），不阻塞用户 |
| GPS 不可用 | 隐藏"标记当前位置"，仅显示手动选择 |
| 空状态 | 引导提示（如"还没有足迹，点击地图开始标记"） |
| 数据加载中 | 骨架屏 / 加载指示器 |

## API 端点

### 认证

| 端点 | 方法 | 说明 |
|------|------|------|
| `/auth/send-code` | POST | 发送短信验证码 |
| `/auth/login` | POST | 验证码登录，返回 JWT |
| `/auth/refresh` | POST | 刷新 Access Token |

### 数据

| 端点 | 方法 | 说明 |
|------|------|------|
| `/regions?parentId={id}&level={level}` | GET | 查询区域列表（分页） |
| `/regions/{id}/boundary` | GET | 获取区域 GeoJSON 边界 |
| `/regions/{id}/attractions` | GET | 获取区域下景点列表（分页） |
| `/attractions/{id}` | GET | 景点详情 |

### 足迹

| 端点 | 方法 | 说明 |
|------|------|------|
| `/footprints` | POST | 创建/更新足迹记录 |
| `/footprints?regionId={id}` | GET | 查询足迹 |
| `/attraction-visits` | POST | 创建/更新景点访问记录 |
| `/attraction-visits?attractionId={id}` | GET | 查询景点访问 |

### 同步

| 端点 | 方法 | 说明 |
|------|------|------|
| `/sync/delta?since={timestamp}` | GET | 增量同步，获取指定时间后的变更 |
| `/sync/push` | POST | 批量推送本地变更 |

### 通用

- 分页：`?page=1&size=20`，响应包含 `total`、`hasMore`
- 错误响应：`{ "code": "ERROR_CODE", "message": "描述" }`
- 频率限制：认证接口 10 次/分钟，数据接口 100 次/分钟

## 商业模式

### 免费功能

- 全国省市区三级浏览
- 双视图切换（地图/色块）
- 手动 + GPS 辅助标记足迹
- 足迹分级（路过/短玩/深度）
- 5A/4A 景点浏览和打卡
- 基础统计（覆盖率）
- 账号注册与云同步

### 高级功能（付费）

- 导出高清足迹图（适合分享）
- 详细统计报告（时间线、趋势）
- 足迹笔记和照片附件
- 自定义足迹图主题配色
- 3A 及以下景点扩展包
- 成就系统（解锁徽章）

## V1 MVP 范围

### 包含

1. **数据基础** — 全国省市县三级行政区 GeoJSON + 5A/4A 景点数据入库
2. **Ktor 后端** — Auth（JWT）+ 足迹/景点 API + 数据同步接口 + PostgreSQL
3. **地图核心** — 双视图切换 + 省→市→区钻入 + 景点锚点 + 点击标记足迹
4. **景点浏览** — 按区域查看景点列表 + 搜索 + 景点详情 + 标记已访问
5. **账号与同步** — 注册/登录 + 离线优先 + 后台云同步
6. **基础统计** — 省市区覆盖率 + 景点打卡率

### 不包含（后续迭代）

- 足迹笔记和照片附件
- 成就系统
- 足迹图导出
- 3A 及以下景点
- 自定义主题配色
- 社交分享功能
