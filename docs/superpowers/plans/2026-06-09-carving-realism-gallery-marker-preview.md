# Carving Realism, Waterfall Gallery & Marker Preview Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade carving rendering to authentic cliff-face (摩崖石刻) aesthetics, replace text-only carving list with waterfall image gallery, and add attraction marker preview cards on the map.

**Architecture:** Three independent feature tracks carved by dependency: (1) carving rendering upgrades in CarvingScreen, (2) data layer + gallery list + edit mode, (3) map marker preview card. Track 2 depends on track 1's preview image generation. Track 3 is fully independent.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Android Ink (androidx.ink), SQLDelight, Koin DI, AMap SDK

---

## File Map

### New Files
- `shared/src/commonMain/composeResources/drawable/cliff_face.png` — replacement cliff texture
- `shared/src/commonMain/kotlin/com/mapchina/ui/carving/StrokeSerializer.kt` — stroke JSON serialization/deserialization
- `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CliffRenderer.kt` — extracted cliff-face rendering logic (shared between on-screen and offscreen)
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/AttractionPreviewCard.kt` — marker preview card composable
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/BottomPanel.kt` — sealed class for mutual exclusivity

### Modified Files
- `shared/src/commonMain/sqldelight/com/mapchina/data/local/Carving.sq` — add preview_aspect_ratio column + updateCarving query
- `shared/src/commonMain/kotlin/com/mapchina/domain/model/Carving.kt` — add previewAspectRatio field
- `shared/src/commonMain/kotlin/com/mapchina/data/repository/CarvingRepository.kt` — add updateCarving, map new column
- `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingViewModel.kt` — edit mode, stroke serialization, ID change
- `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt` — cliff rendering, new brushes, edit mode
- `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt` — waterfall grid, image cards, edit click
- `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt` — add carvingId to CarvingScreen route
- `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt` — wire carvingId, edit navigation
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt` — attraction preview card, BottomPanel integration
- `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt` — BottomPanel state, previewAttraction, AttractionUi.imageUrl
- `shared/src/commonMain/kotlin/com/mapchina/di/AppModule.kt` — wire new dependencies if needed

---

## Chunk 1: Carving Rendering & Data Layer

### Task 1: Add cliff texture resource and update Carving.sq schema

**Files:**
- Create: `shared/src/commonMain/composeResources/drawable/cliff_face.png`
- Modify: `shared/src/commonMain/sqldelight/com/mapchina/data/local/Carving.sq`

- [ ] **Step 1: Add cliff_face.png texture**

Place a natural cliff rock surface image (irregular, blue-grey/ochre tone, with cracks and pits) at `shared/src/commonMain/composeResources/drawable/cliff_face.png`. This must be sourced or generated externally — use a free stock photo of a weathered cliff face, resized to ~1024x1024px.

- [ ] **Step 2: Update Carving.sq schema**

Add `preview_aspect_ratio` column and `updateCarving` query:

```sql
CREATE TABLE carving (
    id TEXT NOT NULL PRIMARY KEY,
    user_id TEXT NOT NULL,
    region_id TEXT NOT NULL,
    region_name TEXT NOT NULL,
    image_path TEXT,
    stroke_data TEXT,
    created_at INTEGER NOT NULL,
    attraction_id TEXT,
    attraction_name TEXT,
    preview_aspect_ratio REAL
);

selectById:
SELECT * FROM carving WHERE id = ?;

selectByRegionId:
SELECT * FROM carving WHERE region_id = ?;

selectByUserId:
SELECT * FROM carving WHERE user_id = ? ORDER BY created_at DESC;

selectByAttractionId:
SELECT * FROM carving WHERE attraction_id = ? ORDER BY created_at DESC;

selectAll:
SELECT * FROM carving ORDER BY created_at DESC;

insertCarving:
INSERT OR REPLACE INTO carving(id, user_id, region_id, region_name, image_path, stroke_data, created_at, attraction_id, attraction_name, preview_aspect_ratio)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateCarving:
UPDATE carving SET image_path = ?, stroke_data = ?, preview_aspect_ratio = ? WHERE id = ?;

