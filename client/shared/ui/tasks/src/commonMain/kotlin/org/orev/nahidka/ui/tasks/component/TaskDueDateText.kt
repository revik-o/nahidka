package org.orev.nahidka.ui.tasks.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import org.orev.nahidka.ui.common.format.DAY_FORMAT

@Composable
internal fun TaskDueDateText(dueDate: LocalDate, modifier: Modifier = Modifier) {
    Text(
        text = "📅 ${dueDate.format(DAY_FORMAT)}",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}
