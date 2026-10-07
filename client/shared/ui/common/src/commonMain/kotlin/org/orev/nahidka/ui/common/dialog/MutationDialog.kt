package org.orev.nahidka.ui.common.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_error_unsaved
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Draft> MutationDialog(
    dialogController: MutationDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    ControlledDialog(
        dialogController = dialogController,
        title = title,
        confirmationTitle = confirmationTitle,
        rejectionText = { stringResource(Res.string.common_error_unsaved) },
        content = content,
    )
}
