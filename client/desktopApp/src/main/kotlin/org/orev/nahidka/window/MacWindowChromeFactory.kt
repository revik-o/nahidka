package org.orev.nahidka.window

import androidx.compose.ui.awt.ComposeWindow

internal class MacWindowChromeFactory : WindowChromeFactory {
    override fun prepare(window: ComposeWindow, controller: DesktopWindowController): WindowChrome =
        JbrWindowChrome(window, controller, controlsAreNative = true).apply { start() }
}
