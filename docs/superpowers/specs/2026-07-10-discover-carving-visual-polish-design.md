# Discover Recommendation And Monumental Carving Visual Polish

Date: 2026-07-10

## Goal

This pass improves two high-visibility moments:

1. Discover page "今日推荐" should feel like a destination card, using the recommended attraction image as the background with a strong gradient mask for readable foreground content.
2. New stele carving should feel closer to large cliff inscriptions, especially the "榜书" brush: big, weighty, recessed, chipped, and visibly embedded in stone.

## Discover Spotlight

The top recommendation card will use the first recommendation's `imageUrl` when available. The image fills the card with `ContentScale.Crop`; over it sits a layered mask:

- dark bottom-to-top vertical gradient for title and reason readability
- subtle top/side tint using MapChina brand colors
- fallback scenic gradient when no image is available

The card keeps its current information architecture: badge, candidate count, title, reason, metrics, and CTA. It should stop feeling like a generic dashboard tile and start feeling like a specific place to go next.

## Carving Studio

The default brush becomes `MONUMENTAL` ("榜书"), because the user's first carving should immediately read as a large cliff inscription. Brush sizes are adjusted upward, with size labels still compact enough for the existing toolbar.

The rendering pipeline will distinguish monumental strokes from the other brushes:

- broader recessed shadow below/right
- warmer carved stone floor
- stronger upper-left rim highlight
- rough chipped edge pass around the stroke
- extra stone dust and debris near large strokes

The UI should avoid adding a separate text input or template system in this pass. The user is still hand-carving, but the visual effect of the hand stroke should feel more like a large inscription than a marker line.

## Testing

Add focused tests around the small pure helpers needed for the behavior:

- Discover spotlight decides whether an image background should be used.
- Default carving brush is `MONUMENTAL`.
- Monumental brush sizing is larger than ordinary chisel sizing.

Full Ink canvas behavior remains device-verified because Robolectric cannot load the native Ink library reliably.

## Device Verification

Because this changes visible UI, final delivery requires:

- focused unit tests
- `:shared:allTests :androidApp:testDebugUnitTest`
- `./gradlew installDebug`
- emulator launch with `adb shell monkey`
- before and after `adb shell screencap` evidence for Discover and Carving

