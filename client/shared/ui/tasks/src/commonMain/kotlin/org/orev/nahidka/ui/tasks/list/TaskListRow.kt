package org.orev.nahidka.ui.tasks.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.TaskStatusMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskListRow(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { taskInteractions.onTaskEdit(taskItem) }
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (layoutWidth) {
            LayoutWidth.COMPACT -> Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TaskTitleText(taskItem)
                if (taskItem.task.description.isNotBlank()) {
                    TaskDescriptionText(taskItem)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    TaskStatusMenu(taskItem, taskInteractions)
                    taskItem.task.dueDate?.let { dueDate ->
                        DateText(dueDate)
                    }
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }

            LayoutWidth.EXPANDED -> {
                TaskTitleText(taskItem, Modifier.weight(TaskListColumn.TITLE.weight))
                TaskDescriptionText(taskItem, Modifier.weight(TaskListColumn.DESCRIPTION.weight))
                Box(Modifier.weight(TaskListColumn.STATUS.weight)) {
                    TaskStatusMenu(taskItem, taskInteractions)
                }
                Box(Modifier.weight(TaskListColumn.DUE_DATE.weight)) {
                    taskItem.task.dueDate?.let { dueDate ->
                        DateText(dueDate)
                    }
                }
                Box(Modifier.weight(TaskListColumn.RATING.weight)) {
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }
        }
        MoreActionsMenu(taskEditingActions(taskItem, taskInteractions))
    }
}

@Composable
private fun TaskTitleText(taskItem: TaskItem, modifier: Modifier = Modifier) {
    Text(
        text = taskItem.task.title,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TaskDescriptionText(taskItem: TaskItem, modifier: Modifier = Modifier) {
    Text(
        text = taskItem.task.description,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
