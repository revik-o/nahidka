package org.orev.nahidka.ui.tasks.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.common_action_delete
import nahidka.shared.ui.common.generated.resources.common_action_save
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.dialog.MutationDialog
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.tasks.model.TaskRatingLevelsDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

@Composable
internal fun TaskRatingLevelsDialog(
    ratingLevelsEditor: MutationDialogController<TaskRatingLevelsDraft>,
    onRatingLevelAdd: () -> Unit,
) {
    MutationDialog(
        dialogController = ratingLevelsEditor,
        title = { stringResource(Res.string.tasks_rating_levels) },
        confirmationTitle = stringResource(CommonResources.string.common_action_save),
    ) { ratingLevelsDraft ->
        Text(
            text = stringResource(Res.string.tasks_rating_levels_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ratingLevelsDraft.ratingLevels.forEachIndexed { ratingLevelIndex, ratingLevel ->
            key(ratingLevel.identifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ratingLevel.reaction,
                        onValueChange = { reaction ->
                            ratingLevelsEditor.edit { draft -> draft.replacingReaction(ratingLevelIndex, reaction) }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.tasks_rating_level_title, ratingLevelIndex + 1)) },
                        singleLine = true,
                    )
                    MoreActionsMenu(ratingLevelActions(ratingLevelsDraft, ratingLevelIndex, ratingLevelsEditor::edit))
                }
            }
        }
        TextButton(onClick = onRatingLevelAdd) {
            Text(stringResource(Res.string.tasks_rating_level_add))
        }
    }
}

@Composable
private fun ratingLevelActions(
    ratingLevelsDraft: TaskRatingLevelsDraft,
    ratingLevelIndex: Int,
    onRatingLevelsDraftEdit: ((TaskRatingLevelsDraft) -> TaskRatingLevelsDraft) -> Unit,
): List<MenuAction> = buildList {
    if (ratingLevelIndex > 0) {
        add(
            MenuAction(stringResource(Res.string.tasks_action_move_up)) {
                onRatingLevelsDraftEdit { draft -> draft.moving(ratingLevelIndex, ratingLevelIndex - 1) }
            },
        )
    }
    if (ratingLevelIndex < ratingLevelsDraft.ratingLevels.lastIndex) {
        add(
            MenuAction(stringResource(Res.string.tasks_action_move_down)) {
                onRatingLevelsDraftEdit { draft -> draft.moving(ratingLevelIndex, ratingLevelIndex + 1) }
            },
        )
    }
    add(
        MenuAction(stringResource(CommonResources.string.common_action_delete)) {
            onRatingLevelsDraftEdit { draft -> draft.removing(ratingLevelIndex) }
        },
    )
}
