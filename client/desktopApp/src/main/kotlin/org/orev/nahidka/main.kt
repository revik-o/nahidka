package org.orev.nahidka

import androidx.compose.ui.window.application
import org.orev.nahidka.window.NahidkaWindow

fun main() = application {
    NahidkaWindow(::exitApplication)
}
