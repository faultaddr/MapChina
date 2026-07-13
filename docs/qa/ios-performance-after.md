# iOS Performance After Optimization

## Changes Under Test

- `GeoPathCache` now separates projection changes from simplification-bucket changes.
- Geometry is simplified only when region geometry identity changes or zoom crosses a simplification threshold.
- Continuous scale changes rebuild projected paths from cached simplified rings without rerunning Douglas-Peucker.
- Xcode Debug and Release configurations now link their matching Kotlin frameworks for simulator and device targets.
- Simulator builds exclude x86_64 because this project produces an `iosSimulatorArm64` Kotlin framework.
- The Gradle daemon heap was raised to 6 GiB because the first real Release Kotlin/Native LTO link exhausted the previous 2 GiB heap.

## Home Map Results

The same warm-up plus four measured-run protocol was repeated with the true Release Kotlin framework.

| Metric | Before median | After median | Change |
|---|---:|---:|---:|
| Main-thread sampled active time | 12,551.5 ms | 7,401.5 ms | -41.0% |
| Douglas-Peucker simplification | 3,962 ms | 0 ms sampled | -100% |
| China map draw stack | 4,408.5 ms | 257 ms | -94.2% |
| Potential hangs over 250 ms | 0 | 0 | unchanged |

Recomposition remained `MapScreen|1` and `ChinaMapView|1` in every measured run. This supports the baseline diagnosis: geometry invalidation and the misconfigured Release artifact were the material problems, not excessive root recomposition.

The app executable changed from 87 MiB to 56 MiB after linking the optimized framework, a 35.6% reduction. The complete simulator app bundle changed from 134 MiB to 103 MiB.

## Regression Flows

| Flow | Main-thread sampled active time | Potential hangs | Result |
|---|---:|---:|---|
| Discover recommendation to detail, scroll and return | 8,108 ms | 0 | PASS, no delayed hero/body layout shift |
| Carving list, edit, draw and save | 7,145 ms | 0 | PASS, drawing 0.826%, decode 0.476% |

One home capture reached the UI-test end but Instruments stalled while finalizing its trace. That incomplete trace was excluded, preserved as `trial-4-capture-failed`, and the same numbered trial was rerun successfully. It is a capture failure, not an application hang.

## Evidence

- Aggregate: `.superpowers/sdd/ios-performance/after/release/home/aggregate.json`
- Measured runs: `.superpowers/sdd/ios-performance/after/release/home/trial-{2..5}`
- Discover verification: `.superpowers/sdd/ios-performance/after/release/discover/trial-1-verification`
- Carving verification: `.superpowers/sdd/ios-performance/after/release/carving/trial-1-verification`
- Trace analyzer: `scripts/qa/analyze_xctrace.rb`

## Measurement Limits

Animation Hitches remains unavailable on this simulator, and physical-device profiling remains blocked by signing. The report therefore does not claim a frame-time P95. Release simulator UI tests, screenshots, Time Profiler samples and Potential Hangs are the verified evidence set.
