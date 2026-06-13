# MapChina

**用脚步丈量中国**

<p align="center">
  <img src="logo/logo_geo_c_transparent.png" width="120" alt="MapChina Logo">
</p>

[English](README_EN.md)

---

## 应用截图

<p align="center">
  <img src="docs/screenshots/map_screen.png" width="180" alt="足迹地图">
  <img src="docs/screenshots/attractions_screen.png" width="180" alt="景点">
  <img src="docs/screenshots/achievement_screen.png" width="180" alt="徽章墙">
  <img src="docs/screenshots/carving_screen.png" width="180" alt="碑刻">
  <img src="docs/screenshots/community_screen.png" width="180" alt="社区">
  <img src="docs/screenshots/profile_screen.png" width="180" alt="我的">
</p>

## 功能特性

### 交互式足迹地图

- 基于 Compose Canvas 自绘中国地图，无需第三方地图 SDK
- 查看全部 34 个省级行政区，点击即可查看访问状态
- 6 种地图主题：经典、水墨、古舆图、宣纸、星夜、山水
- 地图上渲染南海九段线
- 区域标注与地图分享导出

### 成就系统

- 征服省份：访问更多省份，点亮中国版图
- 徽章墙：收集和展示各分类徽章
- 图鉴：按主题浏览成就（名山、遗产、海岸等）
- 可视化进度追踪与解锁奖励

### 景点发现

- 浏览全国景点，按等级分类（5A、4A 等）
- 标记已访问景点，追踪你的收藏
- 添加自定义景点并定位
- 带有关联区域的景点详情卡片

### 碑刻

- 使用 Android Ink API 在区域石壁上留下墨迹题刻
- 手写风格笔画，支持压感
- 按区域或景点浏览碑刻

### 旅行日志

- 创建与特定区域或景点关联的日志条目
- 为条目添加照片
- 浏览和回顾过往旅途

### 社区

- 与其他用户分享旅行故事
- 浏览和互动社区帖子
- 通过他人的足迹发现新目的地

### 个人中心

- 查看旅行统计与足迹总览
- 管理已访问区域、景点和碑刻
- 追踪整体探索进度
- 关键交互触感反馈

## 技术栈

| 层 | 技术 |
|---|---|
| UI 框架 | [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) + [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) |
| 导航 | [Navigation 3](https://developer.android.com/develop/ui/compose/navigation-3) |
| 依赖注入 | [Koin](https://insert-koin.io/) |
| 网络请求 | [Ktor Client](https://ktor.io/docs/client.html) |
| 本地存储 | [SQLDelight](https://cashapp.github.io/sqldelight/) |
| 地图渲染 | Compose Canvas（自研 GeoJSON 渲染器） |
| 墨迹笔画 | [Android Ink API](https://developer.android.com/develop/ui/compose/ink) |
| 图片加载 | [Coil 3](https://coil-kt.github.io/coil/) |
| 后端 | [Ktor Server](https://ktor.io/docs/server.html) + [JetBrains Exposed](https://github.com/JetBrains/Exposed) |
| 数据库 (服务端) | PostgreSQL + HikariCP |
| 认证 | JWT + 限流 |

## 项目结构

```
MapChina/
├── shared/                     # 共享 Kotlin Multiplatform 模块
│   └── src/
│       ├── commonMain/         # 共享业务逻辑和 UI
│       │   └── com/mapchina/
│       │       ├── data/       # 仓库、远程数据、本地数据库模式
│       │       ├── di/         # Koin 模块定义
│       │       ├── domain/     # 领域模型和服务
│       │       ├── location/   # 定位抽象层
│       │       ├── map/        # Canvas 地图渲染、主题、投影
│       │       ├── platform/   # 平台抽象层
│       │       ├── sync/       # 数据同步逻辑
│       │       └── ui/         # Compose UI 界面和主题
│       │           ├── achievement/  # 徽章、图鉴、征服省份
│       │           ├── attraction/   # 景点列表与详情
│       │           ├── carving/      # 碑刻（墨迹笔画）
│       │           ├── community/    # 社区动态与帖子
│       │           ├── journal/      # 旅行日志
│       │           ├── map/          # 地图界面与区域详情
│       │           ├── profile/      # 个人中心与设置
│       │           └── stats/        # 旅行统计总览
│       ├── androidMain/        # Android 平台实现
│       └── iosMain/            # iOS 平台实现
├── androidApp/                 # Android 应用入口
├── iosApp/                     # iOS 应用入口
└── server/                     # Ktor 后端服务
```

## 快速开始

### 前置条件

- JDK 17+
- Android Studio（最新稳定版）
- Xcode 16+（iOS 构建需要）
- PostgreSQL（后端服务需要）

### 构建与运行

**Android 端：**

```bash
./gradlew :androidApp:assembleDebug
```

在 Android Studio 中打开项目，运行 `androidApp` 配置即可。

**iOS 端：**

在 Xcode 中打开 `iosApp/iosApp.xcworkspace`，在模拟器或真机上运行。

**后端服务：**

```bash
./gradlew :server:run
```

在服务端的 `application.conf` 中配置 PostgreSQL 连接信息。

## 架构

应用遵循整洁架构模式，关注点分离明确：

```
UI 层 (Compose) → 视图模型 → 领域服务 → 仓库 → 数据源
```

- **自研地图引擎**：中国地图完全使用 Compose Canvas 渲染，基于 GeoJSON 边界数据、墨卡托投影和空间索引——无需依赖第三方地图 SDK
- **平台抽象**：位置提供者、平台服务通过 expect/actual 声明实现
- **不可变数据模型**：领域模型使用 Kotlin data class 的 copy 语义
- **SQLDelight 迁移**：数据库模式变更通过编号的 `.sqm` 文件管理

## 许可证

私有项目，保留所有权利。
