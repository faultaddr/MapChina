# MapChina UI Interaction Refresh Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Phase 1 product reset into a modern, fresh, usable app by upgrading the four primary tabs, map interaction surfaces, and reusable UI primitives into a coherent “清润山河账本” experience.

**Architecture:** Keep the approved Phase 1 information architecture: 足迹 / 发现 / 山河 / 我的. Add small reusable Compose primitives for page headers, section labels, metric pills, and action rows, then apply them to Discover, Shanhe, Profile, and the map surfaces without changing domain persistence. Existing uncommitted map visual prototype changes may be adopted only when they match this plan and pass device verification.

**Tech Stack:** Kotlin Multiplatform 2.3.20, Compose Multiplatform 1.10.0, Navigation3 1.1.1, Koin 4.0.4, SQLDelight 2.0.2, kotlinx.coroutines 1.10.2, Android compileSdk 36.

## Global Constraints

- MapChina remains a **半自动中国足迹记录器**: "自动发现你可能到过的地方，由你确认点亮中国。"
- Visual direction is **清润山河账本**: clean, modern, trustworthy, restrained, and map-first.
- Do not reintroduce 社区 as a bottom-level tab.
- Discover recommendations must explain what they can help the user点亮.
- Shanhe must feel like a growth home, not a bare menu.
- Profile must remain account/settings focused.
- Do not silently write footprints from location or attraction visits; user confirmation remains mandatory.
- After any UI/runtime code change, run `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug`, launch with `adb shell monkey`, exercise the changed scenario with adb input, and capture before/after screenshots.
- Do not revert or overwrite unrelated existing worktree changes.

---

## File Structure

- Create `shared/src/commonMain/kotlin/com/mapchina/ui/common/ExperiencePrimitives.kt`  
  Shared page header, section header, metric pill, action tile, and soft card primitives for refreshed screens.
- Create `androidApp/src/test/kotlin/com/mapchina/ui/common/ExperiencePrimitivesTest.kt`  
  Compose smoke tests for primitive text and click behavior.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`  
  Upgrade Discover into a recommendation-first page with actionable pending suggestions, richer recommendation cards, and route placeholders.
- Modify `androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt`  
  Assert the refreshed Discover hierarchy, rendered recommendation subtitle, and action copy.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`  
  Add a lightweight `ShanheUi` state model for level, today target, and growth entries.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`  
  Upgrade Shanhe into a growth dashboard with level card, target card, entries, and recent progress.
- Modify `androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt`  
  Assert level, target, entries, and scroll behavior.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`  
  Make Profile a clearer account/settings console with grouped settings copy and stronger theme selection affordance.
- Modify `androidApp/src/test/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt`  
  Assert settings grouping and absence of growth menu entries.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`  
  Adopt the modern top dashboard and bottom panel spacing while preserving map interactions.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt`  
  Replace the oversized decorative FAB with a compact map action button.
- Modify `shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionCard.kt`  
  Upgrade region bottom card into a rounded action sheet with stable two-row actions.
- Modify `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`  
  Keep the quieter default map wash and lower label/stroke visual noise if the current prototype passes tests.
- Modify `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt` only if map fit behavior changes.
- Modify `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`  
  Assert the top dashboard and suggestion card coexist.

---

### Task 0: Phase 2 Baseline And Dirty Diff Boundary

**Files:**
- No tracked app code changes.
- Create screenshots under `/tmp/mapchina-ui-refresh-phase2-*.png`.

**Interfaces:**
- Produces baseline screenshots and a short local report of which existing dirty hunks are eligible for this phase.

- [ ] **Step 1: Record current worktree status**

Run:

```bash
git status --short
git diff --stat
```

Expected: status shows pre-existing dirty files. Do not revert them.

- [ ] **Step 2: Build and install current app**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Launch and capture baseline**

Run:

```bash
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-before.png
```

Expected: screenshot exists and shows the current post-Phase-1 app.

- [ ] **Step 4: Capture the four main tabs**

For a 1280x2856 emulator, run:

```bash
adb shell input tap 200 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-footprint-before.png
adb shell input tap 493 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-discover-before.png
adb shell input tap 787 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-shanhe-before.png
adb shell input tap 1080 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-profile-before.png
```

Expected: all four screenshots exist and no crash occurs.

- [ ] **Step 5: Commit**

No commit is required for screenshots. If a local report is written under an ignored directory, leave it untracked.

---

