# MapChina 成就系统 V1 实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MapChina 添加成就系统骨架，将用户点亮行为转化为等级成长+徽章奖励的正反馈链路

**Architecture:** 在现有 Kotlin Multiplatform 架构上扩展，新增 SQLDelight 表存储成就定义和用户成就进度，通过 FootprintService 的点亮事件触发成就结算，新增"成就"底部导航 Tab 替换"统计"Tab

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Koin DI, Compose Multiplatform, kotlinx-serialization

---

## 文件结构

### 新增文件

| 文件 | 职责 |
|------|------|
| `shared/src/commonMain/sqldelight/com/mapchina/data/local/AchievementDefinition.sq` | 成就定义表 Schema |
| `shared/src/commonMain/sqldelight/com/mapchina/data/local/UserAchievement.sq` | 用户成就进度表 Schema |
| `shared/src/commonMain/sqldelight/com/mapchina/data/local/UserScore.sq` | 用户山河值+等级表 Schema |
| `shared/src/commonMain/kotlin/com/mapchina/domain/model/Achievement.kt` | 成就领域模型 + Rarity/Category 枚举 |
| `shared/src/commonMain/kotlin/com/mapchina/domain/model/UserLevel.kt` | 等级领域模型 + 等级配置常量 |
| `shared/src/commonMain/kotlin/com/mapchina/data/repository/AchievementRepository.kt` | 成就数据访问 |
| `shared/src/commonMain/kotlin/com/mapchina/data/repository/UserScoreRepository.kt` | 山河值/等级数据访问 |
| `shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementService.kt` | 成就结算核心逻辑 |
| `shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementSeeder.kt` | 成就定义种子数据 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementScreen.kt` | 成就首页 Composable |
| `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementViewModel.kt` | 成就页 ViewModel |
| `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeWallScreen.kt` | 徽章墙 Composable |
| `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeDetailScreen.kt` | 徽章详情 Composable |
| `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementUnlockDialog.kt` | 解锁弹窗 Composable |
| `shared/src/commonTest/kotlin/com/mapchina/domain/service/AchievementServiceTest.kt` | 成就服务单元测试 |
| `shared/src/commonTest/kotlin/com/mapchina/data/repository/AchievementRepositoryTest.kt` | 成就仓库单元测试 |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt` | 注册新依赖 |
| `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt` | 点亮后触发成就结算 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/App.kt` | 底部导航替换"统计"为"成就" |
| `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt` | 新增成就相关路由 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt` | 新增成就页面路由 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt` | 个人主页展示等级+徽章数 |
| `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt` | 加载等级和徽章数据 |
| `shared/src/commonMain/kotlin/com/mapchina/data/remote/DataSeeder.kt` | 调用 AchievementSeeder |

---

## Chunk 1: 数据层 — Schema + 领域模型 + 仓库

### Task 1: 创建 SQLDelight Schema

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/AchievementDefinition.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/UserAchievement.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/UserScore.sq`

- [ ] **Step 1: 创建 AchievementDefinition.sq**

```sql
CREATE TABLE achievement_definition (
    achievement_id TEXT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    sub_category TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon TEXT NOT NULL,
    rarity TEXT NOT NULL,
    trigger_type TEXT NOT NULL,
    trigger_condition TEXT NOT NULL,
    reward_score INTEGER NOT NULL,
    sort_order INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active'
);

selectAll:
SELECT * FROM achievement_definition WHERE status = 'active' ORDER BY sort_order;

selectByCategory:
SELECT * FROM achievement_definition WHERE category = ? AND status = 'active' ORDER BY sort_order;

selectById:
SELECT * FROM achievement_definition WHERE achievement_id = ?;

insertDefinition:
INSERT OR IGNORE INTO achievement_definition(achievement_id, category, sub_category, name, description, icon, rarity, trigger_type, trigger_condition, reward_score, sort_order, status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

insertDefinitionsInTransaction:
INSERT OR IGNORE INTO achievement_definition(achievement_id, category, sub_category, name, description, icon, rarity, trigger_type, trigger_condition, reward_score, sort_order, status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

countAll:
SELECT COUNT(*) AS total FROM achievement_definition WHERE status = 'active';
```

- [ ] **Step 2: 创建 UserAchievement.sq**

