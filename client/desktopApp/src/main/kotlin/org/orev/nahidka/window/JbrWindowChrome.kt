package org.orev.nahidka.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import java.awt.event.MouseEvent

internal open class JbrWindowChrome(
    window: ComposeWindow,
    controller: DesktopWindowController,
    final override val controlsAreNative: Boolean,
) : AwtWindowChrome(window, controller) {
    private val decorations = checkNotNull(JBR.getWindowDecorations()) {
        "Nahidka requires JetBrains Runtime 25 with custom title-bar support; use the packaged launcher"
    }
    private val titleBar = decorations.createCustomTitleBar().apply {
        height = 40f
        putProperty("controls.visible", controlsAreNative)
    }
    final override var leftInset by mutableStateOf(0.dp)
        private set
    final override var rightInset by mutableStateOf(0.dp)
        private set

    init {
        decorations.setCustomTitleBar(window, titleBar)
    }

    fun start() {
        installListeners()
        System.err.println("Nahidka window: ${System.getProperty("os.name")}, JBR ${System.getProperty("java.runtime.version")}, nativeControls=$controlsAreNative")
    }

    override fun headerChanged(bounds: Rect, density: Float) {
        if (bounds.height > 0 && density > 0) titleBar.height = bounds.height / density
        refresh()
    }

    override fun refresh() {
        super.refresh()
        if (closed) return
        leftInset = titleBar.leftInset.dp
        rightInset = titleBar.rightInset.dp
    }

    override fun onMouse(event: MouseEvent) {
        if (event.id == MouseEvent.MOUSE_EXITED || event.id == MouseEvent.MOUSE_WHEEL) return
        val current = geometry.get()
        val point = contentPoint(event)
        titleBar.forceHitTest(!acceptsInput() || current.fullscreen || !current.ready ||
            !current.header.contains(point) || current.controls.values.any { it.contains(point) })
    }

    override fun close() {
        if (closed) return
        super.close()
        decorations.setCustomTitleBar(window, null)
    }
}
