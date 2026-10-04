package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import org.orev.nahidka.ui.common.component.ActionsMenu
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskStatusMenu(taskItem: TaskItem, taskInteractions: TaskInteractions) {
    ActionsMenu(taskStatusChangeActions(taskItem, taskInteractions)) { onMenuOpen ->
        TaskStatusBadge(taskItem.task.status, onClick = onMenuOpen)
    }
}
