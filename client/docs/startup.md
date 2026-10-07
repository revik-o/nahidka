# Application startup

Build and launch are separate operations. The IDE, `run-client-desktop.sh`, and
`:desktopApp:run` invoke Gradle; their elapsed time includes configuration and
possibly compilation. The desktop icon and direct launcher execute the packaged
application without a build tool.

## Linux measurements — 28 September 2026

An interleaved comparison alternated 20 launches of each package, reversing their
order on alternate pairs. Both used the same first-frame instrumentation and
bundled JBR 25. Filesystem caches were warm; host load varied. These are measurements
on this machine, not universal startup guarantees.

| Launch request to dashboard frame | Previous UI | Optimized release |
| --- | ---: | ---: |
| Median | 4,241 ms | 3,108 ms |
| p95 | 6,682 ms | 4,145 ms |
| Package image, uncompressed | 181 MiB | 139 MiB |

Median launch time fell **26.7%**. The baseline needed `jdk.unsupported` added to its
bundled runtime: the original package crashed inside Jewel without it. The baseline
otherwise retained the original navigation, themes, dashboard, and Jewel window.
The optimized package includes the native splash. [Raw samples and baseline revision](startup-linux-2026-09-28.json).

An earlier isolated comparison of the optimized UI with and without desktop
shrinking measured 2,871 vs 2,992 ms median and 2,958 vs 3,129 ms p95. The improvement
is modest; shrinking also removed roughly 16 MiB from the image. Those separate
runs are not used for the headline before/after result.

The native desktop splash and dashboard were captured from the packaged launcher.
First-splash latency and first-install disk-cold performance have not been given
benchmark claims; the application marker cannot measure either.

## Desktop

From `client`, build a portable application image once:

```sh
./gradlew :desktopApp:createReleaseDistributable
```

Then launch it directly from the repository root:

```sh
./cli/launch-client-desktop.sh
# Windows: cli\launch-client-desktop.bat
```

Rebuild after changing code. The launcher runs the last built image and deliberately does not invoke Gradle. `NAHIDKA_DESKTOP_APP` can point to the directory
containing the packaged application for a relocated image. Arguments and paths
containing spaces are supported.

For an installer, run `./gradlew :desktopApp:packageReleaseDistributionForCurrentOS`
(or `cli/build-client-desktop.sh`). The Linux package installs a desktop-menu entry
that runs the packaged executable; the Windows installer creates desktop and
Start-menu shortcuts. Windows and macOS installers must be built on those operating systems. `make run-desktop` remains the development workflow.

There is no separate launcher splash window. The main window opens first and its
first frame draws `ApplicationSplash` (the brand mark and name) under the custom
header. `DesktopApplication` composes the application on the following frame, so
the expensive first composition no longer delays the window. Windows and
Linux use application-drawn controls; macOS keeps native traffic lights inside
the header to preserve system hover menus and fullscreen behavior. Native window
integration is validated separately on each supported platform. Linux targets
X11 and XWayland; native Wayland is not included.

The header stays above the application and reserves a 6 dp resize perimeter on
floating Linux windows. When the display supports per-pixel translucency (a
compositing window manager is running), the Linux window is transparent: the
perimeter becomes an invisible resize border and the content is clipped to
rounded corners while floating. Without a compositor the window stays square and
opaque. Windows 11 and macOS keep their native rounded frames. The minimum size
is 360 × 480 AWT logical units. `NAHIDKA_STARTUP_TRACE=1` prints the
`splash_frame` and `dashboard_frame` milestones. See the
[window integration validation record](window-chrome/README.md) for actual checks
and remaining release gates.

Gradle development launches and packaged launchers supply native access and the
Linux XToolkit/module-access flags. Direct IDE main-class launches require
`--enable-native-access=ALL-UNNAMED`; on Linux also supply
`-Dawt.toolkit.name=XToolkit`, `--add-opens=java.desktop/java.awt=ALL-UNNAMED`,
`--add-opens=java.desktop/sun.awt=ALL-UNNAMED`, and
`--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED`. An X11 `DISPLAY` is required
in X11 and XWayland sessions. Use the packaged launcher or configured Gradle task
for the default launch workflow.

