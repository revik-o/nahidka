package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.dialog.ControlledDialog

@Composable
internal fun <Draft> TaskDialog(
    dialogController: TaskDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    ControlledDialog(
        dialogController = dialogController,
        title = title,
        confirmationTitle = confirmationTitle,
        rejectionText = { stringResource(Res.string.tasks_error_unsaved) },
        content = content,
    )
}
