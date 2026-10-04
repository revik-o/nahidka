package org.orev.nahidka.ui.tasks.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DataTable
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TasksContent

private val TASK_LIST_ACTIONS_WIDTH = 48.dp

@Composable
internal fun TasksList(
    tasksContent: TasksContent,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    DataTable(
        rows = tasksContent.taskItems,
        rowKey = { taskItem -> taskItem.task.identifier },
        emptyTableMessage = stringResource(Res.string.tasks_list_empty),
        header = if (layoutWidth == LayoutWidth.EXPANDED) {
            { TaskListHeader() }
        } else {
            null
        },
    ) { taskItem ->
        TaskListRow(taskItem, tasksContent.ratingLevels, layoutWidth, taskInteractions)
    }
}

@Composable
private fun TaskListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TaskListColumn.entries.forEach { taskListColumn ->
            Text(
                text = stringResource(taskListColumn.title),
                modifier = Modifier.weight(taskListColumn.weight),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(TASK_LIST_ACTIONS_WIDTH))
    }
}
