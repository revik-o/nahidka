package org.orev.nahidka.ui.common.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val COMPACT_LAYOUT_WIDTH_LIMIT = 600.dp

enum class LayoutWidth(val screenPadding: Dp) {
    COMPACT(16.dp),
    EXPANDED(24.dp);

    companion object {

        fun of(availableWidth: Dp): LayoutWidth =
            if (availableWidth < COMPACT_LAYOUT_WIDTH_LIMIT) COMPACT else EXPANDED
    }
}
