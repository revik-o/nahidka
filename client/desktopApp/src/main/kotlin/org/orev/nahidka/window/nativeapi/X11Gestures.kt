package org.orev.nahidka.window.nativeapi

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference
import java.awt.Window

internal interface X11Grabs : Library {
    fun XUngrabPointer(display: X11.Display, time: NativeLong): Int
    fun XUngrabKeyboard(display: X11.Display, time: NativeLong): Int
}

internal class X11Gestures(private val window: Window) {
    private val access = AwtX11Access()
    private val x11 = X11.INSTANCE
    private val grabs = Native.load("X11", X11Grabs::class.java)

    private fun root(display: X11.Display, client: X11.Window): X11.Window {
        val attributes = X11.XWindowAttributes()
        check(x11.XGetWindowAttributes(display, client, attributes) != 0)
        return attributes.root
    }

    private fun atoms(display: X11.Display, target: X11.Window, name: String): Set<Long>? {
        val actualType = X11.AtomByReference()
        val format = IntByReference()
        val count = NativeLongByReference()
        val remaining = NativeLongByReference()
        val value = PointerByReference()
        val status = x11.XGetWindowProperty(display, target, x11.XInternAtom(display, name, false),
            NativeLong(0), NativeLong(4096), false, X11.Atom(X11.AnyPropertyType.toLong()),
            actualType, format, count, remaining, value)
        val buffer = value.value
        try {
            if (status != 0 || (actualType.value?.toLong() ?: 0L) == 0L || format.value != 32) return null
            return (0 until count.value.toInt()).mapTo(mutableSetOf()) {
                buffer.getNativeLong(it.toLong() * NativeLong.SIZE).toLong()
            }
        } finally {
            if (buffer != null) x11.XFree(buffer)
        }
    }

    fun verifyPeer() = access.withWindow(window) { _, _ -> Unit }

    fun supports(name: String): Boolean = access.withWindow(window) { display, client ->
        x11.XInternAtom(display, name, false).toLong() in
            (atoms(display, root(display, client), "_NET_SUPPORTED") ?: emptySet())
    }

    fun allowedActions(): Set<String>? = access.withWindow(window) { display, client ->
        val values = atoms(display, client, "_NET_WM_ALLOWED_ACTIONS") ?: return@withWindow null
        listOf("_NET_WM_ACTION_MINIMIZE", "_NET_WM_ACTION_MAXIMIZE_HORZ",
            "_NET_WM_ACTION_MAXIMIZE_VERT", "_NET_WM_ACTION_CLOSE", "_NET_WM_ACTION_RESIZE",
            "_NET_WM_ACTION_MOVE").filterTo(mutableSetOf()) {
            x11.XInternAtom(display, it, false).toLong() in values
        }
    }

    fun moveResize(direction: Int, keyboard: Boolean = false): Boolean = access.withWindow(window) { display, client ->
        val root = root(display, client)
        val atom = x11.XInternAtom(display, "_NET_WM_MOVERESIZE", false)
        if (atom.toLong() !in (atoms(display, root, "_NET_SUPPORTED") ?: emptySet())) return@withWindow false
        val rootReturn = X11.WindowByReference()
        val childReturn = X11.WindowByReference()
        val rootX = IntByReference()
        val rootY = IntByReference()
        val localX = IntByReference()
        val localY = IntByReference()
        val mask = IntByReference()
        if (!x11.XQueryPointer(display, client, rootReturn, childReturn, rootX, rootY, localX, localY, mask)) {
            return@withWindow false
        }
        if (!keyboard && direction != 11 && mask.value and X11.Button1Mask == 0) return@withWindow true
        access.releasePeerGrab(window)
        grabs.XUngrabPointer(display, NativeLong(0))
        grabs.XUngrabKeyboard(display, NativeLong(0))
        send(display, client, root, atom,
            longArrayOf(rootX.value.toLong(), rootY.value.toLong(), direction.toLong(), if (keyboard) 0 else 1, 1))
    }

    fun showMenu(): Boolean = access.withWindow(window) { display, client ->
        val root = root(display, client)
        val atom = x11.XInternAtom(display, "_GTK_SHOW_WINDOW_MENU", false)
        if (atom.toLong() !in (atoms(display, root, "_NET_SUPPORTED") ?: emptySet())) return@withWindow false
        val rootX = IntByReference()
        val rootY = IntByReference()
        val success = x11.XQueryPointer(display, client, X11.WindowByReference(), X11.WindowByReference(),
            rootX, rootY, IntByReference(), IntByReference(), IntByReference())
        if (!success) return@withWindow false
        send(display, client, root, atom, longArrayOf(0, rootX.value.toLong(), rootY.value.toLong(), 0, 0))
    }

    private fun send(display: X11.Display, client: X11.Window, root: X11.Window,
                     atom: X11.Atom, values: LongArray): Boolean {
        val message = X11.XClientMessageEvent().apply {
            type = X11.ClientMessage
            serial = NativeLong(0)
            send_event = 1
            this.display = display
            this.window = client
            message_type = atom
            format = 32
            data.setType(Array<NativeLong>::class.java)
            for (index in values.indices) data.l[index] = NativeLong(values[index])
        }
        val event = X11.XEvent().apply {
            setType(X11.XClientMessageEvent::class.java)
            xclient = message
            write()
        }
        val result = x11.XSendEvent(display, root, 0,
            NativeLong((X11.SubstructureRedirectMask or X11.SubstructureNotifyMask).toLong()), event)
        x11.XFlush(display)
        return result != 0
    }
}
