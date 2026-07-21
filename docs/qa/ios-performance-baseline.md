# iOS Performance Baseline

## Scope

- Target: iPhone 17 Pro simulator, iOS 26.2.
- Flow: launch Footprint home, settle, pan and zoom the national map, then exercise map tools.
- Capture: Xcode Time Profiler plus the Potential Hangs table.
- Trials: one warm-up capture followed by four measured captures.
- App instrumentation: `MapScreen` and `ChinaMapView` recomposition probes.

## Baseline Configuration Finding

The Xcode Release configuration was linking `debugFramework/shared.framework` and always running `linkDebugFramework...`. The baseline therefore represents a Release Swift shell containing a Debug Kotlin framework. It is retained because that is the configuration users were actually running before the fix.

Kotlin exposes separate Debug and Release framework link tasks and output directories; the Xcode project must select the matching artifact for each configuration. See the [Kotlin framework documentation](https://kotlinlang.org/docs/apple-framework.html).

## Results

| Metric | Four-run median | Range |
|---|---:|---:|
| Main-thread sampled active time | 12,551.5 ms | 12,310-12,758 ms |
| Douglas-Peucker simplification | 3,962 ms (31.76%) | 3,912-4,014 ms |
| China map draw stack | 4,408.5 ms (35.24%) | 4,332-4,449 ms |
| Potential hangs over 250 ms | 0 | 0 in all runs |

The recomposition probes emitted exactly `MapScreen|1` and `ChinaMapView|1` in every measured run, including runs with continuous map gestures. The observed cost was therefore not a root-composition loop. Time Profiler instead placed roughly one third of main-thread samples in repeated Douglas-Peucker geometry simplification.

## Evidence

- Aggregate: `.superpowers/sdd/ios-performance/before/release-debug-kmp/home/aggregate.json`
- Representative recomposition log: `.superpowers/sdd/ios-performance/before/release-debug-kmp/home/trial-2/recomposition.log`
- Per-run Time Profiler exports: `.superpowers/sdd/ios-performance/before/release-debug-kmp/home/trial-{2..5}/time-profile.xml`
- Potential hangs: `.superpowers/sdd/ios-performance/before/release-debug-kmp/home/trial-{2..5}/potential-hangs.xml`

## Measurement Limits

Xcode 26 reports `Hitches is not supported on this platform` for Animation Hitches on the iOS simulator. A paired physical iPhone was visible, but a valid Xcode account and provisioning profile were not available for a device capture. No P95 frame duration is reported or inferred. Potential Hangs over 250 ms and sampled main-thread weights are the available repeatable signals for this run.