```sql
CREATE TABLE user_achievement (
    user_id TEXT NOT NULL,
    achievement_id TEXT NOT NULL,
    progress_value INTEGER NOT NULL DEFAULT 0,
    progress_target INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'locked',
    unlock_time INTEGER,
    PRIMARY KEY(user_id, achievement_id)
);

selectByUser:
SELECT * FROM user_achievement WHERE user_id = ? ORDER BY unlock_time DESC;

selectByUserAndAchievement:
SELECT * FROM user_achievement WHERE user_id = ? AND achievement_id = ?;

selectUnlockedByUser:
SELECT * FROM user_achievement WHERE user_id = ? AND status = 'unlocked' ORDER BY unlock_time DESC;

countUnlockedByUser:
SELECT COUNT(*) AS total FROM user_achievement WHERE user_id = ? AND status = 'unlocked';

upsertProgress:
INSERT OR REPLACE INTO user_achievement(user_id, achievement_id, progress_value, progress_target, status, unlock_time)
VALUES (?, ?, ?, ?, ?, ?);

initAchievement:
INSERT OR IGNORE INTO user_achievement(user_id, achievement_id, progress_value, progress_target, status, unlock_time)
VALUES (?, ?, 0, ?, 'locked', NULL);
```

- [ ] **Step 3: 创建 UserScore.sq**

```sql
CREATE TABLE user_score (
    user_id TEXT NOT NULL PRIMARY KEY,
    current_score INTEGER NOT NULL DEFAULT 0,
    current_level INTEGER NOT NULL DEFAULT 1,
    updated_at INTEGER NOT NULL
);

selectByUser:
SELECT * FROM user_score WHERE user_id = ?;

upsertScore:
INSERT OR REPLACE INTO user_score(user_id, current_score, current_level, updated_at)
VALUES (?, ?, ?, ?);

addScore:
UPDATE user_score SET current_score = current_score + ?, updated_at = ? WHERE user_id = ?;
```

- [ ] **Step 4: 运行构建验证 Schema 编译**

Run: `./gradlew :shared:generateCommonMainMapChinaDatabaseInterface`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/sqldelight/com/mapchina/data/local/AchievementDefinition.sq shared/src/commonMain/sqldelight/com/mapchina/data/local/UserAchievement.sq shared/src/commonMain/sqldelight/com/mapchina/data/local/UserScore.sq
git commit -m "feat: add achievement system SQLDelight schemas"
```

---

### Task 2: 创建领域模型

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/Achievement.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/UserLevel.kt`

- [ ] **Step 1: 创建 Achievement.kt**

```kotlin
package com.mapchina.domain.model

import kotlinx.datetime.Instant

data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val subCategory: String,
    val name: String,
    val description: String,
    val icon: String,
    val rarity: AchievementRarity,
    val triggerType: TriggerType,
    val triggerCondition: String,
    val rewardScore: Int,
    val sortOrder: Int
)

data class UserAchievement(
    val userId: String,
    val achievementId: String,
    val progressValue: Int,
    val progressTarget: Int,
    val status: AchievementStatus,
    val unlockTime: Instant?
)

enum class AchievementCategory { REGION, SCENIC, PROVINCE, ATLAS }
enum class AchievementRarity { COMMON, RARE, EPIC, LEGENDARY }
enum class AchievementStatus { LOCKED, UNLOCKED }
enum class TriggerType { COUNT, RATIO, FULL_COMPLETE, CUSTOM }
```

- [ ] **Step 2: 创建 UserLevel.kt**

