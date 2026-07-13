# Cross-Platform Carving V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Android-only carving storage and UI with a versioned shared document, shared cliff renderer, Android Ink input, and iOS pointer input so both platforms can create, preview, reopen, and continue the same carving.

**Architecture:** `commonMain` owns immutable V2 document types, V1 compatibility, editor state, full-document persistence, shared screen/list UI, and deterministic Canvas rendering. Platform source sets own only live stroke capture: Android adapts `androidx.ink` output into normalized shared strokes, while iOS draws an active Compose pointer path and commits the same normalized stroke type.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, kotlinx.serialization, StateFlow, SQLDelight, AndroidX Ink, Kotlin test, Android ADB, Xcode/iOS Simulator, Maestro.

## Global Constraints

- New saves use V2 with `version = 2`, normalized `x/y`, `canvasAspectRatio`, normalized `sizeFraction`, pressure, elapsed time, brush type, and ARGB color.
- Existing `strokeData` remains the database and sync payload; do not add or migrate SQL columns.
- V1 top-level arrays remain readable and are never rewritten until the user explicitly edits and saves.
- Invalid data is read-only and must never be replaced with `[]` or an empty V2 document.
- Android retains AndroidX Ink for live capture; iOS uses Compose pointer input.
- Existing and newly added strokes are saved as one complete document.
- The same shared renderer is used by the editor and list previews on Android and iOS.
- Preserve the approved `摩崖留刻` place flow and `MONUMENTAL`/`榜书` default.
- Preserve unrelated worktree changes, especially `docs/superpowers/plans/2026-06-14-haptic-feedback.md`.
- Every UI/runtime change requires `./gradlew installDebug`, Android launch/tap/swipe/screencap before-and-after proof, iOS build/install/interactive verification, screenshots, and crash-log review.

---

### Task 1: Shared V2 Document And Codec

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocument.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocumentCodec.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingDocumentCodecTest.kt`

**Interfaces:**
- Produces: `CarvingDocument(version: Int, canvasAspectRatio: Float, strokes: List<CarvingStroke>)`
- Produces: `CarvingStroke(brushType, sizeFraction, colorArgb, points)` with computed runtime `brushSpec`
- Produces: `CarvingDocumentCodec.encode(document): String`
- Produces: `CarvingDocumentCodec.decode(data, previewAspectRatio): CarvingDecodeResult`
- Produces: `CarvingDecodeResult.Success`, `Empty`, and `Invalid`

- [ ] **Step 1: Write failing V2 round-trip and validation tests**

```kotlin
class CarvingDocumentCodecTest {
    private val document = CarvingDocument(
        canvasAspectRatio = 0.62f,
        strokes = listOf(
            CarvingStroke(
                brushType = CarvingBrushType.MONUMENTAL,
                sizeFraction = 0.09f,
                colorArgb = 0xFF1A1612.toInt(),
                points = listOf(
                    CarvingPoint(0.2f, 0.3f, 0.5f, 0L),
                    CarvingPoint(0.4f, 0.6f, 0.7f, 14L)
                )
            )
        )
    )

    @Test
    fun v2_roundTrip_preservesDocument() {
        val encoded = CarvingDocumentCodec.encode(document)
        val decoded = assertIs<CarvingDecodeResult.Success>(
            CarvingDocumentCodec.decode(encoded)
        )
        assertEquals(2, decoded.sourceVersion)
        assertEquals(document, decoded.document)
    }

    @Test
    fun invalidJson_isNotTreatedAsEmpty() {
        assertIs<CarvingDecodeResult.Invalid>(CarvingDocumentCodec.decode("not-json"))
    }

    @Test
    fun unknownVersion_isRejectedWithoutFallbackToV1() {
        assertIs<CarvingDecodeResult.Invalid>(
            CarvingDocumentCodec.decode("""{"version":9,"canvasAspectRatio":1,"strokes":[]}""")
        )
    }
}
```

- [ ] **Step 2: Run the focused test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.CarvingDocumentCodecTest --quiet`

Expected: FAIL because the shared document and codec do not exist.

- [ ] **Step 3: Implement serializable document types and explicit decode results**

