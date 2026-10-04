package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.component.taskStatusTitleWithCount

@Composable
internal fun TaskBoardColumn(
    status: TaskStatus,
    taskCount: Int,
    dropTarget: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (dropTarget) 2.dp else 1.dp,
            color = if (dropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(taskStatusTitleWithCount(status, taskCount), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
