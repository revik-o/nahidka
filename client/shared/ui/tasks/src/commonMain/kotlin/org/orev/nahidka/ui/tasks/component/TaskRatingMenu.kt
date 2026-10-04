package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_rating_rate
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.DropdownPopup
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskRatingMenu(
    taskItem: TaskItem,
    ratingLevels: List<TaskRatingLevel>,
    taskInteractions: TaskInteractions,
) {
    if (!taskItem.task.status.acceptsRating || ratingLevels.isEmpty()) {
        return
    }

    DropdownPopup(
        trigger = { onPopupOpen ->
            TextButton(onClick = onPopupOpen) {
                Text(
                    text = taskItem.ratingLevel?.reaction ?: stringResource(Res.string.tasks_rating_rate),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { onPopupClose ->
        TaskRatingReactions(
            ratingLevels = ratingLevels,
            selectedRatingLevel = taskItem.ratingLevel,
            onRatingLevelSelect = { ratingLevel ->
                onPopupClose()
                taskInteractions.onTaskRate(taskItem, ratingLevel)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
