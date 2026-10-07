package org.orev.nahidka.shell

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private const val SELECTED_CONTAINER_ALPHA = 0.16f

internal object NavigationSelectionColors {

    val container: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_CONTAINER_ALPHA)

    val content: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
}
