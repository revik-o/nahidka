# Social battery UI

`SocialBatteryScreen` shows the user's social battery as a large battery surrounded by animated particles. Tap or drag inside the battery to set the level; screen readers adjust it like a slider. The fill, glow, particles, and caption follow the charge: low (0–29 %), medium (30–69 %), and high (70–100 %). An unset battery shows `—` and asks for a level. Strings are available in English, Ukrainian, and Russian.

The layout measures its own width with the shared 600 dp breakpoint (`LayoutWidth`) and sizes the battery from the available width and height, so phones in portrait and landscape, tablets, desktop windows, and browsers all fit without scrolling.

`SocialBatteryScreenGraph` uses the same `SocialBatteryBindings` as the core `SocialBatterySessionGraph`. The app's `ApplicationSessionGraph` retains the ViewModel for the Social battery entry and the dashboard's read-only `SocialBatterySummaryCard`. The level stays in memory for the app session.

## Verification

```sh
./gradlew :shared:core:feature:social-battery:jvmTest --rerun :shared:ui:common:jvmTest --rerun :shared:ui:social-battery:jvmTest --rerun :shared:ui:tasks:jvmTest --rerun :shared:jvmTest --rerun
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
```

`SocialBatteryLayoutTest` renders desktop light and dark screens and Ukrainian phone screens in portrait and landscape. PNG previews are written to `/tmp/nahidka-social-battery-previews`.

Verified on 2026-10-07: 41 tests passed on three consecutive forced runs, including 14 social battery tests and 27 shared UI, Tasks, and host regressions. Mouse click and drag, touch tap and drag, accessibility adjustment, unset versus empty state, charge thresholds, deduplicated revisions, and navigation retention are covered. All charge states were rendered in both themes; Ukrainian portrait and landscape phone renders were inspected. The drag and accessibility tests both failed as expected against a temporary source copy with their handlers disabled.

Android APK assembly, desktop compilation, and JS and Wasm app compilation passed. Core and shared UI test sources also compiled for JS and Wasm; the screen's interaction and rendering tests run on JVM. Verification used separate temporary build outputs to avoid concurrent Gradle runs writing the same test results. iOS builds and physical touchscreens were not verified on this Linux host.

The level remains in memory for the app session. System reduce motion and arrow-key controls are not supported. Partner battery data and persistence remain outside this plan.