```kotlin
@Serializable
data class CarvingDocument(
    val version: Int = CURRENT_CARVING_VERSION,
    val canvasAspectRatio: Float,
    val strokes: List<CarvingStroke>
)

@Serializable
data class CarvingStroke(
    val brushType: CarvingBrushType,
    val sizeFraction: Float,
    val colorArgb: Int,
    val points: List<CarvingPoint>
) {
    val brushSpec: CarvingBrushSpec
        get() = CarvingBrushSpec(brushType, sizeFraction, colorArgb)
}

@Serializable
data class CarvingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val elapsedTimeMillis: Long
)

data class CarvingBrushSpec(
    val type: CarvingBrushType = CarvingBrushType.MONUMENTAL,
    val sizeFraction: Float,
    val colorArgb: Int
)

@Serializable
enum class CarvingBrushType(val label: String) {
    IRON_CHISEL("铁錾"), MONUMENTAL("榜书"), WEATHERED("风化")
}

sealed interface CarvingDecodeResult {
    data class Success(val document: CarvingDocument, val sourceVersion: Int) : CarvingDecodeResult
    data object Empty : CarvingDecodeResult
    data class Invalid(val reason: String) : CarvingDecodeResult
}
```

Use `Json { ignoreUnknownKeys = true; explicitNulls = false }`. Encode only validated V2 objects. Reject unknown versions and non-finite aspect ratios; clamp point coordinates/pressure to `0f..1f`, clamp `sizeFraction` to `0.005f..0.30f`, remove strokes with fewer than two valid points, and keep elapsed time monotonic.

- [ ] **Step 4: Run codec and shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.CarvingDocumentCodecTest --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Commit the document format**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocument.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocumentCodec.kt shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingDocumentCodecTest.kt
git commit -m "feat(carving): add shared V2 stroke document"
```

---

### Task 2: V1 Compatibility Normalizer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/LegacyCarvingNormalizer.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocumentCodec.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingDocumentCodecTest.kt`

**Interfaces:**
- Consumes: legacy Android array fields `inputs`, `brushSize`, `brushColorArgb`, and `brushType`
- Produces: `LegacyCarvingNormalizer.normalize(strokes, preferredAspectRatio): CarvingDocument`
- Preserves: source raw string until explicit save

- [ ] **Step 1: Add failing V1 conversion tests**

```kotlin
@Test
fun v1Array_normalizesGeometryAndKeepsBrushMetadata() {
    val legacy = """[{"inputs":[{"x":100,"y":200,"pressure":0.4,"elapsedTimeMillis":0},{"x":300,"y":600,"pressure":0.8,"elapsedTimeMillis":20}],"brushSize":48,"brushColorArgb":-15000000,"brushType":"MONUMENTAL"}]"""

    val result = assertIs<CarvingDecodeResult.Success>(
        CarvingDocumentCodec.decode(legacy, previewAspectRatio = 0.62f)
    )

    assertEquals(1, result.sourceVersion)
    assertTrue(result.document.strokes.single().points.all { it.x in 0f..1f && it.y in 0f..1f })
    assertEquals(CarvingBrushType.MONUMENTAL, result.document.strokes.single().brushType)
    assertEquals(0.62f, result.document.canvasAspectRatio)
}

@Test
fun v1Conversion_isStableAcrossRepeatedReads() {
    val legacy = """[{"inputs":[{"x":10,"y":20,"pressure":0.5,"elapsedTimeMillis":0},{"x":30,"y":60,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":12,"brushColorArgb":-15000000,"brushType":"MONUMENTAL"}]"""
    val first = CarvingDocumentCodec.decode(legacy, null)
    val second = CarvingDocumentCodec.decode(legacy, null)
    assertEquals(first, second)
}
```

- [ ] **Step 2: Run the V1 tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.carving.CarvingDocumentCodecTest.v1*' --quiet`

Expected: FAIL because top-level arrays are not decoded.

- [ ] **Step 3: Implement origin-independent content fitting**

```kotlin
internal fun normalize(
    strokes: List<LegacyStroke>,
    preferredAspectRatio: Float?
): CarvingDocument {
    val bounds = LegacyBounds.from(strokes).expandByFraction(0.08f)
    val aspect = preferredAspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: bounds.aspectRatio.coerceIn(0.55f, 1.0f)
    val viewport = bounds.fitInside(aspect)
    return CarvingDocument(
        canvasAspectRatio = aspect,
        strokes = strokes.mapNotNull { it.toNormalizedStroke(viewport) }
    )
}
```

