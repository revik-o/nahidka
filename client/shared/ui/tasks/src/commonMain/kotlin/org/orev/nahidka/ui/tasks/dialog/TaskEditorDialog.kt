package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.ChoiceField
import org.orev.nahidka.ui.common.component.DateField
import org.orev.nahidka.ui.tasks.component.TaskRatingReactions
import org.orev.nahidka.ui.tasks.component.title
import org.orev.nahidka.ui.tasks.model.TaskDraft

private const val TASK_DESCRIPTION_MINIMUM_LINES = 3
private const val TASK_DESCRIPTION_MAXIMUM_LINES = 6

@Composable
internal fun TaskEditorDialog(
    taskEditor: TaskDialogController<TaskDraft>,
    ratingLevels: List<TaskRatingLevel>,
) {
    TaskDialog(
        dialogController = taskEditor,
        title = { taskDraft ->
            stringResource(
                if (taskDraft.editedTask == null) Res.string.tasks_creation_title else Res.string.tasks_editing_title,
            )
        },
        confirmationTitle = stringResource(Res.string.tasks_action_save),
    ) { taskDraft ->
        TaskForm(taskDraft, ratingLevels, taskEditor::edit)
    }
}

@Composable
private fun TaskForm(
    taskDraft: TaskDraft,
    ratingLevels: List<TaskRatingLevel>,
    onTaskDraftEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    OutlinedTextField(
        value = taskDraft.title,
        onValueChange = { title -> onTaskDraftEdit { draft -> draft.copy(title = title) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.tasks_field_title)) },
        singleLine = true,
    )
    OutlinedTextField(
        value = taskDraft.description,
        onValueChange = { description -> onTaskDraftEdit { draft -> draft.copy(description = description) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.tasks_field_description)) },
        minLines = TASK_DESCRIPTION_MINIMUM_LINES,
        maxLines = TASK_DESCRIPTION_MAXIMUM_LINES,
    )
    ChoiceField(
        title = stringResource(Res.string.tasks_field_status),
        options = TaskStatus.entries,
        selectedOption = taskDraft.status,
        optionTitle = { status -> stringResource(status.title) },
        onOptionSelect = { status -> onTaskDraftEdit { draft -> draft.withStatus(status) } },
    )
    DateField(
        date = taskDraft.dueDate,
        title = stringResource(Res.string.tasks_field_due_date),
        onDateSelect = { dueDate -> onTaskDraftEdit { draft -> draft.copy(dueDate = dueDate) } },
        onDateClear = { onTaskDraftEdit { draft -> draft.copy(dueDate = null) } },
    )
    if (taskDraft.status.acceptsRating && ratingLevels.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.tasks_field_rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TaskRatingReactions(
                ratingLevels = ratingLevels,
                selectedRatingLevel = taskDraft.ratingLevel,
                onRatingLevelSelect = { ratingLevel ->
                    onTaskDraftEdit { draft -> draft.copy(ratingLevel = ratingLevel) }
                },
            )
        }
    }
}
