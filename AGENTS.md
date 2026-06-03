# Project Rules

## Iron Law: Verify with Device Before Delivery

After ANY code change that affects UI or runtime behavior, you MUST:

1. Build and install the app: `./gradlew installDebug`
2. Launch the app on device/emulator
3. Use `adb shell monkey`, `adb shell input tap/swipe`, and `adb shell screencap` to reproduce the scenario and confirm the fix works
4. Take a screenshot before AND after the fix to prove the change had the intended effect
5. NEVER report a UI/runtime fix as "done" without device verification

This applies to: visual changes, gesture handling, navigation, animations, camera position, map overlays, marker behavior — anything a user can see or interact with.