The release build pins ProGuard 7.9.1 because the Compose plugin's default 7.7.0
cannot process the project's JDK 25 class files.

## Shared UI and platform handoff

- The root renders `ApplicationShell` around every destination: a sidebar on
  expanded widths and a floating bottom bar on compact widths (the shared 600 dp
  `LayoutWidth` breakpoint), plus a top bar with the notifications button. One
  shared theme supplies its colors and typography.
- The dashboard arranges summary cards owned by the feature modules; every card
  reads the same session state as the feature screens.
- `App(onFirstFrame = ...)` is optional. It reports once per composition lifetime,
  after content draws and the next frame begins. This is a render milestone, not
  proof that the OS compositor has presented pixels or the user has interacted.
- Android uses `core-splashscreen` and a static launch theme. Normal platform
  dismissal is retained; nothing blocks drawing waiting for the callback.
  The callback calls `reportFullyDrawn()` for startup measurements.
- iOS uses `UILaunchScreen` assets and a matching SwiftUI/UIKit host background.
- Web displays an inline HTML/CSS splash outside the Compose viewport. It is
  removed after the first dashboard draw. Bootstrap failures show a reload message.
  Performance marks: `nahidka-page`, `nahidka-main`, `nahidka-dashboard-frame`.

## Reproducing desktop measurements

```sh
python3 cli/benchmark-client-desktop.py --output /tmp/nahidka-startup.json -- \
  client/desktopApp/build/compose/binaries/main-release/app/org.orev.nahidka/bin/org.orev.nahidka
```

The runner enables `NAHIDKA_STARTUP_TRACE=1`, records main entry and the after-draw
marker, then terminates the app. It records the first observed launch separately,
followed by 20 fresh-process launches and median/p95 values. Filesystem caches are
not cleared; the first observation is not automatically a first-install result.
Run without compilation, emulators, or other benchmarks competing for resources.
Add `--baseline /path/to/baseline/executable` before `--` to alternate launches of
both packages, reversing order on every other pair to reduce machine-load drift.

Use screen recording to measure icon/launcher-to-splash and verify the handoff.
The runner cannot measure pixels before the JVM or establish interactivity. Test
window controls and dashboard interactions separately. Compare release packages,
not a release package against an IDE debug session.

## Android profiles and benchmarks

The `:baselineprofile` module generates both Baseline and Startup Profiles for the
launch-to-dashboard journey. Use a connected API 33+ device (or a rooted API 28+
device). The app's release build shrinks code and resources and excludes UI tooling.

```sh
cd client
./gradlew :androidApp:generateReleaseBaselineProfile
./gradlew :androidApp:assembleRelease
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.orev.nahidka.baselineprofile.StartupBenchmarks
```

Generation is explicit so ordinary release builds do not require a device. Commit
the generated files under `androidApp/src/release/generated/baselineProfiles` after
regenerating. Never substitute handwritten class lists for measured profiles.
`coldWithProfile` requires an installed profile and fails if one is missing;
`coldWithoutProfile` establishes the comparison. Benchmark performance on a physical
device; a software emulator is useful only for correctness/profile generation.

## Validation

```sh
cd client
./gradlew :shared:jvmTest :shared:ui:dashboard:jvmTest
./gradlew :androidApp:assembleRelease :baselineprofile:assemble
./gradlew :webApp:jsBrowserDistribution :webApp:wasmJsBrowserDistribution
```

Desktop UI tests cover the first-frame callback firing once, session edits through
dialogs, switching between wide and compact layouts, and reaching lazy sections.
On Android, verify launch on API 24–30 and API 31+, cold/warm launches, and returning
from the background. On iOS, use an Xcode Release build and verify cold launch on a
physical device. Test both production web targets with an empty browser cache and
with the main script blocked to verify the bootstrap failure message.
