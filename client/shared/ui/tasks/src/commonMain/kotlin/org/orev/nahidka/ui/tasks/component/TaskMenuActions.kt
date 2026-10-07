package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.editingMenuActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun taskEditingActions(taskItem: TaskItem, taskInteractions: TaskInteractions): List<MenuAction> =
    editingMenuActions(
        onEdit = { taskInteractions.onTaskEdit(taskItem) },
        onDelete = { taskInteractions.onTaskDelete(taskItem) },
    )

@Composable
internal fun taskStatusChangeActions(taskItem: TaskItem, taskInteractions: TaskInteractions): List<MenuAction> =
    TaskStatus.entries
        .filter { status -> status != taskItem.task.status }
        .map { status ->
            MenuAction(stringResource(Res.string.tasks_action_move_to, stringResource(status.title))) {
                taskInteractions.onTaskMove(taskItem, status)
            }
        }
