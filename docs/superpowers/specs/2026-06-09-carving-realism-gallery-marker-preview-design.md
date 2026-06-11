# Carving Realism, Waterfall Gallery & Attraction Marker Preview

**Date:** 2026-06-09
**Status:** Draft

## Overview

Three enhancements to the MapChina app:
1. Upgrade carving rendering from flat hand-drawn strokes to authentic cliff-face carving (摩崖石刻) aesthetics
2. Replace the text-only carving list with a waterfall/masonry grid of image previews, supporting click-to-edit
3. Add a preview card interstitial when tapping attraction photo markers on city-level maps

---

## 1. Cliff-Face Carving Rendering (摩崖石刻)

### 1.1 Background Texture: Natural Rock Face

Replace `stone_wall` with an **irregular natural cliff surface** texture that includes cracks, pits, and lichen traces.

Overlay layers on top of the base texture:
- **Lichen/water-stain layer** — semi-transparent dark-green / dark-brown irregular patches for age
- **Warm centre glow** — subtle warm-toned radial gradient in the centre, simulating natural directional light
- Vignette at edges (existing) is kept

Overall colour tone shifts to **blue-grey / ochre-yellow** (natural cliff colours), away from the current warm slate.

### 1.2 Stroke Rendering: Bold and Rugged

Cliff-face carvings are characterised by large, thick, powerful strokes — not refined knife cuts.

| Aspect | Current | New |
|--------|---------|-----|
| Default brush width | 0.6x (CHISEL) | 1.5x–2x (iron chisel) |
| Path join | `StrokeJoin.Round` | `StrokeJoin.Miter` (sharp corners) |
| Edge regularity | Smooth | Deterministic random offset ±3–5 px per control point |
| Continuity | Continuous | `dashPathEffect` for iron-chisel — short gaps simulating chisel jumps |
| Pressure variation | Uniform | Start heavy, taper slightly at ends |

**Deterministic randomness:** To prevent visual flickering on recomposition, random offsets must be seeded by a stable property of each stroke. Use `stroke.inputs.first().x.toInt() * 31 + stroke.inputs.first().y.toInt()` as the `Random` seed for each stroke's offset calculations. Offsets are computed once at render time per stroke, not per frame.

**Ink in-progress vs finished:** The `dashPathEffect` only applies to Canvas-rendered finished strokes. In-progress Ink strokes render via `InProgressStrokes` which does not support `PathEffect`. This means strokes appear continuous while drawing, then snap to dashed when finished — this is acceptable as it mirrors the "committing a chisel strike" metaphor.

### 1.3 Carving Groove: 3-Layer Rough Rendering

Unlike refined stele carvings, cliff-face grooves are rough and uneven. Use 3 layers only:

1. **Deep shadow** — offset +4,+4; width +10 px; pure black 0.5 alpha — bold shadow of the groove
2. **Groove colour** — no offset; original stroke width; dark colour — the carved trench itself
3. **Upper-rim highlight** — offset −2,−2; width ×0.2; warm white 0.25 alpha — cliff surface catching light at the top edge

Per-segment random variation:
- Random alpha fluctuation (0.8–1.0) and width fluctuation (±2 px) along each stroke, simulating uneven carving depth
- No fine inner-wall reflections — cliff grooves are too rough for that

### 1.4 Weathering Effects

Weathering is the soul of cliff-face carvings:

- **Edge blur** — render the stroke at slightly larger width with low alpha (0.15) behind the main groove, simulating soft weathered edges
- **Lichen patches** — randomly overlay semi-transparent dark-green blobs (4–8 px) near stroke endpoints and corners
- **Spalling** — randomly reduce alpha of certain stroke segments to 0.3–0.6, simulating stone surface flaking

### 1.5 Colour Palette

| Name | Hex | Description |
|------|-----|-------------|
| 崖壁墨 | #1A1612 | Deepest — groove interior |
| 青石灰 | #4A5568 | Blue-grey cliff common carved colour |
| 朱砂 | #8B2500 | Retained — later inscriptions / annotations |
| 风化石 | #9CA3AF | Weathered whitish-grey remnants |

### 1.6 Brush Types

| Name | Replaces | Behaviour |
|------|----------|-----------|
| 铁錾 (iron chisel) | 刻刀 | Bold, discontinuous dash; simulates iron tool carving |
| 榜书 (monumental calligraphy) | 毛笔 | Wide, full, powerful strokes; simulates large cliff-face characters |
| 风化 (weathered) | 粉笔 | Semi-transparent, blurred edges; simulates aged, worn carvings |

### 1.7 Preview Image Generation

