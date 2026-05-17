# 中国足迹地图 App 实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个跨平台中国足迹地图 App，让用户标记到过的城市（省→市→区三级），以景点为锚点发现值得去的地方。

**Architecture:** KMP 共享业务逻辑 + Compose Multiplatform 共享 UI；Ktor 自建后端提供 Auth/API/同步；SQLDelight 本地数据库离线优先；地图组件通过 Expect/Actual 封装各平台原生 SDK。

**Tech Stack:** Kotlin, KMP, Compose Multiplatform, SQLDelight, Ktor (Client + Server), Koin, PostgreSQL, Compose Canvas

---

## 项目结构

```
MapChina/
├── shared/                          # KMP 共享模块
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   └── com/mapchina/
│   │   │       ├── data/
│   │   │       │   ├── local/       # SQLDelight 数据库 + DAO
│   │   │       │   ├── remote/      # Ktor Client API 接口
│   │   │       │   ├── repository/  # Repository 实现
│   │   │       │   └── model/       # 数据模型 (DTO)
│   │   │       ├── domain/
│   │   │       │   ├── model/       # 领域模型
│   │   │       │   └── service/     # 业务服务
│   │   │       ├── ui/
│   │   │       │   ├── map/         # 地图页 ViewModel + Composable
│   │   │       │   ├── attraction/  # 景点页 ViewModel + Composable
│   │   │       │   ├── stats/       # 统计页 ViewModel + Composable
│   │   │       │   ├── profile/     # 我的页 ViewModel + Composable
│   │   │       │   ├── theme/       # 主题 & 色彩定义
│   │   │       │   └── navigation/  # 导航图
│   │   │       ├── sync/            # 同步引擎
│   │   │       ├── location/        # GPS 定位 Expect/Actual
│   │   │       └── map/             # MapController Expect/Actual
│   │   ├── commonTest/kotlin/       # 共享测试
│   │   ├── androidMain/             # Android Actual 实现
│   │   └── iosMain/                 # iOS Actual 实现
│   └── build.gradle.kts
├── androidApp/                      # Android 应用入口
├── iosApp/                          # iOS 应用入口
├── server/                          # Ktor 后端
│   ├── src/main/kotlin/
│   │   └── com/mapchina/server/
│   │       ├── routes/              # API 路由
│   │       ├── auth/                # JWT 认证
│   │       ├── database/            # PostgreSQL 表定义
│   │       └── Application.kt
│   └── build.gradle.kts
├── data/                            # 预置数据
│   ├── regions/                     # GeoJSON 边界数据
│   └── attractions/                 # 景点数据
├── docs/
│   └── superpowers/
│       ├── specs/                   # 设计规格
│       └── plans/                   # 实现计划
└── build.gradle.kts                 # 根项目配置
```

---

## Chunk 1: 项目脚手架 & 共享数据模型

### Task 1: 初始化 KMP 项目结构

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `shared/build.gradle.kts`
- Create: `gradle.properties`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/RegionDto.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/AttractionDto.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/FootprintDto.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/AttractionVisitDto.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/UserDto.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/model/ApiResponse.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/model/DtoSerializationTest.kt`

- [ ] **Step 1: 创建根项目配置**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "MapChina"
include(":shared")
```

- [ ] **Step 2: 创建共享模块 build.gradle.kts**

配置 KMP 插件（Compose Multiplatform, SQLDelight, Ktor, Koin）和 commonMain/androidMain/iosMain 源集。

- [ ] **Step 3: 编写 DTO 序列化测试**

```kotlin
// DtoSerializationTest.kt
class DtoSerializationTest {

    @Test
    fun regionDto_serializesAndDeserializes() {
        val dto = RegionDto(
            id = "110000",
            name = "北京市",
            level = RegionLevel.PROVINCE,
            parentId = null
        )
        val json = Json.encodeToString(RegionDto.serializer(), dto)
        val decoded = Json.decodeFromString(RegionDto.serializer(), json)
        assertEquals(dto, decoded)
    }

    @Test
    fun footprintDto_levelOrdering() {
        assertTrue(FootprintLevel.DEEP > FootprintLevel.SHORT_VISIT)
        assertTrue(FootprintLevel.SHORT_VISIT > FootprintLevel.PASS_BY)
    }

    @Test
    fun apiResponse_wrapsSuccess() {
        val response = ApiResponse.Success(data = "test", total = 1)
        assertTrue(response.isSuccess())
    }

    @Test
    fun apiResponse_wrapsError() {
        val response = ApiResponse.Error<Unit>(code = "NOT_FOUND", message = "区域不存在")
        assertFalse(response.isSuccess())
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.data.model.DtoSerializationTest"`
Expected: FAIL (类不存在)

- [ ] **Step 5: 实现数据模型 DTO**

```kotlin
// RegionDto.kt
@Serializable
data class RegionDto(
    val id: String,
    val name: String,
    val level: RegionLevel,
    val parentId: String? = null
)

@Serializable
enum class RegionLevel {
    PROVINCE, CITY, DISTRICT
}
```

```kotlin
// FootprintDto.kt
@Serializable
data class FootprintDto(
    val userId: String,
    val regionId: String,
    val level: FootprintLevel,
    val timestamp: Instant
)

@Serializable
enum class FootprintLevel : Comparable<FootprintLevel> {
    PASS_BY, SHORT_VISIT, DEEP;

    override fun compareTo(other: FootprintLevel): Int =
        ordinal.compareTo(other.ordinal)
}
```

```kotlin
// ApiResponse.kt
@Serializable
sealed class ApiResponse<T> {
    abstract fun isSuccess(): Boolean

    @Serializable
    data class Success<T>(
        val data: T,
        val total: Long? = null,
        val hasMore: Boolean? = null
    ) : ApiResponse<T>() {
        override fun isSuccess() = true
    }

    @Serializable
    data class Error<T>(
        val code: String,
        val message: String
    ) : ApiResponse<T>() {
        override fun isSuccess() = false
    }
}
```

同理实现 `AttractionDto`, `AttractionVisitDto`, `UserDto`。

- [ ] **Step 6: 运行测试确认通过**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.data.model.DtoSerializationTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts shared/
git commit -m "feat: initialize KMP project with shared data models"
```

---

### Task 2: 领域模型 & 足迹升级逻辑

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/Region.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/Attraction.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/Footprint.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/AttractionVisit.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/User.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/domain/model/FootprintLevelTest.kt`

- [ ] **Step 1: 编写足迹升级逻辑测试**

```kotlin
class FootprintLevelTest {

    @Test
    fun upgradeFromPassByToShortVisit_succeeds() {
        val result = FootprintLevel.PASS_BY.upgradeTo(FootprintLevel.SHORT_VISIT)
        assertEquals(FootprintLevel.SHORT_VISIT, result)
    }

    @Test
    fun downgradeFromDeepToPassBy_fails() {
        val result = FootprintLevel.DEEP.upgradeTo(FootprintLevel.PASS_BY)
        assertEquals(FootprintLevel.DEEP, result) // 不降级，保持原级
    }

    @Test
    fun sameLevel_noChange() {
        val result = FootprintLevel.SHORT_VISIT.upgradeTo(FootprintLevel.SHORT_VISIT)
        assertEquals(FootprintLevel.SHORT_VISIT, result)
    }

    @Test
    fun resolveConflict_higherLevelWins() {
        val local = FootprintLevel.PASS_BY
        val remote = FootprintLevel.DEEP
        val resolved = FootprintLevel.resolveConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.domain.model.FootprintLevelTest"`
Expected: FAIL

- [ ] **Step 3: 实现领域模型**

```kotlin
// domain/model/Footprint.kt
data class Footprint(
    val userId: String,
    val regionId: String,
    val level: FootprintLevel,
    val timestamp: Instant
)

enum class FootprintLevel {
    PASS_BY, SHORT_VISIT, DEEP;

    fun upgradeTo(newLevel: FootprintLevel): FootprintLevel =
        if (newLevel > this) newLevel else this

    companion object {
        fun resolveConflict(local: FootprintLevel, remote: FootprintLevel): FootprintLevel =
            if (local > remote) local else remote
    }
}
```

同理实现 `Region`, `Attraction`, `AttractionVisit`, `User` 领域模型（纯 data class，不含序列化注解）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :shared:commonTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/ shared/src/commonTest/
git commit -m "feat: add domain models with footprint upgrade logic"
```

---

## Chunk 2: SQLDelight 本地数据库 & Repository 层

### Task 3: SQLDelight 数据库定义

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/Region.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/Attraction.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/Footprint.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/AttractionVisit.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/User.sq`
- Create: `shared/src/commonMain/sqldelight/com/mapchina/data/local/SyncQueue.sq`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/local/DatabaseDriverFactory.kt` (expect)
- Create: `shared/src/androidMain/kotlin/com/mapchina/data/local/AndroidDatabaseDriverFactory.kt` (actual)
- Create: `shared/src/iosMain/kotlin/com/mapchina/data/local/IosDatabaseDriverFactory.kt` (actual)
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/local/DatabaseTest.kt`

- [ ] **Step 1: 编写数据库 Schema**

```sql
-- Region.sq
CREATE TABLE region (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    level TEXT NOT NULL,
    parent_id TEXT,
    boundary_json TEXT
);

SELECTById:
SELECT * FROM region WHERE id = ?;

SELECTByParentId:
SELECT * FROM region WHERE parent_id = ? ORDER BY name;

SELECTByLevel:
SELECT * FROM region WHERE level = ? ORDER BY name;

INSERT:
INSERT OR REPLACE INTO region(id, name, level, parent_id, boundary_json)
VALUES (?, ?, ?, ?, ?);
```

```sql
-- Footprint.sq
CREATE TABLE footprint (
    user_id TEXT NOT NULL,
    region_id TEXT NOT NULL,
    level TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    PRIMARY KEY(user_id, region_id)
);

SELECTByUserId:
SELECT * FROM footprint WHERE user_id = ?;

SELECTByUserAndRegion:
SELECT * FROM footprint WHERE user_id = ? AND region_id = ?;

UPSERT:
INSERT OR REPLACE INTO footprint(user_id, region_id, level, timestamp)
VALUES (?, ?, ?, ?);
```