### Task 1: Shared Experience Primitives

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/common/ExperiencePrimitives.kt`
- Create: `androidApp/src/test/kotlin/com/mapchina/ui/common/ExperiencePrimitivesTest.kt`

**Interfaces:**
- Produces: `ExperiencePageHeader(title: String, subtitle: String, modifier: Modifier = Modifier)`
- Produces: `ExperienceSectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier)`
- Produces: `ExperienceMetricPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier)`
- Produces: `ExperienceActionTile(title: String, subtitle: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit)`
- Produces: `ExperienceSoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`

- [ ] **Step 1: Write the failing primitive test**

Create `androidApp/src/test/kotlin/com/mapchina/ui/common/ExperiencePrimitivesTest.kt`:

```kotlin
package com.mapchina.ui.common

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mapchina.ui.theme.MapChinaColors
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class ExperiencePrimitivesTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun actionTileDisplaysTextAndHandlesClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            ExperienceActionTile(
                title = "补地图推荐",
                subtitle = "优先点亮未到访省份",
                accent = MapChinaColors.AccentBlue,
                onClick = { clicks++ }
            )
        }

        onNodeWithText("补地图推荐").assertIsDisplayed()
        onNodeWithText("优先点亮未到访省份").assertIsDisplayed()
        onNodeWithText("补地图推荐").performClick()
        assertEquals(1, clicks)
    }
}
```

- [ ] **Step 2: Run the failing primitive test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.common.ExperiencePrimitivesTest"
```

Expected: FAIL because `ExperienceActionTile` does not exist.

- [ ] **Step 3: Add primitives**

Create `shared/src/commonMain/kotlin/com/mapchina/ui/common/ExperiencePrimitives.kt` with these composables and keep dimensions stable:

```kotlin
package com.mapchina.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun ExperiencePageHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Display)
        Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
    }
}

@Composable
fun ExperienceSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = MapChinaColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ExperienceMetricPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.10f), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MapChinaColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun ExperienceActionTile(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
            Text(subtitle, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
            Spacer(Modifier.height(4.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().height(3.dp).background(accent.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun ExperienceSoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
```

- [ ] **Step 4: Run primitive test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.common.ExperiencePrimitivesTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/common/ExperiencePrimitives.kt androidApp/src/test/kotlin/com/mapchina/ui/common/ExperiencePrimitivesTest.kt
git commit -m "feat: add experience ui primitives"
```

---

### Task 2: Discover Recommendation-First Refresh

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt`

**Interfaces:**
- Consumes: `ExperiencePageHeader`, `ExperienceSectionHeader`, `ExperienceMetricPill`, `ExperienceSoftCard`
- Produces: Discover UI that shows `下一块可点亮`, renders recommendation `subtitle`, and keeps `待补录`, `补地图推荐`, `附近可点亮`, `主题路线`.

- [ ] **Step 1: Update the failing Discover test**

In `DiscoverScreenTest`, add assertions inside `discoverContentShowsPendingAndRecommendations()`:

```kotlin
onNodeWithText("下一块可点亮").assertIsDisplayed()
onNodeWithText("安徽省黄山市").assertIsDisplayed()
onNodeWithText("去足迹确认").assertIsDisplayed()
```

Expected: these fail before implementation because Discover has no hero metric, does not render `subtitle`, and pending suggestion lacks an action.