The viewport must preserve geometry, center the content, include brush radius, and provide 8% minimum padding. Unknown legacy brush names map to `MONUMENTAL`; invalid strokes are skipped. If no valid strokes remain, return `CarvingDecodeResult.Invalid("legacy_document_has_no_valid_strokes")`.

- [ ] **Step 4: Run compatibility and full shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.CarvingDocumentCodecTest --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 5: Commit V1 compatibility**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/LegacyCarvingNormalizer.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingDocumentCodec.kt shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingDocumentCodecTest.kt
git commit -m "feat(carving): preserve legacy Android strokes"
```

---

### Task 3: Full-Document Editor State And Safe Persistence

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingEditorState.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingViewModel.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingEditorStateTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingViewModelTest.kt`

**Interfaces:**
- Produces: `CarvingEditorState(document, sourceVersion, loadError, isDirty, isSaving, saveError)`
- Produces: `beginNew`, `loadCarvingForEdit`, `appendStroke`, `undo`, `clear`, `saveEditorDocument`, and `consumeSaveComplete`
- Removes: UI ownership of `existingStrokeData`

- [ ] **Step 1: Write failing merge, undo, invalid-load, and save tests**

```kotlin
private val thirdStroke = CarvingStroke(
    brushType = CarvingBrushType.MONUMENTAL,
    sizeFraction = 0.08f,
    colorArgb = 0xFF1A1612.toInt(),
    points = listOf(
        CarvingPoint(0.1f, 0.1f, 0.5f, 0L),
        CarvingPoint(0.3f, 0.4f, 0.5f, 12L)
    )
)

private val twoStrokeFixture = CarvingDocumentCodec.encode(
    CarvingDocument(canvasAspectRatio = 0.62f, strokes = listOf(thirdStroke, thirdStroke.copy()))
)

private fun existingCarving(data: String) = Carving(
    id = "carving-1",
    userId = "test-user",
    regionId = "330000",
    regionName = "浙江省",
    imagePath = null,
    strokeData = data,
    createdAt = 1L,
    previewAspectRatio = 0.62f
)

@Test
fun appendAndSave_keepsExistingAndNewStrokes() = runTest {
    repository.insertCarving(existingCarving(twoStrokeFixture))
    viewModel.loadCarvingForEdit("carving-1")
    viewModel.appendStroke(thirdStroke)

    viewModel.saveEditorDocument("330000", "浙江省")
    advanceUntilIdle()

    val saved = repository.getCarving("carving-1")!!
    val decoded = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(saved.strokeData!!))
    assertEquals(3, decoded.document.strokes.size)
}

@Test
fun invalidExistingData_disablesSaveAndPreservesRawData() = runTest {
    repository.insertCarving(existingCarving("broken"))
    viewModel.loadCarvingForEdit("carving-1")

    assertEquals("这方碑刻暂时无法读取", viewModel.editorState.value.loadError)
    assertFalse(viewModel.saveEditorDocument("330000", "浙江省"))
    assertEquals("broken", repository.getCarving("carving-1")!!.strokeData)
}
```

Initialize `repository` and `viewModel` in `@BeforeTest` with `MapChinaDatabase(TestDatabaseDriverFactory().createDriver())`, `CarvingRepository(database)`, user ID `test-user`, and `UnconfinedTestDispatcher()`.

- [ ] **Step 2: Run focused editor tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.carving.Carving*StateTest' --tests com.mapchina.ui.carving.CarvingViewModelTest --quiet`

Expected: FAIL because the editor API does not exist.

- [ ] **Step 3: Implement immutable editing operations**

```kotlin
data class CarvingEditorState(
    val document: CarvingDocument? = null,
    val sourceVersion: Int? = null,
    val loadError: String? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    val canSave: Boolean get() = loadError == null && document?.strokes?.isNotEmpty() == true
}

fun CarvingEditorState.append(stroke: CarvingStroke) = copy(
    document = requireNotNull(document).copy(strokes = requireNotNull(document).strokes + stroke),
    isDirty = true,
    saveError = null
)
```