```sql
-- SyncQueue.sq (离线同步队列)
CREATE TABLE sync_queue (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0
);

SELECTPending:
SELECT * FROM sync_queue ORDER BY created_at ASC LIMIT ?;

DELETEById:
DELETE FROM sync_queue WHERE id = ?;

INCREMENT_RETRY:
UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id = ?;
```

同理实现 `Attraction.sq`, `AttractionVisit.sq`, `User.sq`。

- [ ] **Step 2: 编写数据库查询测试**

```kotlin
class DatabaseTest {

    private lateinit var database: MapChinaDatabase

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
    }

    @Test
    fun insertAndQueryRegion() {
        database.regionQueries.INSERT("110000", "北京市", "PROVINCE", null, null)
        val region = database.regionQueries.SELECTById("110000").executeAsOne()
        assertEquals("北京市", region.name)
    }

    @Test
    fun upsertFootprint_higherLevelWins() {
        database.footprintQueries.UPSERT("u1", "110000", "PASS_BY", 1000L)
        database.footprintQueries.UPSERT("u1", "110000", "DEEP", 2000L)
        val fp = database.footprintQueries.SELECTByUserAndRegion("u1", "110000").executeAsOne()
        // 注意: SQLDelight UPSERT 由 Repository 层控制升级逻辑，这里验证写入
        assertEquals("DEEP", fp.level)
    }

    @Test
    fun syncQueue_insertAndRetrieve() {
        database.syncQueueQueries.insertPending("FOOTPRINT", "110000", "UPSERT", "{}", 1000L)
        val pending = database.syncQueueQueries.SELECTPending(10).executeAsList()
        assertEquals(1, pending.size)
    }
}
```

- [ ] **Step 3: 实现 DatabaseDriverFactory (Expect/Actual)**

```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(MapChinaDatabase.Schema, context, "mapchina.db")
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(MapChinaDatabase.Schema, "mapchina.db")
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.data.local.DatabaseTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/sqldelight/ shared/src/commonMain/kotlin/com/mapchina/data/local/ shared/src/androidMain/ shared/src/iosMain/
git commit -m "feat: add SQLDelight schema and database driver factory"
```

---

