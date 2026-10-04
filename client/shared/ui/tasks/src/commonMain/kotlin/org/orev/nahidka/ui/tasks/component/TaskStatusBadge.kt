package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus

@Composable
internal fun TaskStatusBadge(status: TaskStatus, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = status.containerColor,
        contentColor = status.contentColor,
    ) {
        Text(
            text = stringResource(status.title)
                .uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TaskStatus.containerColor: Color
    @Composable
    get() = when (this) {
        TaskStatus.TO_DO -> MaterialTheme.colorScheme.surfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
        TaskStatus.DONE -> MaterialTheme.colorScheme.tertiaryContainer
    }

private val TaskStatus.contentColor: Color
    @Composable
    get() = when (this) {
        TaskStatus.TO_DO -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
        TaskStatus.DONE -> MaterialTheme.colorScheme.onTertiaryContainer
    }
