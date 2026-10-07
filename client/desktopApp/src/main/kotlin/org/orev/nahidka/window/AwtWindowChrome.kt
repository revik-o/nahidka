package org.orev.nahidka.window

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import java.awt.Dialog
import java.awt.Window
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.AWTEvent
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

internal data class ChromeGeometry(
    val header: Rect = Rect.Zero,
    val controls: Map<WindowControl, Rect> = emptyMap(),
    val enabled: Set<WindowControl> = emptySet(),
    val fullscreen: Boolean = false,
    val inputEnabled: Boolean = true,
    val density: Float = 1f,
    val ready: Boolean = false,
)

internal abstract class AwtWindowChrome(
    protected val window: ComposeWindow,
    protected val controller: DesktopWindowController,
) : WindowChrome {
    final override var isActive by mutableStateOf(false)
        protected set
    override var enabledControls by mutableStateOf(emptySet<WindowControl>())
        protected set
    override var nativeMenuOpen by mutableStateOf(false)
        protected set
    override var interaction by mutableStateOf(NativeControlInteraction())
        protected set
    override val leftInset = 0.dp
    override val rightInset = 0.dp
    override val contentInset = 0.dp
    override val contentShape: Shape = RectangleShape
    override val usesNativeMaximizeHover = false
    protected val geometry = AtomicReference(ChromeGeometry())
    protected var closed = false
    private var peerInstalled = false
    private val windows = object : WindowAdapter() {
        override fun windowActivated(e: WindowEvent) = refresh()
        override fun windowDeactivated(e: WindowEvent) {
            interaction = NativeControlInteraction()
            onDeactivated(e.oppositeWindow)
            refresh()
        }
        override fun windowStateChanged(e: WindowEvent) = refresh()
        override fun windowClosed(e: WindowEvent) = close()
    }
    private val components = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) = refresh()
        override fun componentMoved(e: ComponentEvent) = refresh()
    }
    private val properties = PropertyChangeListener {
        if (it.propertyName == "graphicsConfiguration") {
            val old = (it.oldValue as? java.awt.GraphicsConfiguration)?.defaultTransform
            val new = (it.newValue as? java.awt.GraphicsConfiguration)?.defaultTransform
            if (old?.scaleX != new?.scaleX || old?.scaleY != new?.scaleY) invalidateGeometry()
        }
        refresh()
    }
    private val hierarchy = HierarchyListener {
        if (it.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
            if (window.isDisplayable && !peerInstalled && !closed) {
                try {
                    onPeerReady()
                    peerInstalled = true
                    refresh()
                } catch (failure: Throwable) {
                    close()
                    throw IllegalStateException("Cannot initialize Nahidka desktop window integration", failure)
                }
            } else if (!window.isDisplayable && peerInstalled) close()
        }
    }
    private val mouse = AWTEventListener { event ->
        if (!closed && event is MouseEvent && belongsToWindow(event)) onMouse(event)
        if (!closed && event is WindowEvent && event.window !== window && owns(event.window)) {
            onOwnedWindowEvent(event)
            EventQueue.invokeLater { refresh() }
        }
    }

    protected fun installListeners() {
        window.addWindowListener(windows)
        window.addWindowStateListener(windows)
        window.addComponentListener(components)
        window.addPropertyChangeListener(properties)
        window.addHierarchyListener(hierarchy)
        Toolkit.getDefaultToolkit().addAWTEventListener(mouse,
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.WINDOW_EVENT_MASK)
        refresh()
    }

    protected fun owns(other: Window): Boolean {
        var owner = other.owner
        while (owner != null) {
            if (owner === window) return true
            owner = owner.owner
        }
        return false
    }

    protected fun acceptsInput(): Boolean = window.isEnabled &&
        Window.getWindows().none { it is Dialog && (it.isModal || it.isFocusableWindow && !nativeMenuOpen) && it.isShowing && owns(it) }

    protected fun belongsToWindow(event: MouseEvent): Boolean =
        SwingUtilities.getWindowAncestor(event.component) === window || event.component === window

    protected fun contentPoint(event: MouseEvent): Offset {
        val local = SwingUtilities.convertPoint(event.component, event.point, window.contentPane)
        val scale = window.graphicsConfiguration.defaultTransform
        return Offset((local.x * scale.scaleX).toFloat(), (local.y * scale.scaleY).toFloat())
    }

    protected fun invalidateGeometry() {
        geometry.set(geometry.get().copy(controls = emptyMap(), ready = false))
    }

    override fun updateHeader(bounds: Rect, density: Float) {
        if (closed) return
        val old = geometry.get()
        val changed = old.header != bounds || old.density != density
        geometry.set(old.copy(header = bounds, density = density,
            controls = if (changed) emptyMap() else old.controls,
            ready = controlsAreNative || (!changed && old.ready)))
        headerChanged(bounds, density)
    }

    override fun updateControl(control: WindowControl, bounds: Rect?) {
        if (closed) return
        val old = geometry.get()
        val controls = old.controls.toMutableMap()
        if (bounds == null) controls.remove(control) else controls[control] = bounds
        geometry.set(old.copy(controls = controls.toMap(), ready = controls.size == 3))
    }

    protected open fun headerChanged(bounds: Rect, density: Float) {}
    protected open fun onPeerReady() {}
    protected open fun onMouse(event: MouseEvent) {}
    protected open fun onDeactivated(opposite: Window?) {}
    protected open fun onOwnedWindowEvent(event: WindowEvent) {}
    protected open fun allowedControls(): Set<WindowControl> = WindowControl.entries.toSet()
    final override fun refreshState() = refresh()

    protected open fun refresh() {
        if (closed) return
        isActive = window.isActive
        enabledControls = if (!acceptsInput()) emptySet() else allowedControls().filterTo(mutableSetOf()) {
            it != WindowControl.Maximize || (!controller.isFullscreen && window.isResizable)
        }
        geometry.set(geometry.get().copy(enabled = enabledControls, fullscreen = controller.isFullscreen, inputEnabled = acceptsInput()))
    }

    protected fun publishInteraction(value: NativeControlInteraction) {
        EventQueue.invokeLater { if (!closed) interaction = value }
    }

    @Composable
    override fun Caption(modifier: Modifier, content: @Composable () -> Unit) {
        Box(modifier) { content() }
    }

    @Composable
    override fun FrameOverlay() {}

    override fun close() {
        if (closed) return
        closed = true
        geometry.set(ChromeGeometry())
        Toolkit.getDefaultToolkit().removeAWTEventListener(mouse)
        window.removeWindowListener(windows)
        window.removeWindowStateListener(windows)
        window.removeComponentListener(components)
        window.removePropertyChangeListener(properties)
        window.removeHierarchyListener(hierarchy)
    }
}
