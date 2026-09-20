package org.orev.nahidka.ui.task

import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.TaskEntity

enum class TaskSortField { TITLE, STATUS, PRIORITY }

@Composable
fun TasksTable(
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortField by remember { mutableStateOf(TaskSortField.TITLE) }
    var sortAscending by remember { mutableStateOf(true) }

    val sortedTasks = remember(tasks, sortField, sortAscending) {
        val comparator = when (sortField) {
            TaskSortField.TITLE -> compareBy<TaskEntity> { it.title }
            TaskSortField.STATUS -> compareBy { it.status }
            TaskSortField.PRIORITY -> compareBy { it.priority }
        }
        if (sortAscending) tasks.sortedWith(comparator) else tasks.sortedWith(comparator.reversed())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderCell("Title", TaskSortField.TITLE, sortField, sortAscending) { 
                if (sortField == TaskSortField.TITLE) sortAscending = !sortAscending else { sortField = TaskSortField.TITLE; sortAscending = true }
            }
            HeaderCell("Status", TaskSortField.STATUS, sortField, sortAscending) { 
                if (sortField == TaskSortField.STATUS) sortAscending = !sortAscending else { sortField = TaskSortField.STATUS; sortAscending = true }
            }
            HeaderCell("Priority", TaskSortField.PRIORITY, sortField, sortAscending) { 
                if (sortField == TaskSortField.PRIORITY) sortAscending = !sortAscending else { sortField = TaskSortField.PRIORITY; sortAscending = true }
            }
        }
        Divider()
        
        // Table Body
        LazyColumn {
            items(sortedTasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTaskClick(task) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(task.title, modifier = Modifier.weight(1f))
                    Text(task.status, modifier = Modifier.weight(1f))
                    Text(task.priority.toString(), modifier = Modifier.weight(1f))
                }
                Divider()
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    field: TaskSortField,
    currentSortField: TaskSortField,
    sortAscending: Boolean,
    onClick: () -> Unit
) {
    val sortIcon = if (currentSortField == field) {
        if (sortAscending) " ▲" else " ▼"
    } else ""
    
    Text(
        text = text + sortIcon,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    )
}

@Preview
@Composable
fun TasksTablePreview() {
    NahidkaTheme {
        TasksTable(
            tasks = listOf(
                TaskEntity(id = "1", title = "Mock Task 1", description = "Mock desc", status = "To Do", priority = 1),
                TaskEntity(id = "2", title = "Mock Task 2", description = "Mock desc", status = "In Progress", priority = 2)
            ),
            onTaskClick = {}
        )
    }
}

@Preview
@Composable
fun HeaderCellPreview() {
    NahidkaTheme {
        Row {
            HeaderCell(
                text = "Mock Header",
                field = TaskSortField.TITLE,
                currentSortField = TaskSortField.TITLE,
                sortAscending = true,
                onClick = {}
            )
        }
    }
}
