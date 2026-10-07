package org.orev.nahidka

import androidx.compose.ui.window.application
import org.orev.nahidka.window.NahidkaWindow

fun main() {
    val desktopStartupTrace = DesktopStartupTrace()

    application {
        NahidkaWindow(::exitApplication, desktopStartupTrace)
    }
}