### Task 4: Repository 层 — 足迹与区域

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/RegionRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/FootprintRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/AttractionRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/repository/FootprintRepositoryTest.kt`

- [ ] **Step 1: 编写 FootprintRepository 测试**

```kotlin
class FootprintRepositoryTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var repository: FootprintRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        repository = FootprintRepository(database)
    }

    @Test
    fun markFootprint_newRegion_createsRecord() {
        repository.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)
        val fp = repository.getFootprint("u1", "110000")
        assertNotNull(fp)
        assertEquals(FootprintLevel.SHORT_VISIT, fp!!.level)
    }

    @Test
    fun markFootprint_upgradeLevel_succeeds() {
        repository.markFootprint("u1", "110000", FootprintLevel.PASS_BY)
        repository.markFootprint("u1", "110000", FootprintLevel.DEEP)
        val fp = repository.getFootprint("u1", "110000")
        assertEquals(FootprintLevel.DEEP, fp!!.level)
    }

    @Test
    fun markFootprint_downgradeLevel_ignored() {
        repository.markFootprint("u1", "110000", FootprintLevel.DEEP)
        repository.markFootprint("u1", "110000", FootprintLevel.PASS_BY)
        val fp = repository.getFootprint("u1", "110000")
        assertEquals(FootprintLevel.DEEP, fp!!.level) // 不降级
    }

    @Test
    fun markAttractionVisit_cascadesToRegionFootprint() {
        // 插入区域层级: 省→市→区
        insertRegionHierarchy()

        // 标记景点访问
        repository.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.DEEP)

        // 验证区级足迹升级为 DEEP
        val district = repository.getFootprint("u1", "510107")
        assertEquals(FootprintLevel.DEEP, district!!.level)

        // 验证市级足迹至少 PASS_BY
        val city = repository.getFootprint("u1", "510100")
        assertNotNull(city)
        assertTrue(city!!.level >= FootprintLevel.PASS_BY)

        // 验证省级足迹至少 PASS_BY
        val province = repository.getFootprint("u1", "510000")
        assertNotNull(province)
    }

    private fun insertRegionHierarchy() {
        // 四川省 → 成都市 → 武侯区
        database.regionQueries.INSERT("510000", "四川省", "PROVINCE", null, null)
        database.regionQueries.INSERT("510100", "成都市", "CITY", "510000", null)
        database.regionQueries.INSERT("510107", "武侯区", "DISTRICT", "510100", null)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.data.repository.FootprintRepositoryTest"`
Expected: FAIL

- [ ] **Step 3: 实现 FootprintRepository**

```kotlin
class FootprintRepository(private val database: MapChinaDatabase) {

    fun markFootprint(userId: String, regionId: String, level: FootprintLevel) {
        val existing = database.footprintQueries
            .SELECTByUserAndRegion(userId, regionId)
            .executeAsOneOrNull()

        val effectiveLevel = if (existing != null) {
            val currentLevel = FootprintLevel.valueOf(existing.level)
            currentLevel.upgradeTo(level)
        } else {
            level
        }

        database.footprintQueries.UPSERT(userId, regionId, effectiveLevel.name, Clock.System.now().toEpochMilliseconds())
    }

    fun markAttractionVisit(userId: String, attractionId: String, regionId: String, level: FootprintLevel) {
        // 1. 标记景点访问
        database.attractionVisitQueries.UPSERT(userId, attractionId, level.name, Clock.System.now().toEpochMilliseconds())

        // 2. 级联更新区域足迹
        markFootprint(userId, regionId, level)

        // 3. 向上级联: 区→市→省
        cascadeToParentRegions(userId, regionId)
    }

    private fun cascadeToParentRegions(userId: String, regionId: String) {
        var currentId = regionId
        while (true) {
            val region = database.regionQueries.SELECTById(currentId).executeAsOneOrNull() ?: break
            val parentId = region.parent_id ?: break
            // 父级至少设为 PASS_BY
            markFootprint(userId, parentId, FootprintLevel.PASS_BY)
            currentId = parentId
        }
    }

    fun getFootprint(userId: String, regionId: String): Footprint? {
        val row = database.footprintQueries.SELECTByUserAndRegion(userId, regionId).executeAsOneOrNull()
        return row?.let {
            Footprint(it.user_id, it.region_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }

    fun getFootprintsByUser(userId: String): List<Footprint> {
        return database.footprintQueries.SELECTByUserId(userId).executeAsList().map {
            Footprint(it.user_id, it.region_id, FootprintLevel.valueOf(it.level), Instant.fromEpochMilliseconds(it.timestamp))
        }
    }
}
```

同理实现 `RegionRepository` 和 `AttractionRepository`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :shared:commonTest --tests "com.mapchina.data.repository.FootprintRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/data/repository/ shared/src/commonTest/kotlin/com/mapchina/data/repository/
git commit -m "feat: add repositories with footprint cascade logic"
```

---

## Chunk 3: Ktor 后端

### Task 5: Ktor Server 初始化 & 数据库迁移

**Files:**
- Create: `server/build.gradle.kts`
- Create: `server/src/main/kotlin/com/mapchina/server/Application.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/database/DatabaseFactory.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/database/Tables.kt`
- Create: `server/src/main/resources/application.conf`
- Create: `server/src/main/resources/logback.xml`
- Test: `server/src/test/kotlin/com/mapchina/server/database/TablesTest.kt`

- [ ] **Step 1: 配置 server/build.gradle.kts**

添加 Ktor Server 插件、Exposed ORM、PostgreSQL JDBC 驱动、JWT 依赖。

- [ ] **Step 2: 定义 PostgreSQL 表**

```kotlin
// Tables.kt
object Regions : Table("regions") {
    val id = varchar("id", 10)
    val name = varchar("name", 50)
    val level = varchar("level", 10)
    val parentId = varchar("parent_id", 10).nullable()
    val boundaryJson = text("boundary_json").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Attractions : Table("attractions") {
    val id = varchar("id", 20)
    val name = varchar("name", 100)
    val regionId = varchar("region_id", 10).references(Regions.id)
    val level = varchar("level", 5)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Footprints : Table("footprints") {
    val userId = varchar("user_id", 36).references(Users.id)
    val regionId = varchar("region_id", 10).references(Regions.id)
    val level = varchar("level", 15)
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(userId, regionId)
}

object AttractionVisits : Table("attraction_visits") {
    val userId = varchar("user_id", 36).references(Users.id)
    val attractionId = varchar("attraction_id", 20).references(Attractions.id)
    val level = varchar("level", 15)
    val timestamp = long("timestamp")
    val note = text("note").nullable()

    override val primaryKey = PrimaryKey(userId, attractionId)
}

object Users : Table("users") {
    val id = varchar("id", 36)
    val phone = varchar("phone", 20).uniqueIndex()
    val nickname = varchar("nickname", 50)
    val avatar = varchar("avatar", 500).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 3: 编写数据库初始化测试**

```kotlin
class TablesTest {

    @Test
    fun databaseFactory_createsAllTables() {
        val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(Regions, Attractions, Users, Footprints, AttractionVisits)
        }
        // 验证表存在
        val tables = transaction(database) {
            exec("SHOW TABLES") { rs ->
                generateSequence { if (rs.next()) rs.getString(1) else null }
            }
        }
        assertTrue(tables.containsAll(listOf("regions", "attractions", "users", "footprints", "attraction_visits")))
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :server:test --tests "com.mapchina.server.database.TablesTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/
git commit -m "feat: initialize Ktor server with database schema"
```

---

### Task 6: JWT 认证路由

**Files:**
- Create: `server/src/main/kotlin/com/mapchina/server/auth/JwtConfig.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/routes/AuthRoutes.kt`
- Test: `server/src/test/kotlin/com/mapchina/server/routes/AuthRoutesTest.kt`

- [ ] **Step 1: 编写认证路由测试**

```kotlin
class AuthRoutesTest {

    private val testApp = TestApplication {
        application {
            configureRouting()
            configureSecurity()
        }
    }

    @Test
    fun sendCode_returnsSuccess() {
        testApp.client.post("/auth/send-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun login_withValidCode_returnsTokens() {
        // 先发送验证码
        testApp.client.post("/auth/send-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000"}""")
        }
        // 用验证码登录
        testApp.client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone":"13800138000","code":"123456"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = Json.decodeFromString<JsonObject>(bodyAsText())
            assertTrue(body.containsKey("accessToken"))
            assertTrue(body.containsKey("refreshToken"))
        }
    }

    @Test
    fun refreshToken_returnsNewAccessToken() {
        // 登录获取 refresh token
        val refreshToken = loginAndGetRefreshToken()
        testApp.client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

- [ ] **Step 3: 实现 JwtConfig 和 AuthRoutes**

- [ ] **Step 4: 运行测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add server/src/
git commit -m "feat: add JWT authentication routes (send-code, login, refresh)"
```

---

### Task 7: 数据 & 足迹 API 路由

**Files:**
- Create: `server/src/main/kotlin/com/mapchina/server/routes/RegionRoutes.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/routes/AttractionRoutes.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/routes/FootprintRoutes.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/routes/SyncRoutes.kt`
- Test: `server/src/test/kotlin/com/mapchina/server/routes/RegionRoutesTest.kt`
- Test: `server/src/test/kotlin/com/mapchina/server/routes/FootprintRoutesTest.kt`
- Test: `server/src/test/kotlin/com/mapchina/server/routes/SyncRoutesTest.kt`

- [ ] **Step 1: 编写 Region API 测试**

```kotlin
class RegionRoutesTest {

    @Test
    fun getRegions_byLevel_returnsProvinces() = testApp {
        client.get("/regions?level=PROVINCE").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = Json.decodeFromString<ApiResponse<List<RegionDto>>>(bodyAsText())
            assertTrue(body.isSuccess())
        }
    }

    @Test
    fun getRegions_byParentId_returnsCities() = testApp {
        client.get("/regions?parentId=510000").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun getRegionBoundary_returnsGeoJSON() = testApp {
        client.get("/regions/510000/boundary").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
```

- [ ] **Step 2: 编写 Footprint API 测试**

```kotlin
class FootprintRoutesTest {

    @Test
    fun createFootprint_returnsSuccess() = testAppWithAuth { token ->
        client.post("/footprints") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"regionId":"510000","level":"DEEP"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun createFootprint_conflictResolvesHigherLevel() = testAppWithAuth { token ->
        // 先创建 PASS_BY
        client.post("/footprints") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"regionId":"510000","level":"PASS_BY"}""")
        }
        // 再创建 DEEP
        client.post("/footprints") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"regionId":"510000","level":"DEEP"}""")
        }
        // 查询应为 DEEP
        client.get("/footprints?regionId=510000") {
            header("Authorization", "Bearer $token")
        }.apply {
            val body = Json.decodeFromString<ApiResponse<List<FootprintDto>>>(bodyAsText())
            val fp = (body as ApiResponse.Success).data.first()
            assertEquals("DEEP", fp.level)
        }
    }
}
```

- [ ] **Step 3: 编写同步 API 测试**

```kotlin
class SyncRoutesTest {

    @Test
    fun syncDelta_returnsChangesSinceTimestamp() = testAppWithAuth { token ->
        // 创建一条足迹
        createFootprint(token, "510000", "DEEP")
        // 查询增量
        client.get("/sync/delta?since=0") {
            header("Authorization", "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun syncPush_pushesLocalChanges() = testAppWithAuth { token ->
        client.post("/sync/push") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"footprints":[{"regionId":"510000","level":"DEEP","timestamp":1000}],"attractionVisits":[]}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
```

- [ ] **Step 4: 实现所有路由**

- [ ] **Step 5: 运行所有测试确认通过**

- [ ] **Step 6: Commit**

```bash
git add server/src/
git commit -m "feat: add region, attraction, footprint, and sync API routes"
```

---

## Chunk 4: Domain Service 层 & Koin DI

### Task 8: Domain Services

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/AttractionService.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/AuthService.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/SyncService.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/domain/service/FootprintServiceTest.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/domain/service/SyncServiceTest.kt`

- [ ] **Step 1: 编写 FootprintService 测试**

```kotlin
class FootprintServiceTest {

    private lateinit var footprintRepo: FootprintRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var service: FootprintService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        footprintRepo = FootprintRepository(database)
        regionRepo = RegionRepository(database)
        service = FootprintService(footprintRepo, regionRepo)
    }

    @Test
    fun markRegionFootprint_newRecord() {
        val result = service.markFootprint("u1", "510000", FootprintLevel.SHORT_VISIT)
        assertTrue(result.isSuccess)
        assertEquals(FootprintLevel.SHORT_VISIT, result.footprint.level)
    }

    @Test
    fun markAttractionVisit_cascadesCorrectly() {
        // 设置区域层级
        regionRepo.insertRegion(Region("510000", "四川省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("510100", "成都市", RegionLevel.CITY, "510000"))
        regionRepo.insertRegion(Region("510107", "武侯区", RegionLevel.DISTRICT, "510100"))

        val result = service.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.DEEP)
        assertTrue(result.isSuccess)

        // 区级应为 DEEP
        assertEquals(FootprintLevel.DEEP, footprintRepo.getFootprint("u1", "510107")!!.level)
        // 市级至少 PASS_BY
        assertTrue(footprintRepo.getFootprint("u1", "510100")!!.level >= FootprintLevel.PASS_BY)
        // 省级至少 PASS_BY
        assertTrue(footprintRepo.getFootprint("u1", "510000")!!.level >= FootprintLevel.PASS_BY)
    }

    @Test
    fun getCoverageStats_calculatesCorrectly() {
        // 标记几个足迹
        service.markFootprint("u1", "510000", FootprintLevel.DEEP)
        service.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)

        val stats = service.getCoverageStats("u1")
        assertEquals(2, stats.visitedProvinces)
    }
}
```

- [ ] **Step 2: 编写 SyncService 测试**

```kotlin
class SyncServiceTest {

    @Test
    fun syncPush_resolvesConflicts_higherLevelWins() {
        // 本地 PASS_BY，远端 DEEP → 合并后应为 DEEP
        val local = FootprintDto("u1", "510000", FootprintLevel.PASS_BY, 1000L)
        val remote = FootprintDto("u1", "510000", FootprintLevel.DEEP, 2000L)
        val resolved = SyncService.resolveFootprintConflict(local, remote)
        assertEquals(FootprintLevel.DEEP, resolved.level)
    }

    @Test
    fun syncDelta_returnsOnlyChangesSinceTimestamp() {
        // 验证增量同步只返回时间戳之后的变更
    }
}
```

- [ ] **Step 3: 实现 Services**

- [ ] **Step 4: 运行测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/service/ shared/src/commonTest/kotlin/com/mapchina/domain/service/
git commit -m "feat: add domain services with sync conflict resolution"
```

---

### Task 9: Koin 依赖注入配置

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/di/PlatformModule.kt` (expect)
- Create: `shared/src/androidMain/kotlin/com/mapchina/di/AndroidModule.kt` (actual)
- Create: `shared/src/iosMain/kotlin/com/mapchina/di/IosModule.kt` (actual)
- Test: `shared/src/commonTest/kotlin/com/mapchina/di/AppModuleTest.kt`

- [ ] **Step 1: 编写 DI 模块测试**

```kotlin
class AppModuleTest {

    @Test
    fun appModule_resolvesAllServices() {
        val koin = startKoin {
            modules(appModule, testPlatformModule)
        }.koin
        assertNotNull(koin.get<FootprintService>())
        assertNotNull(koin.get<AttractionService>())
        assertNotNull(koin.get<FootprintRepository>())
        assertNotNull(koin.get<RegionRepository>())
    }
}
```

- [ ] **Step 2: 实现 Koin 模块**

```kotlin
// AppModule.kt
val appModule = module {
    single { FootprintRepository(get()) }
    single { RegionRepository(get()) }
    single { AttractionRepository(get()) }
    single { FootprintService(get(), get()) }
    single { AttractionService(get()) }
    single { AuthService(get()) }
    single { SyncService(get(), get()) }
}

// PlatformModule.kt (expect)
expect val platformModule: Module
```

- [ ] **Step 3: 运行测试确认通过**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/di/ shared/src/androidMain/kotlin/com/mapchina/di/ shared/src/iosMain/kotlin/com/mapchina/di/
git commit -m "feat: add Koin dependency injection configuration"
```

---

## Chunk 5: Compose UI — 导航 & 主题 & 地图页

### Task 10: App 主题 & 导航骨架

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/theme/Theme.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/theme/Color.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/NavigationGraph.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/App.kt`
- Create: `androidApp/src/main/kotlin/com/mapchina/android/MainActivity.kt`
- Create: `androidApp/src/main/AndroidManifest.xml`

- [ ] **Step 1: 定义主题色彩**

```kotlin
// Color.kt
object MapChinaColors {
    // 足迹分级色
    val FootprintDeep = Color(0xFFE94560)
    val FootprintShortVisit = Color(0xFFFF6B6B)
    val FootprintPassBy = Color(0xFFFFA502)
    val FootprintUnvisited = Color(0xFF2D2D44)
    // 色块视图底色
    val BlockViewBackground = Color(0xFF1A1A2E)
    // 主色调
    val Primary = Color(0xFFE94560)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1A1A2E)
}
```

- [ ] **Step 2: 定义导航路由**

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Attractions : Screen("attractions")
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")
    data object RegionDetail : Screen("region/{regionId}") {
        fun createRoute(regionId: String) = "region/$regionId"
    }
    data object AttractionDetail : Screen("attraction/{attractionId}") {
        fun createRoute(attractionId: String) = "attraction/$attractionId"
    }
}
```

- [ ] **Step 3: 实现 NavigationGraph + 底部 Tab**

```kotlin
// App.kt
@Composable
fun MapChinaApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, "足迹地图") },
                    label = { Text("足迹地图") },
                    selected = currentRoute == Screen.Map.route,
                    onClick = { navController.navigate(Screen.Map.route) }
                )
                // 景点、统计、我的 Tab 同理
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Map.route, Modifier.padding(padding)) {
            composable(Screen.Map.route) { MapScreen(navController) }
            composable(Screen.Attractions.route) { AttractionsScreen(navController) }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.RegionDetail.route) { RegionDetailScreen(it.arguments?.getString("regionId")!!) }
        }
    }
}
```

- [ ] **Step 4: 创建 Android 入口 Activity**

- [ ] **Step 5: 运行 Android 应用确认骨架显示**

Run: `./gradlew :androidApp:installDebug`
Expected: 底部 4 个 Tab 可切换，页面显示占位内容

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/ androidApp/
git commit -m "feat: add Compose UI skeleton with navigation and theme"
```

---

### Task 11: MapController Expect/Actual & 色块视图

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/MapController.kt` (expect)
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/MapZoomLevel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/OverlayStyle.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/ColorBlockView.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/BreadcrumbNav.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/map/AndroidMapController.kt` (actual)
- Create: `shared/src/iosMain/kotlin/com/mapchina/map/IosMapController.kt` (actual)
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/map/ColorBlockViewTest.kt`

- [ ] **Step 1: 定义 MapController expect 接口**

```kotlin
// MapController.kt
expect class MapController() {
    fun addOverlay(region: Region, style: OverlayStyle)
    fun removeOverlay(regionId: String)
    fun addMarker(attraction: Attraction, visited: Boolean)
    fun removeMarker(attractionId: String)
    fun setCamera(region: Region, animated: Boolean)
    fun setOnRegionTapListener(listener: (String) -> Unit)
    fun setOnMarkerTapListener(listener: (String) -> Unit)
    fun setZoomLevel(level: MapZoomLevel)
    fun getZoomLevel(): MapZoomLevel
    fun dispose()
}

enum class MapZoomLevel { NATIONAL, PROVINCIAL, CITY, DISTRICT }

data class OverlayStyle(
    val fillColor: ULong,
    val strokeColor: ULong,
    val strokeWidth: Float = 2f,
    val alpha: Float = 0.6f
)
```

- [ ] **Step 2: 实现色块填色视图 (Compose Canvas)**

```kotlin
// ColorBlockView.kt
@Composable
fun ColorBlockView(
    regions: List<RegionFootprintUi>,
    onRegionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { offset ->
            val tapped = findRegionAt(offset, regions)
            tapped?.let { onRegionTap(it.region.id) }
        }
    }) {
        regions.forEach { item ->
            val color = when (item.footprintLevel) {
                FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
                FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
                FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
                null -> MapChinaColors.FootprintUnvisited
            }
            drawPath(item.simplifiedPath, color, alpha = 0.85f)
            drawPath(item.simplifiedPath, Color.White, style = Stroke(width = 1f), alpha = 0.3f)
        }
    }
}
```

- [ ] **Step 3: 实现面包屑导航**

```kotlin
// BreadcrumbNav.kt
@Composable
fun BreadcrumbNav(
    path: List<Region>,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState())) {
        IconButton(onClick = onNavigateUp) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回上级")
        }
        path.forEachIndexed { index, region ->
            if (index > 0) Text(" > ", color = Color.Gray)
            TextButton(onClick = { onNavigateTo(region.id) }) {
                Text(region.name, fontSize = 14.sp)
            }
        }
    }
}
```

- [ ] **Step 4: 编写色块视图点击检测测试**

```kotlin
class ColorBlockViewTest {

    @Test
    fun findRegionAt_pointInsidePolygon_returnsRegion() {
        val polygon = listOf(LatLng(0.0, 0.0), LatLng(1.0, 0.0), LatLng(1.0, 1.0), LatLng(0.0, 1.0))
        val result = pointInPolygon(LatLng(0.5, 0.5), polygon)
        assertTrue(result)
    }

    @Test
    fun findRegionAt_pointOutsidePolygon_returnsNull() {
        val polygon = listOf(LatLng(0.0, 0.0), LatLng(1.0, 0.0), LatLng(1.0, 1.0), LatLng(0.0, 1.0))
        val result = pointInPolygon(LatLng(2.0, 2.0), polygon)
        assertFalse(result)
    }
}
```

- [ ] **Step 5: 实现 AndroidMapController (高德地图)**

使用 AMap SDK，实现 `MapController` actual 类，对接高德地图的 `AMap` API。

- [ ] **Step 6: 实现 IosMapController (MapKit)**

使用 MKMapView，实现 `MapController` actual 类。

- [ ] **Step 7: 运行测试确认通过**

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/ shared/src/commonMain/kotlin/com/mapchina/ui/map/ shared/src/androidMain/kotlin/com/mapchina/map/ shared/src/iosMain/kotlin/com/mapchina/map/
git commit -m "feat: add MapController expect/actual and color block view"
```

---

### Task 12: 地图页 ViewModel & 交互逻辑

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/FootprintSheet.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`

- [ ] **Step 1: 编写 MapViewModel 测试**

```kotlin
class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var footprintService: FootprintService
    private lateinit var regionRepo: RegionRepository

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        footprintService = FootprintService(FootprintRepository(database), RegionRepository(database))
        regionRepo = RegionRepository(database)
        viewModel = MapViewModel(footprintService, regionRepo)
    }

    @Test
    fun drillIntoRegion_updatesCurrentLevel() {
        // 初始为全国级别
        assertEquals(MapZoomLevel.NATIONAL, viewModel.currentLevel.value)
        // 钻入四川
        viewModel.drillIntoRegion("510000")
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
    }

    @Test
    fun navigateUp_goesBackOneLevel() {
        viewModel.drillIntoRegion("510000")
        viewModel.drillIntoRegion("510100")
        viewModel.navigateUp()
        assertEquals(MapZoomLevel.PROVINCIAL, viewModel.currentLevel.value)
    }

    @Test
    fun markFootprint_updatesRegionState() {
        viewModel.markFootprint("510000", FootprintLevel.DEEP)
        val regions = viewModel.regions.value
        val sichuan = regions.find { it.region.id == "510000" }
        assertEquals(FootprintLevel.DEEP, sichuan?.footprintLevel)
    }

    @Test
    fun toggleViewMode_switchesBetweenMapAndBlock() {
        assertEquals(ViewMode.MAP, viewModel.viewMode.value)
        viewModel.toggleViewMode()
        assertEquals(ViewMode.BLOCK, viewModel.viewMode.value)
    }
}
```

- [ ] **Step 2: 实现 MapViewModel**

```kotlin
class MapViewModel(
    private val footprintService: FootprintService,
    private val regionRepository: RegionRepository
) : ViewModel() {
    private val _currentLevel = MutableStateFlow(MapZoomLevel.NATIONAL)
    val currentLevel: StateFlow<MapZoomLevel> = _currentLevel

    private val _currentPath = MutableStateFlow<List<Region>>(emptyList())
    val currentPath: StateFlow<List<Region>> = _currentPath

    private val _viewMode = MutableStateFlow(ViewMode.MAP)
    val viewMode: StateFlow<ViewMode> = _viewMode

    private val _regions = MutableStateFlow<List<RegionFootprintUi>>(emptyList())
    val regions: StateFlow<List<RegionFootprintUi>> = _regions

    fun drillIntoRegion(regionId: String) {
        val region = regionRepository.getRegion(regionId) ?: return
        _currentPath.value = _currentPath.value + region
        _currentLevel.value = when (region.level) {
            RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
            RegionLevel.CITY -> MapZoomLevel.CITY
            RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
        }
        loadChildRegions(regionId)
    }

    fun navigateUp() {
        if (_currentPath.value.size > 1) {
            _currentPath.value = _currentPath.value.dropLast(1)
            val parent = _currentPath.value.last()
            _currentLevel.value = when (parent.level) {
                null -> MapZoomLevel.NATIONAL
                RegionLevel.PROVINCE -> MapZoomLevel.PROVINCIAL
                RegionLevel.CITY -> MapZoomLevel.CITY
                RegionLevel.DISTRICT -> MapZoomLevel.DISTRICT
            }
        } else {
            _currentLevel.value = MapZoomLevel.NATIONAL
            _currentPath.value = emptyList()
            loadTopLevelRegions()
        }
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.MAP) ViewMode.BLOCK else ViewMode.MAP
    }

    fun markFootprint(regionId: String, level: FootprintLevel) {
        footprintService.markFootprint("currentUser", regionId, level)
        refreshRegions()
    }
}
```

- [ ] **Step 3: 实现 MapScreen Composable**

集成 MapController / ColorBlockView + BreadcrumbNav + ViewMode 切换按钮 + 覆盖率统计。

- [ ] **Step 4: 实现 FootprintSheet (底部弹出)**

显示区域信息、足迹状态、标记按钮（路过/短玩/深度）。

- [ ] **Step 5: 运行测试确认通过**

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/ shared/src/commonTest/kotlin/com/mapchina/ui/map/
git commit -m "feat: add map screen with drill-down and footprint marking"
```

---

## Chunk 6: 景点页 & 统计页 & 我的页

### Task 13: 景点浏览页

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionsScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionDetailScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionViewModelTest.kt`

- [ ] **Step 1: 编写景点 ViewModel 测试**

```kotlin
class AttractionViewModelTest {

    @Test
    fun loadAttractionsByRegion_returnsList() {
        // 验证按区域加载景点
    }

    @Test
    fun searchAttractions_returnsMatchingResults() {
        // 验证搜索功能
    }

    @Test
    fun markAttractionVisited_updatesState() {
        // 验证标记已访问
    }
}
```

- [ ] **Step 2: 实现 AttractionViewModel & Screen**

景点列表页：按区域分组显示、搜索栏、已访问/未访问筛选。
景点详情页：名称、级别、简介、位置、标记已访问按钮。

- [ ] **Step 3: 运行测试确认通过**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/attraction/
git commit -m "feat: add attraction browsing and detail screens"
```

---

### Task 14: 统计页

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/stats/StatsViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/stats/StatsScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/stats/StatsViewModelTest.kt`

- [ ] **Step 1: 编写统计 ViewModel 测试**

```kotlin
class StatsViewModelTest {

    @Test
    fun coverageStats_showsCorrectRatios() {
        // 标记 2 个省，验证 2/34
    }

    @Test
    fun attractionVisitCount_showsTotal() {
        // 验证景点打卡数统计
    }
}
```

- [ ] **Step 2: 实现 StatsScreen**

显示：省市区覆盖率（数字 + 进度条）、景点打卡率、足迹热力图预览。

- [ ] **Step 3: 运行测试确认通过**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/stats/
git commit -m "feat: add statistics screen with coverage stats"
```

---

### Task 15: 我的页 & 认证流程

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/LoginScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/SyncStatusIndicator.kt`

- [ ] **Step 1: 实现 LoginScreen**

手机号输入 → 发送验证码 → 输入验证码 → 登录成功跳转。

- [ ] **Step 2: 实现 ProfileScreen**

用户信息、同步状态指示器、同步按钮、设置项。

- [ ] **Step 3: 实现 SyncStatusIndicator**

顶部同步图标：已同步(绿) / 同步中(黄) / 离线(灰)。

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/profile/
git commit -m "feat: add profile and login screens with sync indicator"
```

---

## Chunk 7: 同步引擎 & GPS 集成

### Task 16: 离线同步引擎

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/sync/SyncEngine.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/sync/SyncStatus.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/sync/SyncEngineTest.kt`

- [ ] **Step 1: 编写同步引擎测试**

```kotlin
class SyncEngineTest {

    @Test
    fun pushChanges_sendsPendingItems() {
        // 验证队列中的项目被推送
    }

    @Test
    fun pullChanges_mergesRemoteUpdates() {
        // 验证远端变更被合并
    }

    @Test
    fun conflictResolution_higherFootprintLevelWins() {
        // 本地 PASS_BY，远端 DEEP → 合并为 DEEP
    }

    @Test
    fun retryWithBackoff_onFailure() {
        // 验证指数退避重试
    }

    @Test
    fun syncQueue_persistsAcrossRestart() {
        // 验证队列持久化
    }
}
```

- [ ] **Step 2: 实现 SyncEngine**

```kotlin
class SyncEngine(
    private val apiClient: MapChinaApiClient,
    private val database: MapChinaDatabase,
    private val syncStatus: MutableStateFlow<SyncStatus>
) {
    private val maxRetries = 5

    suspend fun pushChanges() {
        val pending = database.syncQueueQueries.SELECTPending(50).executeAsList()
        for (item in pending) {
            try {
                apiClient.pushSync(item.entity_type, item.payload)
                database.syncQueueQueries.DELETEById(item.id)
            } catch (e: Exception) {
                if (item.retry_count >= maxRetries) {
                    database.syncQueueQueries.DELETEById(item.id)
                } else {
                    database.syncQueueQueries.INCREMENT_RETRY(item.id)
                }
            }
        }
    }

    suspend fun pullChanges(sinceTimestamp: Long) {
        syncStatus.value = SyncStatus.SYNCING
        try {
            val delta = apiClient.getDelta(sinceTimestamp)
            mergeFootprints(delta.footprints)
            mergeAttractionVisits(delta.attractionVisits)
            syncStatus.value = SyncStatus.SYNCED
        } catch (e: Exception) {
            syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private fun mergeFootprints(remoteFootprints: List<FootprintDto>) {
        for (remote in remoteFootprints) {
            val local = database.footprintQueries
                .SELECTByUserAndRegion(remote.userId, remote.regionId)
                .executeAsOneOrNull()
            val effectiveLevel = if (local != null) {
                FootprintLevel.resolveConflict(
                    FootprintLevel.valueOf(local.level),
                    remote.level
                )
            } else {
                remote.level
            }
            database.footprintQueries.UPSERT(
                remote.userId, remote.regionId,
                effectiveLevel.name, remote.timestamp
            )
        }
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/sync/
git commit -m "feat: add offline-first sync engine with conflict resolution"
```

---

### Task 17: GPS 定位集成

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/location/LocationProvider.kt` (expect)
- Create: `shared/src/commonMain/kotlin/com/mapchina/location/LocationResult.kt`
- Create: `shared/src/androidMain/kotlin/com/mapchina/location/AndroidLocationProvider.kt` (actual)
- Create: `shared/src/iosMain/kotlin/com/mapchina/location/IosLocationProvider.kt` (actual)
- Test: `shared/src/commonTest/kotlin/com/mapchina/location/LocationProviderTest.kt`

- [ ] **Step 1: 定义 LocationProvider expect**

```kotlin
// LocationProvider.kt
expect class LocationProvider() {
    fun getCurrentLocation(callback: (Result<LocationResult>) -> Unit)
    fun requestPermissions()
    fun isLocationAvailable(): Boolean
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)
```

- [ ] **Step 2: 实现 Android Actual (FusedLocationProvider)**

- [ ] **Step 3: 实现 iOS Actual (CoreLocation)**

- [ ] **Step 4: 在 FootprintSheet 中集成 GPS**

打开标记 Sheet 时，如果 GPS 可用，高亮当前区域并显示"标记当前位置"快捷按钮。拒绝权限时隐藏该按钮。

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/location/ shared/src/androidMain/kotlin/com/mapchina/location/ shared/src/iosMain/kotlin/com/mapchina/location/
git commit -m "feat: add GPS location provider with platform implementations"
```

---

## Chunk 8: 预置数据 & 集成测试

### Task 18: 区域 & 景点数据准备

**Files:**
- Create: `data/regions/provinces.json`
- Create: `data/regions/cities.json`
- Create: `data/attractions/5a_4a_attractions.json`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/DataSeeder.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/seeder/ServerDataSeeder.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/DataSeederTest.kt`

- [ ] **Step 1: 准备省级行政区数据**

从民政部行政区划数据提取 34 个省级行政区 ID、名称、边界 GeoJSON（高度简化，每个省 < 100 个顶点）。

- [ ] **Step 2: 准备市级行政区数据**

333 个地级行政区，中等简化 GeoJSON。

- [ ] **Step 3: 准备 5A/4A 景点数据**

从文旅部公开数据提取约 1000+ 个 5A/4A 景点：名称、坐标(GCJ-02)、级别、所属区域。

- [ ] **Step 4: 实现 DataSeeder**

```kotlin
class DataSeeder(private val database: MapChinaDatabase) {
    fun seedRegions(regions: List<RegionDto>) {
        database.transaction {
            regions.forEach { region ->
                database.regionQueries.INSERT(
                    region.id, region.name, region.level.name,
                    region.parentId, null // boundary 按需下载
                )
            }
        }
    }

    fun seedAttractions(attractions: List<AttractionDto>) {
        database.transaction {
            attractions.forEach { attraction ->
                database.attractionQueries.INSERT(
                    attraction.id, attraction.name, attraction.regionId,
                    attraction.level.name, attraction.coordinate.lat,
                    attraction.coordinate.lng, attraction.description
                )
            }
        }
    }
}
```

- [ ] **Step 5: 编写 Seeder 测试**

验证省市区三级数据完整性和层级关系正确。

- [ ] **Step 6: 在后端也实现 ServerDataSeeder**

启动时将区域和景点数据初始化到 PostgreSQL。

- [ ] **Step 7: Commit**

```bash
git add data/ shared/src/commonMain/kotlin/com/mapchina/data/DataSeeder.kt server/src/main/kotlin/com/mapchina/server/seeder/
git commit -m "feat: add region and attraction data with seeder"
```

---

### Task 19: Ktor Client API 集成

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/remote/MapChinaApiClient.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/remote/AuthInterceptor.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/remote/MapChinaApiClientTest.kt`

- [ ] **Step 1: 实现 Ktor Client**

```kotlin
class MapChinaApiClient(private val httpClient: HttpClient) {

    suspend fun login(phone: String, code: String): AuthResult {
        val response = httpClient.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(phone, code))
        }
        return response.body<AuthResult>()
    }

    suspend fun getRegions(parentId: String?, level: String?, page: Int, size: Int): ApiResponse<List<RegionDto>> {
        val response = httpClient.get("/regions") {
            parentId?.let { parameter("parentId", it) }
            level?.let { parameter("level", it) }
            parameter("page", page)
            parameter("size", size)
        }
        return response.body()
    }

    suspend fun pushSync(payload: SyncPushRequest): ApiResponse<Unit> {
        val response = httpClient.post("/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return response.body()
    }

    suspend fun getDelta(since: Long): SyncDeltaResponse {
        val response = httpClient.get("/sync/delta") {
            parameter("since", since)
        }
        return response.body()
    }
}
```

- [ ] **Step 2: 实现 AuthInterceptor**

自动添加 Authorization header，刷新过期 Access Token。

- [ ] **Step 3: 编写 API Client 测试（使用 MockKtor）**

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/data/remote/
git commit -m "feat: add Ktor API client with auth interceptor"
```

---

### Task 20: 端到端集成测试

**Files:**
- Create: `shared/src/commonTest/kotlin/com/mapchina/integration/EndToEndTest.kt`

- [ ] **Step 1: 编写端到端流程测试**

```kotlin
class EndToEndTest {

    @Test
    fun fullFlow_markFootprintAndSync() {
        // 1. 初始化本地数据库 + seeder
        val database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        DataSeeder(database).seedRegions(testRegions())

        // 2. 标记足迹
        val service = FootprintService(FootprintRepository(database), RegionRepository(database))
        service.markFootprint("u1", "510000", FootprintLevel.DEEP)

        // 3. 验证本地状态
        val fp = FootprintRepository(database).getFootprint("u1", "510000")
        assertEquals(FootprintLevel.DEEP, fp!!.level)

        // 4. 验证同步队列生成
        val pending = database.syncQueueQueries.SELECTPending(10).executeAsList()
        assertEquals(1, pending.size)
        assertEquals("FOOTPRINT", pending.first().entity_type)
    }

    @Test
    fun fullFlow_markAttractionVisit_cascadesCorrectly() {
        // 完整的景点访问 → 区域足迹级联流程
    }

    @Test
    fun fullFlow_syncConflictResolution() {
        // 本地 DEEP，远端 PASS_BY → 合并后 DEEP
    }
}
```

- [ ] **Step 2: 运行所有测试确认通过**

Run: `./gradlew :shared:allTests :server:test`
Expected: ALL PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/mapchina/integration/
git commit -m "test: add end-to-end integration tests"
```

---

## 任务依赖图

```
Task 1 (项目脚手架 & DTO)
  └→ Task 2 (领域模型)
       └→ Task 3 (SQLDelight Schema)
            └→ Task 4 (Repository 层)
                 ├→ Task 8 (Domain Services)
                 │    └→ Task 9 (Koin DI)
                 │         ├→ Task 12 (MapViewModel)
                 │         ├→ Task 13 (AttractionViewModel)
                 │         ├→ Task 14 (StatsViewModel)
                 │         └→ Task 15 (Profile & Auth)
                 └→ Task 16 (Sync Engine)

Task 5 (Ktor Server 初始化)
  └→ Task 6 (JWT Auth 路由)
       └→ Task 7 (数据 & 足迹路由)

Task 10 (UI 骨架 & 导航) — 可与 Task 5-7 并行
  └→ Task 11 (MapController & 色块视图)
       └→ Task 12 (MapViewModel)

Task 17 (GPS 集成) — 依赖 Task 11
Task 18 (预置数据) — 可与 Task 10+ 并行
Task 19 (Ktor Client) — 依赖 Task 7
Task 20 (集成测试) — 依赖所有其他 Task
```

## 可并行的任务组

| 组 | 可并行的任务 |
|----|-------------|
| A | Task 5, Task 10, Task 18 (后端、UI骨架、数据准备互不依赖) |
| B | Task 11, Task 6 (地图组件、Auth路由互不依赖) |
| C | Task 13, Task 14, Task 15 (景点页、统计页、我的页互不依赖) |
| D | Task 21 (iOS), Task 22 (区级下载), Task 23 (R-tree) 互不依赖 |

---

## Chunk 9: 关键缺失功能（审查修复）

### Task 21: iOS 应用入口

**Files:**
- Create: `iosApp/iosApp.xcodeproj/project.pbxproj`
- Create: `iosApp/iosApp/ContentView.swift`
- Create: `iosApp/iosApp/SceneDelegate.swift`
- Create: `iosApp/iosApp/Info.plist`
- Create: `iosApp/iosApp/iOSApp.swift`

- [ ] **Step 1: 创建 Xcode 项目结构**

使用 Compose Multiplatform iOS 集成模板，设置 SwiftUI hosting 层来承载 Compose 内容。

- [ ] **Step 2: 实现 iOSApp.swift (App 入口)**

```swift
import SwiftUI
import shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
```

- [ ] **Step 3: 实现 ComposeView (SwiftUI → Compose 桥接)**

```swift
struct ComposeView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        MainViewControllerKt.mainViewController()
    }
    func updateUIView(_ uiView: UIView, context: Context) {}
}
```

- [ ] **Step 4: 在 shared/iosMain 添加 MainViewController 工厂**

```kotlin
// shared/src/iosMain/kotlin/com/mapchina/MainViewController.kt
fun mainViewController(): UIViewController {
    return ComposeUIViewController {
        MapChinaApp()
    }
}
```

- [ ] **Step 5: 验证 iOS 编译**

Run: `./gradlew :shared:iosSimulatorArm64Test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add iosApp/ shared/src/iosMain/kotlin/com/mapchina/MainViewController.kt
git commit -m "feat: add iOS app entry point with Compose Multiplatform hosting"
```

---

### Task 22: 区级 GeoJSON 按需下载

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/repository/BoundaryRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/data/remote/BoundaryApiClient.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/data/repository/BoundaryRepositoryTest.kt`

- [ ] **Step 1: 编写 BoundaryRepository 测试**

```kotlin
class BoundaryRepositoryTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var repository: BoundaryRepository

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        repository = BoundaryRepository(database, FakeBoundaryApiClient())
    }

    @Test
    fun getBoundary_cachedLocally_returnsFromCache() {
        // 先缓存省级边界
        database.regionQueries.INSERT("510000", "四川省", "PROVINCE", null, """{"type":"Polygon"}""")
        val boundary = repository.getBoundary("510000")
        assertNotNull(boundary)
    }

    @Test
    fun getBoundary_notCached_downloadsAndStores() {
        // 未缓存的区级边界，触发下载后存入本地
        database.regionQueries.INSERT("510107", "武侯区", "DISTRICT", "510100", null)
        val boundary = repository.getBoundary("510107")
        assertNotNull(boundary)
        // 验证已缓存
        val cached = database.regionQueries.SELECTById("510107").executeAsOne()
        assertNotNull(cached.boundary_json)
    }

    @Test
    fun downloadAllDistrictsForProvince_downloadsAllMissing() {
        // 进入某省时，下载该省所有缺失的区级边界
        insertDistrictsUnderProvince()
        val downloaded = repository.ensureDistrictBoundaries("510000")
        assertTrue(downloaded > 0)
    }

    @Test
    fun boundaryDownloadFailure_returnsCachedOrNull() {
        // 下载失败时返回已缓存数据或 null，不抛异常
        val failingClient = FailingBoundaryApiClient()
        val repo = BoundaryRepository(database, failingClient)
        val result = repo.getBoundary("510107")
        assertNull(result) // 未缓存 + 下载失败 → null
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

- [ ] **Step 3: 实现 BoundaryRepository**

```kotlin
class BoundaryRepository(
    private val database: MapChinaDatabase,
    private val apiClient: BoundaryApiClient
) {
    suspend fun getBoundary(regionId: String): String? {
        val cached = database.regionQueries.SELECTById(regionId).executeAsOneOrNull()
        if (cached?.boundary_json != null) return cached.boundary_json

        return try {
            val boundary = apiClient.fetchBoundary(regionId)
            database.regionQueries.updateBoundary(boundary, regionId)
            boundary
        } catch (e: Exception) {
            null
        }
    }

    suspend fun ensureDistrictBoundaries(provinceId: String): Int {
        val districts = database.regionQueries.SELECTByParentIdRecursive(provinceId).executeAsList()
            .filter { it.level == "DISTRICT" && it.boundary_json == null }

        var downloaded = 0
        for (district in districts) {
            try {
                val boundary = apiClient.fetchBoundary(district.id)
                database.regionQueries.updateBoundary(boundary, district.id)
                downloaded++
            } catch (_: Exception) { continue }
        }
        return downloaded
    }
}
```

- [ ] **Step 4: 在 MapViewModel 中集成：钻入省份时触发区级下载**

MapViewModel.drillIntoRegion 在进入省级视图后调用 `boundaryRepository.ensureDistrictBoundaries(provinceId)`，并在 UI 显示下载进度。

- [ ] **Step 5: 运行测试确认通过**

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/data/repository/BoundaryRepository.kt shared/src/commonMain/kotlin/com/mapchina/data/remote/BoundaryApiClient.kt shared/src/commonTest/kotlin/com/mapchina/data/repository/BoundaryRepositoryTest.kt
git commit -m "feat: add on-demand district boundary download with caching"
```

---

### Task 23: R-tree 空间索引 & 点击检测

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/SpatialIndex.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/map/RTree.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/map/SpatialIndexTest.kt`

- [ ] **Step 1: 编写空间索引测试**

```kotlin
class SpatialIndexTest {

    @Test
    fun insertAndQuery_returnsContainingRegion() {
        val index = RTree<String>()
        index.insert(BoundingBox(0.0, 0.0, 10.0, 10.0), "region_a")
        index.insert(BoundingBox(20.0, 20.0, 30.0, 30.0), "region_b")

        val results = index.query(5.0, 5.0)
        assertEquals(listOf("region_a"), results)
    }

    @Test
    fun queryOutsideAll_returnsEmpty() {
        val index = RTree<String>()
        index.insert(BoundingBox(0.0, 0.0, 10.0, 10.0), "region_a")
        val results = index.query(50.0, 50.0)
        assertTrue(results.isEmpty())
    }

    @Test
    fun queryOverlapping_returnsMultiple() {
        val index = RTree<String>()
        index.insert(BoundingBox(0.0, 0.0, 15.0, 15.0), "region_a")
        index.insert(BoundingBox(10.0, 10.0, 25.0, 25.0), "region_b")
        val results = index.query(12.0, 12.0)
        assertTrue(results.containsAll(listOf("region_a", "region_b")))
    }

    @Test
    fun pointInPolygon_complexPolygon_correctDetection() {
        val polygon = listOf(
            LatLng(0.0, 0.0), LatLng(10.0, 0.0), LatLng(10.0, 5.0),
            LatLng(5.0, 5.0), LatLng(5.0, 10.0), LatLng(0.0, 10.0)
        )
        assertTrue(pointInPolygon(LatLng(3.0, 3.0), polygon))
        assertFalse(pointInPolygon(LatLng(7.0, 7.0), polygon)) // 在凹口区域外
    }

    @Test
    fun twoPhaseDetection_bboxFilterThenPolygonVerify() {
        val regions = listOf(
            RegionBounds("a", BoundingBox(0.0, 0.0, 10.0, 10.0), complexPolygonA()),
            RegionBounds("b", BoundingBox(20.0, 20.0, 30.0, 30.0), complexPolygonB())
        )
        val index = buildSpatialIndex(regions)

        // Phase 1: R-tree bbox filter
        val candidates = index.query(5.0, 5.0)
        assertEquals(1, candidates.size)

        // Phase 2: Polygon verify
        val hit = candidates.firstOrNull { pointInPolygon(LatLng(5.0, 5.0), regions.find { r -> r.id == it }!!.polygon) }
        assertEquals("a", hit)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

- [ ] **Step 3: 实现 RTree 和 SpatialIndex**

```kotlin
data class BoundingBox(
    val minLat: Double, val minLng: Double,
    val maxLat: Double, val maxLng: Double
) {
    fun contains(lat: Double, lng: Double): Boolean =
        lat in minLat..maxLat && lng in minLng..maxLng
}

class RTree<T>(private val maxEntries: Int = 10) {
    private val entries = mutableListOf<Pair<BoundingBox, T>>()

    fun insert(bbox: BoundingBox, value: T) {
        entries.add(bbox to value)
        // 简化实现: 线性搜索; 生产环境可用 R-tree 分裂算法
        // 对于预构建索引数据场景，线性搜索在 <4000 条目下性能可接受
    }

    fun query(lat: Double, lng: Double): List<T> =
        entries.filter { (bbox, _) -> bbox.contains(lat, lng) }.map { it.second }
}

fun pointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].lat; val yi = polygon[i].lng
        val xj = polygon[j].lat; val yj = polygon[j].lng
        if (((yi > point.lng) != (yj > point.lng)) &&
            (point.lat < (xj - xi) * (point.lng - yi) / (yj - yi) + xi)) {
            inside = !inside
        }
        j = i
    }
    return inside
}

