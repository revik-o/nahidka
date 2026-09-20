package org.orev.nahidka.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
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
    tabletContent: (@Composable BoxWithConstraintsScope.() -> Unit)? = null,
    desktopContent: (@Composable BoxWithConstraintsScope.() -> Unit)? = null
) {
    BoxWithConstraints {
        val sizeClass = rememberWindowSizeClass(maxWidth)
        when (sizeClass) {
            WindowSize.EXPANDED -> {
                if (desktopContent != null) desktopContent()
                else if (tabletContent != null) tabletContent()
                else mobileContent()
            }
            WindowSize.MEDIUM -> {
                if (tabletContent != null) tabletContent()
                else if (desktopContent != null) desktopContent()
                else mobileContent()
            }
            WindowSize.COMPACT -> {
                mobileContent()
            }
        }
    }
}

@Preview
@Composable
fun rememberWindowSizeClassPreview() {
    NahidkaTheme {
        Text(text = rememberWindowSizeClass(400.dp).name)
    }
}

@Preview
@Composable
fun ResponsiveLayoutPreview() {
    NahidkaTheme {
        ResponsiveLayout(
            mobileContent = { Text("Mobile") },
            tabletContent = { Text("Tablet") },
            desktopContent = { Text("Desktop") }
        )
    }
}
