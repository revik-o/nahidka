package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_message
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.dialog.DeletionDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskDeletionDialog(taskDeletion: MutationDialogController<TaskItem>) {
    DeletionDialog(taskDeletion, stringResource(Res.string.tasks_deletion_title)) { taskItem ->
        stringResource(Res.string.tasks_deletion_message, taskItem.task.title)
    }
}
