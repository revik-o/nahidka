package org.orev.nahidka.window

import androidx.compose.ui.awt.ComposeWindow

internal class WindowsWindowChromeFactory : WindowChromeFactory {
    override fun prepare(window: ComposeWindow, controller: DesktopWindowController): WindowChrome =
        WindowsWindowChrome(window, controller).apply { start() }
}
