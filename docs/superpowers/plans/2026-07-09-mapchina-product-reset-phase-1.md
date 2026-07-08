# MapChina Product Reset Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first working slice of the approved product reset: bottom navigation becomes 足迹 / 发现 / 山河 / 我的, Discover combines景点推荐 + 补录, Shanhe owns growth features, and V1 footprint suggestions replace silent auto-mark writes.

**Architecture:** Add a small domain-level `FootprintSuggestionService` that owns runtime suggestion generation and confirmation. Keep existing map, attraction, achievement, stats, carving, and profile modules, but add focused `ui/discover` and `ui/shanhe` containers so navigation changes do not sprawl across old screens.

**Tech Stack:** Kotlin Multiplatform 2.3.20, Compose Multiplatform 1.10.0, Navigation3 1.1.1, Koin 4.0.4, SQLDelight 2.0.2, kotlinx.coroutines 1.10.2, Android compileSdk 36.

## Global Constraints

- MapChina is now a **半自动中国足迹记录器**: "自动发现你可能到过的地方，由你确认点亮中国。"
- V1 data sources are **景点访问带出足迹** and **当前位置建议**.
- 景点访问 must not silently write administrative-region footprints; it records the attraction visit and generates a user-confirmed suggestion.
- 用户确认区县足迹时，可自动补全父级市、省为“途经”，但 confirmation UI must explain the cascade.
- Photo EXIF回溯 is not part of this implementation slice.
- 社区 must be removed from the bottom-level navigation.
- UI/runtime changes must be verified on device: `./gradlew installDebug`, launch app, `adb shell monkey`, `adb shell input tap/swipe`, and before/after screenshots via `adb shell screencap`.
- Do not revert or overwrite unrelated existing worktree changes.

---

## File Structure

- Create `shared/src/commonMain/kotlin/com/mapchina/domain/model/FootprintSuggestion.kt`  
  Domain data model for runtime suggestions.
- Create `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintSuggestionService.kt`  
  Runtime queue, suggestion creation, dismissal, and confirmation.
- Create `shared/src/commonTest/kotlin/com/mapchina/domain/service/FootprintSuggestionServiceTest.kt`  
  Tests for attraction-derived suggestions, location-derived suggestions, and confirmation writes.
- Modify `shared/src/commonMain/kotlin/com/mapchina/data/repository/FootprintRepository.kt`  
  Split attraction visit recording from footprint confirmation.
- Modify `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt`  
  Expose `recordAttractionVisit()` without footprint writes.
- Modify `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`  
  Register `FootprintSuggestionService`, `DiscoverViewModel`, and `ShanheViewModel`; inject suggestion service into existing VMs.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`  
  Add `DiscoverScreen` and `ShanheScreen`.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/App.kt`  
  Bottom nav labels/icons and tab list.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`  
  Route Discover, Shanhe, Profile, and remove Community from bottom-level nav.
- Create `shared/src/commonMain/kotlin/com/mapchina/ui/map/FootprintSuggestionCard.kt`  
  Confirm/dismiss suggestion card used on 足迹.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`  
  Generate location suggestions instead of silent GPS writes.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`  
  Show suggestion card and keep map interactions intact.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionViewModel.kt`  
  Generate suggestion after marking an attraction visit.
- Create `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverViewModel.kt`  
  Aggregates pending suggestions, unvisited scenic recommendations, search query, and nearby hooks.
- Create `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`  
  New Discover tab UI.
- Create `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`  
  Aggregates stats, achievements, and next target.
- Create `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`  
  New Shanhe tab UI.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`  
  Remove growth feature grid; keep account/settings/theme/export.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/theme/Copy.kt`  
  Add navigation and suggestion copy.
- Create or modify tests only at the exact test paths listed in each task.

---

### Task 0: Baseline Device Snapshot

**Files:**
- No tracked file changes.

**Interfaces:**
- Produces: `/tmp/mapchina-product-reset-before.png` as the required pre-change screenshot for UI/runtime verification.

- [ ] **Step 1: Build and install the current app before UI changes**

Run:

```bash
./gradlew installDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Launch the current app**

Run:

```bash
adb shell monkey -p com.mapchina.androidApp 1
```

Expected: app opens on the connected device or emulator.

- [ ] **Step 3: Capture the pre-change screenshot**

Run:

```bash
adb shell screencap -p /sdcard/mapchina-product-reset-before.png
adb pull /sdcard/mapchina-product-reset-before.png /tmp/mapchina-product-reset-before.png
```

Expected: screenshot file exists at `/tmp/mapchina-product-reset-before.png`.

- [ ] **Step 4: Record baseline navigation state**

Run:

```bash
adb shell input tap 135 2260
adb shell input tap 405 2260
adb shell input tap 675 2260
adb shell input tap 945 2260
```

Expected: current tabs are still the pre-reset navigation. Keep `/tmp/mapchina-product-reset-before.png` for final before/after comparison.

---

### Task 1: Footprint Suggestion Domain Model And Service

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/model/FootprintSuggestion.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintSuggestionService.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/domain/service/FootprintSuggestionServiceTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/FootprintRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt`

**Interfaces:**
- Produces: `FootprintSuggestion`, `FootprintSuggestionSource`, `FootprintSuggestionStatus`
- Produces: `FootprintSuggestionService.suggestions: StateFlow<List<FootprintSuggestion>>`
- Produces: `FootprintSuggestionService.offerFromAttractionVisit(regionId: String, attractionName: String, level: FootprintLevel): FootprintSuggestion?`
- Produces: `FootprintSuggestionService.offerFromLocation(match: RegionMatch): FootprintSuggestion?`
- Produces: `FootprintSuggestionService.confirm(userId: String, suggestionId: String, level: FootprintLevel): FootprintResult?`
- Produces: `FootprintSuggestionService.dismiss(suggestionId: String)`
- Produces: `FootprintService.recordAttractionVisit(userId: String, attractionId: String, level: FootprintLevel)`

- [ ] **Step 1: Write the failing service tests**

Add this file:

```kotlin
package com.mapchina.domain.service

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestionSource
import com.mapchina.domain.model.FootprintSuggestionStatus
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FootprintSuggestionServiceTest {
    private lateinit var database: MapChinaDatabase
    private lateinit var regionRepository: RegionRepository
    private lateinit var footprintRepository: FootprintRepository
    private lateinit var footprintService: FootprintService
    private lateinit var suggestionService: FootprintSuggestionService

    @BeforeTest
    fun setup() {
        database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        regionRepository = RegionRepository(database)
        footprintRepository = FootprintRepository(database)
        footprintService = FootprintService(footprintRepository, regionRepository, null)
        suggestionService = FootprintSuggestionService(regionRepository, footprintService)

        regionRepository.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        regionRepository.insertRegion(Region("330100", "杭州市", RegionLevel.CITY, "330000"))
        regionRepository.insertRegion(Region("330106", "西湖区", RegionLevel.DISTRICT, "330100"))
    }

    @Test
    fun offerFromAttractionVisit_buildsPendingSuggestionWithPath() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        assertNotNull(suggestion)
        assertEquals(FootprintSuggestionSource.ATTRACTION_VISIT, suggestion.source)
        assertEquals(FootprintSuggestionStatus.PENDING, suggestion.status)
        assertEquals("330106", suggestion.regionId)
        assertEquals("浙江省 / 杭州市 / 西湖区", suggestion.parentPath)
        assertEquals("西湖风景名胜区", suggestion.evidenceLabel)
        assertEquals(FootprintLevel.DEEP, suggestion.suggestedLevel)
        assertEquals(1, suggestionService.suggestions.value.size)
    }

    @Test
    fun offerFromLocation_prefersDistrictThenCityThenProvince() {
        val match = RegionMatch(
            province = regionRepository.getRegion("330000"),
            city = regionRepository.getRegion("330100"),
            district = regionRepository.getRegion("330106")
        )

        val suggestion = suggestionService.offerFromLocation(match)

        assertNotNull(suggestion)
        assertEquals(FootprintSuggestionSource.LOCATION, suggestion.source)
        assertEquals("330106", suggestion.regionId)
        assertEquals("高", suggestion.confidenceLabel)
    }

    @Test
    fun confirm_writesFootprintAndRemovesSuggestion() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.SHORT_VISIT
        )

        val result = suggestionService.confirm("u1", suggestion!!.id, FootprintLevel.SHORT_VISIT)

        assertEquals(true, result?.isSuccess)
        assertEquals(FootprintLevel.SHORT_VISIT, footprintRepository.getFootprint("u1", "330106")?.level)
        assertEquals(0, suggestionService.suggestions.value.size)
    }

    @Test
    fun confirm_marksParentRegionsAsPassBy() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.DEEP
        )

        suggestionService.confirm("u1", suggestion!!.id, FootprintLevel.DEEP)

        assertEquals(FootprintLevel.DEEP, footprintRepository.getFootprint("u1", "330106")?.level)
        assertEquals(FootprintLevel.PASS_BY, footprintRepository.getFootprint("u1", "330100")?.level)
        assertEquals(FootprintLevel.PASS_BY, footprintRepository.getFootprint("u1", "330000")?.level)
    }

    @Test
    fun dismiss_marksSuggestionDismissedAndHidesFromPendingList() {
        val suggestion = suggestionService.offerFromAttractionVisit(
            regionId = "330106",
            attractionName = "西湖风景名胜区",
            level = FootprintLevel.PASS_BY
        )

        suggestionService.dismiss(suggestion!!.id)

        assertEquals(0, suggestionService.suggestions.value.size)
        assertNull(suggestionService.confirm("u1", suggestion.id, FootprintLevel.PASS_BY))
    }
}
```

- [ ] **Step 2: Run the failing tests**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.domain.service.FootprintSuggestionServiceTest"
```

Expected: FAIL with unresolved references to `FootprintSuggestionService` and `FootprintSuggestionSource`.

- [ ] **Step 3: Add the model**

Create `shared/src/commonMain/kotlin/com/mapchina/domain/model/FootprintSuggestion.kt`:

```kotlin
package com.mapchina.domain.model

import kotlin.time.Clock

enum class FootprintSuggestionSource {
    LOCATION,
    ATTRACTION_VISIT,
    PHOTO,
    MANUAL_SEARCH
}

enum class FootprintSuggestionStatus {
    PENDING,
    CONFIRMED,
    DISMISSED
}

data class FootprintSuggestion(
    val id: String,
    val source: FootprintSuggestionSource,
    val regionId: String,
    val regionName: String,
    val parentPath: String,
    val evidenceLabel: String,
    val suggestedLevel: FootprintLevel?,
    val confidenceLabel: String,
    val createdAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val status: FootprintSuggestionStatus = FootprintSuggestionStatus.PENDING
)
```

- [ ] **Step 4: Split attraction visit recording from footprint writes**

Modify `FootprintRepository`:

```kotlin
fun recordAttractionVisit(userId: String, attractionId: String, level: FootprintLevel) {
    database.attractionVisitQueries.upsertVisit(
        userId,
        attractionId,
        level.name,
        Clock.System.now().toEpochMilliseconds(),
        null
    )
}

fun markAttractionVisit(userId: String, attractionId: String, regionId: String, level: FootprintLevel) {
    recordAttractionVisit(userId, attractionId, level)
    markFootprint(userId, regionId, level)
    cascadeToParentRegions(userId, regionId)
}
```

Modify `FootprintService`:

```kotlin
fun recordAttractionVisit(userId: String, attractionId: String, level: FootprintLevel) {
    footprintRepository.recordAttractionVisit(userId, attractionId, level)
}
```

- [ ] **Step 5: Add the service**

Create `shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintSuggestionService.kt`:

```kotlin
package com.mapchina.domain.service

