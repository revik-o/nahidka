package org.orev.nahidka.window

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTRByReference
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import java.awt.EventQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import org.orev.nahidka.window.nativeapi.Win32
import org.orev.nahidka.window.nativeapi.Win32Subclass

internal class WindowsWindowChrome(window: ComposeWindow, controller: DesktopWindowController) :
    JbrWindowChrome(window, controller, controlsAreNative = false) {
    override val usesNativeMaximizeHover = true
    private val user = User32.INSTANCE
    private val api = Win32.api
    private val subclass = Win32Subclass.api
    private val token = sequence.incrementAndGet()
    private val id = ULONG_PTR(token)
    private val privateMessage = api.RegisterWindowMessageW(WString("Nahidka.WindowChrome.OwnerThread"))
    private var root: HWND? = null
    private var thread = 0
    private val installed = ConcurrentHashMap<Long, HWND>()
    private val retiring = AtomicBoolean(false)
    private val reported = AtomicBoolean(false)
    private val origin = AtomicReference(Offset.Zero)
    private val bootstraps = ConcurrentHashMap<Long, BootstrapHook>()
    private var nativePressed: WindowControl? = null
    private val procedure = object : Win32Subclass.Procedure {
        override fun callback(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM,
                              id: ULONG_PTR, data: ULONG_PTR): LRESULT {
            return try {
                handle(hwnd, message, wParam, lParam)
            } catch (failure: Throwable) {
                report(failure)
                subclass.DefSubclassProc(hwnd, message, wParam, lParam)
            }
        }
    }

    override fun onPeerReady() {
        check(privateMessage != 0)
        val hwnd = user.GetAncestor(HWND(Native.getComponentPointer(window)), 2)
        check(hwnd != null && Pointer.nativeValue(hwnd.pointer) != 0L)
        val pid = IntByReference()
        thread = user.GetWindowThreadProcessId(hwnd, pid)
        check(thread != 0 && pid.value == Kernel32.INSTANCE.GetCurrentProcessId())
        root = hwnd
        retained[token] = this
        try {
            ownerOperation(false)
            check(installed.containsKey(address(hwnd))) { "Windows subclass installation did not complete" }
        } catch (failure: Throwable) {
            retiring.set(true)
            runCatching { ownerOperation(true) }
            if (installed.isEmpty() && bootstraps.isEmpty()) retained.remove(token)
            throw failure
        }
    }

    override fun refresh() {
        super.refresh()
        if (closed) return
        val point = SwingUtilities.convertPoint(window.contentPane, 0, 0, window)
        val insets = window.insets
        val scale = window.graphicsConfiguration.defaultTransform
        origin.set(Offset(((point.x - insets.left) * scale.scaleX).toFloat(),
            ((point.y - insets.top) * scale.scaleY).toFloat()))
    }

    private fun address(hwnd: HWND) = Pointer.nativeValue(hwnd.pointer)

    private fun register(hwnd: HWND) {
        val pid = IntByReference()
        if (retiring.get() || user.GetWindowThreadProcessId(hwnd, pid) != thread ||
            pid.value != Kernel32.INSTANCE.GetCurrentProcessId()) return
        if (installed.containsKey(address(hwnd))) return
        check(subclass.SetWindowSubclass(hwnd, procedure, id, id)) { "SetWindowSubclass failed" }
        installed[address(hwnd)] = hwnd
    }

    private fun registerChildren() {
        val hwnd = root ?: return
        val failure = AtomicReference<Throwable?>(null)
        user.EnumChildWindows(hwnd, { child, _ ->
            try {
                register(child)
                true
            } catch (error: Throwable) {
                failure.set(error)
                false
            }
        }, null)
        failure.get()?.let { throw it }
    }

    private fun removeAll() {
        installed.values.toList().forEach { hwnd ->
            if (!user.IsWindow(hwnd) || subclass.RemoveWindowSubclass(hwnd, procedure, id)) installed.remove(address(hwnd))
        }
        if (installed.isEmpty() && bootstraps.isEmpty()) retained.remove(token)
    }

    private fun ownerOperation(remove: Boolean) {
        val hwnd = root ?: return
        val acknowledged = AtomicBoolean(false)
        val failure = AtomicReference<Throwable?>(null)
        val hook = object : Win32.CallWindowHook {
            override fun callback(code: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
                try {
                    if (code >= 0) {
                        val message = WinUser.CWPSTRUCT(Pointer(lParam.toLong()))
                        if (message.hwnd == hwnd && message.message == privateMessage &&
                            message.wParam.toLong() == token && message.lParam.toLong() == (if (remove) 1L else 0L)) {
                            if (!remove && retiring.get()) return user.CallNextHookEx(null, code, wParam, lParam)
                            if (remove) removeAll() else {
                                register(hwnd)
                                registerChildren()
                            }
                            acknowledged.set(true)
                        }
                    }
                } catch (error: Throwable) {
                    failure.set(error)
                    report(error)
                }
                return user.CallNextHookEx(null, code, wParam, lParam)
            }
        }
        val handle = api.SetWindowsHookExW(4, hook, null, thread)
        check(handle != null && Pointer.nativeValue(handle.pointer) != 0L) { "Windows owner-thread hook failed" }
        val hookAddress = Pointer.nativeValue(handle.pointer)
        bootstraps[hookAddress] = BootstrapHook(handle, hook)
        try {
            val result = api.SendMessageTimeoutW(hwnd, privateMessage, WPARAM(token), LPARAM(if (remove) 1L else 0L),
                2, 2000, ULONG_PTRByReference())
            check(result.toLong() != 0L && acknowledged.get()) { "Windows owner-thread operation timed out" }
            failure.get()?.let { throw it }
            if (remove) check(installed.isEmpty()) { "Windows subclass removal was not acknowledged" }
        } finally {
            if (user.UnhookWindowsHookEx(handle)) {
                bootstraps.remove(hookAddress)
                if (installed.isEmpty() && bootstraps.isEmpty()) retained.remove(token)
            } else {
                val failure = IllegalStateException("Windows bootstrap hook removal failed")
                report(failure)
                throw failure
            }
        }
    }

    private fun point(lParam: LPARAM): Offset? {
        val hwnd = root ?: return null
        val value = lParam.toLong()
        val point = POINT((value and 0xffff).toShort().toInt(), ((value ushr 16) and 0xffff).toShort().toInt())
        if (!api.ScreenToClient(hwnd, point)) return null
        val offset = origin.get()
        return Offset(point.x.toFloat() - offset.x, point.y.toFloat() - offset.y)
    }

    private fun hitControl(hit: Int): WindowControl? = when (hit) {
        8 -> WindowControl.Minimize
        9 -> WindowControl.Maximize
        20 -> WindowControl.Close
        else -> null
    }

    private fun handle(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (message == 0x82) {
            subclass.RemoveWindowSubclass(hwnd, procedure, id)
            installed.remove(address(hwnd))
            if (hwnd == root) {
                retiring.set(true)
                EventQueue.invokeLater { close() }
            }
            if (installed.isEmpty() && bootstraps.isEmpty()) retained.remove(token)
            return subclass.DefSubclassProc(hwnd, message, wParam, lParam)
        }
        if (retiring.get()) return subclass.DefSubclassProc(hwnd, message, wParam, lParam)
        if (hwnd == root && message == 0x210) registerChildren()
        val snapshot = geometry.get()
        val enabled = root?.let { user.IsWindowEnabled(it) } == true
        if (message == 0x84) {
            val original = subclass.DefSubclassProc(hwnd, message, wParam, lParam)
            if (hwnd == root && original.toInt() in 10..17) return original
            if (!enabled) return original
            val point = point(lParam) ?: return original
            if (!snapshot.inputEnabled || !snapshot.ready) {
                return if (snapshot.header.contains(point)) LRESULT(1) else original
            }
            val control = snapshot.controls.entries.firstOrNull { it.value.contains(point) }?.key
            val caption = snapshot.header.contains(point) && control == null && !snapshot.fullscreen
            val border = hwnd != root && original.toInt() in 10..17 && !snapshot.fullscreen
            if (hwnd != root) return if (caption || control in snapshot.enabled || border) LRESULT(-1) else original
            return when {
                control in snapshot.enabled -> LRESULT(when (control) {
                    WindowControl.Minimize -> 8L
                    WindowControl.Maximize -> 9L
                    WindowControl.Close -> 20L
                    else -> 1L
                })
                control != null || snapshot.fullscreen && snapshot.header.contains(point) -> LRESULT(1)
                caption -> LRESULT(2)
                else -> original
            }
        }
        if (hwnd == root) {
            if (message == 0x212) publishMenu(false)
            if (message == 0x211) publishMenu(true)
            if (message == 0x1F || message == 0x215 || message == 0x86 && wParam.toLong() == 0L) {
                nativePressed = null
                publishInteraction(NativeControlInteraction())
            }
            if (message == 0x2A2) publishInteraction(NativeControlInteraction())
            val control = hitControl(wParam.toInt())
            if (message == 0xA0 && (!enabled || control !in snapshot.enabled)) {
                publishInteraction(NativeControlInteraction())
            }
            if (enabled && control in snapshot.enabled && message in setOf(0xA0, 0xA1, 0xA2, 0xA3)) {
                if (message == 0xA0) {
                    api.TrackMouseEvent(Win32.MouseTracking().apply { dwFlags = 0x12; hwndTrack = hwnd })
                }
                if (message == 0xA1) nativePressed = control
                if (message == 0xA2) nativePressed = null
                publishInteraction(NativeControlInteraction(control, if (nativePressed == control) control else null))
                val result = api.DefWindowProcW(hwnd, message, wParam, lParam)
                if (message != 0xA0) {
                    nativePressed = null
                    publishInteraction(NativeControlInteraction())
                }
                return result
            }
        }
        return subclass.DefSubclassProc(hwnd, message, wParam, lParam)
    }

    private fun publishMenu(value: Boolean) {
        EventQueue.invokeLater { if (!closed) nativeMenuOpen = value }
    }

    private fun report(failure: Throwable) {
        if (reported.compareAndSet(false, true)) System.err.println("Nahidka Windows integration: ${failure.message}")
    }

    override fun close() {
        if (closed) return
        retiring.set(true)
        if (installed.isNotEmpty()) ownerOperation(true)
        bootstraps.entries.toList().forEach { (address, hook) ->
            if (user.UnhookWindowsHookEx(hook.handle)) bootstraps.remove(address)
        }
        check(bootstraps.isEmpty()) { "Windows bootstrap hook removal was not acknowledged" }
        retained.remove(token)
        super.close()
    }

    private data class BootstrapHook(val handle: WinUser.HHOOK, val callback: Win32.CallWindowHook)

    companion object {
        private val sequence = AtomicLong()
        private val retained = ConcurrentHashMap<Long, WindowsWindowChrome>()
    }
}