On save, capture the Canvas rendering to a compressed image file. This image serves dual purpose:
- Preview thumbnail in the waterfall gallery list
- Persistent visual record (since Ink strokes cannot be perfectly re-rendered on different screen sizes)

Implementation (expect/actual pattern for cross-platform):
- Define `expect fun captureCarvingPreview(width: Int, height: Int, strokes: List<Stroke>, brushType: CarvingBrushType, brushColor: Color, brushSize: Float): String?` in commonMain
- Android actual: use `android.graphics.Bitmap` + `android.graphics.Canvas` to render the same multi-layer stroke rendering pipeline offscreen, then compress to JPEG (quality 80) and save to app-private storage
- The offscreen rendering must replicate the exact same layer stack (texture, gradient, vignette, 3-layer groove, weathering) as the on-screen Canvas
- Return the saved file path as a `String?`
- Store the file path in `Carving.imagePath`
- Compute `previewAspectRatio = canvasWidth.toFloat() / canvasHeight.toFloat()` at capture time; guard against zero dimensions by defaulting to 1.0f

---

## 2. Carving Waterfall Gallery List

### 2.1 Data Model Changes

`Carving` model additions:
- `previewAspectRatio: Float?` — width/height ratio of the preview image, needed by the staggered grid layout to compute card heights. Stored alongside the image on save.

**Carving ID generation change:** Current ID `"carving_${regionId}_${attractionId ?: "region"}_${userId.hashCode().toUInt()}"` only allows one carving per region/attraction per user. Change to `"carving_${regionId}_${attractionId ?: "region"}_${timestamp}"` to support multiple carvings per region (required for the waterfall gallery to show multiple items).

**SQLDelight schema changes:**
- Add `preview_aspect_ratio REAL` column to `carving` table CREATE TABLE
- Add migration: `ALTER TABLE carving ADD COLUMN preview_aspect_ratio REAL;`
- Add `updateCarving` query: `UPDATE carving SET image_path = ?, stroke_data = ?, preview_aspect_ratio = ? WHERE id = ?;`

**Stroke serialization:** The current `saveCarving` sets `strokeData = ""` (hardcoded). For edit mode to work, strokes must be serializable. Define serialization format:
- JSON array of stroke objects: each stroke = `{ inputs: [{x, y, pressure, timestamp}], brushSize: Float, brushColorArgb: Int, brushType: String }`
- Serialize finished strokes on save; deserialize and reconstruct via `Stroke.Builder` on load
- This enables edit mode to restore previous strokes onto the Canvas

`CarvingRepository` additions:
- `updateCarving(carving: Carving)` — uses the `updateCarving` SQL query to update image_path, stroke_data, preview_aspect_ratio for an existing carving

`CarvingViewModel` additions:
- `loadCarvingById(id: String)` — loads a single carving, deserializes its strokeData into `List<Stroke>`, exposes via `existingStrokes`
- `updateCarving(carving: Carving)` — delegates to repository, then refreshes the list

### 2.2 List Layout

Replace current `LazyColumn` text cards with `LazyVerticalStaggeredGrid`:

```
@OptIn(ExperimentalFoundationApi::class)
LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Fixed(2),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    horizontalItemSpacing = 8.dp,
    verticalItemSpacing = 8.dp
)
```

Note: `LazyVerticalStaggeredGrid` requires `@OptIn(ExperimentalFoundationApi::class)` and the `foundation` dependency.

Each cell contains:
- **Carving preview image** — fills card width, height determined by `previewAspectRatio` (fallback 1:1 if null). Show stone texture placeholder while loading; show error stone texture if image fails to load.
- **Bottom gradient overlay** — `Brush.verticalGradient(transparent → semi-black)` for text readability
- **Text on overlay** — attraction/region name (14 sp, bold, white) + date (11 sp, grey)
- Card shape: `RoundedCornerShape(12.dp)`

Empty state is preserved (brush icon + "还没有碑刻" + "点击右下角 + 开始刻字").

FAB remains at bottom-end for creating new carvings.

### 2.3 Card Interactions

- **Tap** → navigate to `CarvingScreen` in edit mode (pass `carvingId`)
- **Long press** → show delete confirmation `AlertDialog` (reuse existing)

### 2.4 Editing Mode

`CarvingScreen` currently only supports creating new carvings. Add edit mode:

- Add `carvingId: String? = null` parameter to `CarvingScreen` composable
- When `carvingId` is provided, load existing carving via `viewModel.loadCarvingById(carvingId)` and render its deserialized strokes on Canvas
- "Save" button calls `viewModel.updateCarving(...)` instead of `viewModel.saveCarving(...)`, generating a new preview image
- After save, re-capture preview image with updated aspect ratio
- `popBackStack()` returns to list, which observes `carvingList` StateFlow and auto-refreshes