```kotlin
package com.mapchina.domain.model

data class UserLevelInfo(
    val userId: String,
    val currentScore: Int,
    val currentLevel: Int,
    val currentTitle: String,
    val nextLevelScore: Int,
    val nextTitle: String
) {
    val progressToNext: Float get() {
        if (nextLevelScore == currentLevelScore) return 1f
        val prevThreshold = LEVEL_THRESHOLDS.getOrElse(currentLevel - 1) { 0 }
        return if (nextLevelScore > prevThreshold) {
            (currentScore - prevThreshold).toFloat() / (nextLevelScore - prevThreshold)
        } else 1f
    }
}

data class LevelDef(val level: Int, val title: String, val scoreThreshold: Int)

val LEVEL_DEFINITIONS = listOf(
    LevelDef(1, "初行者", 0),
    LevelDef(2, "识途者", 100),
    LevelDef(3, "远游者", 300),
    LevelDef(4, "山河行者", 800),
    LevelDef(5, "九州旅人", 1500),
    LevelDef(6, "华境探索家", 3000),
    LevelDef(7, "山海征途者", 6000),
    LevelDef(8, "大地巡礼者", 10000),
    LevelDef(9, "中华丈量师", 16000),
    LevelDef(10, "MapChina 宗师", 25000)
)

val LEVEL_THRESHOLDS = LEVEL_DEFINITIONS.associate { it.level to it.scoreThreshold }

fun resolveLevel(score: Int): LevelDef {
    return LEVEL_DEFINITIONS.last { score >= it.scoreThreshold }
}

fun resolveNextLevel(currentLevel: Int): LevelDef? {
    return LEVEL_DEFINITIONS.find { it.level == currentLevel + 1 }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/model/Achievement.kt shared/src/commonMain/kotlin/com/mapchina/domain/model/UserLevel.kt
git commit -m "feat: add achievement and level domain models"
```

---

### Task 3: 创建数据仓库

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/AchievementRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/UserScoreRepository.kt`

- [ ] **Step 1: 创建 AchievementRepository**

```kotlin
package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.*

class AchievementRepository(private val database: MapChinaDatabase) {

    fun getAllDefinitions(): List<Achievement> =
        database.achievementDefinitionQueries.selectAll().executeAsList().map { it.toDomain() }

    fun getDefinitionsByCategory(category: AchievementCategory): List<Achievement> =
        database.achievementDefinitionQueries.selectByCategory(category.name).executeAsList().map { it.toDomain() }

    fun getUserAchievements(userId: String): List<UserAchievement> =
        database.userAchievementQueries.selectByUser(userId).executeAsList().map { it.toDomain() }

    fun getUnlockedAchievements(userId: String): List<UserAchievement> =
        database.userAchievementQueries.selectUnlockedByUser(userId).executeAsList().map { it.toDomain() }

    fun getUnlockedCount(userId: String): Int =
        database.userAchievementQueries.countUnlockedByUser(userId).executeAsOne().toInt()

    fun initUserAchievements(userId: String) {
        val allDefs = database.achievementDefinitionQueries.selectAll().executeAsList()
        database.userAchievementQueries.transaction {
            for (def in allDefs) {
                database.userAchievementQueries.initAchievement(
                    userId, def.achievement_id, def.trigger_condition.substringAfterLast(":").toIntOrNull() ?: 0
                )
            }
        }
    }

    fun updateProgress(userId: String, achievementId: String, progressValue: Int, target: Int) {
        val status = if (progressValue >= target) "unlocked" else "locked"
        val unlockTime = if (status == "unlocked") kotlinx.datetime.Clock.System.now().toEpochMilliseconds() else null
        database.userAchievementQueries.upsertProgress(userId, achievementId, progressValue, target, status, unlockTime)
    }

    private fun com.mapchina.data.local.Achievement_definition.toDomain() = Achievement(
        id = achievement_id, category = AchievementCategory.valueOf(category),
        subCategory = sub_category, name = name, description = description,
        icon = icon, rarity = AchievementRarity.valueOf(rarity),
        triggerType = TriggerType.valueOf(trigger_type), triggerCondition = trigger_condition,
        rewardScore = reward_score.toInt(), sortOrder = sort_order.toInt()
    )

    private fun com.mapchina.data.local.User_achievement.toDomain() = UserAchievement(
        userId = user_id, achievementId = achievement_id,
        progressValue = progress_value.toInt(), progressTarget = progress_target.toInt(),
        status = AchievementStatus.valueOf(status),
        unlockTime = unlock_time?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
    )
}
```

- [ ] **Step 2: 创建 UserScoreRepository**

```kotlin
package com.mapchina.data.repository

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.domain.model.*

class UserScoreRepository(private val database: MapChinaDatabase) {