deleteById:
DELETE FROM carving WHERE id = ?;
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/composeResources/drawable/cliff_face.png shared/src/commonMain/sqldelight/com/mapchina/data/local/Carving.sq
git commit -m "feat: add cliff texture and carving schema with preview_aspect_ratio"
```

---

### Task 2: Update Carving data model and repository

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/domain/model/Carving.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/data/repository/CarvingRepository.kt`

- [ ] **Step 1: Add previewAspectRatio to Carving model**

```kotlin
// Carving.kt
data class Carving(
    val id: String,
    val userId: String,
    val regionId: String,
    val regionName: String,
    val imagePath: String?,
    val strokeData: String?,
    val createdAt: Long,
    val attractionId: String? = null,
    val attractionName: String? = null,
    val previewAspectRatio: Float? = null
)
```

- [ ] **Step 2: Update CarvingRepository**

Add `preview_aspect_ratio` mapping to `rowToCarving`, `insertCarving` params, and add `updateCarving` method:

```kotlin
// In rowToCarving:
previewAspectRatio = row.preview_aspect_ratio?.toFloat()

// In insertCarving:
database.carvingQueries.insertCarving(
    carving.id, carving.userId, carving.regionId, carving.regionName,
    carving.imagePath, carving.strokeData, carving.createdAt,
    carving.attractionId, carving.attractionName, carving.previewAspectRatio
)

// New method:
fun updateCarving(carving: Carving) {
    database.carvingQueries.updateCarving(
        carving.imagePath, carving.strokeData, carving.previewAspectRatio, carving.id
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/domain/model/Carving.kt shared/src/commonMain/kotlin/com/mapchina/data/repository/CarvingRepository.kt
git commit -m "feat: add previewAspectRatio to Carving model and repository"
```

---

### Task 3: Create StrokeSerializer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/StrokeSerializer.kt`

- [ ] **Step 1: Implement stroke serialization/deserialization**

```kotlin
package com.mapchina.ui.carving

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.strokes.Stroke
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SerializableStroke(
    val inputs: List<SerializableInput>,
    val brushSize: Float,
    val brushColorArgb: Int,
    val brushType: String
)

@Serializable
data class SerializableInput(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long
)

private val json = Json { ignoreUnknownKeys = true }

fun serializeStrokes(strokes: List<Stroke>, brushType: CarvingBrushType, brushColorArgb: Int): String {
    val serializable = strokes.map { stroke ->
        SerializableStroke(
            inputs = stroke.inputs.map { input ->
                SerializableInput(input.x, input.y, input.pressure, input.timestamp)
            },
            brushSize = stroke.brush.size,
            brushColorArgb = stroke.brush.colorIntArgb,
            brushType = brushType.name
        )
    }
    return json.encodeToString(kotlinx.serialization.serializer<List<SerializableStroke>>(), serializable)
}

fun deserializeStrokes(data: String): List<Stroke> {
    if (data.isBlank()) return emptyList()
    val serializable = json.decodeFromString<kotlinx.serialization.serializer<List<SerializableStroke>>>(data)
    return serializable.map { ss ->
        val builder = Stroke.Builder()
        builder.setBrush(
            Brush.Builder()
                .setFamily(BrushFamily())
                .setSize(ss.brushSize)
                .setColorIntArgb(ss.brushColorArgb)
                .build()
        )
        for (input in ss.inputs) {
            builder.addInput(input.timestamp, input.x, input.y, input.pressure)
        }
        builder.build()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/StrokeSerializer.kt
git commit -m "feat: add stroke serialization for carving edit mode"
```

---

### Task 4: Upgrade CarvingScreen rendering to cliff-face style

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`

- [ ] **Step 1: Replace brush types and colour palette**

Replace `CarvingBrushType` enum values and colour palette:

```kotlin
enum class CarvingBrushType(val label: String) {
    IRON_CHISEL("铁錾"),
    MONUMENTAL("榜书"),
    WEATHERED("风化")
}