**Navigation route update:** Add `carvingId` as an optional query parameter to the `CarvingScreen` route definition in `AppNavHost`, following the same pattern as `AttractionDetailScreen(attractionId)`.

### 2.5 Navigation Changes

`CarvingListScreen` composable gains:
- `onEditClick: (String) -> Unit` callback — navigates to `CarvingScreen(carvingId=...)` for editing

`AppNavHost` `CarvingListScreen` composable wires `onEditClick` to navigate to `CarvingScreen` with the carving's region/attraction context plus `carvingId`.

---

## 3. Attraction Marker Preview Card

### 3.1 Interaction Flow

1. User taps an attraction marker on city-level map
2. If a RegionCard is showing, hide it
3. Show attraction preview card (bottom slide-up)
4. Preview card displays: image thumbnail, name, 5A/4A badge, visit status, region name
5. Two actions on the card:
   - **View Detail** → `navController.navigate(AttractionDetailScreen(attractionId))`
   - **Close** (X button or tap same marker again) → dismiss preview card
6. Preview card and RegionCard are mutually exclusive

### 3.2 UI Design

Reuse the same animation pattern as RegionCard:

```
AnimatedVisibility(
    visible = attractionPreview != null,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(150)),
    modifier = Modifier.align(Alignment.BottomCenter)
)
```

Card layout (approx 120 dp height):
- Left: attraction image thumbnail (80×80 dp, RoundedCornerShape 10 dp)
- Right column:
  - Row: 5A/4A badge + attraction name (16 sp, bold)
  - Visit status chip (same style as AttractionSheetCard)
  - Region name in grey (12 sp)
- Top-right: close X button

Card container: `Surface(color = MapChinaColors.SurfaceElevated, shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp)`

### 3.3 State Management

Use a sealed interface to guarantee mutual exclusivity at the type level:

```kotlin
sealed class BottomPanel {
    data object None : BottomPanel()
    data class Region(val regionId: String) : BottomPanel()
    data class AttractionPreview(val attractionId: String) : BottomPanel()
}
```

`MapViewModel` additions:
- `_bottomPanel = MutableStateFlow<BottomPanel>(BottomPanel.None)`
- `val bottomPanel: StateFlow<BottomPanel>`
- `fun showAttractionPreview(attractionId: String)` — looks up from `_attractions` or `_selectedRegionAttractions`; falls back to `attractionService.getAttraction(attractionId)` if not found. Sets `_bottomPanel` to `AttractionPreview`.
- `val previewAttraction: StateFlow<AttractionUi?>` — derived from `bottomPanel` + attraction lookup
- `fun showRegionCard(regionId: String)` — sets `_bottomPanel` to `Region`
- `fun clearBottomPanel()` — sets to `None`

This replaces both `attractionPreview` local state and the `showRegionCard` local state in `MapScreen`, eliminating the risk of desynchronization.

### 3.4 Marker Tap Logic Change

Current `setOnMarkerTapListener`:
```
if cluster → photoPreviewCluster
else → navigate(AttractionDetailScreen)
```

New logic:
```
if cluster → photoPreviewCluster
else → viewModel.showAttractionPreview(markerId)
```

The navigation to detail now happens only from the preview card's "View Detail" button.

### 3.5 Data Model Change

`AttractionUi` gains:
- `imageUrl: String?` — attraction image URL for the preview card thumbnail

Mapping change in `loadAttractionsForRegion` and `loadAttractionsForSelectedRegion`:
```kotlin
AttractionUi(
    id = attraction.id,
    name = attraction.name,
    level = attraction.level.name,
    regionId = attraction.regionId,
    description = attraction.description,
    visitLevel = visits[attraction.id],
    imageUrl = attraction.imageUrl  // NEW
)
```

`showAttractionPreview(attractionId)` looks up from current `_attractions` or `_selectedRegionAttractions` lists first; if not found, falls back to `attractionService.getAttraction(attractionId)` to construct an `AttractionUi`.

### 3.6 Mutual Exclusivity with RegionCard

Handled by the `BottomPanel` sealed interface (Section 3.3). Setting `AttractionPreview` automatically implies `None` for Region, and vice versa. No separate synchronization logic needed.

---

## Implementation Order

1. **Carving realism** — rendering upgrades (bottom texture, stroke style, 3-layer groove, weathering, colours, brushes)
2. **Preview image generation** — Canvas-to-Bitmap capture on save
3. **Waterfall gallery list** — staggered grid layout, edit mode, card interactions
4. **Attraction marker preview** — AttractionUi imageUrl, preview card UI, marker tap logic, mutual exclusivity
