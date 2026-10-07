package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.component.*
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.component.TaskRatingReactions
import org.orev.nahidka.ui.tasks.component.title
import org.orev.nahidka.ui.tasks.model.TaskDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

@Composable
internal fun TaskEditorDialog(
    taskEditor: MutationDialogController<TaskDraft>,
    ratingLevels: List<TaskRatingLevel>,
) {
    MutationDialog(
        dialogController = taskEditor,
        title = { taskDraft ->
            stringResource(
                if (taskDraft.editedTask == null) Res.string.tasks_creation_title else Res.string.tasks_editing_title,
            )
        },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
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
    TitleField(taskDraft.title) { title -> onTaskDraftEdit { draft -> draft.copy(title = title) } }
    DescriptionField(taskDraft.description) { description ->
        onTaskDraftEdit { draft -> draft.copy(description = description) }
    }
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
        LabeledField(stringResource(Res.string.tasks_field_rating)) {
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