Add equivalent pure `undo()` and `clear()` operations. `loadCarvingForEdit` decodes once with the carving's `previewAspectRatio`. `beginNew(aspectRatio)` starts an empty V2 document and resets `_saveComplete` to false.

- [ ] **Step 4: Replace raw-string save with complete-document save**

```kotlin
fun saveEditorDocument(
    regionId: String,
    regionName: String,
    attractionId: String? = null,
    attractionName: String? = null
): Boolean {
    val document = editorState.value.document ?: return false
    if (!editorState.value.canSave) return false
    val strokeData = CarvingDocumentCodec.encode(document)
    persistCarving(regionId, regionName, strokeData, document.canvasAspectRatio, attractionId, attractionName)
    return true
}
```

On repository failure, keep the document and set `saveError = "保存失败，请重试"`. Never catch and substitute `[]`.

- [ ] **Step 5: Run editor, repository, sync, and shared tests**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.carving.*' --quiet`

Expected: PASS.

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 6: Commit safe editor persistence**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingEditorState.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingViewModel.kt shared/src/commonTest/kotlin/com/mapchina/ui/carving
git commit -m "fix(carving): save complete editable documents"
```

---

### Task 4: Deterministic Shared Carving Renderer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingGeometry.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingArtwork.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingGeometryTest.kt`

**Interfaces:**
- Produces: `buildCarvingGeometry(document, widthPx, heightPx): List<CarvingStrokeGeometry>`
- Produces: `deterministicDebris(stroke, strokeIndex, widthPx, heightPx)`
- Produces: `PlatformPoint(x, y, pressure, elapsedTimeMillis)`, `normalizePlatformPoints`, and `coalescePlatformPoints`
- Produces: `@Composable CarvingArtwork(document, modifier, weatheredAlpha, showBackground)`

- [ ] **Step 1: Write failing geometry and determinism tests**

```kotlin
@Test
fun normalizedPoints_scaleToEveryTargetCanvas() {
    val geometry = buildCarvingGeometry(oneStrokeDocument, 620f, 1000f).single()
    assertEquals(124f, geometry.points.first().x, 0.01f)
    assertEquals(300f, geometry.points.first().y, 0.01f)
    assertEquals(55.8f, geometry.widthPx, 0.01f)
}

@Test
fun debris_isStableForSameStrokeAndCanvas() {
    val first = deterministicDebris(stroke, 0, 620f, 1000f)
    val second = deterministicDebris(stroke, 0, 620f, 1000f)
    assertEquals(first, second)
}
```

- [ ] **Step 2: Run geometry tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.CarvingGeometryTest --quiet`

Expected: FAIL because geometry helpers do not exist.

- [ ] **Step 3: Implement normalized geometry and stable seeds**

```kotlin
internal fun buildCarvingGeometry(document: CarvingDocument, widthPx: Float, heightPx: Float) =
    document.strokes.mapIndexed { index, stroke ->
        CarvingStrokeGeometry(
            index = index,
            brush = stroke.brushSpec,
            widthPx = stroke.sizeFraction * minOf(widthPx, heightPx),
            points = stroke.points.map { Offset(it.x * widthPx, it.y * heightPx) }
        )
    }

internal fun stableStrokeSeed(stroke: CarvingStroke, index: Int): Int =
    stroke.points.fold(31 + index) { seed, point ->
        31 * seed + point.x.toBits() + 17 * point.y.toBits()
    }

internal data class PlatformPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val elapsedTimeMillis: Long
)
```

`normalizePlatformPoints` divides `x/y` by non-zero canvas width/height, clamps normalized values and pressure to `0f..1f`, and returns no points for an empty canvas. `coalescePlatformPoints` drops a point only when it shares the previous timestamp and is within 0.5 physical pixels.

- [ ] **Step 4: Implement one cached Canvas rendering pipeline**

Use `painterResource(Res.drawable.cliff_face)` for the shared background. In one `drawWithCache` pass build centerline paths and deterministic debris. Draw broad displacement shadow, offset groove shadow, groove floor, upper-left rim highlight, impact craters, and debris. Use fixed per-stroke width; preserve pressure in data without applying platform-specific pressure curves.

```kotlin
Canvas(
    modifier = modifier.drawWithCache {
        val geometry = buildCarvingGeometry(document, size.width, size.height)
        val paths = geometry.map(::buildCenterlinePath)
        val debris = geometry.flatMap { deterministicDebris(document.strokes[it.index], it.index, size.width, size.height) }
        onDrawBehind { drawCarvingLayers(geometry, paths, debris, weatheredAlpha) }
    }
) {}
```

- [ ] **Step 5: Run geometry and shared tests**

Run: `./gradlew :shared:allTests --quiet`

Expected: PASS.

- [ ] **Step 6: Commit the shared renderer**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingGeometry.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingArtwork.kt shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingGeometryTest.kt
git commit -m "feat(carving): render deterministic cliff inscriptions"
```

