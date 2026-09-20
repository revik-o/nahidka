package org.orev.nahidka.ui.common.shell

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NahidkaMobileBottomNav(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier,
        content = content
    )
}

@Preview
@Composable
fun NahidkaMobileBottomNavPreview() {
    NahidkaTheme {
        NahidkaMobileBottomNav {
            Text("Bottom Nav Item")
        }
    }
}
