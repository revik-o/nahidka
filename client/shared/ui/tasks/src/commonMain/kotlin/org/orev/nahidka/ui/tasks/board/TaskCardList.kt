package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_column_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskCardList(
    taskItems: List<TaskItem>,
    modifier: Modifier = Modifier,
    taskCard: @Composable (TaskItem) -> Unit,
) {
    if (taskItems.isEmpty()) {
        Text(
            text = stringResource(Res.string.tasks_column_empty),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(taskItems, key = { taskItem -> taskItem.task.identifier }) { taskItem ->
                taskCard(taskItem)
            }
        }
    }
}