fun buildSpatialIndex(regions: List<RegionBounds>): RTree<String> {
    val tree = RTree<String>()
    regions.forEach { region ->
        tree.insert(region.bbox, region.id)
    }
    return tree
}
```

- [ ] **Step 4: 在 ColorBlockView 中集成两阶段点击检测**

替换原有简单 `findRegionAt` 为 R-tree bbox 过滤 → polygon 精确验证。

- [ ] **Step 5: 运行测试确认通过**

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/map/SpatialIndex.kt shared/src/commonMain/kotlin/com/mapchina/map/RTree.kt shared/src/commonTest/kotlin/com/mapchina/map/SpatialIndexTest.kt
git commit -m "feat: add R-tree spatial index with two-phase click detection"
```

---

### Task 24: GCJ-02 坐标系合规

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/coord/CoordinateConverter.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/coord/CoordinateConverterTest.kt`

- [ ] **Step 1: 编写 WGS-84 → GCJ-02 转换测试**

```kotlin
class CoordinateConverterTest {

    @Test
    fun wgs84ToGcj02_knownBeijingOffset() {
        // 天安门 WGS-84: 39.9087, 116.3975
        val (lat, lng) = CoordinateConverter.wgs84ToGcj02(39.9087, 116.3975)
        // GCJ-02 应有约 500m 偏移
        assertNotEquals(39.9087, lat)
        assertNotEquals(116.3975, lng)
        // 偏移应在合理范围内 (<0.01 度 ≈ 1km)
        assertTrue(abs(lat - 39.9087) < 0.01)
        assertTrue(abs(lng - 116.3975) < 0.01)
    }

