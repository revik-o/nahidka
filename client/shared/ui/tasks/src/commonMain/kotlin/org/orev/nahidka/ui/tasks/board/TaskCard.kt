package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.tasks.component.TaskDueDateText
import org.orev.nahidka.ui.tasks.component.TaskRatingMenu
import org.orev.nahidka.ui.tasks.component.taskEditingActions
import org.orev.nahidka.ui.tasks.component.taskStatusChangeActions
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

private const val TASK_CARD_DESCRIPTION_MAXIMUM_LINES = 3

@Composable
internal fun TaskCard(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    taskInteractions: TaskInteractions,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = { taskInteractions.onTaskEdit(taskItem) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = taskItem.task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MoreActionsMenu(
                    menuActions = taskStatusChangeActions(taskItem, taskInteractions) +
                        taskEditingActions(taskItem, taskInteractions),
                )
            }
            if (taskItem.task.description.isNotBlank()) {
                Text(
                    text = taskItem.task.description,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = TASK_CARD_DESCRIPTION_MAXIMUM_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (taskItem.task.dueDate != null || taskItem.task.status.acceptsRating) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    taskItem.task.dueDate?.let { dueDate ->
                        TaskDueDateText(dueDate)
                    }
                    Spacer(Modifier.weight(1f))
                    TaskRatingMenu(taskItem, ratingLevels, taskInteractions)
                }
            }
        }
    }
}
