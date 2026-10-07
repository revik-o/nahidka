package org.orev.nahidka.ui.goal.dialog

import androidx.compose.runtime.Composable
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_deletion_message
import nahidka.shared.ui.goal.generated.resources.goal_deletion_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.dialog.DeletionDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController

@Composable
internal fun GoalDeletionDialog(goalDeletion: MutationDialogController<GoalRecord>) {
    DeletionDialog(goalDeletion, stringResource(Res.string.goal_deletion_title)) { goal ->
        stringResource(Res.string.goal_deletion_message, goal.title)
    }
}