// In colour selector row, replace palette:
val palette = listOf(
    Color(0xFF1A1612) to "崖壁墨",
    Color(0xFF4A5568) to "青石灰",
    Color(0xFF8B2500) to "朱砂",
    Color(0xFF9CA3AF) to "风化石"
)
```

- [ ] **Step 2: Update brush configuration in rememberCarvingBrush**

Change epsilon and size multipliers:

```kotlin
val (epsilon, adjustedSize) = when (brushType) {
    CarvingBrushType.IRON_CHISEL -> 0.15f to size * 1.8f
    CarvingBrushType.MONUMENTAL -> 0.05f to size * 2.2f
    CarvingBrushType.WEATHERED -> 0.3f to size * 1.5f
}
```

- [ ] **Step 3: Replace stone_wall with cliff_face texture**

Change `painterResource(Res.drawable.stone_wall)` to `painterResource(Res.drawable.cliff_face)`.

- [ ] **Step 4: Update Canvas gradient overlays**

Replace the light gradient with a blue-grey/ochre warm glow:

```kotlin
// Layer 2: Warm centre glow (replaces "light gradient")
Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(
        brush = ComposeBrush.radialGradient(
            colors = listOf(
                Color(0x15D4C5A0), // warm ochre centre
                Color.Transparent,
                Color(0x0D1A2020)  // cool blue-grey edge
            ),
            center = Offset(size.width * 0.45f, size.height * 0.4f),
            radius = size.width * 0.6f
        )
    )
}
```

- [ ] **Step 5: Replace 3-layer (shadow/groove/highlight) rendering with cliff 3-layer**

Replace the finished strokes Canvas block. Add deterministic random offsets, `StrokeJoin.Miter`, `dashPathEffect` for iron chisel, and 3-layer cliff groove:

```kotlin
if (finishedStrokes.isNotEmpty()) {
    // Layer A: Weathered edge blur (soft halo behind everything)
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (stroke in finishedStrokes) {
            val path = strokeToPath(stroke)
            val width = stroke.brush.size
            drawPath(
                path = path,
                color = Color(0x261A1612),
                style = DrawStroke(
                    width = width + 12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    // Layer B: Deep shadow (offset +4,+4, wide, dark)
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (stroke in finishedStrokes) {
            val path = strokeToPath(stroke, offsetDx = 4f, offsetDy = 4f)
            val width = stroke.brush.size
            drawPath(
                path = path,
                color = Color(0x80000000),
                style = DrawStroke(
                    width = width + 10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Miter,
                    miter = 3f
                )
            )
        }
    }

    // Layer C: Groove colour (no offset, Miter join, dash for iron chisel)
    Canvas(modifier = Modifier.fillMaxSize()) {
        for ((index, stroke) in finishedStrokes.withIndex()) {
            val path = strokeToPath(stroke)
            val width = stroke.brush.size
            val color = stroke.brush.colorIntArgb.toComposeColor()

            val pathEffect = when (brushType) {
                CarvingBrushType.IRON_CHISEL -> PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                else -> null
            }

            // Spalling: deterministic alpha reduction
            val rng = kotlin.random.Random(stroke.inputs.first().x.toInt() * 31 + index)
            val spallAlpha = if (rng.nextFloat() < 0.15f) 0.3f + rng.nextFloat() * 0.3f else 1f

            drawPath(
                path = path,
                color = color.copy(alpha = spallAlpha),
                style = DrawStroke(
                    width = width + rng.nextFloat() * 4f - 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Miter,
                    miter = 3f,
                    pathEffect = pathEffect
                )
            )
        }
    }

    // Layer D: Upper-rim highlight (offset -2,-2, thin, warm white)
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (stroke in finishedStrokes) {
            val path = strokeToPath(stroke, offsetDx = -2f, offsetDy = -2f)
            val width = stroke.brush.size
            drawPath(
                path = path,
                color = Color(0x40FFF8E1),
                style = DrawStroke(
                    width = width * 0.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Miter,
                    miter = 3f
                )
            )
        }
    }

    // Layer E: Lichen patches (small green blobs near stroke endpoints)
    Canvas(modifier = Modifier.fillMaxSize()) {
        for ((index, stroke) in finishedStrokes.withIndex()) {
            val rng = kotlin.random.Random(stroke.inputs.first().x.toInt() * 31 + index + 7)
            if (rng.nextFloat() < 0.3f) {
                val lastInput = stroke.inputs.last()
                drawCircle(
                    color = Color(0xFF2D4A2D).copy(alpha = 0.25f + rng.nextFloat() * 0.15f),
                    radius = 4f + rng.nextFloat() * 4f,
                    center = Offset(lastInput.x + rng.nextFloat() * 10f - 5f, lastInput.y + rng.nextFloat() * 10f - 5f)
                )
            }
        }
    }
}
```

- [ ] **Step 6: Update strokeToPath to support offset**

Add offset parameters and deterministic jitter:

```kotlin
private fun strokeToPath(stroke: Stroke, offsetDx: Float = 0f, offsetDy: Float = 0f): Path {
    val inputs = stroke.inputs
    val path = Path()
    if (inputs.isEmpty()) return path

    val first = inputs[0]
    path.moveTo(first.x + offsetDx, first.y + offsetDy)

    if (inputs.size == 1) return path

    for (i in 1 until inputs.size) {
        val curr = inputs[i]
        val cx = curr.x + offsetDx
        val cy = curr.y + offsetDy
        if (i < inputs.size - 1) {
            val next = inputs[i + 1]
            val midX = (cx + next.x + offsetDx) / 2f
            val midY = (cy + next.y + offsetDy) / 2f
            path.quadraticBezierTo(cx, cy, midX, midY)
        } else {
            path.lineTo(cx, cy)
        }
    }
    return path
}
```

- [ ] **Step 7: Update existing strokes rendering to match cliff style**

Update the existing-strokes Canvas block to use the same 3-layer cliff groove style but with lower alpha (weathered appearance):

```kotlin
if (existingStrokes.isNotEmpty()) {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }) {
        for (stroke in existingStrokes) {
            val path = strokeToPath(stroke, offsetDx = 4f, offsetDy = 4f)
            drawPath(path, Color(0x55000000), style = DrawStroke(width = stroke.brush.size + 8f, cap = StrokeCap.Round, join = StrokeJoin.Miter, miter = 3f))
        }
        for (stroke in existingStrokes) {
            val path = strokeToPath(stroke)
            drawPath(path, stroke.brush.colorIntArgb.toComposeColor().copy(alpha = 0.6f), style = DrawStroke(width = stroke.brush.size, cap = StrokeCap.Round, join = StrokeJoin.Miter, miter = 3f))
        }
        for (stroke in existingStrokes) {
            val path = strokeToPath(stroke, offsetDx = -2f, offsetDy = -2f)
            drawPath(path, Color(0x25FFF8E1), style = DrawStroke(width = stroke.brush.size * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Miter, miter = 3f))
        }
    }
}
```

- [ ] **Step 8: Update BrushTypeButton icons**

```kotlin
val icon = when (type) {
    CarvingBrushType.IRON_CHISEL -> Icons.Default.Edit
    CarvingBrushType.MONUMENTAL -> Icons.Default.Brush
    CarvingBrushType.WEATHERED -> Icons.Default.Gesture
}
```

- [ ] **Step 9: Build and verify**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt
git commit -m "feat: upgrade carving rendering to cliff-face (摩崖石刻) style"
```

