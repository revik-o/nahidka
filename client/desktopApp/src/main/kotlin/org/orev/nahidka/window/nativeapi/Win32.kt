package org.orev.nahidka.window.nativeapi

import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTRByReference
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser.HHOOK
import com.sun.jna.platform.win32.WinUser.HOOKPROC
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

internal interface Win32 : StdCallLibrary {
    fun RegisterWindowMessageW(name: WString): Int
    fun SendMessageTimeoutW(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM,
                            flags: Int, timeout: Int, result: ULONG_PTRByReference): LRESULT
    fun ScreenToClient(hwnd: HWND, point: POINT): Boolean
    fun MapWindowPoints(from: HWND?, to: HWND?, point: POINT, count: Int): Int
    fun DefWindowProcW(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM): LRESULT
    fun TrackMouseEvent(event: MouseTracking): Boolean
    fun SetWindowsHookExW(kind: Int, callback: CallWindowHook, module: HINSTANCE?, thread: Int): HHOOK?

    interface CallWindowHook : HOOKPROC {
        fun callback(code: Int, wParam: WPARAM, lParam: LPARAM): LRESULT
    }

    @Structure.FieldOrder("cbSize", "dwFlags", "hwndTrack", "dwHoverTime")
    class MouseTracking : Structure() {
        @JvmField var cbSize = 0
        @JvmField var dwFlags = 0
        @JvmField var hwndTrack: HWND? = null
        @JvmField var dwHoverTime = 0
        init { cbSize = size() }
    }

    companion object {
        val api: Win32 by lazy { Native.load("user32", Win32::class.java, W32APIOptions.UNICODE_OPTIONS) }
    }
}