import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.model.FootprintSuggestionSource
import com.mapchina.domain.model.FootprintSuggestionStatus
import com.mapchina.domain.model.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FootprintSuggestionService(
    private val regionRepository: RegionRepository,
    private val footprintService: FootprintService
) {
    private val pendingById = linkedMapOf<String, FootprintSuggestion>()
    private val dismissedIds = mutableSetOf<String>()
    private val _suggestions = MutableStateFlow<List<FootprintSuggestion>>(emptyList())
    val suggestions: StateFlow<List<FootprintSuggestion>> = _suggestions.asStateFlow()

    fun offerFromAttractionVisit(
        regionId: String,
        attractionName: String,
        level: FootprintLevel
    ): FootprintSuggestion? {
        val region = regionRepository.getRegion(regionId) ?: return null
        return offer(
            source = FootprintSuggestionSource.ATTRACTION_VISIT,
            region = region,
            evidenceLabel = attractionName,
            suggestedLevel = level,
            confidenceLabel = "高"
        )
    }

    fun offerFromLocation(match: RegionMatch): FootprintSuggestion? {
        val region = match.district ?: match.city ?: match.province ?: return null
        val confidence = when {
            match.district != null -> "高"
            match.city != null -> "中"
            else -> "低"
        }
        return offer(
            source = FootprintSuggestionSource.LOCATION,
            region = region,
            evidenceLabel = "当前位置",
            suggestedLevel = FootprintLevel.PASS_BY,
            confidenceLabel = confidence
        )
    }

    fun confirm(userId: String, suggestionId: String, level: FootprintLevel): FootprintResult? {
        val suggestion = pendingById.remove(suggestionId) ?: return null
        _suggestions.value = pendingById.values.toList()
        val result = footprintService.markFootprint(userId, suggestion.regionId, level)
        markParentsAsPassBy(userId, suggestion.regionId)
        return result
    }

    fun dismiss(suggestionId: String) {
        pendingById.remove(suggestionId)
        dismissedIds.add(suggestionId)
        _suggestions.value = pendingById.values.toList()
    }

    private fun offer(
        source: FootprintSuggestionSource,
        region: Region,
        evidenceLabel: String,
        suggestedLevel: FootprintLevel?,
        confidenceLabel: String
    ): FootprintSuggestion? {
        val id = "${source.name}:${region.id}"
        if (id in dismissedIds) return null
        val suggestion = FootprintSuggestion(
            id = id,
            source = source,
            regionId = region.id,
            regionName = region.name,
            parentPath = buildPath(region),
            evidenceLabel = evidenceLabel,
            suggestedLevel = suggestedLevel,
            confidenceLabel = confidenceLabel,
            status = FootprintSuggestionStatus.PENDING
        )
        pendingById[id] = suggestion
        _suggestions.value = pendingById.values.toList()
        return suggestion
    }

    private fun buildPath(region: Region): String {
        val path = mutableListOf(region.name)
        var parentId = region.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            path.add(0, parent.name)
            parentId = parent.parentId
        }
        return path.joinToString(" / ")
    }

    private fun markParentsAsPassBy(userId: String, regionId: String) {
        var parentId = regionRepository.getRegion(regionId)?.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            footprintService.markFootprint(userId, parent.id, FootprintLevel.PASS_BY)
            parentId = parent.parentId
        }
    }
}
```

- [ ] **Step 6: Run the tests**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.domain.service.FootprintSuggestionServiceTest"
```

Expected: PASS for all tests in `FootprintSuggestionServiceTest`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/model/FootprintSuggestion.kt \
  shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintSuggestionService.kt \
  shared/src/commonMain/kotlin/com/mapchina/data/repository/FootprintRepository.kt \
  shared/src/commonMain/kotlin/com/mapchina/domain/service/FootprintService.kt \
  shared/src/commonTest/kotlin/com/mapchina/domain/service/FootprintSuggestionServiceTest.kt
git commit -m "feat: add footprint suggestion service"
```

---

### Task 2: Wire Suggestions Into Existing ViewModels

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionViewModelTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt`

**Interfaces:**
- Consumes: `FootprintSuggestionService.offerFromAttractionVisit(regionId: String, attractionName: String, level: FootprintLevel): FootprintSuggestion?`
- Consumes: `FootprintSuggestionService.offerFromLocation(match: RegionMatch): FootprintSuggestion?`
- Produces: `MapViewModel.footprintSuggestions: StateFlow<List<FootprintSuggestion>>`
- Produces: `MapViewModel.confirmSuggestion(suggestionId: String, level: FootprintLevel)`
- Produces: `MapViewModel.dismissSuggestion(suggestionId: String)`

- [ ] **Step 1: Add a failing attraction ViewModel test**

Append to `AttractionViewModelTest`:

```kotlin
@Test
fun markVisit_recordsVisitAndCreatesSuggestionWithoutWritingFootprint() {
    val regionRepo = RegionRepository(database)
    regionRepo.insertRegion(com.mapchina.domain.model.Region("110000", "北京市", com.mapchina.domain.model.RegionLevel.PROVINCE, null))
    regionRepo.insertRegion(com.mapchina.domain.model.Region("110101", "东城区", com.mapchina.domain.model.RegionLevel.DISTRICT, "110000"))
    val suggestionService = FootprintSuggestionService(regionRepo, footprintService)
    val vm = AttractionViewModel(
        attractionRepository = attractionRepo,
        footprintService = footprintService,
        footprintRepository = footprintRepo,
        detailProvider = null,
        attractionService = null,
        userId = "u1",
        footprintSuggestionService = suggestionService,
        dispatcher = UnconfinedTestDispatcher()
    )

    vm.markVisit("a1", "110101", FootprintLevel.DEEP)

    assertEquals(FootprintLevel.DEEP, footprintRepo.getAttractionVisit("u1", "a1")?.level)
    assertEquals(null, footprintRepo.getFootprint("u1", "110101"))
    assertEquals(1, suggestionService.suggestions.value.size)
    assertEquals("110101", suggestionService.suggestions.value.first().regionId)
}
```

- [ ] **Step 2: Run the failing attraction test**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.ui.attraction.AttractionViewModelTest.markVisit_recordsVisitAndCreatesSuggestionWithoutWritingFootprint"
```

Expected: FAIL because `AttractionViewModel` does not accept `footprintSuggestionService` and still writes a footprint through `markAttractionVisit()`.

- [ ] **Step 3: Modify `AttractionViewModel` constructor and mark flow**

Add parameter:

```kotlin
private val footprintSuggestionService: FootprintSuggestionService? = null,
```

Use this `markVisit()` body:

```kotlin
fun markVisit(attractionId: String, regionId: String, level: FootprintLevel) {
    footprintService.recordAttractionVisit(userId, attractionId, level)
    val attractionName = getAttractionById(attractionId)?.name ?: attractionId
    footprintSuggestionService?.offerFromAttractionVisit(regionId, attractionName, level)
    refreshAttractions()
}
```

- [ ] **Step 4: Modify `AppModule` injection**

Add import:

```kotlin
import com.mapchina.domain.service.FootprintSuggestionService
```

Register the singleton after `FootprintService`:

```kotlin
single { FootprintSuggestionService(get(), get()) }
```

Pass it into `AttractionViewModel`:

```kotlin
footprintSuggestionService = getOrNull<FootprintSuggestionService>(),
```

- [ ] **Step 5: Replace silent GPS auto-mark with suggestions in `MapViewModel`**

Add constructor parameter:

```kotlin
private val footprintSuggestionService: FootprintSuggestionService? = null
```

Expose suggestions:

```kotlin
val footprintSuggestions: StateFlow<List<FootprintSuggestion>> =
    footprintSuggestionService?.suggestions ?: MutableStateFlow(emptyList())
