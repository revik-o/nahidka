package org.orev.nahidka.ui.common.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.orev.nahidka.ui.common.ResponsiveLayout

@Composable
fun NahidkaAppShell(
    modifier: Modifier = Modifier,
    sidebarHeader: (@Composable ColumnScope.() -> Unit)? = null,
    sidebarContent: @Composable ColumnScope.() -> Unit = {},
    bottomNavContent: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    ResponsiveLayout(
        mobileContent = {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NahidkaMobileBottomNav(content = bottomNavContent)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    content()
                }
            }
        },

        desktopContent = {
            Row(modifier = modifier.fillMaxSize()) {
                NahidkaDesktopSidebar(
                    header = sidebarHeader,
                    content = sidebarContent
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    )
}

@Preview
@Composable
fun NahidkaAppShellPreview() {
    NahidkaTheme {
        NahidkaAppShell(
            sidebarHeader = { Text("Header") },
            sidebarContent = { Text("Sidebar") },
            bottomNavContent = { Text("Bottom Nav") }
        ) {
            Text("Content")
        }
    }
}
