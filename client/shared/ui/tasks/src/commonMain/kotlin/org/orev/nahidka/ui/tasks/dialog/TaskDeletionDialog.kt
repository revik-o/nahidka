package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_action_delete
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_message
import nahidka.shared.ui.tasks.generated.resources.tasks_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskDeletionDialog(taskDeletion: TaskDialogController<TaskItem>) {
    TaskDialog(
        dialogController = taskDeletion,
        title = { stringResource(Res.string.tasks_deletion_title) },
        confirmationTitle = stringResource(Res.string.tasks_action_delete),
    ) { taskItem ->
        Text(stringResource(Res.string.tasks_deletion_message, taskItem.task.title))
    }
}