---

### Task 5: Add edit mode and stroke serialization to CarvingViewModel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingViewModel.kt`

- [ ] **Step 1: Add carvingId parameter and edit mode support**

```kotlin
class CarvingViewModel(
    private val carvingRepository: CarvingRepository,
    private val userId: String = ""
) {
    // ... existing fields ...

    private var editingCarvingId: String? = null

    fun loadCarvingForEdit(carvingId: String) {
        editingCarvingId = carvingId
        val carving = carvingRepository.getCarving(carvingId)
        _currentCarving.value = carving
        carving?.strokeData?.let { data ->
            _existingStrokes.value = deserializeStrokes(data)
        } ?: run {
            _existingStrokes.value = emptyList()
        }
    }

    // Update saveCarving to use timestamp-based ID and serialize strokes
    fun saveCarving(
        regionId: String,
        regionName: String,
        strokes: List<Stroke>,
        brushType: CarvingBrushType,
        brushColorArgb: Int,
        imagePath: String? = null,
        previewAspectRatio: Float? = null,
        attractionId: String? = null,
        attractionName: String? = null
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = editingCarvingId ?: "carving_${regionId}_${attractionId ?: "region"}_$now"
        val strokeData = serializeStrokes(strokes, brushType, brushColorArgb)
        val carving = Carving(
            id = id,
            userId = userId,
            regionId = regionId,
            regionName = regionName,
            imagePath = imagePath,
            strokeData = strokeData,
            createdAt = if (editingCarvingId != null) carvingRepository.getCarving(id)?.createdAt ?: now else now,
            attractionId = attractionId,
            attractionName = attractionName,
            previewAspectRatio = previewAspectRatio
        )
        vmScope.launch {
            if (editingCarvingId != null) {
                carvingRepository.updateCarving(carving)
            } else {
                carvingRepository.insertCarving(carving)
            }
            _currentCarving.value = carving
            editingCarvingId = null
        }
    }

    fun deleteCarving(id: String) {
        vmScope.launch {
            carvingRepository.deleteCarving(id)
            _currentCarving.value = null
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingViewModel.kt
git commit -m "feat: add edit mode and stroke serialization to CarvingViewModel"
```

