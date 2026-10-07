package org.orev.nahidka.shell

import androidx.compose.runtime.Composable
import org.orev.nahidka.ui.common.layout.LayoutWidth

data class ApplicationTopBarState(
    val title: String,
    val layoutWidth: LayoutWidth,
    val onNavigateBack: (() -> Unit)? = null,
)

typealias ApplicationTopBarRenderer = @Composable (
    applicationTopBarState: ApplicationTopBarState,
    applicationTopBarActions: @Composable () -> Unit,
) -> Unit