---

### Task 5: Shared Editor And Gallery UI

**Files:**
- Rewrite: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/PlatformCarvingInput.kt`
- Create: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingUiPolicyTest.kt`

**Interfaces:**
- Produces concrete common `CarvingScreen` and `CarvingListScreen`; removes their `expect` declarations
- Produces `expect @Composable PlatformCarvingInputSurface(brush, enabled, modifier, onStrokeCommitted)`
- Preserves navigation signatures consumed by `AppNavHost`

- [ ] **Step 1: Write failing policy tests for title, empty save, and context target**

```kotlin
@Test
fun editorTitle_prefersAttractionThenRegion() {
    assertEquals("西湖", carvingPlaceTitle("浙江省", "西湖"))
    assertEquals("浙江省", carvingPlaceTitle("浙江省", null))
}

@Test
fun invalidDocument_neverOffersSave() {
    assertFalse(CarvingEditorState(loadError = "bad").canSave)
}

@Test
fun galleryContext_fallsBackToChinaLandscape() {
    assertEquals("cn_landscape", carvingContextTarget("我的碑刻", null, null).regionId)
}
```

- [ ] **Step 2: Run policy tests and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.CarvingUiPolicyTest --quiet`

Expected: FAIL because policy helpers do not exist.

- [ ] **Step 3: Build the common editor shell**

The top app bar uses the shared back button, `摩崖留刻`, and a save icon with content description `保存碑刻`. The canvas keeps a stable full-width weighted size and reports its aspect ratio to `beginNew`. The toolbar uses icon controls for undo/clear and compact labeled brush choices. Add invisible test tags: `carving-canvas`, `carving-save`, `carving-undo`, and `carving-clear`.

```kotlin
PlatformCarvingInputSurface(
    brush = selectedBrush,
    enabled = state.loadError == null,
    modifier = Modifier.fillMaxSize().testTag("carving-canvas"),
    onStrokeCommitted = viewModel::appendStroke
)
```

The invalid state shows `这方碑刻暂时无法读取` over the cliff background and omits save/clear controls. Empty save triggers haptic warning and `先刻下一笔，再落成碑刻`.

Collect `saveComplete` in the common editor. When it becomes true, call `viewModel.consumeSaveComplete()` before `onBack()` so the singleton ViewModel cannot immediately close the next editor session.

- [ ] **Step 4: Move the gallery and place picker to commonMain**

Use `LazyVerticalStaggeredGrid` with `key = carving.id`, 8dp card corners, fixed 180dp artwork area, and `CarvingArtwork`. Decode each card with `remember(carving.strokeData, carving.previewAspectRatio)` and show a restrained unreadable state for invalid content. Preserve long-press delete confirmation and the four existing recommended places.

- [ ] **Step 5: Run common tests and compile both targets**

Run: `./gradlew :shared:allTests :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: FAIL only because platform input actuals are not implemented yet; common tests PASS and no common source errors appear.

- [ ] **Step 6: Commit the shared UI shell**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/PlatformCarvingInput.kt shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingUiPolicyTest.kt
git commit -m "feat(carving): share editor and gallery UI"
```

---

### Task 6: Android Ink Adapter

**Files:**
- Create: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/PlatformCarvingInput.android.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt`
- Delete: `shared/src/androidMain/kotlin/com/mapchina/ui/carving/StrokeSerializer.kt`
- Create: `shared/src/androidUnitTest/kotlin/com/mapchina/ui/carving/AndroidStrokeAdapterTest.kt`

