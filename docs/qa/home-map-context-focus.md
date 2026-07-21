# Home Map Context Focus QA

## Test Targets

- Android: clean `Pixel_9_Pro_QA` AVD, package `com.mapchina.android`
- iOS: iPhone 17 Pro simulator, iOS 26.2, bundle `com.mapchina.iosApp`
- Feature branch: `feat/home-map-context-focus`
- Verification date: 2026-07-21

## Automated Gates

| Gate | Result | Notes |
|---|---|---|
| Shared + Android tests | PASS | `:shared:allTests :androidApp:testDebugUnitTest` produced 1,149 test cases across 207 XML suites, with 0 failures, errors, or skips. |
| Android/shared compilation | PASS | `:shared:compileDebugKotlinAndroid` completed from a forced rerun. |
| iOS shared compilation | PASS | `:shared:compileKotlinIosSimulatorArm64` completed from a forced rerun. |
| iOS framework link | PASS | `:shared:linkDebugFrameworkIosSimulatorArm64` completed from a forced rerun. |
| iOS application build | PASS | Xcode Debug simulator build completed with `** BUILD SUCCEEDED **`. |

## Functional Matrix

| Flow | Android | iOS | Evidence and notes |
|---|---|---|---|
| Fresh “晨雾青瓷” national home | PASS | PASS | `.superpowers/sdd/final-review-evidence/android-current-national.png`, `.superpowers/sdd/final-review-evidence/ios-current-national.png` |
| Tap region, animate to safe center, then show card | PASS | PASS | Android: `.superpowers/sdd/final-review-evidence/android-final-immediate-tap.mp4`; iOS current build: `.superpowers/sdd/final-review-evidence/ios-current-focused.png` |
| Keep national/neighbor context while focused | PASS | PASS | Both focused screenshots retain surrounding province boundaries at reduced opacity. |
| Latest rapid tap wins | PASS | SHARED-CODE VERIFIED | Android rapid Sichuan → Hubei interaction ends on the Hubei card: `.superpowers/sdd/final-review-evidence/android-final-rapid-tap.mp4` and `android-final-rapid-tap-sheet.png`. Immediate rapid-tap dispatch is covered in common code used by both platforms; this exact rapid-tap stimulus was not repeated in the iOS simulator. |
| Drill into province while retaining context | PASS | SHARED-CODE VERIFIED | Android current build: `.superpowers/sdd/final-review-evidence/android-current-drilldown.png`. The final-review changes compile on iOS and use the same controller path, but current-build iOS drill-down was not recaptured. |
| City-to-district drill-down and terminal-node guard | PASS | SHARED-CODE VERIFIED | Android current build: Chengdu still opens its 20 district/county boundaries in `.superpowers/sdd/city-district-bug-evidence/after-expandable-city.png`; Dongcheng now shows “已到最下级” without raising a layer error in `.superpowers/sdd/city-district-bug-evidence/after-terminal-city.png`. Packaged-child availability is implemented on both Android and iOS and compiled for both targets; this follow-up was not separately recaptured on iOS. |
| Home current-location button recenters and zooms | PASS | PASS | Android panned/located frames: `.superpowers/sdd/current-location-bug-evidence/android-before-location.png` and `android-after-location.png`. The iOS 26.2 recording `.superpowers/sdd/current-location-bug-evidence/ios-current-location.mp4` shows a real `当前定位` tap followed by the national geometry leaving the viewport as the camera moves to the simulated Beijing coordinate; representative frames are `ios-before-location.png` and `ios-after-location.png`. |
| Return to national map | PASS | SHARED-CODE VERIFIED | Android current build: `.superpowers/sdd/final-review-evidence/android-current-returned.png`. The final-review changes compile on iOS, but current-build iOS return was not recaptured. |
| Aurora FAB motion while collapsed | PASS | SHARED-CODE VERIFIED | Shared implementation; Android frame evidence: `.superpowers/sdd/task-6-evidence/collapsed-motion-t0-ring.png` through `collapsed-motion-t2-ring.png`; the current iOS national screenshot confirms the same ring treatment, not the full motion cycle. |
| Aurora FAB pauses while expanded | PASS | SHARED-CODE VERIFIED | Android expanded evidence: `.superpowers/sdd/task-6-evidence/expanded-paused-v2.png`; the policy is common code, but current-build iOS expanded-pause frames were not recaptured. |
| Reduced motion disables long-running FAB motion | PASS | SHARED-POLICY VERIFIED | Android runtime evidence: `.superpowers/sdd/task-6-evidence/reduced-static-settled-t0.png` and `reduced-static-settled-t1.png`; shared policy tests cover both platform consumers, but the iOS accessibility setting was not toggled during runtime QA. |

## Rapid-Tap Regression

The original Compose double-tap recognizer delayed ordinary taps while it waited for a possible second tap, and its fallback could zoom without completing the region-focus lifecycle. The map now hit-tests every completed tap immediately. A second rapid tap therefore supersedes the first through the normal latest-wins focus path; background double-tap zoom is added only when both taps were unconsumed.

The acceptance recording was captured after the fix in one uninterrupted device-side session. It shows the national frame, the focus transition, Hubei highlighted in the safe viewport, surrounding context preserved, and the Hubei card appearing only after camera completion.

## Stability

- Android crash buffer after the final interaction run: empty (`.superpowers/sdd/final-review-evidence/android-current-crash-buffer.txt`).
- Android `dumpsys activity lastanr`: `<no ANR has occurred since boot>` (`.superpowers/sdd/final-review-evidence/android-current-lastranr.txt`).
- The original `Pixel_9_Pro` AVD had independent SystemUI ANRs and was excluded from acceptance; all credited Android evidence comes from the clean `Pixel_9_Pro_QA` AVD.
- One earlier iOS UI-automation return attempt terminated once with `SIGABRT` and produced no crash diagnostic. It did not reproduce through either return path, and the post-fix Xcode build/install/launch/focus smoke completed normally. If it recurs outside UI automation, capture a simulator crash report before triage.

## City-to-District Follow-up

The reported “区级地图暂时无法展开” path was reproduced on Android through Beijing → Dongcheng. The failure was not a general district-loader outage: Sichuan → Chengdu loaded correctly. The root cause was that every non-district region was presented as drillable even when neither the database nor the packaged assets contained a child layer.

The drill-down affordance now requires either loaded child regions or a packaged child-region file. Expandable cities such as Chengdu retain “查看下级”; terminal nodes such as Dongcheng show “已到最下级” and no longer start a doomed layer request.

## iOS Current-Location Follow-up

The home location button previously made one synchronous read from `CLLocationManager`. On a cold iOS request, `requestLocation()` returns before its delegate supplies coordinates, so that first read was `null` and the button silently stopped.

The home action now uses the shared current-location abstraction, waits through the existing 900 ms location warm-up window, and retries once before giving up. Its camera-intent generation guard remains in place, so a late location callback cannot override navigation that the user started afterward. A sequenced provider regression test covers the exact `null` then coordinate response.

## Exit Result

PASS with the runtime matrix above. The approved visual, focus-motion, context-retention, latest-wins, city-to-district availability, current-location, FAB-motion, and reduced-motion contracts pass shared-code gates plus full Android runtime QA. Current-build iOS runtime QA covers launch, region focus, context retention, and home current-location recentering; rapid-tap, drill-down/return and the terminal-node follow-up, full FAB motion/pause, and the reduced-motion toggle remain shared-code verified rather than separately stimulated.
