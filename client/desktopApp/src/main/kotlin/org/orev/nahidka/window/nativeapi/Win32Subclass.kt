package org.orev.nahidka.window.nativeapi

import com.sun.jna.Native
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.win32.StdCallLibrary

internal interface Win32Subclass : StdCallLibrary {
    fun SetWindowSubclass(hwnd: HWND, callback: Procedure, id: ULONG_PTR, data: ULONG_PTR): Boolean
    fun RemoveWindowSubclass(hwnd: HWND, callback: Procedure, id: ULONG_PTR): Boolean
    fun DefSubclassProc(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM): LRESULT

    interface Procedure : StdCallLibrary.StdCallCallback {
        fun callback(hwnd: HWND, message: Int, wParam: WPARAM, lParam: LPARAM,
                     id: ULONG_PTR, data: ULONG_PTR): LRESULT
    }

    companion object {
        val api: Win32Subclass by lazy { Native.load("comctl32", Win32Subclass::class.java) }
    }
}
