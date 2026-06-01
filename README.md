# MapChina

*Measure China with Your Footsteps*

[中文文档](#中文文档)

---

## Screenshots

<p align="center">
  <img src="docs/screenshots/map_screen.png" width="240" alt="Footprint Map">
  <img src="docs/screenshots/attractions_screen.png" width="240" alt="Attractions">
  <img src="docs/screenshots/achievement_screen.png" width="240" alt="Achievements">
  <img src="docs/screenshots/profile_screen.png" width="240" alt="Profile">
</p>

## Features

### Interactive Footprint Map

- View all 34 provincial-level regions on a China map (AMap on Android, MapKit on iOS)
- Tap a region to see your visit status — unvisited, short visit, or deep exploration
- Automatic region boundary rendering from GeoJSON data

### Achievement System

- Province Conquest: unlock badges as you visit more provinces
- Badge Wall: collect and display your earned badges
- Progress tracking with visual indicators

### Attraction Discovery

- Browse attractions across China, categorized by level (5A, 4A, etc.)
- Mark attractions as visited and track your collection
- Detailed attraction cards with region association

### Travel Journal

- Create journal entries tied to specific regions
- Attach photos to your entries
- Browse and revisit past trips

### Personal Profile

- View your travel statistics
- Manage your visited regions and attractions
- Track overall exploration progress

## Tech Stack

| Layer | Technology |
|---|---|
| UI Framework | [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) + [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) |
| Navigation | Jetbrains Navigation Compose |
| DI | [Koin](https://insert-koin.io/) |
| Networking | [Ktor Client](https://ktor.io/docs/client.html) |
| Local Storage | [SQLDelight](https://cashapp.github.io/sqldelight/) |
| Map (Android) | [AMap 3D SDK](https://lbs.amap.com/api/android-sdk/summary) |
| Map (iOS) | Apple MapKit |
| Image Loading | [Coil 3](https://coil-kt.github.io/coil/) |
| Backend | [Ktor Server](https://ktor.io/docs/server.html) + [JetBrains Exposed](https://github.com/JetBrains/Exposed) |
| Database (Server) | PostgreSQL + HikariCP |
| Auth | JWT + Rate Limiting |

## Project Structure

```
MapChina/
├── shared/                     # Shared Kotlin Multiplatform module
│   └── src/
│       ├── commonMain/         # Shared business logic & UI
│       │   └── com/mapchina/
│       │       ├── data/       # Repositories, remote data, local schemas
│       │       ├── di/         # Koin module definitions
│       │       ├── domain/     # Domain models & services
│       │       ├── location/   # Location abstraction
│       │       ├── map/        # Map controller interface
│       │       ├── platform/   # Platform abstractions
│       │       ├── sync/       # Data sync logic
│       │       └── ui/         # Compose UI screens & theme
│       ├── androidMain/        # Android-specific implementations
│       └── iosMain/            # iOS-specific implementations
├── androidApp/                 # Android application entry point
├── iosApp/                     # iOS application entry point
└── server/                     # Ktor backend server
```

## Getting Started

### Prerequisites

- JDK 17+
- Android Studio (latest stable)
- Xcode 15+ (for iOS builds)
- PostgreSQL (for the backend server)

### Build & Run

**Android:**

```bash
./gradlew :androidApp:assembleDebug
```

Open the project in Android Studio and run the `androidApp` configuration.

**iOS:**

Open `iosApp/iosApp.xcworkspace` in Xcode and run on a simulator or device.

**Server:**

```bash
./gradlew :server:run
```

Configure your PostgreSQL connection in the server's `application.conf`.

## Architecture

The app follows a clean architecture pattern with clear separation of concerns:

```
UI Layer (Compose) → ViewModel → Domain Services → Repositories → Data Sources
```

- **Platform abstractions**: Map controller, location provider, and platform services are defined as expect/actual declarations
- **Immutable data models**: Domain models use Kotlin data classes with copy semantics
- **SQLDelight migrations**: Schema changes are managed through numbered `.sqm` files

## License

Private project. All rights reserved.

---
---

<a id="中文文档"></a>

# MapChina

**用脚步丈量中国**

[English](#mapchina)

---

## 应用截图

<p align="center">
  <img src="docs/screenshots/map_screen.png" width="240" alt="足迹地图">
  <img src="docs/screenshots/attractions_screen.png" width="240" alt="景点">
  <img src="docs/screenshots/achievement_screen.png" width="240" alt="成就">
  <img src="docs/screenshots/profile_screen.png" width="240" alt="我的">
</p>

## 功能特性

### 交互式足迹地图

- 在中国地图上查看全部 34 个省级行政区（Android 使用高德地图，iOS 使用 MapKit）
- 点击区域查看访问状态 — 未访问、短暂到访、深度探索
- 基于 GeoJSON 数据自动渲染区域边界

### 成就系统

- 征服省份：访问更多省份即可解锁徽章
- 徽章墙：收集和展示你获得的徽章
- 可视化进度追踪

### 景点发现

- 浏览全国景点，按等级分类（5A、4A 等）
- 标记已访问景点，追踪你的收藏
- 带有关联区域的景点详情卡片

### 旅行日志

- 创建与特定区域关联的日志条目
- 为条目添加照片
- 浏览和回顾过往旅途

### 个人中心

- 查看旅行统计数据
- 管理已访问区域和景点
- 追踪整体探索进度

## 技术栈

| 层 | 技术 |
|---|---|
| UI 框架 | [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) + [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) |
| 导航 | Jetbrains Navigation Compose |
| 依赖注入 | [Koin](https://insert-koin.io/) |
| 网络请求 | [Ktor Client](https://ktor.io/docs/client.html) |
| 本地存储 | [SQLDelight](https://cashapp.github.io/sqldelight/) |
| 地图 (Android) | [高德 3D 地图 SDK](https://lbs.amap.com/api/android-sdk/summary) |
| 地图 (iOS) | Apple MapKit |
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
│       │       ├── map/        # 地图控制器接口
│       │       ├── platform/   # 平台抽象层
│       │       ├── sync/       # 数据同步逻辑
│       │       └── ui/         # Compose UI 界面和主题
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
- Xcode 15+（iOS 构建需要）
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

- **平台抽象**：地图控制器、位置提供者、平台服务通过 expect/actual 声明实现
- **不可变数据模型**：领域模型使用 Kotlin data class 的 copy 语义
- **SQLDelight 迁移**：数据库模式变更通过编号的 `.sqm` 文件管理

## 许可证

私有项目，保留所有权利。
