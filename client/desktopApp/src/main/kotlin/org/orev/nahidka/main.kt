package org.orev.nahidka

import androidx.compose.ui.window.application
import org.orev.nahidka.window.NahidkaWindow

fun main() {
    val startup = DesktopStartupTrace()

    application {
        NahidkaWindow(::exitApplication, startup::onFirstFrame)
    }
}
