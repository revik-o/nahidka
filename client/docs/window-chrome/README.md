# Desktop window integration validation

The [integrated desktop title bar](title-bar-layout/README.md) includes the current layout and its validation. The native integration checks below record the earlier implementation.

The [live resize investigation](live-resize/README.md) records the 8 October
2026 Linux resize synchronization change and its validation limits.

Implementation applied on 2 October 2026 against the existing working tree.
No source comments or unit tests were added, as requested. The shared `App`
API, startup callback, initial window size, and other platform entry points were
retained. Native code and dependencies are confined to the desktop module.

## Environment and build

Debian 13.7, amd64; JetBrains Runtime 25.0.4.1+1-b583.48; Compose 1.11.1;
JBR API 1.9.0; JNA 5.17.0. The host uses KDE Wayland with XWayland. Native X11
checks run in Xephyr 21.1.16 with KWin X11 6.3.6 on a separate D-Bus session.

Desktop compilation, shrinking, portable release-image creation, and Debian
installer creation were checked with:

```sh
JAVA_HOME=/home/oleg/.gradle/jdks/jetbrains_s_r_o_-25-amd64-linux.2 \
./client/gradlew -p client :desktopApp:createReleaseDistributable \
  :desktopApp:packageReleaseDistributionForCurrentOS \
  :shared:jvmTest :shared:ui:dashboard:jvmTest --offline
```

The eight existing shared/dashboard tests passed. No new test dependencies or
unit-test files were added. The launch configuration includes native access,
Linux XToolkit, and the three required module opens. The release runtime includes
`jdk.unsupported`; ProGuard preserves JBR services and native mapping declarations.

## GUI checks

The isolated GUI runner uses XTest pointer/keyboard events, inspects actual
window bounds and WM properties, and launches the built application directly.
The test environment uses software rendering and disables vsync/frame limiting,
since Xephyr reports a zero refresh rate. Compose automatic DPI detection is
turned off only in the runner so the requested AWT scale takes effect. These
flags do not change packaged application defaults.

| Environment | Evidence | Result |
| --- | --- | --- |
| Release image, KWin X11, 100% | [JSON](kde-x11-release-100/linux-smoke.json) | 19 native geometry/control checks passed |
| Release image, KWin X11, 200% | [JSON](kde-x11-release-200/linux-smoke.json) | 19 native geometry/control checks passed; minimum hints 720 × 960 physical pixels |
| Release image fallback, KWin X11 | [JSON](kde-x11-release-100-fallback/linux-smoke.json) | 23 checks passed, including keyboard Move/Resize and Escape restoration |
| Host XWayland | Initial diagnostic files in this directory | Launch/first frame checked; synthetic WM gestures were not accepted, so physical-input acceptance remains open |
| 150% / mixed DPI | Diagnostic attempt in `kde-x11-release-150` | Not validated: automatic DPI overrides the test property, and XToolkit truncates fractional debug scale; use real compositor/monitor configuration |
| Windows 11 | No runner available | Not tested; blocks Windows release approval |
| macOS | No runner available | Not tested; blocks macOS release approval |
| GNOME X11/XWayland | No runner available | Not tested |

The native checks cover maximize/restore with matching restored bounds, caption
movement and double-click, all eight resize directions, opposite-edge behavior,
minimum-size enforcement, canceling a dragged control, minimize, and close.
The fallback fixture removes only moveresize/window-menu capabilities from the
isolated WM's advertised support. Its keyboard checks verify 10-unit steps and
Escape returning exactly to the starting bounds. Fallback results do not count
as native WM parity.

Early diagnostic screenshots and JSON include failed harness attempts. Missing
X11 properties during peer creation, menu opening/focus handling, and stale
cursor updates were corrected before final verification. Early resize-paint
captures were stale with both the unchanged baseline and new implementation;
disabling the isolated runner's unsupported vsync resolves that capture issue.

## Remaining platform acceptance

Windows Snap Layout hover/selection, native child-HWND routing, mixed/negative
monitor origins, shortcuts and native hook disposal still need real Windows
execution. macOS traffic-light hover, native tiling/fullscreen, caption settings,
and insets need real macOS execution. Linux physical-input snapping,
drag-to-restore, advertised native menus, modal/accessibility behavior, fractional
and mixed DPI, and GPU/splash/resize visual checks remain manual acceptance gates.

Windows uses owner-thread subclasses because the API forbids cross-thread
subclassing. [Microsoft SetWindowSubclass documentation](https://learn.microsoft.com/en-us/windows/win32/api/commctrl/nf-commctrl-setwindowsubclass).
Linux gestures use the borrowed AWT X11 connection and the WM protocol.
[EWMH specification](https://specifications.freedesktop.org/wm/latest-single/).

## Startup comparison

The pre-change release image was retained at `/tmp/nahidka-window-baseline`.
The existing benchmark runner compares 20 interleaved fresh-process launches
per image on the host, without concurrent builds or GUI fixtures. Filesystem
caches were not cleared. Timings measure delivery of the dashboard after-draw
marker, not compositor presentation. [Raw samples](startup-linux-2026-10-02.json).

| Launch to dashboard frame | Pre-change release | Final release | Change |
| --- | --- | --- | --- |
| Median | 3448.5 ms | 3727.7 ms | +8.1% |
| p95 | 3887.6 ms | 3952.3 ms | +1.7% |

Both changes remain below the plan's investigation thresholds (10% median,
15% p95). This result establishes the measured overhead on this host; it does
not establish Windows/macOS startup performance or first-install performance.