    @Test
    fun gcj02ToWgs84_roundTripApproximate() {
        val originalLat = 39.9087
        val originalLng = 116.3975
        val (gcjLat, gcjLng) = CoordinateConverter.wgs84ToGcj02(originalLat, originalLng)
        val (wgsLat, wgsLng) = CoordinateConverter.gcj02ToWgs84(gcjLat, gcjLng)
        // 逆向转换应接近原始值 (精度约 1-2m)
        assertTrue(abs(wgsLat - originalLat) < 0.0001)
        assertTrue(abs(wgsLng - originalLng) < 0.0001)
    }

    @Test
    fun isOutOfChina_overseasCoordinates() {
        assertFalse(CoordinateConverter.isInChina(40.0, -74.0)) // 纽约
        assertTrue(CoordinateConverter.isInChina(39.9, 116.4)) // 北京
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

- [ ] **Step 3: 实现 CoordinateConverter**

```kotlin
object CoordinateConverter {
    private const val A = 6378245.0 // 长半轴
    private const val EE = 0.00669342162296594323 // 扁率

    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (!isInChina(lat, lng)) return lat to lng
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * Math.PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * Math.PI)
        return (lat + dLat) to (lng + dLng)
    }

    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
        return (lat * 2 - gcjLat) to (lng * 2 - gcjLng)
    }

    fun isInChina(lat: Double, lng: Double): Boolean =
        lng in 73.66..135.05 && lat in 3.86..53.55

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * abs(x)
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * Math.PI) + 40.0 * sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * Math.PI) + 320.0 * sin(y * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * abs(x)
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * Math.PI) + 40.0 * sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * Math.PI) + 300.0 * sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }
}
```

- [ ] **Step 4: 在 GPS 定位中集成转换**

LocationProvider 返回 WGS-84 坐标后，使用 `CoordinateConverter.wgs84ToGcj02()` 转换为 GCJ-02 再用于地图定位和区域匹配。

- [ ] **Step 5: 在景点数据中标注坐标系**

DataSeeder 中景点坐标必须为 GCJ-02，添加文档注释说明数据源的坐标系要求。

- [ ] **Step 6: 运行测试确认通过**

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/coord/ shared/src/commonTest/kotlin/com/mapchina/coord/
git commit -m "feat: add GCJ-02 coordinate converter for Chinese mapping compliance"
```

---

### Task 25: JWT Token 安全存储 & 轮换 & 后端频率限制

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/auth/TokenStorage.kt` (expect)
- Create: `shared/src/androidMain/kotlin/com/mapchina/auth/AndroidTokenStorage.kt` (actual)
- Create: `shared/src/iosMain/kotlin/com/mapchina/auth/IosTokenStorage.kt` (actual)
- Create: `shared/src/commonMain/kotlin/com/mapchina/auth/TokenManager.kt`
- Create: `server/src/main/kotlin/com/mapchina/server/auth/RateLimiter.kt`
- Modify: `server/src/main/kotlin/com/mapchina/server/auth/JwtConfig.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/auth/TokenManagerTest.kt`
- Test: `server/src/test/kotlin/com/mapchina/server/auth/RateLimiterTest.kt`

- [ ] **Step 1: 编写 TokenManager 测试**

```kotlin
class TokenManagerTest {

    @Test
    fun accessTokenExpired_returnsTrue() {
        val manager = TokenManager(FakeTokenStorage())
        manager.saveTokens("access", "refresh", expiresAt = 0L)
        assertTrue(manager.isAccessTokenExpired())
    }

    @Test
    fun refreshTokenRotation_savesNewRefreshToken() {
        val storage = FakeTokenStorage()
        val manager = TokenManager(storage)
        manager.saveTokens("access1", "refresh1", expiresAt = 1000L)
        manager.rotateRefreshToken("access2", "refresh2", expiresAt = 2000L)
        assertEquals("refresh2", storage.getRefreshToken())
    }

    @Test
    fun clearTokens_removesAll() {
        val storage = FakeTokenStorage()
        val manager = TokenManager(storage)
        manager.saveTokens("a", "r", 1000L)
        manager.clearTokens()
        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
    }
}
```

- [ ] **Step 2: 编写后端 RateLimiter 测试**

```kotlin
class RateLimiterTest {

    @Test
    fun underLimit_allowsRequest() {
        val limiter = RateLimiter(maxRequests = 10, windowMs = 60_000)
        assertTrue(limiter.tryAcquire("user1"))
    }

    @Test
    fun overLimit_rejectsRequest() {
        val limiter = RateLimiter(maxRequests = 2, windowMs = 60_000)
        limiter.tryAcquire("user1")
        limiter.tryAcquire("user1")
        assertFalse(limiter.tryAcquire("user1"))
    }

    @Test
    fun differentUsers_independentLimits() {
        val limiter = RateLimiter(maxRequests = 1, windowMs = 60_000)
        limiter.tryAcquire("user1")
        assertTrue(limiter.tryAcquire("user2"))
    }
}
```

- [ ] **Step 3: 实现 TokenStorage (Expect/Actual)**

```kotlin
// commonMain - expect
expect class TokenStorage() {
    fun saveAccessToken(token: String?)
    fun getAccessToken(): String?
    fun saveRefreshToken(token: String?)
    fun getRefreshToken(): String?
}

// androidMain - actual (EncryptedSharedPreferences)
actual class TokenStorage(private val context: Context) {
    actual fun saveAccessToken(token: String?) { /* EncryptedSharedPreferences */ }
    actual fun getAccessToken(): String? { /* EncryptedSharedPreferences */ }
    actual fun saveRefreshToken(token: String?) { /* EncryptedSharedPreferences */ }
    actual fun getRefreshToken(): String? { /* EncryptedSharedPreferences */ }
}

// iosMain - actual (Keychain)
actual class TokenStorage {
    actual fun saveAccessToken(token: String?) { /* Keychain kSecClassGenericPassword */ }
    actual fun getAccessToken(): String? { /* Keychain */ }
    actual fun saveRefreshToken(token: String?) { /* Keychain */ }
    actual fun getRefreshToken(): String? { /* Keychain */ }
}
```

- [ ] **Step 4: 实现 TokenManager**

管理 Access Token (30 分钟 TTL, 存内存) 和 Refresh Token (30 天 TTL, 存安全存储)，自动刷新过期 Token。

- [ ] **Step 5: 更新 JwtConfig 设置 TTL**

```kotlin
// JwtConfig.kt
val accessTokenTTL = 30.minutes
val refreshTokenTTL = 30.days
// Refresh Token 轮换: 每次刷新颁发新 refresh token，旧 token 加入黑名单
```

- [ ] **Step 6: 实现 RateLimiter 并集成到路由**

在 AuthRoutes 和其他路由中添加频率限制检查。

- [ ] **Step 7: 运行所有测试确认通过**

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/auth/ shared/src/androidMain/kotlin/com/mapchina/auth/ shared/src/iosMain/kotlin/com/mapchina/auth/ server/src/main/kotlin/com/mapchina/server/auth/
git commit -m "feat: add secure token storage, rotation, and API rate limiting"
```

---

### Task 26: 错误处理 UI & 缩放阈值逻辑 & 色块视图覆盖率 & 景点长按

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/common/ErrorScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/common/LoadingScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/common/EmptyStateScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/CoverageOverlay.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/ZoomThresholdHandler.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/AttractionDetailCard.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/map/ZoomThresholdHandlerTest.kt`

- [ ] **Step 1: 编写缩放阈值测试**

```kotlin
class ZoomThresholdHandlerTest {

    @Test
    fun zoomBelow6_nationalLevel() {
        assertEquals(MapZoomLevel.NATIONAL, ZoomThresholdHandler.levelForZoom(5.5f))
    }

    @Test
    fun zoom6To9_provincialLevel() {
        assertEquals(MapZoomLevel.PROVINCIAL, ZoomThresholdHandler.levelForZoom(7.0f))
    }

    @Test
    fun zoom9To12_cityLevel() {
        assertEquals(MapZoomLevel.CITY, ZoomThresholdHandler.levelForZoom(10.0f))
    }

    @Test
    fun zoomAbove12_districtLevel() {
        assertEquals(MapZoomLevel.DISTRICT, ZoomThresholdHandler.levelForZoom(13.0f))
    }

    @Test
    fun zoomCrossesThreshold_returnsPrompt() {
        val current = MapZoomLevel.NATIONAL
        val zoomLevel = 7.0f
        val result = ZoomThresholdHandler.evaluateZoomChange(current, zoomLevel)
        assertEquals(ZoomPrompt.ENTER_REGION, result)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

- [ ] **Step 3: 实现 ZoomThresholdHandler**

```kotlin
object ZoomThresholdHandler {
    fun levelForZoom(zoom: Float): MapZoomLevel = when {
        zoom < 6 -> MapZoomLevel.NATIONAL
        zoom < 9 -> MapZoomLevel.PROVINCIAL
        zoom < 12 -> MapZoomLevel.CITY
        else -> MapZoomLevel.DISTRICT
    }

    fun evaluateZoomChange(currentLevel: MapZoomLevel, newZoom: Float): ZoomPrompt? {
        val newLevel = levelForZoom(newZoom)
        return if (newLevel != currentLevel) ZoomPrompt.ENTER_REGION else null
    }
}

enum class ZoomPrompt { ENTER_REGION }
```

- [ ] **Step 4: 实现通用错误 UI 组件**

- `ErrorScreen`: 全屏错误页 + 重试按钮（用于 GeoJSON 加载失败等）
- `LoadingScreen`: 骨架屏 / 加载指示器
- `EmptyStateScreen`: 引导提示文字

- [ ] **Step 5: 实现 CoverageOverlay (色块视图覆盖率)**

在色块视图左上角显示 "8/34 省 · 23/333 市" 统计信息。

- [ ] **Step 6: 实现 AttractionDetailCard (景点长按详情卡)**

长按景点锚点弹出的详情卡片，包含名称、级别、已访问状态、标记按钮。

- [ ] **Step 7: 运行测试确认通过**

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/common/ shared/src/commonMain/kotlin/com/mapchina/ui/map/CoverageOverlay.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/ZoomThresholdHandler.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/AttractionDetailCard.kt shared/src/commonTest/kotlin/com/mapchina/ui/map/ZoomThresholdHandlerTest.kt
git commit -m "feat: add error UI, zoom thresholds, coverage overlay, and attraction detail card"
```

---

### Task 27: 补充缺失测试 (Task 15, 17, 13, 14)

**Files:**
- Test: `shared/src/commonTest/kotlin/com/mapchina/ui/profile/ProfileViewModelTest.kt`
- Test: `shared/src/commonTest/kotlin/com/mapchina/location/LocationHelperTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionViewModelTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/stats/StatsViewModelTest.kt`

- [ ] **Step 1: 编写 ProfileViewModel 测试**

```kotlin
class ProfileViewModelTest {

    @Test
    fun login_withPhoneAndCode_savesTokens() {
        val viewModel = ProfileViewModel(FakeAuthService(), FakeTokenManager())
        viewModel.login("13800138000", "123456")
        assertEquals(LoginState.SUCCESS, viewModel.loginState.value)
    }

    @Test
    fun logout_clearsTokensAndFootprints() {
        val viewModel = ProfileViewModel(FakeAuthService(), FakeTokenManager())
        viewModel.logout()
        assertEquals(LoginState.LOGGED_OUT, viewModel.loginState.value)
    }

    @Test
    fun syncStatus_reflectsSyncEngineState() {
        val syncStatus = MutableStateFlow(SyncStatus.SYNCING)
        val viewModel = ProfileViewModel(FakeAuthService(), FakeTokenManager(), syncStatus)
        assertEquals(SyncStatus.SYNCING, viewModel.syncStatus.value)
    }
}
```

- [ ] **Step 2: 编写 LocationHelper 测试**

```kotlin
class LocationHelperTest {

    @Test
    fun locationAvailable_showsMarkCurrentButton() {
        val state = LocationUiState(available = true, currentRegionId = "510000")
        assertTrue(state.showMarkCurrentButton)
    }

    @Test
    fun locationUnavailable_hidesMarkCurrentButton() {
        val state = LocationUiState(available = false, currentRegionId = null)
        assertFalse(state.showMarkCurrentButton)
    }

    @Test
    fun wgs84ConvertedToGcj02_beforeRegionLookup() {
        val wgs84 = LatLng(39.9087, 116.3975)
        val gcj02 = CoordinateConverter.wgs84ToGcj02(wgs84.lat, wgs84.lng)
        // 验证转换后的坐标用于区域匹配
        assertNotEquals(wgs84.lat, gcj02.first)
    }
}
```

- [ ] **Step 3: 补充 AttractionViewModel 完整测试**

```kotlin
class AttractionViewModelTest {

    private lateinit var viewModel: AttractionViewModel
    private lateinit var attractionService: AttractionService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        attractionService = AttractionService(AttractionRepository(database))
        viewModel = AttractionViewModel(attractionService)
    }

    @Test
    fun loadAttractionsByRegion_returnsList() {
        seedAttractions()
        viewModel.loadByRegion("510000")
        assertTrue(viewModel.attractions.value.isNotEmpty())
    }

    @Test
    fun searchAttractions_returnsMatchingResults() {
        seedAttractions()
        viewModel.search("九寨")
        val results = viewModel.attractions.value
        assertTrue(results.any { it.name.contains("九寨") })
    }

    @Test
    fun markAttractionVisited_updatesState() {
        seedAttractions()
        viewModel.markVisited("attr1", FootprintLevel.DEEP)
        val attraction = viewModel.attractions.value.find { it.id == "attr1" }
        assertTrue(attraction!!.visited)
    }
}
```

- [ ] **Step 4: 补充 StatsViewModel 完整测试**

```kotlin
class StatsViewModelTest {

    private lateinit var viewModel: StatsViewModel
    private lateinit var footprintService: FootprintService

    @BeforeTest
    fun setup() {
        val database = MapChinaDatabase(InMemoryDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        footprintService = FootprintService(FootprintRepository(database), regionRepo)
        viewModel = StatsViewModel(footprintService)
    }

    @Test
    fun coverageStats_showsCorrectRatios() {
        seedRegions()
        footprintService.markFootprint("u1", "510000", FootprintLevel.DEEP)
        footprintService.markFootprint("u1", "110000", FootprintLevel.SHORT_VISIT)
        viewModel.refreshStats()
        val stats = viewModel.stats.value
        assertEquals(2, stats.visitedProvinces)
        assertEquals(34, stats.totalProvinces)
    }

    @Test
    fun attractionVisitCount_showsTotal() {
        seedAttractions()
        footprintService.markAttractionVisit("u1", "attr1", "510107", FootprintLevel.DEEP)
        viewModel.refreshStats()
        val stats = viewModel.stats.value
        assertEquals(1, stats.visitedAttractions)
    }
}
```

- [ ] **Step 5: 运行所有测试确认通过**

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonTest/
git commit -m "test: add missing tests for profile, location, attraction, and stats viewmodels"
```

---

## 更新后的任务依赖图

```
Task 1 (项目脚手架 & DTO)
  └→ Task 2 (领域模型)
       └→ Task 3 (SQLDelight Schema)
            └→ Task 4 (Repository 层)
                 ├→ Task 8 (Domain Services)
                 │    └→ Task 9 (Koin DI)
                 │         ├→ Task 12 (MapViewModel)
                 │         ├→ Task 13 (景点 ViewModel) ← Task 27 补充测试
                 │         ├→ Task 14 (统计 ViewModel) ← Task 27 补充测试
                 │         └→ Task 15 (我的 & Auth) ← Task 27 补充测试
                 └→ Task 16 (Sync Engine)

Task 5 (Ktor Server 初始化)
  └→ Task 6 (JWT Auth 路由) ← Task 25 (Token 安全 & Rate Limit) 扩展
       └→ Task 7 (数据 & 足迹路由)

Task 10 (UI 骨架 & 导航) — 可与 Task 5-7 并行
  └→ Task 11 (MapController & 色块视图) ← Task 23 (R-tree) 集成
       └→ Task 12 (MapViewModel) ← Task 26 (错误UI, 缩放阈值, 覆盖率) 扩展

Task 17 (GPS 集成) — 依赖 Task 11, Task 24 (GCJ-02) ← Task 27 补充测试
Task 18 (预置数据) — 可与 Task 10+ 并行
Task 19 (Ktor Client) — 依赖 Task 7
Task 20 (集成测试) — 依赖所有其他 Task

Task 21 (iOS 入口) — 依赖 Task 10
Task 22 (区级下载) — 依赖 Task 4, Task 19
Task 24 (GCJ-02) — 依赖 Task 2
Task 25 (Token 安全 & Rate Limit) — 依赖 Task 6
Task 26 (错误UI & 缩放 & 覆盖率) — 依赖 Task 11, Task 12
Task 27 (补充测试) — 依赖 Task 13, 14, 15, 17
```
