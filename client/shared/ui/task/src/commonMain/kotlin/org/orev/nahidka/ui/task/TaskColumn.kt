package org.orev.nahidka.ui.task

import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.TaskEntity

@Composable
fun TaskColumn(
    statusName: String,
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Note: To add ability to drag the tasks between TaskColumn, 
    // you would typically wrap items in a Draggable/DragTarget and the column in a DropTarget.
    Column(
        modifier = modifier
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Text(
            text = statusName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            items(tasks, key = { it.identifier }) { task ->
                TaskColumnItem(
                    task = task,
                    onClick = onTaskClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
fun TaskColumnPreview() {
    NahidkaTheme {
        TaskColumn(
            statusName = "To Do",
            tasks = listOf(
                TaskEntity(identifier = "1", title = "Mock Task 1", description = "Mock desc", status = org.orev.nahidka.api.TaskStatus.TO_DO, priority = 1),
                TaskEntity(identifier = "2", title = "Mock Task 2", description = "Mock desc", status = org.orev.nahidka.api.TaskStatus.TO_DO, priority = 2)
            ),
            onTaskClick = {}
        )
    }
}