```

Replace `autoMarkFromGps()` body with:

```kotlin
fun autoMarkFromGps() {
    val provider = locationProvider ?: return
    val matcher = regionMatcher ?: return
    val suggestions = footprintSuggestionService ?: return
    if (!provider.isAvailable()) return
    vmScope.launch {
        val location = provider.getCurrentLocation() ?: return@launch
        val match = matcher.match(location.first, location.second)
        val suggestion = suggestions.offerFromLocation(match)
        if (suggestion != null) {
            showAutoMarkMessage("发现可能足迹：${suggestion.parentPath}")
        }
    }
}
```

Add:

```kotlin
fun confirmSuggestion(suggestionId: String, level: FootprintLevel) {
    vmScope.launch {
        val result = footprintSuggestionService?.confirm(userId, suggestionId, level) ?: return@launch
        invalidateCaches()
        refreshRegions()
        if (result.achievementResult != null && result.achievementResult.newlyUnlocked.isNotEmpty()) {
            _achievementUnlock.value = result.achievementResult
        }
    }
}

fun dismissSuggestion(suggestionId: String) {
    footprintSuggestionService?.dismiss(suggestionId)
}
```

Replace the entire `autoMarkFromPhotos()` function body for this phase:

```kotlin
private fun autoMarkFromPhotos() {
    showAutoMarkMessage("照片回溯将在后续版本开放")
}
```

- [ ] **Step 6: Update `AppModule` MapViewModel construction**

Pass suggestion service into `MapViewModel`:

```kotlin
getOrNull<FootprintSuggestionService>()
```

Keep all existing constructor arguments in their current order and append the new optional argument at the end to reduce call-site churn.

- [ ] **Step 7: Run ViewModel tests**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.ui.attraction.AttractionViewModelTest"
./gradlew :shared:allTests --tests "com.mapchina.ui.map.MapViewModelTest"
```

Expected: PASS after updating any constructor calls in tests to use the new optional parameter.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/attraction/AttractionViewModel.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt \
  shared/src/commonTest/kotlin/com/mapchina/ui/attraction/AttractionViewModelTest.kt \
  shared/src/commonTest/kotlin/com/mapchina/ui/map/MapViewModelTest.kt
git commit -m "feat: wire footprint suggestions into visits and location"
```

---

### Task 3: Navigation Reset And Screen Shells

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/theme/Copy.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/AppNavigationTest.kt`

**Interfaces:**
- Produces: `DiscoverScreen : Screen`
- Produces: `ShanheScreen : Screen`
- Produces: `DiscoverScreenComposable(viewModel: DiscoverViewModel, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier)`
- Produces: `ShanheScreenComposable(viewModel: ShanheViewModel, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write failing navigation smoke test**

Create `androidApp/src/test/kotlin/com/mapchina/ui/AppNavigationTest.kt`:

```kotlin
package com.mapchina.ui

import com.mapchina.ui.navigation.DiscoverScreen
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.ShanheScreen
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigationTest {
    @Test
    fun bottomNavItems_useProductResetTabs() {
        assertEquals(
            listOf("足迹", "发现", "山河", "我的"),
            bottomNavItems.map { it.label }
        )
    }

    @Test
    fun navKeys_existForNewMainTabs() {
        val screens = listOf(MapScreen, DiscoverScreen, ShanheScreen)
        assertEquals(3, screens.size)
    }
}
```

- [ ] **Step 2: Run the failing navigation test**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.AppNavigationTest"
```

Expected: FAIL because `DiscoverScreen`, `ShanheScreen`, and reset tab labels do not exist.

- [ ] **Step 3: Add navigation keys and copy**

Modify `Screen.kt`:

```kotlin
@Serializable data object DiscoverScreen : Screen()
@Serializable data object ShanheScreen : Screen()
```

Modify `Copy.kt`:

```kotlin
const val TAB_FOOTPRINT = "足迹"
const val TAB_DISCOVER = "发现"
const val TAB_SHANHE = "山河"
const val TAB_PROFILE = "我的"

const val DISCOVER_TITLE = "发现下一站"
const val DISCOVER_SUBTITLE = "推荐能帮你点亮版图的景点和城市"
const val SHANHE_TITLE = "山河"
const val SHANHE_SUBTITLE = "成就、图鉴、碑刻和统计"
```

- [ ] **Step 4: Update bottom navigation items**

Modify `bottomNavItems` in `App.kt`:

```kotlin
val bottomNavItems = listOf(
    BottomNavItem(MapScreen, Copy.TAB_FOOTPRINT, Icons.Default.LocationOn),
    BottomNavItem(DiscoverScreen, Copy.TAB_DISCOVER, Icons.Default.Explore),
    BottomNavItem(ShanheScreen, Copy.TAB_SHANHE, Icons.Default.WorkspacePremium),
    BottomNavItem(ProfileScreen, Copy.TAB_PROFILE, Icons.Default.Person),
)
```

Add imports for `DiscoverScreen`, `ShanheScreen`, `Icons.Default.Explore`, and `Icons.Default.WorkspacePremium`.

- [ ] **Step 5: Create shell ViewModels and screens**

Create `DiscoverViewModel.kt`:

```kotlin
package com.mapchina.ui.discover

class DiscoverViewModel
```

Create `DiscoverScreen.kt`:

```kotlin
package com.mapchina.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(Copy.DISCOVER_TITLE, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Display)
        Text(Copy.DISCOVER_SUBTITLE, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
    }
}
```

Create `ShanheViewModel.kt`:

```kotlin
package com.mapchina.ui.shanhe

class ShanheViewModel
```