---

## Chunk 2: Waterfall Gallery List

### Task 6: Add carvingId to CarvingScreen route and update CarvingScreen for edit mode

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt`

- [ ] **Step 1: Add carvingId to CarvingScreen route**

```kotlin
@Serializable data class CarvingScreen(
    val regionId: String,
    val regionName: String,
    val attractionId: String? = null,
    val attractionName: String? = null,
    val carvingId: String? = null
) : Screen()
```

- [ ] **Step 2: Add carvingId parameter to CarvingScreen composable**

Add `carvingId: String? = null` parameter. When non-null, call `viewModel.loadCarvingForEdit(carvingId)` in a `LaunchedEffect`:

```kotlin
LaunchedEffect(carvingId) {
    if (carvingId != null) {
        viewModel.loadCarvingForEdit(carvingId)
    }
}
```

- [ ] **Step 3: Update save button to pass brush info and handle edit mode**

In the save action, serialize strokes and include brush metadata:

```kotlin
IconButton(onClick = {
    viewModel.saveCarving(
        regionId, regionName, finishedStrokes,
        brushType = brushType,
        brushColorArgb = brushColor.toArgb(),
        attractionId = attractionId,
        attractionName = attractionName
    )
    onBack()
}) { ... }
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/navigation/Screen.kt shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingScreen.kt
git commit -m "feat: add edit mode support to CarvingScreen with carvingId"
```

---

### Task 7: Rewrite CarvingListScreen as waterfall gallery

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt`

- [ ] **Step 1: Replace LazyColumn with LazyVerticalStaggeredGrid**

Replace imports and layout:

```kotlin
import androidx.compose.foundation.foundation.LazyVerticalStaggeredGrid
import androidx.compose.foundation.foundation.StaggeredGridCells
import androidx.compose.foundation.ExperimentalFoundationApi
```

