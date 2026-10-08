# Linux live resize investigation — 8 October 2026

The reported symptom is a detached black rectangle while dragging the right
window border on KDE Wayland/XWayland; the application repaints correctly when
the mouse button is released.

## Finding and implementation

The Linux border gesture already delegates resizing to the window manager via
`_NET_WM_MOVERESIZE`. The gesture handler does not block the AWT event thread.
However, the JBR XToolkit window did not advertise `_NET_WM_SYNC_REQUEST` or
provide its XSync counter. Resizing the native window and its heavyweight Skiko
canvas could therefore run ahead of submitting a frame at the new dimensions.
This missing coordination is a plausible cause of the reported artifact; the
exact host artifact has not yet been verified with physical input after the fix.

`X11ResizeSync` now advertises the standard client protocol and counter, receives
requests through the existing AWT X11 connection, and associates each request
with its following `ConfigureNotify`. On the AWT event thread, the Linux chrome
invalidates and validates the layout, then calls Compose's `renderImmediately()`
before advancing the counter. Explicit invalidation is necessary because JBR's
native configure handler updates the frame size before the queued AWT resize
event invalidates its child layout. A request is acknowledged only if the
rendered token and current window dimensions still match. Physical-to-logical
size conversion follows JBR's `ceil(value - 0.5)` rounding, including odd pixel
widths at 200% scale. This also covers same-size configure events and the initial
mapping handshake. Disposal removes the dispatcher and destroys the counter.

KWin uses these acknowledgements to pace interactive resizing. On XWayland it
also blocks surface commits until acknowledgement and then waits for the
surface commit. The implementation preserves the existing ComposeWindow,
OpenGL renderer, transparency and rounded corners. No new dependency is needed.

The integration intentionally does not require `_NET_WM_SYNC_REQUEST` in the
root window's `_NET_SUPPORTED` property: KWin 6.3.6 accepts this client protocol
without listing it there. It requires the XSync extension and skips installation
if another handler already owns the client protocol/counter. The existing
`java.desktop/sun.awt` and `java.desktop/sun.awt.X11` module opens are required.

References: [EWMH resize synchronization](https://specifications.freedesktop.org/wm/latest-single/),
[KWin 6.3.6 X11 window implementation](https://invent.kde.org/plasma/kwin/-/blob/v6.3.6/src/x11window.cpp),
[KWin root capability declarations](https://invent.kde.org/plasma/kwin/-/blob/v6.3.6/src/netinfo.cpp),
[Skiko 0.144.6 Linux OpenGL redrawer](https://github.com/JetBrains/skiko/blob/v0.144.6/skiko/src/awtMain/kotlin/org/jetbrains/skiko/redrawer/LinuxOpenGLRedrawer.kt).

## Validation

Environment: JBR 25.0.4.1+1-b583.48, Compose 1.11.1, Skiko 0.144.6,
KDE/KWin 6.3.6. The host uses XWayland and Mesa Intel OpenGL. Isolated interactive
checks use Xephyr, KWin X11 and Skiko's software renderer, with vsync/frame
limiting disabled because Xephyr reports a zero refresh rate. Application
defaults are unchanged.

- `:desktopApp:jar --offline` passed.
- All 12 isolated geometry/control checks passed: eight border/corner resize
  directions, maximize, restore, minimize and close. See [results](results.json).
- The trace contains 43 matching request/paint acknowledgements, including the
  initial 800 × 600 mapping request. See [application trace](app.log).
- Held-button captures show no exposed black band against a colored desktop
  backdrop. See [during the drag](held-40.png) and [after release](after.png).
- The actual host starts with `resize sync=true` and the Intel OpenGL renderer,
  and acknowledges its initial mapping request. See [host trace](host-app.log).

The colored backdrop corrected an early measurement error: uncovered black
desktop pixels had been counted as a black application buffer. The unchanged
baseline also looks normal in this isolated software-rendering fixture. Thus
the screenshots verify rendering and geometry in the fixture, not elimination
of the user's exact Wayland artifact. Direct host `XMoveResizeWindow` buffer
snapshots still contain transient black areas, but that path bypasses the WM
resize-request protocol. Synthetic border-drag events are ignored on the host
Wayland session, so physical-input acceptance remains open.

## Physical-input check

Restart `:desktopApp:run` so it uses the changed sources. The startup diagnostic
should include `EWMH moveresize=true, resize sync=true`. Drag the right border
slowly and rapidly, hold the mouse button at the new size, then release. Repeat
with a corner and the Dashboard page. Confirm that the detached black rectangle
and flicker are gone, and that maximize/restore still works.

For a diagnostic trace, add `-Dnahidka.window.resizeSync.trace=true` to the
application JVM options. Each `request=N size=WxH` should be followed by a
matching `painted=N size=WxH`. No VM-option change is required for normal use.
