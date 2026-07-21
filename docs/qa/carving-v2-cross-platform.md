# Carving V2 Cross-Platform Verification

## Contract

Carving V2 stores normalized stroke coordinates, the source canvas aspect ratio and a document version. Android and iOS render the same document into their current canvas and preserve the normalized data when appending strokes.

## Final Round Trip

1. Android saved a V2 document with seven strokes.
2. iOS Release loaded all seven Android strokes, appended an eighth stroke and saved it.
3. Android Debug loaded all eight iOS strokes, appended a ninth stroke and saved it.
4. The final Android database row reports `version=2`, `strokes=9`, `canvasAspectRatio=0.5498282`, `previewAspectRatio=0.5498282` and a 43,524-byte payload.

## Device Evidence

- Android final install: `./gradlew installDebug`, Pixel 9 Pro Android 16 emulator.
- Android launch and interaction: `adb shell monkey`, `adb shell input tap`, `adb shell input swipe` and `adb shell screencap`.
- iOS final app: Xcode Release simulator build, iPhone 17 Pro iOS 26.2.
- iOS interaction: XCUITest display, append and save flow.

Key artifacts:

- iOS reading Android seven strokes: `.superpowers/sdd/carving-v2/final-ios-release-roundtrip-screenshots/D6B155*.png`
- iOS saving eight strokes: `.superpowers/sdd/carving-v2/cross-data/ios-final-release-after-eight-strokes.db`
- Android reading iOS eight strokes: `.superpowers/sdd/carving-v2/android-final-ios-eight-editor.png`
- Android saving nine strokes: `.superpowers/sdd/carving-v2/android-final-nine-saved-gallery.png`
- Final Android database: `.superpowers/sdd/carving-v2/cross-data/android-final-after-nine-strokes.db`

## Compatibility Result

- V2 Android to iOS display/edit/save: PASS.
- V2 iOS to Android display/edit/save: PASS.
- Existing V1 Android documents remain decoded through the compatibility path and are not rewritten until edited.
