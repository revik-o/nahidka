package org.orev.nahidka.window

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

internal class DesktopWindowController(
    private val state: WindowState,
    private val onCloseRequest: () -> Unit,
) {
    val isMaximized: Boolean
        get() = state.placement == WindowPlacement.Maximized

    val isFullscreen: Boolean
        get() = state.placement == WindowPlacement.Fullscreen

    val isFloating: Boolean
        get() = state.placement == WindowPlacement.Floating

    fun minimize() {
        if (closing) return
        state.isMinimized = true
    }

    fun toggleMaximize() {
        if (closing || isFullscreen) return
        state.isMinimized = false
        state.placement = if (isMaximized) {
            WindowPlacement.Floating
        } else {
            WindowPlacement.Maximized
        }
    }

    private var closing = false

    fun restore() {
        if (closing || isFullscreen) return
        state.isMinimized = false
        state.placement = WindowPlacement.Floating
    }

    fun close() {
        if (closing) return
        closing = true
        onCloseRequest()
    }
}
