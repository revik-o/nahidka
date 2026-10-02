package org.orev.nahidka.window

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.orev.nahidka.ui.common.theme.NahidkaOnBackground
import org.orev.nahidka.ui.common.theme.NahidkaSurface

@Composable
internal fun LinuxWindowMenu(
    position: IntOffset,
    controller: DesktopWindowController,
    enabled: Set<WindowControl>,
    canMove: Boolean,
    canResize: Boolean,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onResize: () -> Unit,
) {
    Popup(offset = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(Modifier.width(180.dp).background(NahidkaSurface).padding(vertical = 4.dp)) {
            MenuItem("Restore", controller.isMaximized && WindowControl.Maximize in enabled) { onDismiss(); controller.restore() }
            MenuItem("Move", canMove, onMove)
            MenuItem("Resize", canResize, onResize)
            MenuItem("Minimize", WindowControl.Minimize in enabled) { onDismiss(); controller.minimize() }
            MenuItem("Maximize", !controller.isMaximized && WindowControl.Maximize in enabled) { onDismiss(); controller.toggleMaximize() }
            MenuItem("Close", WindowControl.Close in enabled) { onDismiss(); controller.close() }
        }
    }
}

@Composable
private fun MenuItem(label: String, enabled: Boolean, action: () -> Unit) {
    Text(label, color = NahidkaOnBackground.copy(alpha = if (enabled) 1f else 0.35f),
        modifier = Modifier.width(180.dp).clickable(enabled = enabled, role = Role.Button, onClick = action)
            .padding(horizontal = 12.dp, vertical = 8.dp))
}
