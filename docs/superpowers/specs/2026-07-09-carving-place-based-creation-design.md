# Place-Based Carving Creation Design

**Date:** 2026-07-09
**Status:** Approved

## Goal

Turn new carving creation from a generic drawing canvas into a place-based "摩崖留刻" flow. A carving should feel like it belongs to a real visited city or attraction, not like text drawn on a random background.

## Product Direction

The chosen direction is **A: real scenic/city cliff-face carving**.

Carving remains part of the 山河 growth system. It should reinforce a user's footprint memory and future attraction recommendation loop:

- From attraction detail: "留碑刻" opens the carving studio with that attraction context.
- From region map/list: carving belongs to that region.
- From "我的碑刻": tapping `+` must not open an empty-context canvas. It first asks the user to choose a place.

## User Flow

### 1. Choose Place

When the user creates from "我的碑刻", show a lightweight place picker before the canvas.

Initial V1 options:

- 最近点亮: use regions already available in the app navigation context when present.
- 推荐地点: provide a few built-in scenic/city-style presets when no context exists.
- Manual fallback: allow "中国山河" only as a fallback label, never as a blank string.

The picker produces:

- `regionId: String`
- `regionName: String`
- optional `attractionId: String?`
- optional `attractionName: String?`

### 2. Carving Studio

The studio becomes a field site instead of a plain editor:

- Header copy: `摩崖留刻`
- Location line: attraction name when available, otherwise region name.
- Secondary line: short atmospheric copy, e.g. `在这面山石上刻下今日足迹`.
- Background: keep the existing `cliff_face` asset, but strengthen realism through layered light, vignette, moss/crack overlays, and a central carving-safe zone.
- Tools: keep the three existing carving brushes and cliff palette, but present them as carving choices rather than drawing-app controls.

### 3. Save

Saving empty carvings is blocked.

- If there are no new strokes, show an inline hint: `先刻下一笔，再落成碑刻`.
- Saving a valid carving uses the existing `CarvingViewModel.saveCarving` path.
- On save completion, return to the previous screen.

## UI Requirements

- No blank title such as `题刻 · `.
- No blank `regionId`/`regionName` when launching a new carving.
- The canvas must visually imply a real cliff wall, not a flat beige panel.
- Top and bottom controls must not fight the canvas; the stone surface should remain the main object.
- Existing edit mode must keep working.

## Testing

Automated tests should verify:

- Creating from all-carvings context shows a place picker instead of launching a blank carving.
- The place picker can launch a carving with a non-empty region name.
- The carving studio displays `摩崖留刻`, the selected place name, and the empty-save hint after trying to save without strokes.

Device verification is required after implementation:

- Build and install with `./gradlew installDebug`.
- Launch on emulator/device.
- Navigate to 山河 → 碑刻 → `+`.
- Capture before and after screenshots.
- Tap through the picker into the carving studio.
- Try saving empty carving and confirm the hint appears.
