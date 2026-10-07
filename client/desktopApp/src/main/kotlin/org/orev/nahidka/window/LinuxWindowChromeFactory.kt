package org.orev.nahidka.window

import androidx.compose.ui.awt.ComposeWindow
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

internal class LinuxWindowChromeFactory : WindowChromeFactory {
    override val transparentWindow = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)

    override fun prepare(window: ComposeWindow, controller: DesktopWindowController): WindowChrome {
        check(Toolkit.getDefaultToolkit().javaClass.name == "sun.awt.X11.XToolkit") {
            "Nahidka supports X11/XWayland; use the packaged launcher with -Dawt.toolkit.name=XToolkit and an X11 DISPLAY"
        }
        return LinuxWindowChrome(window, controller, transparentWindow).apply { start() }
    }
}
