package org.orev.nahidka.ui.common.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_delete
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Item> DeletionDialog(
    deletion: MutationDialogController<Item>,
    title: String,
    message: @Composable (Item) -> String,
) {
    MutationDialog(
        dialogController = deletion,
        title = { title },
        confirmationTitle = stringResource(Res.string.common_action_delete),
    ) { item ->
        Text(message(item))
    }
}