Create `ShanheScreen.kt`:

```kotlin
package com.mapchina.ui.shanhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun ShanheScreen(
    viewModel: ShanheViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(Copy.SHANHE_TITLE, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Display)
        Text(Copy.SHANHE_SUBTITLE, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
    }
}
```

- [ ] **Step 6: Register screen routes and DI**

In `AppModule.kt`:

```kotlin
single { com.mapchina.ui.discover.DiscoverViewModel() }
single { com.mapchina.ui.shanhe.ShanheViewModel() }
```

In `AppNavHost.kt`, import aliases:

```kotlin
import com.mapchina.ui.discover.DiscoverScreen as DiscoverScreenComposable
import com.mapchina.ui.discover.DiscoverViewModel
import com.mapchina.ui.shanhe.ShanheScreen as ShanheScreenComposable
import com.mapchina.ui.shanhe.ShanheViewModel
```

Add entries:

```kotlin
entry<DiscoverScreen> {
    val vm: DiscoverViewModel = koinInject()
    DiscoverScreenComposable(viewModel = vm, onNavigate = navigate)
}

entry<ShanheScreen> {
    val vm: ShanheViewModel = koinInject()
    ShanheScreenComposable(viewModel = vm, onNavigate = navigate)
}
```

- [ ] **Step 7: Run navigation tests**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.AppNavigationTest"
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/App.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/theme/Copy.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/discover \
  shared/src/commonMain/kotlin/com/mapchina/ui/shanhe \
  androidApp/src/test/kotlin/com/mapchina/ui/AppNavigationTest.kt
git commit -m "feat: reset primary navigation"
```

---

### Task 4: 足迹 Suggestion Card On Map

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/FootprintSuggestionCard.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/map/FootprintSuggestionCardTest.kt`

**Interfaces:**
- Consumes: `FootprintSuggestion`
- Consumes: `MapViewModel.footprintSuggestions`
- Produces: confirm/dismiss UI callbacks into `MapViewModel.confirmSuggestion()` and `MapViewModel.dismissSuggestion()`

- [ ] **Step 1: Write failing Compose test for the card**

Create `FootprintSuggestionCardTest.kt`:

```kotlin
package com.mapchina.ui.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.model.FootprintSuggestionSource
import kotlin.test.Test
import kotlin.test.assertEquals

class FootprintSuggestionCardTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cardDisplaysRegionPathAndConfirmActions() = runComposeUiTest {
        var confirmed: FootprintLevel? = null
        var dismissed = false
        val suggestion = FootprintSuggestion(
            id = "LOCATION:330106",
            source = FootprintSuggestionSource.LOCATION,
            regionId = "330106",
            regionName = "西湖区",
            parentPath = "浙江省 / 杭州市 / 西湖区",
            evidenceLabel = "当前位置",
            suggestedLevel = FootprintLevel.PASS_BY,
            confidenceLabel = "高"
        )

        setContent {
            FootprintSuggestionCard(
                suggestion = suggestion,
                onConfirm = { confirmed = it },
                onDismiss = { dismissed = true }
            )
        }

        onNodeWithText("发现可能足迹").assertIsDisplayed()
        onNodeWithText("浙江省 / 杭州市 / 西湖区").assertIsDisplayed()
        onNodeWithText("确认后会同时将上级地区标记为途经").assertIsDisplayed()
        onNodeWithText("途经").performClick()
        assertEquals(FootprintLevel.PASS_BY, confirmed)
        onNodeWithText("忽略").performClick()
        assertEquals(true, dismissed)
    }
}
```

