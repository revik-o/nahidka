package org.orev.nahidka.ui.common.shell

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NahidkaDesktopSidebar(
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    NavigationRail(
        modifier = modifier,
        header = header,
        content = content
    )
}

@Preview
@Composable
fun NahidkaDesktopSidebarPreview() {
    NahidkaTheme {
        NahidkaDesktopSidebar(
            header = { Text("Header") }
        ) {
            Text("Sidebar Content")
        }
    }
}
