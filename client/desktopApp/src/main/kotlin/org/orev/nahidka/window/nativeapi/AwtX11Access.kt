package org.orev.nahidka.window.nativeapi

import com.sun.jna.Pointer
import com.sun.jna.platform.unix.X11
import java.awt.Component
import java.awt.EventQueue
import java.awt.Window
import java.lang.reflect.Method

internal class AwtX11Access {
    private val componentPeer = Component::class.java.getDeclaredField("peer").apply {
        check(trySetAccessible()) { "Missing java.desktop/java.awt module access" }
    }
    private val toolkit = Class.forName("sun.awt.X11.XToolkit")
    private val sunToolkit = Class.forName("sun.awt.SunToolkit")
    private val peerType = Class.forName("sun.awt.X11.XWindowPeer")
    private val baseType = Class.forName("sun.awt.X11.XBaseWindow")
    private val lock = accessible(sunToolkit, "awtLock")
    private val unlock = accessible(sunToolkit, "awtUnlock")
    private val display = accessible(toolkit, "getDisplay")
    private val xid = accessible(baseType, "getWindow")
    private val setGrab = accessible(peerType, "setGrab", Boolean::class.javaPrimitiveType!!)

    fun <T> withWindow(window: Window, block: (X11.Display, X11.Window) -> T): T {
        check(EventQueue.isDispatchThread())
        check(window.isDisplayable)
        lock.invoke(null)
        try {
            val peer = checkNotNull(componentPeer.get(window))
            check(peerType.isInstance(peer)) { "Native Wayland peer is not supported" }
            val displayAddress = display.invoke(null) as Long
            val windowId = xid.invoke(peer) as Long
            check(displayAddress != 0L && windowId != 0L)
            val borrowedDisplay = X11.Display().apply { pointer = Pointer(displayAddress) }
            return block(borrowedDisplay, X11.Window(windowId))
        } finally {
            unlock.invoke(null)
        }
    }

    fun releasePeerGrab(window: Window) {
        check(EventQueue.isDispatchThread())
        setGrab.invoke(checkNotNull(componentPeer.get(window)), false)
    }

    private fun accessible(owner: Class<*>, name: String, vararg types: Class<*>): Method =
        owner.getMethod(name, *types).apply {
            check(trySetAccessible()) { "Missing module access for ${owner.name}.$name" }
        }
}
