# South China Sea Nine-Dash Line Design

## Goal

Replace the current nested dashed polylines on the Footprint home map with nine clean, independent cartographic strokes. The result should read as a deliberate map symbol rather than a broken decorative outline on Android and iOS.

## Current Problem

`SouthChinaSea.DASH_SEGMENTS` already models nine geographic segments, but every segment is rendered with another `dashPathEffect`. Each intended stroke is therefore fragmented into several tiny marks. Three-point `lineTo` paths also expose hard corners and inconsistent visual rhythm.

## Rendering Design

- Keep exactly nine geographic segments.
- Render every segment as one continuous stroke with no nested dash path effect.
- Convert each three-point segment into a smooth quadratic curve that passes through the geographic control points using midpoint-based interpolation.
- Use round caps and round joins so each stroke has a controlled cartographic finish.
- Preserve the current projection pipeline; curves are built from projected points so pan and zoom remain correct.
- Keep the symbol subordinate to province boundaries and labels:
  - national view: `1.05dp`, label color at `0.50` alpha;
  - closer view: `0.85dp`, label color at `0.42` alpha.
- Do not add glow, shadow, labels, islands or an inset frame.

## Geographic Rhythm

The existing coordinates remain the source of truth for placement. Only obvious spacing defects may be adjusted while inspecting device screenshots:

- eastern segments descend naturally from the waters east and south of Taiwan;
- southern segments form a broad, shallow turn;
- western segments rise toward the waters southeast of Hainan;
- adjacent strokes remain visibly separate at national-map scale.

## Code Boundaries

- `SouthChinaSea.kt` owns segment data, curve construction and drawing parameters.
- `ChinaMapView.kt` continues to provide projection and theme-derived color.
- Curve construction is exposed as an internal pure helper so it can be tested without a canvas.
- No changes to map gesture, hit testing, region geometry or persisted data.

## Verification

- Unit tests assert exactly nine segments, one continuous path per segment and stable projected endpoints.
- Existing shared and Android test suites pass.
- Android: `installDebug`, monkey launch, tap/swipe interaction and before/after screenshots on the Pixel 9 Pro emulator.
- iOS: Debug simulator build and screenshot on iPhone 17 Pro, confirming the same nine-stroke geometry and visual weight.
- Device review checks that no segment is internally fragmented, no hard elbow remains and the line does not compete with Hainan, Taiwan or province labels.

## Non-Goals

- Replacing map boundary data.
- Adding a South China Sea inset map.
- Changing political labeling or region-selection behavior.
- Introducing bitmap or SVG overlays that lose fidelity during zoom.
