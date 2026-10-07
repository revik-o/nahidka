package org.orev.nahidka.window

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import java.awt.EventQueue
import java.awt.Frame
import java.awt.Component
import java.awt.Cursor
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher
import java.awt.event.WindowEvent
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.dnd.DragSource
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.orev.nahidka.window.nativeapi.X11Gestures

private val FLOATING_WINDOW_SHAPE = RoundedCornerShape(10.dp)

internal class LinuxWindowChrome(
    window: ComposeWindow,
    controller: DesktopWindowController,
    private val transparentWindow: Boolean,
) : AwtWindowChrome(window, controller) {
    override val controlsAreNative = false
    override val contentInset get() = if (controller.isFloating && window.isResizable) 6.dp else 0.dp
    override val contentShape get() = if (transparentWindow && controller.isFloating) FLOATING_WINDOW_SHAPE else RectangleShape
    private val native = X11Gestures(window)
    private val fallback = FallbackWindowGesture(window)
    private var peerReady = false
    private var nativeMoveResize = false
    private var actions: Set<String>? = null
    private var press: Point? = null
    private var pendingMenu: IntOffset? = null
    private var previousPress: Point? = null
    private var previousTime = 0L
    private var floatingBounds: Rectangle? = null
    private var fallbackReported = false
    private var cursorGeneration = 0
    private var resizeCursorComponent: Component? = null
    private var menuPosition by mutableStateOf<IntOffset?>(null)
    private val keys = KeyEventDispatcher { event ->
        if (!closed && event.id == KeyEvent.KEY_PRESSED && window.isActive && acceptsInput() &&
            event.isAltDown && event.keyCode == KeyEvent.VK_SPACE) {
            if (peerReady && native.showMenu()) nativeMenuOpen = true else {
                val current = geometry.get()
                menuPosition = IntOffset((current.header.left + 12 * current.density).toInt(), current.header.bottom.toInt())
                nativeMenuOpen = true
            }
            true
        } else false
    }

    fun start() {
        installListeners()
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys)
    }

    override fun onPeerReady() {
        native.verifyPeer()
        peerReady = true
        nativeMoveResize = native.supports("_NET_WM_MOVERESIZE")
        System.err.println("Nahidka window: Linux ${Toolkit.getDefaultToolkit().javaClass.simpleName}, JBR ${System.getProperty("java.runtime.version")}, EWMH moveresize=$nativeMoveResize")
    }

    override fun refresh() {
        if (peerReady && window.isDisplayable && !closed) actions = native.allowedActions()
        super.refresh()
        if (!controller.isFloating || !window.isResizable || !acceptsInput()) clearResizeCursor()
        if (controller.isFloating && !window.isMinimized && window.extendedState and Frame.MAXIMIZED_BOTH == 0 && !fallback.active) floatingBounds = Rectangle(window.bounds)
    }

    override fun allowedControls(): Set<WindowControl> {
        val available = actions ?: return super.allowedControls()
        return buildSet {
            if ("_NET_WM_ACTION_MINIMIZE" in available) add(WindowControl.Minimize)
            if ("_NET_WM_ACTION_CLOSE" in available) add(WindowControl.Close)
            if ("_NET_WM_ACTION_MAXIMIZE_HORZ" in available && "_NET_WM_ACTION_MAXIMIZE_VERT" in available) add(WindowControl.Maximize)
        }
    }

    private fun reportFallback() {
        if (!fallbackReported) {
            fallbackReported = true
            System.err.println("Nahidka window: WM gesture capability unavailable; using application move/resize fallback")
        }
    }

    override fun onMouse(event: MouseEvent) {
        if (!acceptsInput() || controller.isFullscreen || !peerReady) return
        val current = geometry.get()
        val point = contentPoint(event)
        val caption = current.isCaptionHit(point)
        val scale = window.graphicsConfiguration.defaultTransform
        val edge = if (controller.isFloating && window.isResizable &&
            (actions == null || "_NET_WM_ACTION_RESIZE" in actions!!)) {
            resizeEdge(point.x, point.y,
                (window.contentPane.width * scale.scaleX).toFloat(),
                (window.contentPane.height * scale.scaleY).toFloat(), 6 * current.density)
        } else null
        if (event.id == MouseEvent.MOUSE_MOVED || event.id == MouseEvent.MOUSE_ENTERED) {
            if (menuPosition == null) nativeMenuOpen = false
            val generation = ++cursorGeneration
            event.component.cursor = Cursor.getPredefinedCursor(edge?.cursor ?: Cursor.DEFAULT_CURSOR)
            resizeCursorComponent = if (edge != null) event.component else null
            if (edge != null) EventQueue.invokeLater {
                if (!closed && controller.isFloating && generation == cursorGeneration) {
                    event.component.cursor = Cursor.getPredefinedCursor(edge.cursor)
                }
            }
        }
        when (event.id) {
            MouseEvent.MOUSE_PRESSED -> {
                if (event.button == MouseEvent.BUTTON3 && caption) {
                    event.consume()
                    press = null
                    fallback.finish(false)
                    if (native.showMenu()) nativeMenuOpen = true else {
                        pendingMenu = IntOffset(point.x.toInt(), point.y.toInt())
                    }
                } else if (event.button == MouseEvent.BUTTON1 && edge != null) {
                    event.consume()
                    if (!native.moveResize(edge.protocolValue)) {
                        reportFallback()
                        fallback.begin(event.locationOnScreen, edge)
                    }
                } else if (event.button == MouseEvent.BUTTON1 && caption &&
                    (actions == null || "_NET_WM_ACTION_MOVE" in actions!!)) {
                    event.consume()
                    val interval = (Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") as? Int) ?: 500
                    val previous = previousPress
                    if (previous != null && event.`when` - previousTime <= interval &&
                        previous.distance(event.locationOnScreen) <= DragSource.getDragThreshold()) {
                        press = null
                        previousPress = null
                        if (WindowControl.Maximize in enabledControls) controller.toggleMaximize()
                    } else {
                        press = Point(event.locationOnScreen)
                        previousPress = Point(event.locationOnScreen)
                        previousTime = event.`when`
                    }
                }
            }
            MouseEvent.MOUSE_DRAGGED -> {
                if (fallback.active) {
                    event.consume()
                    fallback.drag(event)
                } else {
                    val start = press ?: return
                    event.consume()
                    if (start.distance(event.locationOnScreen) >= DragSource.getDragThreshold()) {
                        press = null
                        previousPress = null
                        if (nativeMoveResize) {
                            if (JBR.isWindowMoveSupported()) {
                                JBR.getWindowMove().startMovingTogetherWithMouse(window, MouseEvent.BUTTON1)
                            } else native.moveResize(8)
                        } else {
                            reportFallback()
                            val wasMaximized = controller.isMaximized
                            if (wasMaximized) {
                                val restore = floatingBounds ?: Rectangle(window.x, window.y, 800, 600)
                                val local = SwingUtilities.convertPoint(event.component, event.point, window.contentPane)
                                val fraction = local.x.toDouble() / window.width.coerceAtLeast(1)
                                controller.restore()
                                window.extendedState = Frame.NORMAL
                                window.bounds = Rectangle(event.xOnScreen - (fraction * restore.width).toInt(),
                                    event.yOnScreen - local.y, restore.width, restore.height)
                            }
                            fallback.begin(if (wasMaximized) event.locationOnScreen else start, null)
                            fallback.drag(event)
                        }
                    }
                }
            }
            MouseEvent.MOUSE_RELEASED -> {
                if (event.button == MouseEvent.BUTTON3 && pendingMenu != null) {
                    pendingMenu = null
                    if (caption) {
                        menuPosition = IntOffset(point.x.toInt(), point.y.toInt())
                        nativeMenuOpen = true
                    }
                    event.consume()
                    return
                }
                if (press != null || fallback.active || edge != null || caption) event.consume()
                press = null
                fallback.finish(false)
            }
            MouseEvent.MOUSE_EXITED -> if (event.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK == 0) {
                press = null
                fallback.finish(false)
                clearResizeCursor()
                event.component.cursor = Cursor.getDefaultCursor()
            }
        }
    }

    private fun clearResizeCursor() {
        cursorGeneration++
        resizeCursorComponent?.cursor = Cursor.getDefaultCursor()
        resizeCursorComponent = null
    }

    override fun onDeactivated(opposite: Window?) {
        if (nativeMenuOpen) return
        press = null
        pendingMenu = null
        clearResizeCursor()
        fallback.finish(false)
        dismissMenu()
    }

    override fun onOwnedWindowEvent(event: WindowEvent) {
        if (event.id == WindowEvent.WINDOW_DEACTIVATED && menuPosition != null) {
            EventQueue.invokeLater {
                val active = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
                if (!closed && active !== window && (active == null || !owns(active))) dismissMenu()
            }
        }
    }

    private fun dismissMenu() {
        menuPosition = null
        nativeMenuOpen = false
    }

    private fun keyboardGesture(resize: Boolean) {
        dismissMenu()
        if (!controller.isFloating) return
        if (!native.moveResize(if (resize) 9 else 10, keyboard = true)) {
            reportFallback()
            fallback.begin(Point(window.x, window.y), if (resize) ResizeEdge.SouthEast else null, keyboard = true)
        }
    }

    @Composable
    override fun FrameOverlay() {
        menuPosition?.let { position ->
            LinuxWindowMenu(position, controller, enabledControls,
                canMove = controller.isFloating && (actions == null || "_NET_WM_ACTION_MOVE" in actions!!),
                canResize = controller.isFloating && window.isResizable && (actions == null || "_NET_WM_ACTION_RESIZE" in actions!!),
                onDismiss = ::dismissMenu, onMove = { keyboardGesture(false) }, onResize = { keyboardGesture(true) })
        }
    }

    override fun close() {
        if (closed) return
        clearResizeCursor()
        fallback.close()
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys)
        super.close()
    }
}