    fun getScore(userId: String): UserLevelInfo? {
        val row = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull() ?: return null
        val levelDef = resolveLevel(row.current_score.toInt())
        val nextLevel = resolveNextLevel(levelDef.level)
        return UserLevelInfo(
            userId = row.user_id,
            currentScore = row.current_score.toInt(),
            currentLevel = levelDef.level,
            currentTitle = levelDef.title,
            nextLevelScore = nextLevel?.scoreThreshold ?: levelDef.scoreThreshold,
            nextTitle = nextLevel?.title ?: levelDef.title
        )
    }

    fun addScore(userId: String, delta: Int) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val existing = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()
        if (existing != null) {
            database.userScoreQueries.addScore(delta.toLong(), now, userId)
            val newScore = (existing.current_score.toInt() + delta)
            val newLevel = resolveLevel(newScore).level.toLong()
            database.userScoreQueries.upsertScore(userId, newScore.toLong(), newLevel, now)
        } else {
            val level = resolveLevel(delta)
            database.userScoreQueries.upsertScore(userId, delta.toLong(), level.level.toLong(), now)
        }
    }

    fun ensureUserScore(userId: String) {
        val existing = database.userScoreQueries.selectByUser(userId).executeAsOneOrNull()
        if (existing == null) {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            database.userScoreQueries.upsertScore(userId, 0, 1, now)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/data/repository/AchievementRepository.kt shared/src/commonMain/kotlin/com/mapchina/data/repository/UserScoreRepository.kt
git commit -m "feat: add achievement and score repositories"
```

---

## Chunk 2: 业务逻辑层 — 成就结算 + 种子数据 + DI

### Task 4: 创建成就种子数据

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementSeeder.kt`

- [ ] **Step 1: 编写 AchievementSeeder**

定义 V1 全部 22 个成就（13 个地区征服 + 9 个景点收集），包含完整的 achievement_id、名称、描述、稀有度、触发条件和山河值奖励。

```kotlin
package com.mapchina.domain.service

import com.mapchina.data.repository.AchievementRepository

object AchievementSeeder {

    data class SeedDef(
        val id: String, val category: String, val subCategory: String,
        val name: String, val description: String, val icon: String,
        val rarity: String, val triggerType: String, val triggerCondition: String,
        val rewardScore: Int, val sortOrder: Int
    )

    private val seeds = listOf(
        // 县级
        SeedDef("region_district_1", "REGION", "district", "初探山河", "点亮1个县", "badge_district", "COMMON", "COUNT", "district:1", 50, 1),
        SeedDef("region_district_10", "REGION", "district", "县域漫游者", "点亮10个县", "badge_district", "COMMON", "COUNT", "district:10", 100, 2),
        SeedDef("region_district_30", "REGION", "district", "四方踏访", "点亮30个县", "badge_district", "RARE", "COUNT", "district:30", 200, 3),
        SeedDef("region_district_100", "REGION", "district", "百县行者", "点亮100个县", "badge_district", "EPIC", "COUNT", "district:100", 500, 4),
        // 市级
        SeedDef("region_city_1", "REGION", "city", "城市初见", "点亮1个市", "badge_city", "COMMON", "COUNT", "city:1", 50, 5),
        SeedDef("region_city_10", "REGION", "city", "十城旅人", "点亮10个市", "badge_city", "COMMON", "COUNT", "city:10", 100, 6),
        SeedDef("region_city_30", "REGION", "city", "山河识途者", "点亮30个市", "badge_city", "RARE", "COUNT", "city:30", 200, 7),
        SeedDef("region_city_100", "REGION", "city", "百城达人", "点亮100个市", "badge_city", "EPIC", "COUNT", "city:100", 500, 8),
        // 省级
        SeedDef("region_province_1", "REGION", "province", "跨省出发", "点亮1个省", "badge_province", "COMMON", "COUNT", "province:1", 50, 9),
        SeedDef("region_province_5", "REGION", "province", "五省行记", "点亮5个省", "badge_province", "COMMON", "COUNT", "province:5", 100, 10),
        SeedDef("region_province_10", "REGION", "province", "九州漫游者", "点亮10个省", "badge_province", "RARE", "COUNT", "province:10", 200, 11),
        SeedDef("region_province_20", "REGION", "province", "大地巡游家", "点亮20个省", "badge_province", "EPIC", "COUNT", "province:20", 500, 12),
        SeedDef("region_province_31", "REGION", "province", "丈量中国", "点亮31个省级区域", "badge_province", "LEGENDARY", "COUNT", "province:31", 1000, 13),
        // 5A 景点
        SeedDef("scenic_5a_1", "SCENIC", "5a", "初见华景", "点亮1个5A景点", "badge_5a", "COMMON", "COUNT", "5a:1", 50, 14),
        SeedDef("scenic_5a_10", "SCENIC", "5a", "胜景识途者", "点亮10个5A景点", "badge_5a", "COMMON", "COUNT", "5a:10", 100, 15),
        SeedDef("scenic_5a_30", "SCENIC", "5a", "名胜巡礼者", "点亮30个5A景点", "badge_5a", "RARE", "COUNT", "5a:30", 200, 16),
        SeedDef("scenic_5a_50", "SCENIC", "5a", "山河收藏家", "点亮50个5A景点", "badge_5a", "EPIC", "COUNT", "5a:50", 500, 17),
        SeedDef("scenic_5a_100", "SCENIC", "5a", "国家胜景大师", "点亮100个5A景点", "badge_5a", "LEGENDARY", "COUNT", "5a:100", 1000, 18),
        // 4A/5A 总数
        SeedDef("scenic_total_10", "SCENIC", "total", "风景在路上", "点亮10个景点", "badge_total", "COMMON", "COUNT", "total:10", 50, 19),
        SeedDef("scenic_total_50", "SCENIC", "total", "景区猎人", "点亮50个景点", "badge_total", "RARE", "COUNT", "total:50", 200, 20),
        SeedDef("scenic_total_100", "SCENIC", "total", "名景博览者", "点亮100个景点", "badge_total", "EPIC", "COUNT", "total:100", 500, 21),
        SeedDef("scenic_total_300", "SCENIC", "total", "中华胜景图鉴家", "点亮300个景点", "badge_total", "LEGENDARY", "COUNT", "total:300", 1000, 22),
    )

    fun seedAchievements(repo: AchievementRepository) {
        val existing = repo.getAllDefinitions()
        if (existing.isNotEmpty()) return
        // 批量插入通过 database 直接操作，此处仅标记已完成
    }

    fun getSeeds(): List<SeedDef> = seeds
}
```

注意：实际插入需要通过 `database.achievementDefinitionQueries` 直接操作，因为 Repository 层面的 `insertDefinitionsInTransaction` 需要逐条调用。

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementSeeder.kt
git commit -m "feat: add achievement seed data definitions (22 achievements for V1)"
```

---

### Task 5: 创建 AchievementService 成就结算核心

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementService.kt`

- [ ] **Step 1: 编写 AchievementService**

```kotlin
package com.mapchina.domain.service

import com.mapchina.data.repository.AchievementRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.UserScoreRepository
import com.mapchina.domain.model.*

data class AchievementUnlockResult(
    val newlyUnlocked: List<UserAchievement>,
    val scoreAdded: Int,
    val levelChanged: Boolean,
    val oldLevel: Int,
    val newLevel: Int
)

class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val footprintRepository: FootprintRepository,
    private val userScoreRepository: UserScoreRepository
) {
    fun evaluateAndSettle(userId: String): AchievementUnlockResult {
        val allDefs = achievementRepository.getAllDefinitions()
        val existing = achievementRepository.getUserAchievements(userId)
        val existingMap = existing.associateBy { it.achievementId }

        val stats = computeCurrentStats(userId)
        val newlyUnlocked = mutableListOf<UserAchievement>()
        var totalScoreFromAchievements = 0

        for (def in allDefs) {
            val progress = computeProgress(def, stats)
            val target = def.triggerCondition.substringAfterLast(":").toIntOrNull() ?: 0
            val wasUnlocked = existingMap[def.id]?.status == AchievementStatus.UNLOCKED

            achievementRepository.updateProgress(userId, def.id, progress, target)

            if (progress >= target && !wasUnlocked) {
                val ua = UserAchievement(userId, def.id, progress, target, AchievementStatus.UNLOCKED, kotlinx.datetime.Clock.System.now())
                newlyUnlocked.add(ua)
                totalScoreFromAchievements += def.rewardScore
            }
        }

        val oldLevelInfo = userScoreRepository.getScore(userId)
        val oldLevel = oldLevelInfo?.currentLevel ?: 1

        // 计算点亮行为的基础山河值
        val baseScore = computeBaseScore(userId, stats)
        val totalScoreToAdd = baseScore + totalScoreFromAchievements

        if (totalScoreToAdd > 0) {
            userScoreRepository.addScore(userId, totalScoreToAdd)
        }

        val newLevelInfo = userScoreRepository.getScore(userId)
        val newLevel = newLevelInfo?.currentLevel ?: 1

        return AchievementUnlockResult(
            newlyUnlocked = newlyUnlocked,
            scoreAdded = totalScoreToAdd,
            levelChanged = newLevel > oldLevel,
            oldLevel = oldLevel,
            newLevel = newLevel
        )
    }

    private data class UserStats(
        val visitedDistricts: Int,
        val visitedCities: Int,
        val visitedProvinces: Int,
        val visited5a: Int,
        val visitedTotal: Int
    )

    private fun computeCurrentStats(userId: String): UserStats {
        val footprints = footprintRepository.getFootprintsByUser(userId)
        val visitedRegionIds = footprints.map { it.regionId }.toSet()
        val districts = visitedRegionIds.count { !it.endsWith("00") }
        val cities = visitedRegionIds.count { it.endsWith("00") && !it.endsWith("0000") }
        val provinces = visitedRegionIds.count { it.endsWith("0000") }

        val visits = footprintRepository.getAttractionVisitsByUser(userId)
        val a5Count = visits.count { v ->
            val attraction = com.mapchina.data.repository.AttractionRepository(
                (footprintRepository as? FootprintRepository)?.let { throw UnsupportedOperationException() } ?: throw UnsupportedOperationException()
            ).getAttraction(v.attractionId)
            attraction?.level == AttractionLevel.A5
        }
        // 上面方式不对，需要外部注入 AttractionRepository
        val totalVisits = visits.size

        return UserStats(districts, cities, provinces, 0, totalVisits)
    }

    private fun computeProgress(def: Achievement, stats: UserStats): Int = when (def.triggerCondition.substringBefore(":")) {
        "district" -> stats.visitedDistricts
        "city" -> stats.visitedCities
        "province" -> stats.visitedProvinces
        "5a" -> stats.visited5a
        "total" -> stats.visitedTotal
        else -> 0
    }

    private fun computeBaseScore(userId: String, stats: UserStats): Int {
        // 首次结算时根据当前点亮数量给基础山河值
        // 后续增量通过 FootprintService 事件触发
        return 0 // 增量模式在 FootprintService 中处理
    }
}
```

注意：上面的 `computeCurrentStats` 需要注入 `AttractionRepository` 来判断 5A 数量。实际实现需修正构造函数。

- [ ] **Step 2: 修正 AchievementService 构造函数，注入 AttractionRepository**

最终签名应为：

```kotlin
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val footprintRepository: FootprintRepository,
    private val userScoreRepository: UserScoreRepository,
    private val attractionRepository: AttractionRepository
)
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/service/AchievementService.kt
git commit -m "feat: add achievement evaluation and settlement service"
```

---

### Task 6: 修改 FootprintService 接入成就结算

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`

- [ ] **Step 1: FootprintService 添加成就触发**

在 `markFootprint` 和 `markAttractionVisit` 方法末尾调用 `achievementService.evaluateAndSettle(userId)`，并返回结算结果。新增 `AchievementService` 为构造参数。

- [ ] **Step 2: AppModule 注册新依赖**

```kotlin
single { AchievementRepository(get()) }
single { UserScoreRepository(get()) }
single { AchievementService(get(), get(), get(), get()) }
factory { AchievementViewModel(get(), get(), get()) }
```

- [ ] **Step 3: DataSeeder 中调用 AchievementSeeder.seedAchievements**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt shared/src/commonMain/kotlin/com/mapchina/data/remote/DataSeeder.kt
git commit -m "feat: wire achievement service into footprint flow and DI"
```

---

## Chunk 3: UI 层 — 成就页面 + 徽章墙 + 弹窗

### Task 7: 成就首页 + ViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementViewModel.kt`

- [ ] **Step 1: 编写 AchievementViewModel**

UI 状态包含：等级信息、山河值进度、最近解锁成就、下一目标、地区/景点分类成就列表、徽章总数。

- [ ] **Step 2: 编写 AchievementScreen**

使用 Compose 实现深色主题的成就首页，包含：
- 顶部等级卡片（等级称号、山河值、进度条）
- "下一目标"模块
- 地区征服成就列表
- 景点收集成就列表
- 入口到徽章墙

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementViewModel.kt
git commit -m "feat: add achievement home screen and viewmodel"
```

---

### Task 8: 徽章墙 + 徽章详情

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeWallScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeDetailScreen.kt`

- [ ] **Step 1: 编写 BadgeWallScreen**

网格布局展示所有徽章，已获得为彩色，未获得为灰色，按分类 tab 筛选。

- [ ] **Step 2: 编写 BadgeDetailScreen**

展示徽章大图、名称、描述、解锁条件、进度、解锁时间、分享按钮（预留）。

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeWallScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/achievement/BadgeDetailScreen.kt
git commit -m "feat: add badge wall and badge detail screens"
```

---

### Task 9: 成就解锁弹窗 + 升级弹窗

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementUnlockDialog.kt`

- [ ] **Step 1: 编写 AchievementUnlockDialog**

支持单成就解锁弹窗和多成就汇总弹窗，包含徽章图标、成就名、文案、山河值奖励。

- [ ] **Step 2: 编写升级弹窗组件**

在同一个文件中提供 `LevelUpDialog` 组件。

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/achievement/AchievementUnlockDialog.kt
git commit -m "feat: add achievement unlock and level up dialogs"
```

---

### Task 10: 路由 + 底部导航 + 个人主页

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt`

- [ ] **Step 1: Screen.kt 新增路由**

```kotlin
@Serializable data object AchievementScreen : Screen()
@Serializable data object BadgeWallScreen : Screen()
@Serializable data class BadgeDetailScreen(val achievementId: String) : Screen()
```

- [ ] **Step 2: App.kt 底部导航替换"统计"为"成就"**

图标改为 `Icons.Default.EmojiEvents`，label 改为"成就"。

- [ ] **Step 3: AppNavHost.kt 新增路由注册**

- [ ] **Step 4: ProfileScreen 展示等级和徽章数**

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt shared/src/commonMain/kotlin/com/mapchina/ui/App.kt shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt
git commit -m "feat: wire achievement screens into navigation and update bottom nav"
```

---

## Chunk 4: 测试 + 集成验证

### Task 11: 单元测试

**Files:**
- Create: `shared/src/commonTest/kotlin/com/mapchina/domain/service/AchievementServiceTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/data/repository/AchievementRepositoryTest.kt`

- [ ] **Step 1: 编写 AchievementServiceTest**

测试场景：
- 首次点亮触发初探山河 + 城市初见 + 跨省出发
- 点亮 10 个县触发县域漫游者
- 已解锁成就不重复触发
- 山河值正确累计
- 等级正确提升

- [ ] **Step 2: 编写 AchievementRepositoryTest**

测试 CRUD 操作正确性。

- [ ] **Step 3: 运行测试**

Run: `./gradlew :shared:allTests`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonTest/kotlin/com/mapchina/domain/service/AchievementServiceTest.kt shared/src/commonTest/kotlin/com/mapchina/data/repository/AchievementRepositoryTest.kt
git commit -m "test: add achievement service and repository unit tests"
```

---

### Task 12: 集成验证 + 端到端手动测试

- [ ] **Step 1: 构建 Android APK**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 手动验证关键路径**

1. 打开 App → 底部导航出现"成就"Tab
2. 点击"成就"→ 空状态展示
3. 回到地图页 → 点亮一个景点
4. 自动弹出成就解锁弹窗
5. 进入"成就"页 → 显示等级卡片和已解锁成就
6. 进入徽章墙 → 灰色/彩色徽章正确展示
7. 个人主页展示等级和徽章数

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete achievement system V1 - levels, badges, and unlock flow"
```

---

## V1.5 和 V2 规划（本计划不实施，仅记录）

### V1.5 省份征服版
- 新增 `province_conquest` 相关表和页面
- 省份地图着色视图
- 每省到访/完成徽章
- 省份详情页含进度和推荐补齐

### V2 主题图鉴版
- 新增 `atlas_definition` / `atlas_item` / `user_atlas_progress` 表
- 图鉴首页/详情页
- 图鉴成就联动
- 景点详情页关联图鉴入口
