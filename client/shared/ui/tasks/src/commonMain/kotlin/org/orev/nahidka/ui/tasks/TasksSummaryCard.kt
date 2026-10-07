package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_list_empty
import nahidka.shared.ui.tasks.generated.resources.tasks_summary_all_done
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.SupportingText
import org.orev.nahidka.ui.common.component.SummaryCard
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.tasks.component.title

private const val SUMMARY_TASK_LIMIT = 3

@Composable
fun TasksSummaryCard(
    tasksViewModel: TasksViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tasksContent by tasksViewModel.tasksContent.collectAsStateWithLifecycle()
    val upcomingTaskItems = tasksContent.upcomingTaskItems.take(SUMMARY_TASK_LIMIT)

    SummaryCard(
        destination = ApplicationDestination.TASKS,
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskStatus.entries.forEach { status ->
                Column(Modifier.weight(1f)) {
                    Text(
                        text = tasksContent
                            .taskItemsWithStatus(status)
                            .size
                            .toString(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    SupportingText(
                        text = stringResource(status.title),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
        if (upcomingTaskItems.isEmpty()) {
            SupportingText(
                stringResource(if (tasksContent.taskItems.isEmpty()) Res.string.tasks_list_empty else Res.string.tasks_summary_all_done),
            )
        }
        upcomingTaskItems.forEach { taskItem ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = taskItem.task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                taskItem.task.dueDate?.let { dueDate ->
                    DateText(dueDate)
                }
            }
        }
    }
}