Replace the `LazyColumn` block:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Fixed(2),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    horizontalItemSpacing = 8.dp,
    verticalItemSpacing = 8.dp,
    modifier = Modifier.fillMaxSize()
) {
    items(carvings, key = { it.id }) { carving ->
        CarvingImageCard(
            carving = carving,
            onClick = { onEditClick(carving.id) },
            onLongPress = { deleteTarget = carving }
        )
    }
    item { Spacer(modifier = Modifier.height(88.dp)) }
}
```

- [ ] **Step 2: Implement CarvingImageCard composable**

```kotlin
@Composable
private fun CarvingImageCard(
    carving: Carving,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
    ) {
        // Preview image or placeholder
        if (carving.imagePath != null) {
            // Load local image — use platform-specific loader
            AsyncImage(
                model = carving.imagePath,
                contentDescription = carving.attractionName ?: carving.regionName,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
                fallback = painterResource(Res.drawable.cliff_face),
                error = painterResource(Res.drawable.cliff_face)
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.cliff_face),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Bottom gradient overlay with text
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    carving.attractionName ?: carving.regionName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDate(carving.createdAt),
                    color = Color(0xAAFFFFFF),
                    fontSize = 11.sp
                )
            }
        }
    }
}
```

Note: `AsyncImage` requires Coil dependency. If not available, use `Image(painter = BitmapPainter(...))` with a local file loader. Check project dependencies first.

- [ ] **Step 3: Add onEditClick callback to CarvingListScreen**

Add parameter:
```kotlin
onEditClick: (Carving) -> Unit = {},
```

Wire it in the grid items:
```kotlin
CarvingImageCard(
    carving = carving,
    onClick = { onEditClick(carving) },
    onLongPress = { deleteTarget = carving }
)
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/carving/CarvingListScreen.kt
git commit -m "feat: replace carving list with waterfall gallery layout"
```

---

### Task 8: Wire CarvingListScreen edit navigation in AppNavHost

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt`

- [ ] **Step 1: Update CarvingListScreen composable in AppNavHost**

Add `onEditClick` wiring:

