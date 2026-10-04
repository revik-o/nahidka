package org.orev.nahidka.ui.common.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ActionsMenu(
    menuActions: List<MenuAction>,
    trigger: @Composable (onMenuOpen: () -> Unit) -> Unit,
) {
    DropdownPopup(trigger) { onPopupClose ->
        menuActions.forEach { menuAction ->
            DropdownMenuItem(
                text = { Text(menuAction.title) },
                onClick = {
                    onPopupClose()
                    menuAction.onSelect()
                },
            )
        }
    }
}