**Interfaces:**
- Actualizes: `PlatformCarvingInputSurface`
- Produces: `inkStrokeToCarvingStroke(stroke, brushSpec, canvasSize): CarvingStroke?`

- [ ] **Step 1: Write a failing pure normalization test**

```kotlin
@Test
fun platformPoints_areNormalizedAgainstCanvas() {
    val points = normalizePlatformPoints(
        listOf(PlatformPoint(100f, 200f, 0.7f, 0L), PlatformPoint(300f, 600f, 0.8f, 12L)),
        IntSize(400, 800)
    )
    assertEquals(0.25f, points.first().x)
    assertEquals(0.25f, points.first().y)
    assertEquals(0.75f, points.last().x)
}
```

- [ ] **Step 2: Run the adapter test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests com.mapchina.ui.carving.AndroidStrokeAdapterTest --quiet`

Expected: FAIL because the adapter does not exist.

- [ ] **Step 3: Implement the Ink actual and conversion**

Track the input surface `IntSize` with `onSizeChanged`. Build the Ink brush from `sizeFraction * minDimension`. In `InProgressStrokes.onStrokesFinished`, read every `StrokeInput`, normalize coordinates, preserve pressure/time, attach the brush active when the stroke began, and commit each valid shared stroke. Keep heavy haptic feedback after each committed stroke.

```kotlin
InProgressStrokes(
    defaultBrush = inkBrush,
    modifier = modifier.onSizeChanged { canvasSize = it },
    onStrokesFinished = { strokes ->
        strokes.mapNotNull { inkStrokeToCarvingStroke(it, brush, canvasSize) }
            .forEach(onStrokeCommitted)
    }
)
```

- [ ] **Step 4: Run Android tests and install**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest installDebug --quiet`

Expected: PASS and debug APK installed.

- [ ] **Step 5: Commit the Android adapter**

```bash
git add shared/src/androidMain/kotlin/com/mapchina/ui/carving shared/src/androidUnitTest/kotlin/com/mapchina/ui/carving/AndroidStrokeAdapterTest.kt
git commit -m "feat(carving): adapt Android Ink to V2 strokes"
```

---

### Task 7: iOS Pointer Adapter

**Files:**
- Create: `shared/src/iosMain/kotlin/com/mapchina/ui/carving/PlatformCarvingInput.ios.kt`
- Delete: `shared/src/iosMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingGeometryTest.kt`

**Interfaces:**
- Actualizes: `PlatformCarvingInputSurface`
- Uses: shared `normalizePlatformPoints` and `CarvingStroke`
- Keeps active-point state inside the platform input composable so parent chrome does not recompose per pointer event

- [ ] **Step 1: Add failing duplicate-point coalescing tests**

```kotlin
@Test
fun coalescePlatformPoints_dropsSubPixelDuplicateAtSameTime() {
    val points = coalescePlatformPoints(
        listOf(
            PlatformPoint(10f, 10f, 0.5f, 0L),
            PlatformPoint(10.2f, 10.2f, 0.5f, 0L),
            PlatformPoint(20f, 20f, 0.5f, 8L)
        )
    )
    assertEquals(2, points.size)
}
```

- [ ] **Step 2: Run the shared test and verify red**

Run: `./gradlew :shared:testDebugUnitTest --tests 'com.mapchina.ui.carving.CarvingGeometryTest.coalesce*' --quiet`

Expected: FAIL because point coalescing is not implemented.

- [ ] **Step 3: Implement iOS pointer capture and active preview**

Use `awaitEachGesture`, `awaitFirstDown`, and `awaitPointerEvent`. Keep a `mutableStateListOf<PlatformPoint>` inside the actual composable, draw its centerline in a local Canvas, and create one immutable `CarvingStroke` on pointer up. Use event pressure when positive; otherwise `0.5f`. Ignore additional pointers.

