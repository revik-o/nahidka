package org.orev.nahidka.ui.goal.dialog

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.goal.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.DateField
import org.orev.nahidka.ui.common.component.DescriptionField
import org.orev.nahidka.ui.common.component.LabeledField
import org.orev.nahidka.ui.common.component.TitleField
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.goal.model.GoalDraft
import kotlin.math.roundToInt
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

private const val GOAL_PROGRESS_STEP_PERCENTAGE = 5f
private val GOAL_PROGRESS_SLIDER_STEPS =
    (GoalRecord.PROGRESS_PERCENTAGE_RANGE.endInclusive / GOAL_PROGRESS_STEP_PERCENTAGE).roundToInt() - 1

@Composable
internal fun GoalEditorDialog(goalEditor: MutationDialogController<GoalDraft>) {
    MutationDialog(
        dialogController = goalEditor,
        title = { goalDraft ->
            stringResource(
                if (goalDraft.editedGoal == null) Res.string.goal_creation_title else Res.string.goal_editing_title,
            )
        },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
    ) { goalDraft ->
        GoalForm(goalDraft, goalEditor::edit)
    }
}

@Composable
private fun GoalForm(goalDraft: GoalDraft, onGoalDraftEdit: ((GoalDraft) -> GoalDraft) -> Unit) {
    TitleField(goalDraft.title) { title -> onGoalDraftEdit { draft -> draft.copy(title = title) } }
    DescriptionField(goalDraft.description) { description ->
        onGoalDraftEdit { draft -> draft.copy(description = description) }
    }
    GoalPictureField(goalDraft, onGoalDraftEdit)
    LabeledField(stringResource(Res.string.goal_field_progress, goalDraft.progressPercentage.roundToInt())) {
        Slider(
            value = goalDraft.progressPercentage,
            onValueChange = { progressPercentage ->
                onGoalDraftEdit { draft -> draft.copy(progressPercentage = progressPercentage) }
            },
            valueRange = GoalRecord.PROGRESS_PERCENTAGE_RANGE,
            steps = GOAL_PROGRESS_SLIDER_STEPS,
        )
    }
    DateField(
        date = goalDraft.deadlineDate,
        title = stringResource(Res.string.goal_field_deadline),
        onDateSelect = { deadlineDate -> onGoalDraftEdit { draft -> draft.copy(deadlineDate = deadlineDate) } },
        onDateClear = { onGoalDraftEdit { draft -> draft.copy(deadlineDate = null) } },
    )
}
