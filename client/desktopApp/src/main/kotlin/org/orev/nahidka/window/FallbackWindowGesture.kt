package org.orev.nahidka.window

import androidx.compose.ui.awt.ComposeWindow
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.KeyEventDispatcher

internal class FallbackWindowGesture(private val window: ComposeWindow) : AutoCloseable {
    private var start: Rectangle? = null
    private var pointer = Point()
    private var configuration = window.graphicsConfiguration
    private var edge: ResizeEdge? = null
    private var keyboard = false
    private val keys = KeyEventDispatcher { event ->
        if (start != null && KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow !== window) {
            finish(false)
            false
        } else if (start == null || event.id != KeyEvent.KEY_PRESSED) false else {
            when (event.keyCode) {
                KeyEvent.VK_ESCAPE -> finish(true)
                KeyEvent.VK_ENTER -> finish(false)
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN -> {
                    if (!keyboard) return@KeyEventDispatcher false
                    val step = if (event.isShiftDown) 1 else 10
                    val dx = when (event.keyCode) { KeyEvent.VK_LEFT -> -step; KeyEvent.VK_RIGHT -> step; else -> 0 }
                    val dy = when (event.keyCode) { KeyEvent.VK_UP -> -step; KeyEvent.VK_DOWN -> step; else -> 0 }
                    applyDelta(window.bounds, dx, dy)
                }
                else -> return@KeyEventDispatcher false
            }
            true
        }
    }
    val active get() = start != null

    fun begin(point: Point, edge: ResizeEdge?, keyboard: Boolean = false) {
        finish(false)
        start = Rectangle(window.bounds)
        pointer = Point(point)
        configuration = window.graphicsConfiguration
        this.edge = edge
        this.keyboard = keyboard
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys)
    }

    fun drag(event: MouseEvent) {
        val original = start ?: return
        if (keyboard) return
        if (configuration !== window.graphicsConfiguration || event.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK == 0) {
            finish(false)
            return
        }
        applyDelta(original, event.xOnScreen - pointer.x, event.yOnScreen - pointer.y)
    }

    private fun applyDelta(bounds: Rectangle, dx: Int, dy: Int) {
        val resize = edge
        if (resize == null) {
            window.setBounds(bounds.x + dx, bounds.y + dy, bounds.width, bounds.height)
            return
        }
        var left = bounds.x
        var top = bounds.y
        var right = bounds.x + bounds.width
        var bottom = bounds.y + bounds.height
        if (resize.west) left = minOf(left + dx, right - 360)
        if (resize.east) right = maxOf(right + dx, left + 360)
        if (resize.north) top = minOf(top + dy, bottom - 480)
        if (resize.south) bottom = maxOf(bottom + dy, top + 480)
        window.setBounds(left, top, right - left, bottom - top)
    }

    fun finish(cancel: Boolean) {
        val original = start ?: return
        start = null
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys)
        if (cancel) window.bounds = original
    }

    override fun close() = finish(false)
}