- [ ] **Step 2: Run the failing card test**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.map.FootprintSuggestionCardTest"
```

Expected: FAIL because `FootprintSuggestionCard` does not exist.

- [ ] **Step 3: Implement the card**

Create `FootprintSuggestionCard.kt`:

```kotlin
package com.mapchina.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun FootprintSuggestionCard(
    suggestion: FootprintSuggestion,
    onConfirm: (FootprintLevel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MapChinaColors.SurfaceElevated.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("发现可能足迹", color = MapChinaColors.Primary, style = MapChinaTypography.Caption, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(suggestion.parentPath, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
            Spacer(Modifier.height(4.dp))
            Text("${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}", color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
            Text("确认后会同时将上级地区标记为途经", color = MapChinaColors.TextTertiary, style = MapChinaTypography.Caption)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionLevelButton(Copy.FOOTPRINT_PASS, MapChinaColors.FootprintPassBy) { onConfirm(FootprintLevel.PASS_BY) }
                SuggestionLevelButton(Copy.FOOTPRINT_SHORT, MapChinaColors.FootprintShortVisit) { onConfirm(FootprintLevel.SHORT_VISIT) }
                SuggestionLevelButton(Copy.FOOTPRINT_DEEP, MapChinaColors.FootprintDeep) { onConfirm(FootprintLevel.DEEP) }
                Text(
                    "忽略",
                    color = MapChinaColors.TextTertiary,
                    style = MapChinaTypography.Body,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionLevelButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = color,
        style = MapChinaTypography.Body,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
```

- [ ] **Step 4: Render the top pending suggestion in `MapScreen`**

Collect suggestions near other state:

```kotlin
val footprintSuggestions by viewModel.footprintSuggestions.collectAsState()
val topSuggestion = footprintSuggestions.firstOrNull()
```

Place above the bottom region card, hidden in share mode:

```kotlin
if (topSuggestion != null && !shareMode && bottomPanel !is BottomPanel.Region) {
    FootprintSuggestionCard(
        suggestion = topSuggestion,
        onConfirm = { level -> viewModel.confirmSuggestion(topSuggestion.id, level) },
        onDismiss = { viewModel.dismissSuggestion(topSuggestion.id) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 12.dp, end = 12.dp, bottom = bottomBarOffset + 8.dp)
    )
}
```

- [ ] **Step 5: Run card and map tests**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.map.FootprintSuggestionCardTest"
./gradlew :shared:allTests --tests "com.mapchina.ui.map.MapViewModelTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/FootprintSuggestionCard.kt \
  shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt \
  androidApp/src/test/kotlin/com/mapchina/ui/map/FootprintSuggestionCardTest.kt
git commit -m "feat: show footprint suggestions on map"
```

---

### Task 5: Discover Tab Recommendations And补录

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/discover/DiscoverViewModelTest.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt`

**Interfaces:**
- Produces: `DiscoverUi`
- Produces: `DiscoverRecommendation`
- Produces: `DiscoverViewModel.ui: StateFlow<DiscoverUi>`
- Produces: `DiscoverViewModel.search(query: String)`

- [ ] **Step 1: Write failing Discover ViewModel test**

Create `DiscoverViewModelTest.kt`:

```kotlin
package com.mapchina.ui.discover

import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.Attraction
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.Region
import com.mapchina.domain.model.RegionLevel
import com.mapchina.domain.service.FootprintService
import com.mapchina.domain.service.FootprintSuggestionService
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoverViewModelTest {
    @Test
    fun recommendationsPreferUnvisitedA5AttractionsAndExplainPointValue() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)

        regionRepo.insertRegion(Region("340000", "安徽省", RegionLevel.PROVINCE, null))
        regionRepo.insertRegion(Region("341000", "黄山市", RegionLevel.CITY, "340000"))
        regionRepo.insertRegion(Region("341002", "屯溪区", RegionLevel.DISTRICT, "341000"))
        attractionRepo.insertAttraction(Attraction("a1", "黄山风景区", "341002", AttractionLevel.A5, 30.13, 118.16, null))
        attractionRepo.insertAttraction(Attraction("a2", "普通景区", "341002", AttractionLevel.A4, 30.12, 118.15, null))

        val vm = DiscoverViewModel(
            attractionRepository = attractionRepo,
            footprintRepository = footprintRepo,
            regionRepository = regionRepo,
            suggestionService = suggestionService,
            userId = "u1",
            dispatcher = UnconfinedTestDispatcher()
        )

        val first = vm.ui.value.recommendations.first()
        assertEquals("黄山风景区", first.title)
        assertEquals("可点亮 安徽省 / 黄山市 / 屯溪区", first.reason)
        assertEquals("5A", first.levelLabel)
    }

    @Test
    fun pendingSuggestionsComeFromSuggestionService() {
        val database = MapChinaDatabase(TestDatabaseDriverFactory().createDriver())
        val regionRepo = RegionRepository(database)
        val attractionRepo = AttractionRepository(database)
        val footprintRepo = FootprintRepository(database)
        val footprintService = FootprintService(footprintRepo, regionRepo, null)
        val suggestionService = FootprintSuggestionService(regionRepo, footprintService)

        regionRepo.insertRegion(Region("330000", "浙江省", RegionLevel.PROVINCE, null))
        suggestionService.offerFromAttractionVisit("330000", "西湖风景名胜区", FootprintLevel.PASS_BY)

        val vm = DiscoverViewModel(attractionRepo, footprintRepo, regionRepo, suggestionService, "u1", UnconfinedTestDispatcher())

        assertEquals(1, vm.ui.value.pendingSuggestions.size)
    }
}
```

- [ ] **Step 2: Run failing Discover ViewModel test**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.ui.discover.DiscoverViewModelTest"
```

Expected: FAIL because `DiscoverUi` and `DiscoverViewModel` behavior do not exist.

- [ ] **Step 3: Implement Discover ViewModel**

Replace `DiscoverViewModel.kt` with:

```kotlin
package com.mapchina.ui.discover

import com.mapchina.data.repository.AttractionRepository
import com.mapchina.data.repository.FootprintRepository
import com.mapchina.data.repository.RegionRepository
import com.mapchina.domain.model.AttractionLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.service.FootprintSuggestionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val levelLabel: String,
    val reason: String,
    val imageUrl: String?
)

data class DiscoverUi(
    val searchQuery: String = "",
    val pendingSuggestions: List<FootprintSuggestion> = emptyList(),
    val recommendations: List<DiscoverRecommendation> = emptyList()
)

class DiscoverViewModel(
    private val attractionRepository: AttractionRepository,
    private val footprintRepository: FootprintRepository,
    private val regionRepository: RegionRepository,
    private val suggestionService: FootprintSuggestionService,
    private val userId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _ui = MutableStateFlow(DiscoverUi())
    val ui: StateFlow<DiscoverUi> = _ui.asStateFlow()

    init {
        refresh()
        scope.launch {
            suggestionService.suggestions.collect { pending ->
                _ui.value = _ui.value.copy(pendingSuggestions = pending)
            }
        }
    }

    fun refresh() {
        val visitedAttractions = footprintRepository.getAttractionVisitsByUser(userId).map { it.attractionId }.toSet()
        val recommendations = attractionRepository.getAllAttractions()
            .filter { it.id !in visitedAttractions }
            .sortedWith(compareByDescending<com.mapchina.domain.model.Attraction> { it.level == AttractionLevel.A5 }.thenBy { it.name })
            .take(12)
            .map { attraction ->
                DiscoverRecommendation(
                    id = attraction.id,
                    title = attraction.name,
                    subtitle = attraction.description ?: regionRepository.getRegion(attraction.regionId)?.name.orEmpty(),
                    levelLabel = when (attraction.level) {
                        AttractionLevel.A5 -> "5A"
                        AttractionLevel.A4 -> "4A"
                        AttractionLevel.CUSTOM -> "自定义"
                    },
                    reason = "可点亮 ${buildRegionPath(attraction.regionId)}",
                    imageUrl = attraction.imageUrl
                )
            }
        _ui.value = _ui.value.copy(
            pendingSuggestions = suggestionService.suggestions.value,
            recommendations = recommendations
        )
    }

    fun search(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        val results = if (query.isBlank()) {
            attractionRepository.getAllAttractions()
        } else {
            attractionRepository.searchAttractions(query)
        }
        val visitedAttractions = footprintRepository.getAttractionVisitsByUser(userId).map { it.attractionId }.toSet()
        _ui.value = _ui.value.copy(
            recommendations = results
                .filter { it.id !in visitedAttractions }
                .take(20)
                .map { attraction ->
                    DiscoverRecommendation(
                        id = attraction.id,
                        title = attraction.name,
                        subtitle = attraction.description ?: regionRepository.getRegion(attraction.regionId)?.name.orEmpty(),
                    levelLabel = when (attraction.level) {
                        AttractionLevel.A5 -> "5A"
                        AttractionLevel.A4 -> "4A"
                        AttractionLevel.CUSTOM -> "自定义"
                    },
                        reason = "可点亮 ${buildRegionPath(attraction.regionId)}",
                        imageUrl = attraction.imageUrl
                    )
                }
        )
    }

    private fun buildRegionPath(regionId: String): String {
        val region = regionRepository.getRegion(regionId) ?: return regionId
        val names = mutableListOf(region.name)
        var parentId = region.parentId
        while (parentId != null) {
            val parent = regionRepository.getRegion(parentId) ?: break
            names.add(0, parent.name)
            parentId = parent.parentId
        }
        return names.joinToString(" / ")
    }
}
```

- [ ] **Step 4: Wire DI**

In `AppModule.kt`, replace the shell registration:

```kotlin
single {
    com.mapchina.ui.discover.DiscoverViewModel(
        attractionRepository = get(),
        footprintRepository = get(),
        regionRepository = get(),
        suggestionService = get(),
        userId = get<AuthService>().getCurrentUser()?.id.orEmpty()
    )
}
```

- [ ] **Step 5: Implement Discover UI**

Replace `DiscoverScreen.kt` with a screen that renders:

```kotlin
val ui by viewModel.ui.collectAsState()
```

Render these visible text anchors:

The implemented screen must display the exact section labels `待补录`, `补地图推荐`, `附近可点亮`, and `主题路线`.

For each pending suggestion:

The implemented screen must display `suggestion.parentPath` and `"${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}"`.

For each recommendation:

The implemented screen must display `recommendation.title` and `recommendation.reason`.

When a recommendation is clicked:

```kotlin
onNavigate(AttractionDetailScreen(recommendation.id))
```

Structure the file with a testable content function:

```kotlin
@Composable
internal fun DiscoverContent(
    ui: DiscoverUi,
    onSearch: (String) -> Unit,
    onRecommendationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(Copy.DISCOVER_TITLE, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Display)
            Text(Copy.DISCOVER_SUBTITLE, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
        }
        item {
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("搜索去过或想去的景点") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { DiscoverSectionTitle("待补录") }
        if (ui.pendingSuggestions.isEmpty()) {
            item { DiscoverEmptyLine("暂无待确认足迹") }
        } else {
            items(ui.pendingSuggestions, key = { it.id }) { suggestion ->
                DiscoverInfoCard(
                    title = suggestion.parentPath,
                    subtitle = "${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}",
                    onClick = {}
                )
            }
        }
        item { DiscoverSectionTitle("补地图推荐") }
        items(ui.recommendations, key = { it.id }) { recommendation ->
            DiscoverInfoCard(
                title = recommendation.title,
                subtitle = recommendation.reason,
                badge = recommendation.levelLabel,
                onClick = { onRecommendationClick(recommendation.id) }
            )
        }
        item { DiscoverSectionTitle("附近可点亮") }
        item { DiscoverEmptyLine("开启定位后显示附近可点亮景点") }
        item { DiscoverSectionTitle("主题路线") }
        item { DiscoverEmptyLine("五岳、丝路、海岸线等主题路线将在推荐能力增强阶段补齐") }
    }
}
```

Add these local helpers in the same file:

```kotlin
@Composable
private fun DiscoverSectionTitle(text: String) {
    Text(text, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
}

@Composable
private fun DiscoverEmptyLine(text: String) {
    Text(text, color = MapChinaColors.TextTertiary, style = MapChinaTypography.Body)
}

@Composable
private fun DiscoverInfoCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
                Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
            }
            if (badge != null) {
                Text(badge, color = MapChinaColors.AccentBlue, style = MapChinaTypography.Caption)
            }
        }
    }
}
```

`DiscoverScreen()` must call:

```kotlin
DiscoverContent(
    ui = ui,
    onSearch = viewModel::search,
    onRecommendationClick = { id -> onNavigate(AttractionDetailScreen(id)) },
    modifier = modifier
)
```

- [ ] **Step 6: Add Discover screen test**

Create `DiscoverScreenTest.kt`:

```kotlin
package com.mapchina.ui.discover

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.domain.model.FootprintSuggestionSource
import kotlin.test.Test

class DiscoverScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun discoverContentShowsPendingAndRecommendations() = runComposeUiTest {
        val ui = DiscoverUi(
            pendingSuggestions = listOf(
                FootprintSuggestion(
                    id = "LOCATION:330106",
                    source = FootprintSuggestionSource.LOCATION,
                    regionId = "330106",
                    regionName = "西湖区",
                    parentPath = "浙江省 / 杭州市 / 西湖区",
                    evidenceLabel = "当前位置",
                    suggestedLevel = FootprintLevel.PASS_BY,
                    confidenceLabel = "高"
                )
            ),
            recommendations = listOf(
                DiscoverRecommendation(
                    id = "a1",
                    title = "黄山风景区",
                    subtitle = "安徽省黄山市",
                    levelLabel = "5A",
                    reason = "可点亮 安徽省 / 黄山市",
                    imageUrl = null
                )
            )
        )

        setContent {
            DiscoverContent(
                ui = ui,
                onSearch = {},
                onRecommendationClick = {}
            )
        }

        onNodeWithText("发现下一站").assertIsDisplayed()
        onNodeWithText("待补录").assertIsDisplayed()
        onNodeWithText("补地图推荐").assertIsDisplayed()
        onNodeWithText("浙江省 / 杭州市 / 西湖区").assertIsDisplayed()
        onNodeWithText("黄山风景区").assertIsDisplayed()
        onNodeWithText("可点亮 安徽省 / 黄山市").assertIsDisplayed()
    }
}
```

- [ ] **Step 7: Run Discover tests**

Run:

```bash
./gradlew :shared:allTests --tests "com.mapchina.ui.discover.DiscoverViewModelTest"
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest"
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/discover \
  shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt \
  shared/src/commonTest/kotlin/com/mapchina/ui/discover/DiscoverViewModelTest.kt \
  androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt
git commit -m "feat: build discover recommendations tab"
```

---

### Task 6: Shanhe Tab And Profile Simplification

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt`

**Interfaces:**
- Produces: Shanhe page sections for 山河等级, 今日目标, 勋章, 图鉴, 征版, 碑刻, 统计
- Consumes: `AchievementViewModel`, `StatsViewModel`, and navigation callbacks to existing screens

- [ ] **Step 1: Write failing Shanhe screen test**

Create `ShanheScreenTest.kt`:

```kotlin
package com.mapchina.ui.shanhe

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class ShanheScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shanheScreenShowsGrowthEntrypoints() = runComposeUiTest {
        setContent {
            ShanheScreen(
                viewModel = ShanheViewModel(),
                onNavigate = {}
            )
        }

        onNodeWithText("山河").assertIsDisplayed()
        onNodeWithText("勋章").assertIsDisplayed()
        onNodeWithText("图鉴").assertIsDisplayed()
        onNodeWithText("征版").assertIsDisplayed()
        onNodeWithText("碑刻").assertIsDisplayed()
        onNodeWithText("统计").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run failing Shanhe screen test**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest"
```

Expected: FAIL because Shanhe shell does not render growth entry points.

- [ ] **Step 3: Implement Shanhe entry grid**

In `ShanheScreen.kt`, render a simple vertical page with:

The implemented screen must display the exact title `山河` and subtitle `成就、图鉴、碑刻和统计`.

Add six entry chips:

```kotlin
ShanheEntry("勋章", "旅行荣誉", onClick = { onNavigate(BadgeWallScreen) })
ShanheEntry("图鉴", "主题览胜", onClick = { onNavigate(AtlasScreen) })
ShanheEntry("征版", "点亮中国版图", onClick = { onNavigate(ProvinceConquestScreen) })
ShanheEntry("碑刻", "石壁留名", onClick = { onNavigate(CarvingListScreen(showAll = "true")) })
ShanheEntry("统计", "足迹总览", onClick = { onNavigate(StatsScreen) })
ShanheEntry("目标", "下一块版图", onClick = { onNavigate(DiscoverScreen) })
```

Keep `ShanheEntry` local to this file:

```kotlin
@Composable
private fun ShanheEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
            Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
        }
    }
}
```

- [ ] **Step 4: Simplify Profile**

In `ProfileScreen.kt`, remove the two-row feature grid that calls:

- `onNavigateToJournals`
- `onNavigateToBadgeWall`
- `onNavigateToProvinceConquest`
- `onNavigateToAtlas`
- `onNavigateToCarvings`
- `onNavigateToStats`

Keep:

- `UserInfoCard`
- `StatsBar`
- Settings card
- MapThemeSelector
- Logout
- Version

Change settings heading to:

The implemented heading text must be exactly `账号与设置`.

Do not remove the callback parameters yet; keeping them avoids AppNavHost churn in this task.

- [ ] **Step 5: Update Profile test expectations**

In `ProfileScreenTest`, assert settings remain visible and growth entries are absent:

```kotlin
onNodeWithText("账号与设置").assertIsDisplayed()
onNodeWithText("勋章").assertDoesNotExist()
onNodeWithText("图鉴").assertDoesNotExist()
```

- [ ] **Step 6: Run Shanhe and Profile tests**

Run:

```bash
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest"
./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.profile.ProfileScreenTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/shanhe \
  shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt \
  androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt \
  androidApp/src/test/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt
git commit -m "feat: add shanhe tab and simplify profile"
```

---

### Task 7: Full Verification And Device QA

**Files:**
- Modify: `docs/superpowers/plans/2026-07-09-mapchina-product-reset-phase-1.md` only if verification notes need correction.
- Create screenshots under a local ignored directory such as `/tmp/mapchina-product-reset-before.png` and `/tmp/mapchina-product-reset-after.png`.

**Interfaces:**
- Consumes all previous tasks.
- Produces verified Android runtime behavior.

- [ ] **Step 1: Run full relevant test suite**

Run:

```bash
./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build and install debug app**

Run:

```bash
./gradlew installDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Launch app with monkey**

Run:

```bash
adb shell monkey -p com.mapchina.androidApp 1
```

Expected: app opens on device/emulator.

- [ ] **Step 4: Capture before/landing screenshot**

Run:

```bash
adb shell screencap -p /sdcard/mapchina-product-reset-landing.png
adb pull /sdcard/mapchina-product-reset-landing.png /tmp/mapchina-product-reset-landing.png
```

Expected: screenshot file exists at `/tmp/mapchina-product-reset-landing.png`.

- [ ] **Step 5: Exercise primary tabs**

Use taps appropriate for the attached emulator resolution. For a 1080x2400 device, run:

```bash
adb shell input tap 135 2260
adb shell input tap 405 2260
adb shell input tap 675 2260
adb shell input tap 945 2260
```

Expected: tabs show 足迹, 发现, 山河, 我的 without crashes.

- [ ] **Step 6: Exercise map and suggestion gestures**

Run:

```bash
adb shell input swipe 540 1200 540 800 300
adb shell input tap 930 700
adb shell input tap 540 1800
```

Expected: map stays interactive; location/suggestion flow does not silently write a footprint; if a suggestion appears, it offers 途经、小驻、深游 and 忽略.

- [ ] **Step 7: Capture after screenshot**

Run:

```bash
adb shell screencap -p /sdcard/mapchina-product-reset-after.png
adb pull /sdcard/mapchina-product-reset-after.png /tmp/mapchina-product-reset-after.png
```

Expected: screenshot file exists at `/tmp/mapchina-product-reset-after.png`.

- [ ] **Step 8: Check git status**

Run:

```bash
git status --short
```

Expected: only intentional changes from this plan remain, or the working tree is clean after commits. Existing unrelated pre-plan changes may still appear and must not be reverted.

- [ ] **Step 9: Final commit if verification changed docs**

If verification notes were added to tracked docs:

```bash
git add docs/superpowers/plans/2026-07-09-mapchina-product-reset-phase-1.md
git commit -m "docs: record product reset verification"
```

If no tracked docs changed, skip this commit.

---

## Self-Review

- Spec coverage: This plan covers Phase 1 information architecture, Phase 2 V1 suggestion foundations, Discover as recommendation + 补录, Shanhe as growth home, Profile simplification, and device verification. Phase 3 recommendation ranking, Phase 4 sharing growth, and Phase 5 photo回溯 remain separate implementation plans.
- Placeholder scan: no placeholder markers or unspecified edge-handling steps remain.
- Type consistency: `FootprintSuggestionService`, `DiscoverViewModel`, `ShanheScreen`, and navigation keys are introduced before tasks consume them.