```kotlin
Box(
    modifier = modifier
        .onSizeChanged { canvasSize = it }
        .pointerInput(brush, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startedAt = down.uptimeMillis
                activePoints += PlatformPoint(down.position.x, down.position.y, 0.5f, 0L)
                var pressed = true
                while (pressed) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    pressed = change.pressed
                    if (pressed) {
                        activePoints += PlatformPoint(
                            change.position.x,
                            change.position.y,
                            change.pressure.takeIf { it > 0f } ?: 0.5f,
                            change.uptimeMillis - startedAt
                        )
                    }
                }
                normalizePlatformPoints(coalescePlatformPoints(activePoints), canvasSize)
                    .takeIf { it.size >= 2 }
                    ?.let {
                        onStrokeCommitted(
                            CarvingStroke(brush.type, brush.sizeFraction, brush.colorArgb, it)
                        )
                    }
                activePoints.clear()
            }
        }
) { ActiveStrokeCanvas(activePoints, brush) }
```

- [ ] **Step 4: Build the iOS app and run shared tests**

Run: `./gradlew :shared:allTests :shared:compileKotlinIosSimulatorArm64 --quiet`

Expected: PASS.

Run: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -configuration Debug build`

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5: Commit the iOS adapter**

```bash
git add shared/src/iosMain/kotlin/com/mapchina/ui/carving shared/src/commonTest/kotlin/com/mapchina/ui/carving/CarvingGeometryTest.kt
git commit -m "feat(carving): enable iOS cliff inscription input"
```

---

### Task 8: Dual-Platform Runtime And Cross-Data Verification

**Files:**
- Create: `docs/qa/carving-v2-cross-platform-verification.md`
- Runtime artifacts: `.superpowers/sdd/carving-v2/`

**Interfaces:**
- Consumes: installed Android debug APK and built iOS Simulator app
- Produces: Android-generated V2 payload opened on iOS and iOS-generated V2 payload opened on Android
- Produces: before/after screenshots, logs, commands, and payload hashes

- [ ] **Step 1: Capture Android before evidence from the pre-change commit if missing**

Use the retained pre-change screenshot from `.superpowers/sdd` when it clearly shows the Android-only state. If absent, use `git worktree` at commit `15f9e1c`, build/install there, navigate to the carving gallery, and capture with:

```bash
adb shell screencap -p /sdcard/carving-before.png
adb pull /sdcard/carving-before.png .superpowers/sdd/carving-v2/android-before.png
```

- [ ] **Step 2: Verify the complete Android flow**

Run: `./gradlew :shared:allTests :androidApp:testDebugUnitTest installDebug --quiet`

Launch with `adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1`. Use `adb shell input tap/swipe` to open 山河 → 碑刻 → new place → draw at least three strokes → undo → draw again → save → reopen → continue → save. Capture list and editor screenshots with `adb shell screencap`; save logcat filtered for `FATAL EXCEPTION`, `Carving`, and serialization errors.

Expected: visible cliff letters in editor and list, original strokes remain after continue-save, no crash or codec error.

- [ ] **Step 3: Verify the complete iOS flow**

Build and install to `iPhone 17 Pro`, then use Maestro to open 山河 → 碑刻 → new place → draw at least three strokes → undo → draw again → save → reopen → continue → save. Capture list and editor screenshots plus the simulator error/fault log.

Expected: visible cliff letters in editor and list, no unsupported-platform copy, no stroke loss, no crash.

- [ ] **Step 4: Exchange real payloads between app containers**

Android database: `/data/data/com.mapchina.android/databases/mapchina.db` via `adb exec-out run-as com.mapchina.android`.

iOS container: `xcrun simctl get_app_container booted com.mapchina.iosApp data`, then locate `mapchina.db` below the returned directory.

Checkpoint WAL or close each app before copying. Export the newest carving's `stroke_data`, `preview_aspect_ratio`, and identity fields with `sqlite3`. Insert the Android row into iOS and the iOS row into Android under new IDs, relaunch each app, and open both through the formal gallery UI. Record SHA-256 hashes of the raw payloads and normalized decoded fixture output.

Expected: both payloads render on both platforms; stroke count/order/brush/color match; normalized point error is at most `0.001`; continuing on the receiving platform keeps old strokes.

- [ ] **Step 5: Write the verification report**

Record exact build commands, simulator/emulator names, screenshot paths, payload hashes, test results, log findings, and any evidence limitation in `docs/qa/carving-v2-cross-platform-verification.md`.

- [ ] **Step 6: Commit verification documentation**

```bash
git add docs/qa/carving-v2-cross-platform-verification.md
git commit -m "test(carving): verify V2 across Android and iOS"
```