- [ ] **Step 2: Run the failing Discover test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest"
```

Expected: FAIL on the new assertions.

- [ ] **Step 3: Implement refreshed Discover layout**

In `DiscoverContent`, keep the existing LazyColumn and replace the title/section/card composables with:

```kotlin
ExperiencePageHeader(Copy.DISCOVER_TITLE, Copy.DISCOVER_SUBTITLE)
ExperienceSoftCard {
    Text("下一块可点亮", color = MapChinaColors.TextPrimary, style = MapChinaTypography.Title)
    Text(ui.recommendations.firstOrNull()?.reason ?: "先确认一个足迹，系统会推荐下一站", color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
}
```

For pending suggestions, show:

```kotlin
DiscoverInfoCard(
    title = suggestion.parentPath,
    subtitle = "${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}",
    badge = "去足迹确认",
    onClick = {}
)
```

For recommendations, render both `subtitle` and `reason`:

```kotlin
DiscoverInfoCard(
    title = recommendation.title,
    subtitle = "${recommendation.subtitle} · ${recommendation.reason}",
    badge = recommendation.levelLabel,
    onClick = { onRecommendationClick(recommendation.id) }
)
```

- [ ] **Step 4: Run Discover tests**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.discover.DiscoverScreenTest"
```

Expected: PASS.

- [ ] **Step 5: Device verify Discover**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell input tap 493 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-discover-after.png
```

Expected: screenshot shows the refreshed Discover hierarchy and no text overlap.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/discover/DiscoverScreen.kt androidApp/src/test/kotlin/com/mapchina/ui/discover/DiscoverScreenTest.kt
git commit -m "feat: refresh discover recommendation experience"
```

---

### Task 3: Shanhe Growth Dashboard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt`

**Interfaces:**
- Produces: `data class ShanheUi(val levelTitle: String = "山河初识", val scoreLabel: String = "0 山河值", val targetTitle: String = "今日目标", val targetBody: String = "确认 1 条可能足迹，点亮下一块版图")`
- Produces: `ShanheViewModel.ui: StateFlow<ShanheUi>`

- [ ] **Step 1: Update the failing Shanhe test**

Add assertions:

```kotlin
onNodeWithText("山河初识").assertIsDisplayed()
onNodeWithText("0 山河值").assertIsDisplayed()
onNodeWithText("今日目标").assertIsDisplayed()
onNodeWithText("确认 1 条可能足迹，点亮下一块版图").assertIsDisplayed()
```

Expected: FAIL because the current Shanhe screen only shows entry cards.

- [ ] **Step 2: Run the failing Shanhe test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest"
```

Expected: FAIL on level and target assertions.

- [ ] **Step 3: Implement Shanhe UI state**

Replace `ShanheViewModel` shell with:

```kotlin
package com.mapchina.ui.shanhe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShanheUi(
    val levelTitle: String = "山河初识",
    val scoreLabel: String = "0 山河值",
    val targetTitle: String = "今日目标",
    val targetBody: String = "确认 1 条可能足迹，点亮下一块版图"
)

class ShanheViewModel {
    private val _ui = MutableStateFlow(ShanheUi())
    val ui: StateFlow<ShanheUi> = _ui.asStateFlow()
}
```

- [ ] **Step 4: Implement dashboard cards**

In `ShanheScreen`, collect `viewModel.ui` and render before entries:

```kotlin
ExperienceSoftCard {
    Text(ui.levelTitle, color = MapChinaColors.TextPrimary, style = MapChinaTypography.Headline)
    Text(ui.scoreLabel, color = MapChinaColors.AccentGold, style = MapChinaTypography.Title)
}
ExperienceSoftCard {
    Text(ui.targetTitle, color = MapChinaColors.Primary, style = MapChinaTypography.Title)
    Text(ui.targetBody, color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
}
ExperienceSectionHeader("成长入口")
```

- [ ] **Step 5: Run Shanhe test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.shanhe.ShanheScreenTest"
```

Expected: PASS.

- [ ] **Step 6: Device verify Shanhe**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell input tap 787 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-shanhe-after.png
```

Expected: Shanhe first screen shows a level card and target card above entries.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheViewModel.kt shared/src/commonMain/kotlin/com/mapchina/ui/shanhe/ShanheScreen.kt androidApp/src/test/kotlin/com/mapchina/ui/shanhe/ShanheScreenTest.kt
git commit -m "feat: refresh shanhe growth dashboard"
```

---

### Task 4: Profile Settings Console Polish

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt`

**Interfaces:**
- Produces clearer account/settings copy: `足迹记录设置`, `地图显示`, `数据与同步`
- Keeps growth entries absent from Profile.

- [ ] **Step 1: Update the failing Profile test**

Add assertions:

```kotlin
onNodeWithText("足迹记录设置").assertIsDisplayed()
onNodeWithText("地图显示").assertIsDisplayed()
onNodeWithText("数据与同步").assertIsDisplayed()
```

Expected: FAIL because the current Profile screen only has `账号与设置`.

- [ ] **Step 2: Run the failing Profile test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.profile.ProfileScreenTest"
```

Expected: FAIL on new grouping copy.

- [ ] **Step 3: Add grouped settings labels**

Inside Profile settings card, group the existing controls:

```kotlin
Text("足迹记录设置", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
SettingsRow(Copy.PHOTO_MARKERS, photoMarkersVisible, {
    haptic.perform(HapticType.SELECTION)
    photoMarkersVisible = it
    settingsRepository?.setString("photo_markers_visible", if (it) "true" else "false")
})
SettingsRow(Copy.AUTO_FOOTPRINT, autoMarkFootprint, {
    haptic.perform(HapticType.SELECTION)
    autoMarkFootprint = it
    settingsRepository?.setString("auto_mark_footprint", if (it) "true" else "false")
})
Text("地图显示", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
MapThemeSelector(settingsRepository = settingsRepository)
Text("数据与同步", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MapChinaColors.TextPrimary)
Text("登录后可同步足迹与地图主题", color = MapChinaColors.TextSecondary, style = MapChinaTypography.Body)
```

Do not add real sync persistence in this task.

- [ ] **Step 4: Run Profile test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.profile.ProfileScreenTest"
```

Expected: PASS.

- [ ] **Step 5: Device verify Profile**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell input tap 1080 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-profile-after.png
```

Expected: Profile remains clean and growth menu entries are absent.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/profile/ProfileScreen.kt androidApp/src/test/kotlin/com/mapchina/ui/profile/ProfileScreenTest.kt
git commit -m "feat: polish profile settings console"
```

---

### Task 5: Footprint Map Surface Polish

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionCard.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt`
- Modify: `androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt` only if `fitChinaInView()` expected zoom changes.

**Interfaces:**
- Produces a top dashboard card on the map with current scope and progress.
- Produces a compact FAB that does not dominate the map.
- Produces a rounded region action sheet with stable action rows.

- [ ] **Step 1: Review existing dirty map prototype**

Inspect:

```bash
git diff -- shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionCard.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt
```

Expected: adopt only map header, compact FAB, card spacing, quieter map wash, and fit changes that support current screenshots. Leave unrelated hunks unstaged.

- [ ] **Step 2: Update MapScreen test**

In `MapScreenTest`, assert the map dashboard and suggestion card text can coexist:

```kotlin
onNodeWithText("中国").assertIsDisplayed()
onNodeWithText("已点亮").assertIsDisplayed()
onNodeWithText("完成度").assertIsDisplayed()
onNodeWithText("发现可能足迹").assertIsDisplayed()
```

Expected: FAIL if current committed map screen lacks the dashboard.

- [ ] **Step 3: Run failing MapScreen test**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.map.MapScreenTest"
```

Expected: FAIL on dashboard assertions before implementation.

- [ ] **Step 4: Implement or stage the approved map surface changes**

Apply the map surface changes shown in the current prototype only if they compile:

- `HomeMapHeader` in `MapScreen.kt`
- compact 52dp FAB in `MapFab.kt`
- rounded `RegionCard` with a drag handle and two action rows
- quieter default map wash in `ChinaMapView.kt`
- `MapTheme.DEFAULT.oceanColor = Color(0xFFF1F7F6)`
- `ViewportState.fitChinaInView(padding: Float = 1.02f)`

- [ ] **Step 5: Run map tests**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :androidApp:testDebugUnitTest --tests "com.mapchina.ui.map.MapScreenTest" :shared:allTests --tests "com.mapchina.map.ViewportStateTest"
```

Expected: PASS. If `ViewportStateTest` fails only because the default fit padding changed, update its expected zoom/center values based on the new deterministic output.

- [ ] **Step 6: Device verify footprint map**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell input tap 200 2750
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-map-after.png
adb shell input tap 930 700
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-region-card-after.png
```

Expected: top dashboard, map labels, compact FAB, suggestion card, and region action sheet do not overlap.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapFab.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/RegionCard.kt shared/src/commonMain/kotlin/com/mapchina/map/ChinaMapView.kt shared/src/commonMain/kotlin/com/mapchina/map/MapTheme.kt shared/src/commonMain/kotlin/com/mapchina/map/ViewportState.kt androidApp/src/test/kotlin/com/mapchina/ui/map/MapScreenTest.kt shared/src/commonTest/kotlin/com/mapchina/map/ViewportStateTest.kt
git commit -m "feat: polish footprint map surfaces"
```

---

### Task 6: Full Phase 2 Verification

**Files:**
- No tracked code changes unless verification notes require a docs correction.

**Interfaces:**
- Consumes all previous tasks.
- Produces verified Android runtime behavior and screenshots.

- [ ] **Step 1: Run full tests**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install and launch**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
```

Expected: install and launch succeed.

- [ ] **Step 3: Exercise primary flows**

Run:

```bash
adb shell input tap 200 2750
adb shell input swipe 640 1500 640 950 300
adb shell input tap 493 2750
adb shell input swipe 640 2200 640 900 300
adb shell input tap 787 2750
adb shell input swipe 640 2200 640 900 300
adb shell input tap 1080 2750
```

Expected: 足迹, 发现, 山河, 我的 all render without crash and with no incoherent overlap.

- [ ] **Step 4: Capture final screenshots**

Run:

```bash
adb exec-out screencap -p > /tmp/mapchina-ui-refresh-phase2-final.png
adb shell uiautomator dump /sdcard/mapchina-ui-refresh-phase2-final.xml
adb pull /sdcard/mapchina-ui-refresh-phase2-final.xml /tmp/mapchina-ui-refresh-phase2-final.xml
```

Expected: screenshot and UI dump exist.

- [ ] **Step 5: Check git status**

Run:

```bash
git status --short
```

Expected: only unrelated pre-existing dirty files remain, or the worktree is clean after commits. Do not revert unrelated user changes.

---

## Self-Review

- Spec coverage: This plan implements the approved product reset visual direction by upgrading Discover, Shanhe, Profile, and Footprint surfaces while preserving the Phase 1 domain/navigation work.
- Placeholder scan: no placeholder markers remain; tasks include exact file paths, expected copy, commands, and verification outputs.
- Type consistency: `Experience*` primitive names are introduced in Task 1 before use in later tasks; `ShanheUi` is introduced in Task 3 before screen consumption.
- Scope note: This plan intentionally excludes photo回溯, backend recommendation ranking, and public community feed redesign. Those are separate product phases.