```kotlin
composable<CarvingListScreen> { backStackEntry ->
    val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
    val regionId = backStackEntry.arguments?.getString("regionId")
    val regionName = backStackEntry.arguments?.getString("regionName")
    val attractionId = backStackEntry.arguments?.getString("attractionId")
    val showAll = backStackEntry.arguments?.getString("showAll") == "true"
    com.mapchina.ui.carving.CarvingListScreen(
        viewModel = vm,
        title = if (showAll) "我的碑刻" else "碑刻 · ${regionName ?: ""}",
        regionId = regionId,
        attractionId = attractionId,
        showAll = showAll,
        onCreateClick = {
            val rId = regionId ?: ""
            val rName = regionName ?: ""
            navController.navigate(com.mapchina.ui.navigation.CarvingScreen(regionId = rId, regionName = rName, attractionId = attractionId))
        },
        onEditClick = { carving ->
            navController.navigate(com.mapchina.ui.navigation.CarvingScreen(
                regionId = carving.regionId,
                regionName = carving.regionName,
                attractionId = carving.attractionId,
                attractionName = carving.attractionName,
                carvingId = carving.id
            ))
        },
        onBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 2: Update CarvingScreen composable in AppNavHost to pass carvingId**

```kotlin
composable<com.mapchina.ui.navigation.CarvingScreen> { backStackEntry ->
    val vm: com.mapchina.ui.carving.CarvingViewModel = koinInject()
    val regionId = backStackEntry.arguments?.getString("regionId") ?: ""
    val regionName = backStackEntry.arguments?.getString("regionName") ?: ""
    val attractionId = backStackEntry.arguments?.getString("attractionId")
    val attractionName = backStackEntry.arguments?.getString("attractionName")
    val carvingId = backStackEntry.arguments?.getString("carvingId")
    com.mapchina.ui.carving.CarvingScreen(
        regionId = regionId,
        regionName = regionName,
        viewModel = vm,
        onBack = { navController.popBackStack() },
        attractionId = attractionId,
        attractionName = attractionName,
        carvingId = carvingId
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/navigation/AppNavHost.kt
git commit -m "feat: wire carving edit mode navigation"
```

---

## Chunk 3: Attraction Marker Preview Card

### Task 9: Add BottomPanel sealed class and update MapViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/BottomPanel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt`

- [ ] **Step 1: Create BottomPanel sealed class**

```kotlin
package com.mapchina.ui.map

sealed class BottomPanel {
    data object None : BottomPanel()
    data class Region(val regionId: String) : BottomPanel()
    data class AttractionPreview(val attractionId: String) : BottomPanel()
}
```

- [ ] **Step 2: Add imageUrl to AttractionUi and update mapping**

In `MapViewModel.kt`:

```kotlin
data class AttractionUi(
    val id: String,
    val name: String,
    val level: String,
    val regionId: String,
    val description: String?,
    val visitLevel: FootprintLevel?,
    val imageUrl: String? = null
)
```

Update `loadAttractionsForRegion` and `loadAttractionsForSelectedRegion` to include `imageUrl = attraction.imageUrl`.

- [ ] **Step 3: Add BottomPanel state and previewAttraction to MapViewModel**

```kotlin
private val _bottomPanel = MutableStateFlow<BottomPanel>(BottomPanel.None)
val bottomPanel: StateFlow<BottomPanel> = _bottomPanel.asStateFlow()

private val _previewAttraction = MutableStateFlow<AttractionUi?>(null)
val previewAttraction: StateFlow<AttractionUi?> = _previewAttraction.asStateFlow()

fun showAttractionPreview(attractionId: String) {
    val fromList = _attractions.value.find { it.id == attractionId }
        ?: _selectedRegionAttractions.value.find { it.id == attractionId }
    if (fromList != null) {
        _previewAttraction.value = fromList
    } else {
        val attraction = attractionService.getAttraction(attractionId) ?: return
        val visits = getAttractionVisitsCache()
        _previewAttraction.value = AttractionUi(
            id = attraction.id,
            name = attraction.name,
            level = attraction.level.name,
            regionId = attraction.regionId,
            description = attraction.description,
            visitLevel = visits[attraction.id],
            imageUrl = attraction.imageUrl
        )
    }
    _bottomPanel.value = BottomPanel.AttractionPreview(attractionId)
}

fun showRegionPanel(regionId: String) {
    _bottomPanel.value = BottomPanel.Region(regionId)
    _previewAttraction.value = null
}

fun clearBottomPanel() {
    _bottomPanel.value = BottomPanel.None
    _previewAttraction.value = null
}
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/BottomPanel.kt shared/src/commonMain/kotlin/com/mapchina/ui/map/MapViewModel.kt
git commit -m "feat: add BottomPanel state and attraction preview to MapViewModel"
```

---

### Task 10: Create AttractionPreviewCard composable

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mapchina/ui/map/AttractionPreviewCard.kt`

- [ ] **Step 1: Implement AttractionPreviewCard**

```kotlin
package com.mapchina.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun AttractionPreviewCard(
    attraction: AttractionUi,
    onViewDetail: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: image thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MapChinaColors.BorderSubtle)
            ) {
                if (attraction.imageUrl != null) {
                    // Load image with Coil or similar — placeholder shown if loading
                    // AsyncImage(model = attraction.imageUrl, ...)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Attractions,
                            contentDescription = null,
                            tint = MapChinaColors.TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val levelBadge = when (attraction.level) {
                        "A5" -> "5A"
                        "A4" -> "4A"
                        else -> attraction.level
                    }
                    Text(
                        levelBadge,
                        color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.2f) else MapChinaColors.AccentBlue.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        attraction.name,
                        color = MapChinaColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Visit status chip
                val statusText = when (attraction.visitLevel) {
                    FootprintLevel.DEEP -> "深度游"
                    FootprintLevel.SHORT_VISIT -> "短暂停留"
                    FootprintLevel.PASS_BY -> "路过"
                    null -> "未到访"
                }
                val statusColor = when (attraction.visitLevel) {
                    FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
                    FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
                    FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
                    null -> MapChinaColors.TextTertiary
                }
                Text(
                    statusText,
                    color = if (attraction.visitLevel != null) MapChinaColors.FootprintDeep else MapChinaColors.TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (attraction.visitLevel != null) MapChinaColors.Primary.copy(alpha = 0.2f) else MapChinaColors.BorderSubtle)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clickable { onViewDetail() }
                )
            }

            // Close button
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MapChinaColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private val Icons.Default.Attractions: androidx.compose.ui.graphics.vector.ImageVector
    get() = androidx.compose.material.icons.filled.Attractions
```

Note: The `clickable` on the status chip navigates to detail as a shortcut. The main `onViewDetail` callback is wired to the whole card or status chip tap.

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/AttractionPreviewCard.kt
git commit -m "feat: add AttractionPreviewCard composable"
```

---

### Task 11: Integrate preview card into MapScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt`

- [ ] **Step 1: Add bottomPanel and previewAttraction state collection**

```kotlin
val bottomPanel by viewModel.bottomPanel.collectAsState()
val previewAttraction by viewModel.previewAttraction.collectAsState()
```

- [ ] **Step 2: Replace showRegionCard local state with bottomPanel**

Replace `var showRegionCard by remember { mutableStateOf(false) }` with observation of `bottomPanel`:

```kotlin
val showRegionCard = bottomPanel is BottomPanel.Region
```

Update region tap listener:
```kotlin
mapController.setOnRegionTapListener { regionId ->
    viewModel.selectRegion(regionId)
    viewModel.showRegionPanel(regionId)
}
```

Update RegionCard visibility to use `showRegionCard && selectedRegion != null`.

- [ ] **Step 3: Update marker tap listener**

Replace:
```kotlin
mapController.setOnMarkerTapListener { markerId ->
    val cluster = photoClusters.find { it.id == markerId }
    if (cluster != null) {
        photoPreviewCluster = cluster
    } else {
        navController.navigate(AttractionDetailScreen(markerId))
    }
}
```

With:
```kotlin
mapController.setOnMarkerTapListener { markerId ->
    val cluster = photoClusters.find { it.id == markerId }
    if (cluster != null) {
        photoPreviewCluster = cluster
    } else {
        viewModel.showAttractionPreview(markerId)
    }
}
```

- [ ] **Step 4: Add AttractionPreviewCard in the Box layout**

After the RegionCard `AnimatedVisibility` block, add:

```kotlin
// Attraction preview card
val bottomBarOffset = com.mapchina.ui.LocalScaffoldBottomPadding.current
AnimatedVisibility(
    visible = bottomPanel is BottomPanel.AttractionPreview && previewAttraction != null,
    enter = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(tween(200)),
    exit = slideOutVertically(
        targetOffsetY = { it / 2 },
        animationSpec = tween(200)
    ) + fadeOut(tween(150)),
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = bottomBarOffset)
) {
    if (previewAttraction != null) {
        AttractionPreviewCard(
            attraction = previewAttraction!!,
            onViewDetail = {
                navController.navigate(AttractionDetailScreen(previewAttraction!!.id))
            },
            onClose = { viewModel.clearBottomPanel() }
        )
    }
}
```

- [ ] **Step 5: Update RegionCard onClose to use clearBottomPanel**

Change `onClose = { showRegionCard = false; viewModel.clearSelection() }` to:
```kotlin
onClose = {
    viewModel.clearBottomPanel()
    viewModel.clearSelection()
}
```

- [ ] **Step 6: Build and verify**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/mapchina/ui/map/MapScreen.kt
git commit -m "feat: integrate attraction preview card and BottomPanel into MapScreen"
```

---

### Task 12: Full build and smoke test

- [ ] **Step 1: Run full Android build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual smoke test on device/emulator**

Test checklist:
- [ ] Open carving screen → verify cliff texture background
- [ ] Draw with 铁錾 brush → verify bold, dashed strokes with 3-layer groove
- [ ] Draw with 榜书 brush → verify wide, smooth strokes
- [ ] Draw with 风化 brush → verify semi-transparent strokes
- [ ] Save carving → verify it appears in list
- [ ] Open "我的碑刻" → verify waterfall grid layout
- [ ] Tap a carving card → verify it opens in edit mode with existing strokes
- [ ] Edit and save → verify preview updates in list
- [ ] Navigate to city-level map → verify attraction markers visible
- [ ] Tap an attraction marker → verify preview card slides up
- [ ] Tap "View Detail" → verify navigation to attraction detail
- [ ] Tap close → verify card dismisses
- [ ] Tap region overlay → verify RegionCard shows (preview card hides)
