# Home Map Context Focus QA

## Test Targets

- Android: clean `Pixel_9_Pro_QA` AVD, package `com.mapchina.android`
- iOS: iPhone 17 Pro simulator, iOS 26.2, bundle `com.mapchina.iosApp`
- Feature branch: `feat/home-map-context-focus`
- Verification date: 2026-07-20

## Automated Gates

| Gate | Result | Notes |
|---|---|---|
| Shared + Android tests | PASS | `:shared:allTests :androidApp:testDebugUnitTest` produced 1,123 test cases across 203 XML suites, with 0 failures, errors, or skips. |
| Android/shared compilation | PASS | `:shared:compileDebugKotlinAndroid` completed from a forced rerun. |
| iOS shared compilation | PASS | `:shared:compileKotlinIosSimulatorArm64` completed from a forced rerun. |
| iOS framework link | PASS | `:shared:linkDebugFrameworkIosSimulatorArm64` completed from a forced rerun. |
| iOS application build | PASS | Xcode Debug simulator build completed with `** BUILD SUCCEEDED **`. |

## Functional Matrix

| Flow | Android | iOS | Evidence and notes |
|---|---|---|---|
| Fresh “晨雾青瓷” national home | PASS | PASS | `/tmp/mapchina-home-context-focus-qa/android-clean-national.png`, `/tmp/mapchina-home-context-focus-qa/ios-after-fix-launch.png` |
| Tap region, animate to safe center, then show card | PASS | PASS | Android: `/tmp/mapchina-home-context-focus-qa/android-clean-focused.png`; iOS current build: `/tmp/mapchina-home-context-focus-qa/ios-after-fix-focused.png` |
| Keep national/neighbor context while focused | PASS | PASS | Both focused screenshots retain surrounding province boundaries at reduced opacity. |
| Latest rapid tap wins | PASS | PASS (shared path) | Android rapid Sichuan → Hubei interaction ends on the Hubei card: `/tmp/mapchina-home-context-focus-qa/android-clean-rapid-after-fix-v5.mp4` and `android-clean-rapid-after-fix-v5-sheet.png`. The collapsed-double-tap dispatch path is covered by `MapRegionFocusTapHandlersTest` in common code used by both platforms. |
| Drill into province while retaining context | PASS | PASS | `/tmp/mapchina-home-context-focus-qa/android-clean-drilldown.png`; iOS drill-down is present in `/tmp/mapchina-home-context-focus-qa/ios-interaction.mp4`. |
| Return to national map | PASS | PASS | Android: `/tmp/mapchina-home-context-focus-qa/android-clean-returned.png`; both the iOS top control and tool-menu return path succeeded on repeat. |
| Aurora FAB motion while collapsed | PASS | PASS | Shared implementation; Android frame evidence: `.superpowers/sdd/task-6-evidence/collapsed-motion-t0-ring.png` through `collapsed-motion-t2-ring.png`; iOS national screenshots show the same ring treatment. |
| Aurora FAB pauses while expanded | PASS | PASS | Android expanded evidence: `.superpowers/sdd/task-6-evidence/expanded-paused-v2.png`; iOS frames `/tmp/mapchina-home-context-focus-qa/ios-fab-expanded-t0.png` and `ios-fab-expanded-t1.png` are byte-identical after two seconds. |
| Reduced motion disables long-running FAB motion | PASS | PASS (shared policy) | Android runtime evidence: `.superpowers/sdd/task-6-evidence/reduced-static-settled-t0.png` and `reduced-static-settled-t1.png`; shared policy tests cover both platform consumers. |

## Rapid-Tap Regression

Android Compose collapses sufficiently close taps into `onDoubleTap`. The previous screen wiring cleared the double-tap listener, which let `MapController` zoom without completing the region-focus lifecycle or opening the card. The screen now routes both single- and collapsed-double-tap region events through the same latest-wins focus handler.

The acceptance recording was captured after the fix in one uninterrupted device-side session. It shows the national frame, the focus transition, Hubei highlighted in the safe viewport, surrounding context preserved, and the Hubei card appearing only after camera completion.

## Stability

- Android crash buffer after the final interaction run: empty (`android-clean-after-fix-crash-buffer.txt`).
- Android `dumpsys activity lastanr`: `<no ANR has occurred since boot>` (`android-clean-after-fix-lastranr.txt`).
- The original `Pixel_9_Pro` AVD had independent SystemUI ANRs and was excluded from acceptance; all credited Android evidence comes from the clean `Pixel_9_Pro_QA` AVD.
- One earlier iOS UI-automation return attempt terminated once with `SIGABRT` and produced no crash diagnostic. It did not reproduce through either return path, and the post-fix Xcode build/install/launch/focus smoke completed normally. If it recurs outside UI automation, capture a simulator crash report before triage.

## Exit Result

PASS. The accepted implementation matches the approved visual, focus-motion, context-retention, latest-wins, FAB-motion, and reduced-motion contracts on the verified shared Android/iOS code path.
