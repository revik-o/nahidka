package org.orev.nahidka.ui.common.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_delete
import nahidka.shared.ui.common.generated.resources.common_action_edit
import org.jetbrains.compose.resources.stringResource

@Composable
fun editingMenuActions(onEdit: () -> Unit, onDelete: () -> Unit): List<MenuAction> = listOf(
    MenuAction(stringResource(Res.string.common_action_edit), onEdit),
    MenuAction(stringResource(Res.string.common_action_delete), onDelete),
)
