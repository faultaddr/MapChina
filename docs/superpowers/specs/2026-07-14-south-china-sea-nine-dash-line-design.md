# South China Sea Ten-Dash Line Design

## Goal

Replace the current nested dashed polylines on the Footprint home map with ten clean, independent cartographic strokes: the nine South China Sea segments plus the segment east of Taiwan. The result should read as a deliberate map symbol rather than a broken decorative outline on Android and iOS.

## Current Problem

`SouthChinaSea.DASH_SEGMENTS` originally modeled nine approximate geographic segments, but every segment was rendered with another `dashPathEffect`. Each intended stroke was therefore fragmented into several tiny marks. The approximate coordinates also counted the Taiwan-east segment inside those nine and distorted the South China Sea placement.

## Rendering Design

- Keep exactly ten geographic segments, including the segment east of Taiwan.
- Render every segment as one continuous stroke with no nested dash path effect.
- Convert each segment into a Catmull-Rom-derived cubic curve that passes through its geographic control points.
- Use round caps and round joins so each stroke has a controlled cartographic finish.
- Preserve the current projection pipeline; curves are built from projected points so pan and zoom remain correct.
- Keep the symbol subordinate to province boundaries and labels:
  - national view: `1.05dp`, label color at `0.50` alpha;
  - closer view: `0.85dp`, label color at `0.42` alpha.
- Do not add glow, shadow, labels, islands or an inset frame.

## Geographic Rhythm

Use the ten-dash reference coordinates sourced from the Ministry of Civil Affairs map as the source of truth for placement:

- the Taiwan-east segment remains distinct from the nine South China Sea segments;
- eastern segments descend naturally through the waters south of Taiwan and west of the Philippines;
- southern segments form a broad, shallow turn;
- western segments rise toward the waters southeast of Hainan;
- adjacent strokes remain visibly separate at national-map scale.

## Code Boundaries

- `SouthChinaSea.kt` owns segment data, curve construction and drawing parameters.
- `ChinaMapView.kt` continues to provide projection and theme-derived color.
- Curve construction is exposed as an internal pure helper so it can be tested without a canvas.
- No changes to map gesture, hit testing, region geometry or persisted data.

## Verification

- Unit tests assert exactly ten segments, reference geographic extents, one continuous path per segment and stable projected endpoints.
- Existing shared and Android test suites pass.
- Android: `installDebug`, monkey launch, tap/swipe interaction and before/after screenshots on the Pixel 9 Pro emulator.
- iOS: Debug simulator build and screenshot on iPhone 17 Pro, confirming the same ten-stroke geometry and visual weight.
- Device review checks that no segment is internally fragmented, no hard elbow remains and the line does not compete with Hainan, Taiwan or province labels.

## Non-Goals

- Replacing map boundary data.
- Adding a South China Sea inset map.
- Changing political labeling or region-selection behavior.
- Introducing bitmap or SVG overlays that lose fidelity during zoom.
