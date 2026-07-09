# MapChina Cloud Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement end-to-end local-first cloud sync for MapChina user data and deploy the Ktor backend locally against PostgreSQL.

**Architecture:** The server stores generic per-user `sync_items` records keyed by `(user_id, entity_type, entity_id)`. The client keeps SQLDelight as local source of truth, persists offline changes in `sync_queue`, pushes generic batches, and merges remote deltas back into local tables without re-enqueueing pulled records.

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Ktor Client, Ktor Server, Exposed, PostgreSQL, H2 tests, Gradle, native macOS PostgreSQL tools.

## Global Constraints

- Preserve unrelated dirty worktree changes, including `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- Keep the app local-first: local writes must succeed even when no server exists.
- Sync V1 covers `FOOTPRINT`, `ATTRACTION_VISIT`, `CARVING`, `JOURNAL`, `JOURNAL_PHOTO`, `JOURNAL_TRACK_POINT`, and `APP_SETTING`.
- V1 syncs image paths and photo metadata only, not binary files.
- JWT auth remains required for `/sync/*`.
- Android runtime/UI changes require `./gradlew installDebug`, app launch, adb input, and screenshots per `AGENTS.md`.

---

### Task 1: Server Generic Sync Schema And Routes

**Files:**
- Modify: `server/src/main/kotlin/com/mapchina/server/database/Tables.kt`
- Modify: `server/src/main/kotlin/com/mapchina/server/database/DatabaseFactory.kt`
- Modify: `server/src/main/kotlin/com/mapchina/server/routes/DataRoutes.kt`
- Modify: `server/src/test/kotlin/com/mapchina/server/database/TablesTest.kt`
- Create: `server/src/test/kotlin/com/mapchina/server/routes/SyncRoutesTest.kt`

**Interfaces:**
- Produces: `object SyncItems : Table("sync_items")`
- Produces: `@Serializable data class GenericSyncPushRequest(val items: List<GenericSyncItem> = emptyList())`
- Produces: `@Serializable data class GenericSyncItem(...)`
- Produces: `@Serializable data class GenericSyncPushResponse(val accepted: Int, val serverTime: Long)`
- Produces: `@Serializable data class GenericSyncDeltaResponse(val items: List<GenericSyncItem>, val serverTime: Long)`

- [ ] Write failing server schema test requiring `SYNC_ITEMS`.
- [ ] Write failing route test that logs in, posts two generic sync items, pulls with `since=0`, and sees both items.
- [ ] Add `SyncItems` table with columns `userId`, `entityType`, `entityId`, `operation`, `payload`, `updatedAt`, `deleted`.
- [ ] Include `SyncItems` in `SchemaUtils.create(...)`.
- [ ] Update `/sync/push` to accept `GenericSyncPushRequest` as the primary contract and upsert by user/entity key.
- [ ] Keep typed footprint/attraction visit support by translating typed request fields into generic sync items when `items` is empty.
- [ ] Update `/sync/pull` to return generic items changed after `since`.
- [ ] Run `./gradlew :server:test --tests "com.mapchina.server.database.TablesTest" --tests "com.mapchina.server.routes.SyncRoutesTest"`.

### Task 2: Shared Sync DTOs, API Client, And Engine

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/sync/SyncDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/remote/MapChinaApiClient.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/sync/SyncEngine.kt`
- Modify: `shared/src/androidUnitTest/kotlin/com/mapchina/sync/SyncEngineTest.kt`

**Interfaces:**
- Produces: `enum class SyncEntityType`
- Produces: `enum class SyncOperation`
- Produces: `@Serializable data class SyncQueueItem(...)`
- Produces: `@Serializable data class SyncPushRequest(val items: List<SyncQueueItem>)`
- Produces: `@Serializable data class SyncPushResponse(val accepted: Int, val serverTime: Long)`
- Produces: `@Serializable data class SyncDelta(val items: List<SyncQueueItem> = emptyList(), val timestamp: Long = 0L)`
- Changes: `RemoteSyncClient.pushChanges(items: List<SyncQueueItem>): Boolean`

- [ ] Update tests to require batched push rather than one HTTP call per queue row.
- [ ] Add tests for pulling footprint and carving generic items into local tables.
- [ ] Add tests that a newer delete tombstone removes local records.
- [ ] Implement shared DTOs.
- [ ] Change `MapChinaApiClient` to post `SyncPushRequest` to `/sync/push` and parse generic pull responses.
- [ ] Change `SyncEngine.pushChanges()` to collect pending rows into one batch, delete accepted rows on success, and retry rows on failure.
- [ ] Change `SyncEngine.pullChanges(since)` to merge generic items by entity type.
- [ ] Run `./gradlew :shared:allTests --tests "com.mapchina.sync.SyncEngineTest"`.

### Task 3: Local Change Enqueueing

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/sync/SyncChangeWriter.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/FootprintRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/CarvingRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/JournalRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/SettingsRepository.kt`
- Modify: existing repository tests under `shared/src/commonTest` or `shared/src/androidUnitTest`

**Interfaces:**
- Produces: `class SyncChangeWriter(private val database: MapChinaDatabase)`
- Produces: `fun enqueueUpsert(entityType: SyncEntityType, entityId: String, payload: String, updatedAt: Long)`
- Produces: `fun enqueueDelete(entityType: SyncEntityType, entityId: String, updatedAt: Long)`
- Repository constructors accept `syncChangeWriter: SyncChangeWriter? = null`.

- [ ] Add failing repository tests for footprint, carving, journal, and setting writes enqueueing sync records.
- [ ] Add `SyncChangeWriter`.
- [ ] Wire optional writer into repositories.
- [ ] Ensure pull-side repository writes can bypass enqueueing by calling direct SQL query methods inside `SyncEngine`.
- [ ] Update Koin `AppModule` to provide one `SyncChangeWriter` and pass it to repositories.
- [ ] Run focused repository tests.

### Task 4: Client Configuration And Sync Status Surface

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/domain/service/AuthService.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileViewModel.kt`

**Interfaces:**
- Produces: `fun defaultApiBaseUrl(): String`
- Produces: `AuthService` tracks `accessToken` and `refreshToken` for current process.
- Produces: profile copy names sync as server-backed after login.

- [ ] Add tests for `defaultApiBaseUrl()`.
- [ ] Centralize API base URL as `http://10.0.2.2:8080` for Android emulator builds and `http://127.0.0.1:8080` fallback for other local clients.
- [ ] Store access token in `AuthService` after phone login and mirror it into `MapChinaApiClient`.
- [ ] Expose pending sync count and last sync status in profile view model if already cheap from `sync_queue`.
- [ ] Update profile copy from "logging in can sync footprints and theme" to the V1 scope.

### Task 5: Local Deployment Scripts

**Files:**
- Create: `scripts/setup_local_server.sh`
- Create: `scripts/smoke_sync.py`
- Modify: `scripts/seed_database.py`
- Modify: `README.md`

**Interfaces:**
- Produces: `scripts/setup_local_server.sh` that creates PostgreSQL role/database, starts Ktor server, seeds map data, and writes logs under `scripts/output/`.
- Produces: `scripts/smoke_sync.py` that verifies `/health`, `/auth/login`, `/sync/push`, and `/sync/pull`.

- [ ] Make `seed_database.py` read `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
- [ ] Add a shell script that uses native `createdb`, `psql`, and `pg_ctl` when available.
- [ ] Add Python smoke test with only standard library HTTP calls.
- [ ] Document local server run and Android emulator base URL in README.
- [ ] Run `scripts/setup_local_server.sh`.
- [ ] Run `scripts/smoke_sync.py`.

### Task 6: Full Verification

**Files:**
- No new source files unless verification fails.

- [ ] Run `./gradlew :server:test :shared:allTests :androidApp:testDebugUnitTest`.
- [ ] Run `./gradlew installDebug`.
- [ ] Launch with `adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1`.
- [ ] Capture at least one screenshot showing the app still launches.
- [ ] Verify local server still responds to `/health` after app install.
- [ ] Commit implementation files only, preserving unrelated untracked files.

