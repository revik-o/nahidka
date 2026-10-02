package org.orev.nahidka.window

import java.awt.Cursor

internal enum class ResizeEdge(val protocolValue: Int, val cursor: Int) {
    NorthWest(0, Cursor.NW_RESIZE_CURSOR), North(1, Cursor.N_RESIZE_CURSOR),
    NorthEast(2, Cursor.NE_RESIZE_CURSOR), East(3, Cursor.E_RESIZE_CURSOR),
    SouthEast(4, Cursor.SE_RESIZE_CURSOR), South(5, Cursor.S_RESIZE_CURSOR),
    SouthWest(6, Cursor.SW_RESIZE_CURSOR), West(7, Cursor.W_RESIZE_CURSOR);

    val west get() = this == West || this == NorthWest || this == SouthWest
    val east get() = this == East || this == NorthEast || this == SouthEast
    val north get() = this == North || this == NorthWest || this == NorthEast
    val south get() = this == South || this == SouthWest || this == SouthEast
}

internal fun resizeEdge(x: Float, y: Float, width: Float, height: Float, inset: Float): ResizeEdge? {
    if (inset <= 0 || x < 0 || y < 0 || x >= width || y >= height) return null
    val left = x < inset
    val right = x >= width - inset
    val top = y < inset
    val bottom = y >= height - inset
    return when {
        (left && y < 2 * inset) || (top && x < 2 * inset) -> ResizeEdge.NorthWest
        (right && y < 2 * inset) || (top && x >= width - 2 * inset) -> ResizeEdge.NorthEast
        (left && y >= height - 2 * inset) || (bottom && x < 2 * inset) -> ResizeEdge.SouthWest
        (right && y >= height - 2 * inset) || (bottom && x >= width - 2 * inset) -> ResizeEdge.SouthEast
        left -> ResizeEdge.West
        right -> ResizeEdge.East
        top -> ResizeEdge.North
        bottom -> ResizeEdge.South
        else -> null
    }
}
