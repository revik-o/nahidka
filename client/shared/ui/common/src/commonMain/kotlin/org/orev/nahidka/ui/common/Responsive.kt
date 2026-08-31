package org.orev.nahidka.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize {
    COMPACT, MEDIUM, EXPANDED
}

@Composable
fun rememberWindowSizeClass(width: Dp): WindowSize {
    return when {
        width < 600.dp -> WindowSize.COMPACT
        width < 840.dp -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

@Composable
fun ResponsiveLayout(
    mobileContent: @Composable BoxWithConstraintsScope.() -> Unit,
    desktopContent: @Composable BoxWithConstraintsScope.() -> Unit
) {
    BoxWithConstraints {
        val sizeClass = rememberWindowSizeClass(maxWidth)
        if (sizeClass == WindowSize.EXPANDED) {
            desktopContent()
        } else {
            mobileContent()
        }
    }
}
