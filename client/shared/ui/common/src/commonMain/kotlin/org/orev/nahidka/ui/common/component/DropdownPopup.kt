package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*

@Composable
fun DropdownPopup(
    trigger: @Composable (onPopupOpen: () -> Unit) -> Unit,
    content: @Composable ColumnScope.(onPopupClose: () -> Unit) -> Unit,
) {
    var popupExpanded by remember { mutableStateOf(false) }

    Box {
        trigger { popupExpanded = true }
        DropdownMenu(expanded = popupExpanded, onDismissRequest = { popupExpanded = false }) {
            content { popupExpanded = false }
        }
    }
}
