# MapChina Cross-Platform UI Regression

## Test Targets

- Android: Pixel 9 Pro emulator, package `com.mapchina.android`
- iOS: iPhone 17 Pro simulator, iOS 26.2, bundle `com.mapchina.iosApp`
- Shared baseline: `./gradlew :shared:allTests :androidApp:testDebugUnitTest`
- Status values: `PASS`, `FAIL`, `BLOCKED`, `PENDING`

## App Shell

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Splash to Footprint home | PASS | PASS | `.superpowers/sdd/mapchina-android-cold-start-latest.png`, `.superpowers/sdd/mapchina-ios-home-atlas-zero-fixed.png` | iOS CoreLocation delegate crash fixed; both platforms reach a populated map. |
| Footprint / Discover / Shanhe / Profile tabs | PASS | PASS | Android ADB and iOS Simulator interactive runs | Selected state updates and each first screen is reachable. |
| Safe-area and system-back behavior | PASS | FAIL | `.superpowers/sdd/mapchina-android-badge-back-final.png` | Shared badge back action is fixed. iOS composed screens still do not expose a native edge-back gesture. |

## Footprint Map

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Zero-state combined action and exact three-entry menu | PASS | PASS | `.superpowers/sdd/mapchina-android-first-menu-latest.png`, `.superpowers/sdd/mapchina-ios-first-menu-after.png` | One combined command; grouped menu has three exact entries. |
| Search autofocus and return | PASS | PENDING | `.superpowers/sdd/mapchina-first-activation-search-final.xml` | Android evidence comes from the preceding activation iteration. |
| Simulated current location | PASS | PASS | `.superpowers/sdd/mapchina-android-clean-location-after.png`, `.superpowers/sdd/mapchina-ios-first-success-after.png` | Both use Shanghai coordinates and resolve to Huangpu District. Android rejects stale last-known data and warms up async providers. |
| Map region selection and direct visit depth | PASS | PASS | `.superpowers/sdd/mapchina-android-clean-location-after.png`, iOS accessibility trace | Direct `途经 / 小驻 / 深游`, no intermediate command. |
| First success, regular dock, grouped menu | PASS | PASS | `.superpowers/sdd/mapchina-android-first-success-final.png`, `.superpowers/sdd/mapchina-android-regular-menu-final.png`, `.superpowers/sdd/mapchina-ios-first-success-after.png` | Dock has direct location plus grouped tools; menu has no duplicate location. |
| Drill-down and return to national | PENDING | PENDING | | Verify stable national framing after return. |

## Discover

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Daily recommendation hero | PASS | PASS | `.superpowers/sdd/mapchina-ios-discover-handoff-contact-sheet.png` | Gradient mask and real attraction image render on both platforms. |
| Recommendation list imagery | PASS | PASS | `.superpowers/sdd/mapchina-ios-discover-handoff-contact-sheet.png` | Every visible item uses its header image as background. |
| List image to detail hero handoff | PASS | PASS | `.superpowers/sdd/mapchina-discover-detail-jitter-frames-after.png`, `.superpowers/sdd/mapchina-ios-discover-transition-frames.png` | iOS frame sheet confirms image expansion and stable detail body. |
| Detail scroll and return path | PASS | PASS | `.superpowers/sdd/mapchina-ios-discover-handoff.mov` | Explicit back works; native iOS edge-back remains tracked in App Shell. |

## Carving And Records

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Region carving entry | PENDING | BLOCKED | iOS Simulator interactive run | iOS routes to `雕刻功能暂不支持此平台`. |
| Create carving background | PENDING | BLOCKED | `shared/src/iosMain/.../CarvingScreen.kt` | The optimized drawing canvas is currently Android-only. |
| Large calligraphic title effect | PENDING | BLOCKED | `shared/src/iosMain/.../CarvingScreen.kt` | The optimized drawing canvas is currently Android-only. |
| Journal and atlas entry smoke | PENDING | PENDING | | Reachable screen and back path only. |

## Growth And Account

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Shanhe first screen and reachable achievements | PASS | PASS | `.superpowers/sdd/mapchina-android-badge-back-final.png`, `.superpowers/sdd/maestro-ios-badge-final/screenshots/mapchina-ios-badge-back-final.png`, `.superpowers/sdd/maestro-ios-badge-final/screenshots/mapchina-ios-badge-return-final.png` | Latest iOS build passes the explicit badge-wall back flow through black-box accessibility automation. |
| Profile first screen and sync/account status | PASS | PASS | Android and iOS interactive traces | Logged-out/local-save state and map preferences render correctly. |

## Responsive And Appearance

| Flow | Android | iOS | Evidence | Notes |
|---|---|---|---|---|
| Large text map title and dock | PASS | PASS | `.superpowers/sdd/mapchina-home-atlas-large-after.png`, `.superpowers/sdd/mapchina-ios-home-atlas-large-after.png` | Android font scale 1.5; iOS XXXL content size. |
| Large text grouped menu | PASS | PASS | `.superpowers/sdd/mapchina-home-atlas-large-menu-after.png`, `.superpowers/sdd/mapchina-ios-home-atlas-large-menu-after.png` | Full `照片回溯 · 实验` remains visible. |
| Dark appearance contrast | PENDING | PASS | `.superpowers/sdd/mapchina-ios-home-atlas-dark-after.png` | iOS intentionally retains the controlled light map palette; system bars and content remain legible. |

## Known Platform Gaps

- iOS carving editor/list are placeholders; creation, realistic field background and calligraphic drawing cannot be parity-tested yet.
- iOS photo picker and device-photo provider currently report unavailable even though profile toggles are visible.
- Compose navigation supports explicit back actions, but native iOS edge-back is not wired into the shared navigation stack.

## Exit Conditions

- All rows touched by the current UI iteration are `PASS` on both platforms.
- No app `FATAL EXCEPTION`, iOS process fault, or ANR appears during the tested flows.
- Android font scale is restored to `1.0`; iOS content size is restored to `large` and appearance to `light`.
- Screenshot evidence is captured only after animations and loading have settled.
